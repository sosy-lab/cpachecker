package cfa.objectmodel;

import cfa.objectmodel.CFANode;

public class CFAExitNode extends CFANode{

    private String functionName;
    
    public CFAExitNode (int lineNumber, String functionName)
    {
        super (lineNumber);
        this.functionName = functionName;
    }
    
    public String getFunctionName ()
    {
        return functionName;
    }
    
    public void setFunctionName (String s)
    {
    	functionName = s;
    }

}
