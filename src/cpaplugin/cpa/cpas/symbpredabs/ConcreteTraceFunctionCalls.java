package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Vector;

public class ConcreteTraceFunctionCalls implements ConcreteTrace {
    private Vector<String> functionNames;
    
    public ConcreteTraceFunctionCalls() {
        functionNames = new Vector<String>();
    }
    
    public void add(String fn) {
        if (functionNames.isEmpty() || 
                !functionNames.lastElement().equals(fn)) {
            functionNames.add(fn);
        }
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\nSequence of function calls:\n" +
                   "---------------------------");
        for (String fn : functionNames) {
            buf.append("\n\t");
            buf.append(fn);
        }
        buf.append("\n");
        return buf.toString();
    }
}
