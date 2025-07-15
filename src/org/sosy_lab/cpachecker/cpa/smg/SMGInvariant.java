package org.sosy_lab.cpachecker.cpa.smg;

public class SMGInvariant {

    public enum InvariantType {
        POINTER_VALIDITY,
        ALLOCATION_STATUS,
        BUFFER_BOUNDS,
        TEMPORAL_SAFETY
    }

    public enum Property {
        MEMORY_SAFETY
        // Extend as necessary for other properties
    }

    private final InvariantType type;
    private final Property property;
    private final String pointer;
    private final String expression;
    private final int size;
    private final String timepoint;

    public SMGInvariant(
            InvariantType type,
            Property property,
            String pointer,
            String expression,
            int size,
            String timepoint
    ) {
        this.type = type;
        this.property = property;
        this.pointer = pointer;
        this.expression = expression;
        this.size = size;
        this.timepoint = timepoint;
    }

    public InvariantType getType() { return type; }
    public Property getProperty() { return property; }
    public String getPointer() { return pointer; }
    public String getExpression() { return expression; }
    public int getSize() { return size; }
    public String getTimepoint() { return timepoint; }

    public <T> T accept(org.sosy_lab.cpachecker.util.yamlwitnessexport.ACSLConverter converter) {
        return (T) converter.convertToACSL(this);
    }
}
