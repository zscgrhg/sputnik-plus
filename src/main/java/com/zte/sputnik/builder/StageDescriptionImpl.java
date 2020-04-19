package com.zte.sputnik.builder;

import org.junit.runner.Description;

public class StageDescriptionImpl implements StageDescription {
    Description description;

    public StageDescriptionImpl(Description description) {
        this.description = description;
    }

    @Override
    public String getTestClass() {
        return this.description.getClassName();
    }

    @Override
    public String getTestMethod() {
        return this.description.getMethodName();
    }

    @Override
    public String getDisplayName() {
        return this.description.getDisplayName();
    }
}
