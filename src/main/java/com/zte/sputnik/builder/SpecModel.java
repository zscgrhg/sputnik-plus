package com.zte.sputnik.builder;

import com.zte.sputnik.instrument.MethodNames;
import lombok.Data;

import java.util.*;

@Data
public class SpecModel {
    boolean imports = false;
    String pkg;
    Long id;
    String className;
    String fileName;
    String subject;
    String method;
    String subjectDecl;
    boolean mockArgs = false;
    List<String> mockBlock;
    Set<HashMap.Entry<Long, Set<String>>> staticInvokes;
    Set<HashMap.Entry<Long, MethodNames>> staticNames;
    String actionDecl = "1==1";
    String assertDecl = "1==1";
    Set<HashMap.Entry<String, List<GroovyLine>>> Inputs;
    Set<HashMap.Entry<String, List<GroovyLine>>> Outputs;
    Set<HashMap.Entry<String, List<GroovyLine>>> Returned;

    public   boolean hasStaticInvoke(){
        return !Optional.ofNullable(staticInvokes).map(Set::isEmpty).orElse(true);
    }
}
