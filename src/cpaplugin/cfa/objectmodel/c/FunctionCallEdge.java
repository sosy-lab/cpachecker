package cpaplugin.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFANode;

public class FunctionCallEdge extends AbstractCFAEdge 
{
	private IASTExpression[] functionArguments;
	private boolean isExternalCall = false;
	
    public FunctionCallEdge (String rawStatement, IASTExpression arguments)
    {
        super (rawStatement);
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.FunctionCallEdge;
    }
    
    public void setArguments(IASTExpression[] args){
    	this.functionArguments = args;
    }
    
    public IASTExpression[] getArguments(){
    	return this.functionArguments;
    }
	
	public void setExternalCall() {
		this.isExternalCall = true;
	}
	
	public boolean isExternalCall() {
		return this.isExternalCall;
	}
}
