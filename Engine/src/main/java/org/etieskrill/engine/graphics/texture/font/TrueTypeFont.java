package org.etieskrill.engine.graphics.texture.font;

import org.etieskrill.engine.util.ResourceReader;
import org.joml.RoundingMode;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.freetype.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.etieskrill.engine.config.ResourcePaths.FONT_PATH;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11C.glPixelStorei;
import static org.lwjgl.util.freetype.FreeType.*;

public class TrueTypeFont implements Font {

    private static final Logger logger = LoggerFactory.getLogger(TrueTypeFont.class);
    
    private static long library;
    
    private final FT_Face face;
    private final boolean scalable;
    private int[] sizes;
    
    private String family;
    private String style;
    
    private int ret;
    
    public static final class ScalableFont extends TrueTypeFont {
        public ScalableFont(String file) throws IOException {
            super(file);
        }
    }
    
    public TrueTypeFont(String file) throws IOException {
        if (library == 0) initLibrary();

        PointerBuffer faceBuffer = BufferUtils.createPointerBuffer(1);
        ByteBuffer fontFile = ResourceReader.getRawClassPathResource(FONT_PATH + file);
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
    }
    
    private static void initLibrary() throws IOException {
        PointerBuffer libBuffer = BufferUtils.createPointerBuffer(1);
        check(FT_Init_FreeType(libBuffer), "Unable to initialise FreeType library");
        library = libBuffer.get();
    }
    
    //TODO bitmap texture packer
    public BitmapFont generateBitmapFont(int pixelWidth, int pixelHeight, boolean verticalUp) throws IOException {
        if (pixelHeight <= 0) throw new IllegalArgumentException("Font height must be greater than zero");
        if (pixelWidth < 0) throw new IllegalArgumentException("Font width must not be smaller than zero");
        
        check(FT_Set_Pixel_Sizes(face, pixelWidth, pixelHeight),
                "Failed to set font pixel size");
        
//        check(FT_Set_Char_Size(face, pixelWidth, pixelHeight, 1920, 1080),
//                "succ muh dicc");
        
        Map<Character, Glyph> glyphs = new HashMap<>();
        for (int i = 0; i < 128; i++) {
            check(FT_Load_Char(face, i, FT_LOAD_RENDER),
                    "Could not load character \"%c\"".formatted((char) i));
            
            FT_GlyphSlot ftGlyph = face.glyph();
            if (ftGlyph == null) {
                logger.warn("Failed to load character \"{}\" from face", (char) i);
                continue;
            }
            //despite the enticing suggestion by intellisense, do NOT close these resources, it causes significant pain
            FT_Bitmap bitmap = ftGlyph.bitmap();
            FT_Vector adv = ftGlyph.advance();
            
            Vector2f size = new Vector2f(bitmap.width(), bitmap.rows());
            Vector2f position = new Vector2f(ftGlyph.bitmap_left(), ftGlyph.bitmap_top());
            if (!verticalUp) position.mul(1, -1).add(0, getMinLineHeight());
            Vector2f advance = new Vector2f(adv.x(), adv.y()).mul(1 / 64f);
            if (!verticalUp) advance.mul(1, -1);
            
            ByteBuffer buffer = bitmap.buffer((int) (2 * size.x() * size.y()));
            if (buffer == null) logger.trace("Encountered glyph ({}) without buffer, proceeding with blank bitmap", (char) i);
            
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1); //TODO more permanent solution
            Texture2D texture = new Texture2D.BufferBuilder(
                    buffer,
                    size.get(RoundingMode.TRUNCATE, new Vector2i()),
                    AbstractTexture.Format.ALPHA
            )
                    .setType(AbstractTexture.Type.DIFFUSE)
                    .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
                    .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_EDGE)
                    .build();
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            
            Glyph glyph = new Glyph(size, position, advance, texture, (char) i);
            
            glyphs.put((char) i, glyph);
        }
        
        //TODO figure out whether the face height is actually scaled via FT_Set_Pixel_Sizes or the like
        // alr it seems like it does not, but how to get the height then?
        return new BitmapFont(glyphs, getLineHeight(), getMinLineHeight(), face.family_nameString(), face.style_nameString());
    }
    
    public BitmapFont generateBitmapFont(int pixelWidth, int pixelHeight) throws IOException {
        return generateBitmapFont(pixelWidth, pixelHeight, false);
    }
    
    public BitmapFont generateBitmapFont(int pixelSize, boolean verticalUp) throws IOException {
        return generateBitmapFont(0, pixelSize, verticalUp);
    }
    
    public BitmapFont generateBitmapFont(int pixelSize) throws IOException {
        return generateBitmapFont(0, pixelSize, false);
    }
    
    //TODO implement
    @Override
    public Glyph getGlyph(char c) {
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
        try {
            check(FT_Done_Face(face), "Failed to release font resource");
        } catch (IOException ignored) {}
        disposed = true;
    }
    
    public static void disposeLibrary() {
        try {
            check(FT_Done_Library(library), "Failed to release freetype library");
        } catch (IOException ignored) {}
    }
    
    private static void check(int retCode, String failureMessage) throws IOException {
        if (retCode != FT_Err_Ok) {
            //TODO custom exception or revert to local exceptions
            throw new IOException("%s: 0x%s %s".formatted(
                    failureMessage, Integer.toHexString(retCode), FT_Error_String(retCode)));
        }
    }
    
}
