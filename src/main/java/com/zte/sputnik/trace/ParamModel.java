package com.zte.sputnik.trace;

import lombok.Data;

@Data
public class ParamModel {
    public static final String INPUTS = "in";
    public static final String OUTPUTS = "out";
    Long invocationId;
    Object[] args;
    Class[] argsType;
    String[] argsGenericType;
    String name;
    Object returned;
    Class returnedType;
    String returnedGenericType;
    Throwable thrown;
    String exception;

    public static Class[] valuesTypeOf(Object[] args) {
        int length = args.length;
        Class[] ret = new Class[length];
        for (int i = 0; i < length; i++) {

            Object argi = args[i];
            ret[i] = typeOf(argi);
        }
        return ret;
    }

    public static Class typeOf(Object v) {
        if (v == null) {
            return null;
        }
        return v.getClass();
    }
}
