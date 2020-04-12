package com.zte.sputnik.parse;

import lombok.Data;

@Data
public class ValueObjectModel {
    Object value;
    Class valueClass;
    String declType;
}
