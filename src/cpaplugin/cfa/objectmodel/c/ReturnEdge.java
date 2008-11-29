package cpaplugin.cfa.objectmodel.c;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFANode;


public class ReturnEdge extends AbstractCFAEdge 
{

	private boolean isExitingRecursiveCall = false;
	
	public ReturnEdge (String rawStatement)
    {
        super (rawStatement);
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.ReturnEdge;
    }

	public void setExitFromRecursive() {
		isExitingRecursiveCall = true;
	}
	
	public boolean isExitingRecursiveCall() {
		return isExitingRecursiveCall;
	}

}
