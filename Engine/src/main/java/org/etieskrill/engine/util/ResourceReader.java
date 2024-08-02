package org.etieskrill.engine.util;

import org.etieskrill.engine.common.ResourceLoadException;
import org.etieskrill.engine.input.Input;
import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ResourceReader {

    public static InputStream getClasspathResourceAsStream(String name) {
        InputStream stream = ClassLoader.getSystemResourceAsStream(name);
        if (stream == null) {
            throw new ResourceLoadException("Could not load %s from classpath".formatted(name));
        }
        return stream;
    }

    public static String getClasspathResource(String name) {
        try (InputStream inputStream = ResourceReader.class.getClassLoader().getResourceAsStream(name)) {
            if (inputStream == null)
                throw new ResourceLoadException("Resource %s could not be located or access was denied".formatted(name));
            try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                for (int result = bis.read(); result != -1; result = bis.read()) {
                    buf.write((byte) result);
                }
                return buf.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ResourceLoadException(e);
        }
    }

    public static ByteBuffer getRawClassPathResource(String name) {
        try (InputStream inputStream = ResourceReader.class.getClassLoader().getResourceAsStream(name)) {
            if (inputStream == null)
                throw new ResourceLoadException("Resource %s could not be located or access was denied".formatted(name));
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            return buffer.put(bytes).rewind();
        } catch (IOException e) {
            throw new ResourceLoadException(e);
        }
    }

    public static boolean requestRaw(String name, StringBuffer resource) {
        resource.delete(0, resource.length());
        try {
            resource.append(getClasspathResource(name));
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    public static <T> List<T> getCSV(String name, StringParser<T> parser, String delimiter) throws IOException {
        if (delimiter.length() != 1)
            throw new IllegalArgumentException("Delimiter must be exactly one character");
        
        BufferedReader reader;
        
        try {
            reader = new BufferedReader(new FileReader(name));
        } catch (IOException e) {
            throw new IOException("Error while reading file:\n" + e.getMessage());
        }
        
        List<T> list = new ArrayList<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            list.add(parser.parse(line.split(delimiter)));
        }
        
        return list;
    }

}
