package com.zte.sputnik.builder;

import lombok.Data;

@Data
public class SpecID {
    String subject;
    String signature;
    String fromClass;
    String fromMethod;
    String displayName;
    public String toHashcode(){
        int hashCode = this.hashCode();
        if(hashCode>=0){
            return "0x"+Integer.toUnsignedString(hashCode,16);
        }else {
            return "1x"+Integer.toUnsignedString(hashCode,16);
        }
    }
}
