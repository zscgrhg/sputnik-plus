package com.zte.sputnik.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    public static String replaceAll(String target, String pattern, Replace replace) {
        return replaceAll(target, Pattern.compile(pattern), replace);
    }

    public static String replaceAll(String target, Pattern pattern, Replace replace) {
        Matcher matcher = pattern.matcher(target);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, replace.replace(matcher));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String formatSeparatedValue(String value,String separator){
        if(value==null){
            return null;
        }
        String escapeSeparator="\\Q"+separator.replace("\\E","\\\\E\\Q")+"\\E";
        String replacement=separator.replace("\\","\\\\")
                .replace("$","\\$");
        return value.replaceAll("^(?:"+escapeSeparator+"|\\s)+","")
                .replaceAll("(?:"+escapeSeparator+"|\\s)+$","")
                .replaceAll("\\s*"+escapeSeparator+"(?:"+escapeSeparator+"|\\s)+",replacement);
    }

    public static interface Replace {
        String replace(Matcher matcher);
    }
}
