package com.zte.sputnik.builder;

import lombok.Data;

@Data
public class GroovyLine {
    public static final String COMMA = ",";
    public static final String NEW_LINE = "\n";
    public static final String COLON = ":";
    public static final String COMMA_NEW_LINE = ",";
    public static final String SEMICOLON = ";";
    String ident;
    String tokens;
    String lineEnd;

    public GroovyLine(String ident, String tokens, String lineEnd) {
        this.ident = ident;
        this.tokens = tokens;
        this.lineEnd = lineEnd;
    }

    public GroovyLine(String ident, String tokens) {
        this.ident = ident;
        this.tokens = tokens;
        this.lineEnd = COMMA_NEW_LINE;
    }
}
