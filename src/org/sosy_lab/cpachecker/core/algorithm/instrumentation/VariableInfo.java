package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

public class VariableInfo {
    private boolean variable;
    private String name;

    public VariableInfo(boolean isVariable, String name){
        this.variable = isVariable;
        this.name = name;
    }

    public boolean isVariable(){
        return variable;
    }

    public String getName(){
        return name;
    }
    
}
