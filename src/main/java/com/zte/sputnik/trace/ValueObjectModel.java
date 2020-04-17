package com.zte.sputnik.trace;

import lombok.Data;

@Data
public class ValueObjectModel {
    Object value;
    Class valueClass;
    String declType;
}
