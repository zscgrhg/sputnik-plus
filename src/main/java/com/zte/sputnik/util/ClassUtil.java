package com.zte.sputnik.util;

import lombok.SneakyThrows;
import net.jodah.typetools.TypeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ClassUtil {
    public static Type resolve(Type type, Class inheritorClass) {
        return TypeResolver.reify(type, inheritorClass);
    }

    public static boolean hasAnnotation(Class target, Class anno) {
        if (target == null || Object.class.equals(target)) {
            return false;
        }
        Annotation[] annotations = target.getAnnotationsByType(anno);
        return (annotations != null && annotations.length > 0) ||
                hasAnnotation(target.getSuperclass(), anno) ||
                Stream.of(target.getInterfaces()).anyMatch(sf -> hasAnnotation(sf, anno));

    }

    public static boolean hasAnnotation(Field target, Class anno) {
        if (target == null) {
            return false;
        }

        Annotation[] annotations = target.getAnnotationsByType(anno);
        return (annotations != null && annotations.length > 0);

    }

    public static boolean hasAnnotation(Parameter target, Class anno) {
        if (target == null) {
            return false;
        }

        Annotation[] annotations = target.getAnnotationsByType(anno);
        return (annotations != null && annotations.length > 0);

    }

    public static Collection<Field> fieldsOf(Class clazz){
        return fieldsOf(clazz,false);
    }

    public static Collection<Field> fieldsOf(Class clazz,boolean includeStatic){
        Map<String,Field> fields=new HashMap<>();
        while (clazz!=null&&!Object.class.equals(clazz)){
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                String name = declaredField.getName();
                if(includeStatic|| !Modifier.isStatic(declaredField.getModifiers())){
                    fields.putIfAbsent(name,declaredField);
                }
            }
            clazz=clazz.getSuperclass();
        }
        return fields.values();
    }
    @SneakyThrows
    public static Object getFieldValue(Field f,Object owner){
        f.setAccessible(true);
        return f.get(owner);
    }
}
