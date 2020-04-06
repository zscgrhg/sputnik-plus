package com.zte.sputnik.util;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.zte.sputnik.instrument.BMRuleMustacheModel;


import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

public class MustacheUtil {
    public static final MustacheFactory TEXT_FACTORY = new DefaultTextMustacheFactory();

    public static String format(String tmpl, Object... scopes) {
        HashMap map = new HashMap();
        for (int i = 0; i < scopes.length; i++) {
            map.put(Integer.toString(i), scopes[i]);
        }
        StringWriter sw = new StringWriter();
        Mustache mustache = TEXT_FACTORY.compile(new StringReader(tmpl), null);
        mustache.execute(sw, map);
        sw.flush();
        return sw.toString();
    }

    public static String render(String file, Object model) {
        StringWriter sw = new StringWriter();
        Mustache mustache = TEXT_FACTORY.compile(file);
        mustache.execute(sw, model);
        sw.flush();
        return sw.toString();
    }

    public static String render(BMRuleMustacheModel model) {
        StringWriter sw = new StringWriter();
        Mustache mustache = TEXT_FACTORY.compile("btm/btm.mustache");
        mustache.execute(sw, model);
        sw.flush();
        return sw.toString();
    }


    public static void main(String[] args) {
        String aha = render("{{0}}", "aha");
        System.out.println(aha);
    }
}
