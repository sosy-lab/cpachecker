package cpaplugin.cfa.objectmodel;

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
