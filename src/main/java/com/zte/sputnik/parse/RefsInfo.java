package com.zte.sputnik.parse;

import lombok.Data;

@Data
public class RefsInfo {
    public RefType type;
    public String name;
    public int index;
    public Long returnedFrom;
    public Class declaredType;


    public enum RefType {
        FIELD,
        ARG,
        RETURNED
    }
}
