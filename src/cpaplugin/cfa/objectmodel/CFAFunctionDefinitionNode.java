package cpaplugin.cfa.objectmodel;


public abstract class CFAFunctionDefinitionNode extends CFANode
{
    private String functionName;
    
    public CFAFunctionDefinitionNode (int lineNumber, String functionName)
    {
        super (lineNumber);
        this.functionName = functionName;
    }
    
    public String getFunctionName ()
    {
        return functionName;
    }
}
