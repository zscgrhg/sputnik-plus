package com.zte.sputnik.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;

public class DefaultTextMustacheFactory extends DefaultMustacheFactory {
    @Override
    public void encode(String value, Writer writer) {
        try {
            int length = value.length();
            for (int i = 0; i < length; i++) {
                char c = value.charAt(i);
                writer.write(c);
            }
        } catch (IOException e) {
            throw new MustacheException("Failed to encode value: " + value, e);
        }
    }
}
