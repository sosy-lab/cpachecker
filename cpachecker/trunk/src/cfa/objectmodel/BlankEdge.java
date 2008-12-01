package cfa.objectmodel;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;


public class BlankEdge extends AbstractCFAEdge
{
    private boolean jumpEdge;

    public BlankEdge (String rawStatement)
    {
        super (rawStatement);
        jumpEdge = false;
    }

    public void setIsJumpEdge (boolean jumpEdge)
    {
        this.jumpEdge = jumpEdge;
    }

    @Override
    public boolean isJumpEdge ()
    {
        return jumpEdge;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.BlankEdge;
    }

}
