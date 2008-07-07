package cpaplugin.cfa.objectmodel;


public abstract class CFAFunctionDefinitionNode extends CFANode
{
    private String functionName;
    // Check if call edges are added in the second pass
    private CFAExitNode exitNode;
    
    public CFAFunctionDefinitionNode (int lineNumber, String functionName)
    {
        super (lineNumber);
        this.functionName = functionName;
    }
    
    public String getFunctionName ()
    {
        return this.functionName;
    }
    
    public void setFunctionName (String s)
    {
    	this.functionName = s;
    }
    
    public CFANode getExitNode ()
    {
        return this.exitNode;
    }
    
    public void setExitNode (CFAExitNode en)
    {
    	this.exitNode = en;
    }
}
