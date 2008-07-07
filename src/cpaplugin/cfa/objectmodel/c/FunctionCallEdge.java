package cpaplugin.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFANode;

public class FunctionCallEdge extends AbstractCFAEdge 
{
	private IASTExpression[] functionArguments;
	private boolean isRecursiveCallEdge = false;
	
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

	public void setRecursive() {
		this.isRecursiveCallEdge = true;
	}
	
	public boolean isRecursive() {
		return this.isRecursiveCallEdge;
	}
}
