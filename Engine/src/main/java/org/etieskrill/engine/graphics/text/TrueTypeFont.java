package org.etieskrill.engine.graphics.text;

import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.ArrayTexture;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.util.ResourceReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.freetype.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.etieskrill.engine.graphics.text.Fonts.DEFAULT_FONT;
import static org.lwjgl.BufferUtils.zeroBuffer;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_MAX_3D_TEXTURE_SIZE;
import static org.lwjgl.util.freetype.FreeType.*;

public class TrueTypeFont implements Font {

    private static final Logger logger = LoggerFactory.getLogger(TrueTypeFont.class);

    private static long library;

    private final FT_Face face;
    private final boolean scalable;
    private int[] sizes;

    private final String family;
    private final String style;

    private final Vector2i pixelSize;

    public TrueTypeFont(String file) throws IOException {
        if (library == 0) initLibrary();

        PointerBuffer faceBuffer = BufferUtils.createPointerBuffer(1);
        ByteBuffer fontFile = ResourceReader.getRawResource(file);
        check(FT_New_Memory_Face(library, fontFile, 0, faceBuffer),
                "Font could not be loaded from file \"" + file + "\"");

        face = FT_Face.create(faceBuffer.get());

        scalable = (face.face_flags() & FT_FACE_FLAG_SCALABLE) != 0;

        if (face.num_fixed_sizes() > 0) {
            FT_Bitmap_Size.Buffer buffer = face.available_sizes();
            if (buffer != null) {
                sizes = new int[face.num_fixed_sizes()];
                int i = 0;
                while (buffer.hasRemaining())
                    sizes[i++] = buffer.get().height();
            }
        }

        this.family = face.family_nameString();
        this.style = face.style_nameString();

        this.pixelSize = new Vector2i(INVALID_PIXEL_SIZE);
    }

    private static void initLibrary() throws IOException {
        PointerBuffer libBuffer = BufferUtils.createPointerBuffer(1);
        check(FT_Init_FreeType(libBuffer), "Unable to initialise FreeType library");
        library = libBuffer.get();
    }

    /**
     * @param characters a subset of characters to load (optional)
     * @return a rendered bitmap font
     */
    //TODO bitmap texture packer
    //FIXME there is probably some use-after-free or similarly nasty issue here, which only surfaces sporadically if the
    // number of loaded characters is relatively large (>~1000, dependent on platform) (pathetically small really)
    // try using e.g. STBTruetype if this cannot be resolved
    public BitmapFont generateBitmapFont(
            int pixelWidth,
            int pixelHeight,
            boolean verticalUp,
            @Nullable Set<Character> characters
    ) throws IOException {
        if (pixelHeight <= 0) throw new IllegalArgumentException("Font height must be greater than zero");
        if (pixelWidth < 0) throw new IllegalArgumentException("Font width must not be smaller than zero");
        if (disposed)
            throw new IllegalStateException("Cannot generate bitmap as freetype resource was already disposed");

        check(FT_Select_Charmap(face, FT_ENCODING_UNICODE), "Failed to select unicode character map");
        var gid = BufferUtils.createIntBuffer(1);
        long charcode = FT_Get_First_Char(face, gid);
        var availableChars = new ArrayList<Long>();
        availableChars.add(0L);
        while (gid.get(0) != 0) {
            if (characters == null || characters.contains((char) charcode) || charcode == 0L) {
                availableChars.add(charcode);
            }
            charcode = FT_Get_Next_Char(face, charcode, gid);

            if (charcode == 0x1F723 || charcode == 0x1F724) {
                FT_Load_Char(face, 0x1F724, FT_LOAD_RENDER);
                var metrics = face.glyph().metrics();
                System.out.println("Font metrics for " + (char) charcode + ", " + gid.get(0) + ": " + metrics.width() / 64 + "x" + metrics.height() / 64 + " of " + pixelWidth + "x" + pixelHeight);
            }
        }

        //TODO probably split into multiple textures
        var maxChars = glGetInteger(GL_MAX_3D_TEXTURE_SIZE);
        if (availableChars.size() > maxChars) {
            throw new IllegalArgumentException("Can only load " + maxChars + " characters due to platform limitations; load a subset of characters instead");
        }

        //Is FT_Set_Char_Size ever going to be more practical than this?
        check(FT_Set_Pixel_Sizes(face, pixelWidth, pixelHeight), "Failed to set font pixel size");

        int glyphIndex = 0;

        //TODO validate size better & determine glyph size and apply
        if (pixelWidth == 0)
            pixelWidth = pixelHeight; //Currently, if any glyph is wider than it is tall, this will cause everything to break TODO maybe not?
        pixelSize.set(pixelWidth, pixelHeight);

        ArrayTexture.BufferBuilder textures = (ArrayTexture.BufferBuilder) new ArrayTexture.BufferBuilder(
                new Vector2i(pixelWidth, pixelHeight),
                availableChars.size(),
                AbstractTexture.Format.ALPHA
        )
                .setType(AbstractTexture.Type.DIFFUSE)
                .setMipMapping(AbstractTexture.MinFilter.NEAREST, AbstractTexture.MagFilter.LINEAR) //TODO min nearest is better for small text, but when rendering using pixel sizes this should never be used anyway
                .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_BORDER);

        ByteBuffer buffer = BufferUtils.createByteBuffer(pixelWidth * pixelHeight);

