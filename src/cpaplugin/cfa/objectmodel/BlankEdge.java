package cpaplugin.cfa.objectmodel;


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
    
    public boolean isJumpEdge ()
    {
        return jumpEdge;
    }
    
    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.BlankEdge;
    }

}
