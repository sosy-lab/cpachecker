package cfa.objectmodel.c;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFANode;


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
