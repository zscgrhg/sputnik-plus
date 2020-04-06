package com.zte.sputnik.builder;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
    String actionDecl = "1==1";
    String assertDecl = "1==1";
    Set<HashMap.Entry<String, List<GroovyLine>>> Inputs;
    Set<HashMap.Entry<String, List<GroovyLine>>> Outputs;
    Set<HashMap.Entry<String, List<GroovyLine>>> Returned;
}
