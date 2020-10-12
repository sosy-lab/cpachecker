package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.Property;

public class PropertyViolationException extends RuntimeException {

    private Collection<Property> properties;

    public PropertyViolationException(Collection<Property> pProperties) {
        this.properties = pProperties;
    }

    public Collection<Property> getViolatedProperties(){
        return properties;
    }

}
