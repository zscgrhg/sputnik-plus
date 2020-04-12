package com.zte.sputnik.instrument;

import lombok.EqualsAndHashCode;
import org.jboss.byteman.agent.submit.ScriptText;

@EqualsAndHashCode
public class ScriptTextValue extends ScriptText {
    public ScriptTextValue(String fileName, String text) {
        super(fileName, text);
    }

    public ScriptTextValue(String text) {
        super(text);
    }
}
