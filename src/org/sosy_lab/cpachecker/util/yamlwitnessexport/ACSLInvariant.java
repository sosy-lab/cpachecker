package org.sosy_lab.cpachecker.util.yamlwitnessexport;

public class ACSLInvariant {
    private final String expression;
    private final String format;
    private final String type;
    private final String scope;
    
    public ACSLInvariant(String expression, String format, String type, String scope) {
        this.expression = expression;
        this.format = format;
        this.type = type;
        this.scope = scope;
    }
    
    // Getters
    public String getExpression() { return expression; }
    public String getFormat() { return format; }
    public String getType() { return type; }
    public String getScope() { return scope; }
}
