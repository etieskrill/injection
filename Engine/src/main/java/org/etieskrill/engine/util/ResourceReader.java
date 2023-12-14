package org.etieskrill.engine.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ResourceReader {

    public static String getRaw(String name) {
        try {
            return Files.readString(Path.of(name));
        } catch (IOException e) {
            throw new RuntimeException("Plaintext resource %s could not be located".formatted(name), e);
        }
    }

    public static boolean requestRaw(String name, StringBuffer resource) {
        resource.delete(0, resource.length());
        try {
            resource.append(getRaw(name));
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
