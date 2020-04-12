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

/**
 * The type Class util.
 */
public class ClassUtil {
    /**
     * 获取参数化类型实例的类型信息
     *
     * @param type           参数化类型的实例
     * @param inheritorClass 参数化类型
     * @return the type with generic type information resolved
     */
    public static Type resolve(Type type, Class inheritorClass) {
        return TypeResolver.reify(type, inheritorClass);
    }

    /**
     * 检查类是否存在指定的注解
     *
     * @param target 被检查的类
     * @param anno   查找的注解类型
     * @return true  如果存在返回true，不存在返回false
     */
    public static boolean hasAnnotation(Class target, Class anno) {
        if (target == null || Object.class.equals(target)) {
            return false;
        }
        Annotation[] annotations = target.getAnnotationsByType(anno);
        return (annotations != null && annotations.length > 0) ||
                hasAnnotation(target.getSuperclass(), anno) ||
                Stream.of(target.getInterfaces()).anyMatch(sf -> hasAnnotation(sf, anno));

    }

    /**
     * 检查字段是否存在指定的注解
     *
     * @param target 被检查的字段
     * @param anno   查找的注解类型
     * @return true  如果存在返回true，不存在返回false
     */
    public static boolean hasAnnotation(Field target, Class anno) {
        if (target == null) {
            return false;
        }

        Annotation[] annotations = target.getAnnotationsByType(anno);
        return (annotations != null && annotations.length > 0);

    }

    /**
     * 检查参数是否存在指定的注解
     *
     * @param target 被检查的参数
     * @param anno   查找的注解类型
     * @return true  如果存在返回true，不存在返回false
     */
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


    /**
     * 获取类的所有字段，包括继承的所有字段，继承自{@link java.lang.Object}的除外，也不包括超类被覆盖的字段
     *
     * @param clazz         目标类
     * @param includeStatic 为true则返回结果包含静态字段，否则不包含静态字段
     * @return  Collection<Field>
     */
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

    /**
     * Get field value
     *
     * @param f     the f
     * @param owner the owner
     * @return the value
     */
    @SneakyThrows
    public static Object getFieldValue(Field f,Object owner){
        f.setAccessible(true);
        return f.get(owner);
    }
}
