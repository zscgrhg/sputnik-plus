package com.zte.sputnik.extension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.parse.RefsInfo;
import com.zte.sputnik.util.JsonUtil;
import groovy.lang.Closure;
import lombok.SneakyThrows;

import net.sf.cglib.beans.BeanCopier;
import org.codehaus.groovy.runtime.NullObject;
import shade.sputnik.org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ObjectGroovyMethods {
    private static final Logger LOGGER = LoggerBuilder.of(ObjectGroovyMethods.class);

    public static boolean propertyMatches(Object self, Object target) {
        NullObject nullObject=NullObject.getNullObject();
        if (nullObject.is(self)&&nullObject.is(target)) {
            LOGGER.debug("arg matches1:{},{}<>{}", true, self, target);
            return true;
        }
        if (nullObject.is(self)||nullObject.is(target)) {
            LOGGER.debug("arg matches2:{},{}<>{}", false, self, target);
            return false;
        }
        if(self instanceof Comparable&&target instanceof Comparable){
            boolean comp = ((Comparable) self).compareTo(target)==0;
            LOGGER.debug("arg matches3:{},{}<>{}", comp, self, target);
            return comp;
        }
        if (Objects.equals(self, target)) {
            LOGGER.debug("arg matches4:{},{}<>{}", true, self, target);
            return true;
        }

        if(Objects.equals(self.getClass(),target.getClass())){
            LOGGER.debug("arg matches5:{},{}<>{}", false, self, target);
            return false;
        }
        boolean equals=Objects.equals(reconstruction(target,self.getClass()),self);
        LOGGER.debug("arg matches6:{},{}<>{}", equals, self, target);
        return equals;
    }

    public static <T> T reconstructionFromJson(Object target, TypeReference<T> genericSignature) {
        String json = JsonUtil.write(target);
        return JsonUtil.readerFor(genericSignature, json);
    }

    public static <T> T reconstruction(Object target, TypeReference<T> genericSignature) {
        return JsonUtil.convert(genericSignature,target);
    }
    public static <T> T reconstruction(Object target, Class<T> genericSignature) {
        return JsonUtil.convert(genericSignature,target);
    }
    public static <V> V reconstruction(Object target, Closure<V> closure) {
        return closure.call(target);
    }


    public static <V> boolean copyDirty(V source, V target) {
        if (Objects.equals(source, target) || source == null || target == null
                || source instanceof NullObject || target instanceof NullObject || source instanceof RefsInfo) {
            return true;
        }
        try {
            Class<?> targetClass = target.getClass();
            Class<?> sourceClass = source.getClass();
            if (targetClass.isArray()) {
                Object[] s = (Object[]) source;
                Object[] t = (Object[]) target;
                for (int i = 0; i < Math.min(s.length, t.length); i++) {
                    if(s.length!=t.length){
                        t[i]=s[i];
                    }else {
                        copyDirty(s[i],t[i]);
                    }
                }
            } else if (Collection.class.isAssignableFrom(targetClass)) {
                Collection c = (Collection) target;
                Collection s = (Collection) source;
                if(c instanceof List && s instanceof List&&c.size()==s.size()){
                    for (int i = 0; i < c.size(); i++) {
                        copyDirty(((List) s).get(i),((List) c).get(i));
                    }
                }else {
                    c.clear();
                    c.addAll((Collection) source);
                }
            } else {
                if (!propertyMatches(source, target)) {
                    BeanCopier beanCopier = BeanCopier.create(target.getClass(), sourceClass, false);
                    beanCopier.copy(source, target, null);
                }

            }
            return true;
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
            return false;
        }
    }

    @SneakyThrows
    public static <V> V deepCopy(V target) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(target);
        oos.flush();
        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bin);
        return (V) ois.readObject();
    }

}
