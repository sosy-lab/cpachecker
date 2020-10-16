package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.Property;

public class PropertyViolationException extends RuntimeException {

    private static final long serialVersionUID = -5448539471003034117L;
    
    private Collection<Property> properties;

    public PropertyViolationException(Collection<Property> pProperties) {
        this.properties = pProperties;
    }

    public Collection<Property> getViolatedProperties(){
        return properties;
    }

}