        Map<Character, Glyph> glyphs = new HashMap<>();
        for (long c : availableChars) {
            var charLoadRet = FT_Load_Char(face, c, FT_LOAD_RENDER);
            if (charLoadRet == FT_Err_Invalid_Outline) continue;
            check(charLoadRet, "Could not load character \"%c\"".formatted((char) c));

            FT_GlyphSlot ftGlyph = face.glyph();
            if (ftGlyph == null) {
                logger.warn("Failed to load character \"{}\" from face", (char) c);
                continue;
            }

            @SuppressWarnings("resource") FT_Bitmap bitmap = ftGlyph.bitmap();
            @SuppressWarnings("resource") FT_Vector adv = ftGlyph.advance();

            Vector2f size = new Vector2f(bitmap.width(), bitmap.rows());
            Vector2f position = new Vector2f(ftGlyph.bitmap_left(), ftGlyph.bitmap_top());
            if (!verticalUp) position.mul(1, -1).add(0, getMinLineHeight());
            Vector2f advance = new Vector2f(adv.x() / 64f, adv.y() / 64f);
            if (!verticalUp) advance.mul(1, -1);

            var metrics = ftGlyph.metrics();
            ByteBuffer _buffer = bitmap.buffer((int) (size.x() * size.y()));

            if (metrics.width() == 0 || metrics.height() == 0 || _buffer == null) {
                logger.trace("Character with code '{}' (code: {}) has no bitmap, using empty texture", (char) c, c);

                zeroBuffer(buffer.position(0).limit(buffer.capacity()));
                textures.addTexture(buffer);
                Glyph glyph = new Glyph(size, position, advance, glyphIndex++, (char) c);

                glyphs.put((char) c, glyph);

                continue;
            }

            //Pad top and right of buffer to specified pixel size
            buffer.position(0).limit(buffer.capacity());
            //FIXME how to handle characters that go out of texture bounds?
            var clampedWidth = Math.min(pixelSize.x, size.x());
            var clampedHeight = Math.min(pixelSize.y, size.y());
            for (int j = 0; j < clampedHeight; j++) {
//                for (int k = 0; k < size.x(); k++) buffer.put(_buffer.get());
//                for (int k = 0; k < pixelWidth - size.x(); k++) buffer.put((byte) 0);
                for (int k = 0; k < clampedWidth; k++) buffer.put(_buffer.get());
                for (int k = 0; k < pixelWidth - size.x(); k++) buffer.put((byte) 0);
            }
            for (int j = 0; j < pixelHeight - size.y(); j++) {
                for (int k = 0; k < clampedWidth; k++) buffer.put((byte) 0);
            }
            buffer.rewind();

            textures.addTexture(buffer);
            Glyph glyph = new Glyph(size, position, advance, glyphIndex++, (char) c);

            glyphs.put((char) c, glyph);
        }

        //TODO figure out whether the face height is actually scaled via FT_Set_Pixel_Sizes or the like
        // alr it seems like it does not, but how to get the height then?
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        ArrayTexture glyphTextures = textures.build();
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        return new BitmapFont(
                glyphs,
                getLineHeight(),
                getMinLineHeight(),
                getPixelSize(),
                face.family_nameString(),
                face.style_nameString(),
                glyphTextures
        );
    }

    public BitmapFont generateBitmapFont(int pixelWidth, int pixelHeight) throws IOException {
        return generateBitmapFont(pixelWidth, pixelHeight, false, null);
    }

    public BitmapFont generateBitmapFont(int pixelSize, boolean verticalUp) throws IOException {
        return generateBitmapFont(0, pixelSize, verticalUp, null);
    }

    public BitmapFont generateBitmapFont(int pixelSize, Set<Character> characters) throws IOException {
        return generateBitmapFont(0, pixelSize, false, characters);
    }

    public BitmapFont generateBitmapFont(int pixelSize) throws IOException {
        return generateBitmapFont(0, pixelSize, false, null);
    }

    //TODO implement
    @Override
    public @NotNull Glyph getGlyph(char c) {
        if (disposed) return null;
        if (FT_Load_Char(face, c, FT_LOAD_RENDER) != FT_Err_Ok) return null;
        return null;
    }

    @Override
    public Glyph[] getGlyphs(String s) {
        if (disposed) return null;
        return null;
    }

    @Override
    public Vector2ic getPixelSize() {
        return pixelSize;
    }

    @Override
    public int getLineHeight() {
        return (int) face.size().metrics().height() / 64;
    }

    @Override
    public int getMinLineHeight() {
        FT_Size_Metrics metrics = face.size().metrics();
        return (int) ((metrics.ascender() - metrics.descender()) / 64f);
    }

    public boolean isScalable() {
        return scalable;
    }

    public int[] getAvailableSizes() {
        return sizes;
    }

    public String getFamily() {
        return family;
    }

    public String getStyle() {
        return style;
    }

    //TODO not very innovative, doing this in multiple classes. either devise a simpler solution or just declare usage at own risk
    private boolean disposed = false;

    @Override
    public void dispose() {
        if (disposed) return;
        try {
            check(FT_Done_Face(face), "Failed to release font resource");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        disposed = true;
    }

    private static boolean libraryDisposed = false;

    public static void disposeLibrary() {
        if (library == 0L || libraryDisposed) return;
        try {
            check(FT_Done_Library(library), "Failed to release freetype library");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        libraryDisposed = true;
    }

    private static void check(int retCode, String failureMessage) throws IOException {
        if (retCode != FT_Err_Ok) {
            //TODO custom exception or revert to local exceptions
            throw new IOException("%s: 0x%s %s".formatted(
                    failureMessage, Integer.toHexString(retCode), FT_Error_String(retCode)));
        }
    }

}
