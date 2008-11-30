package cpaplugin.cfa.objectmodel;

import cpaplugin.exceptions.CFAGenerationRuntimeException;


public abstract class AbstractCFAEdge implements CFAEdge
{
    protected CFANode predecessor;
    protected CFANode successor;
    
    private String rawStatement;
    
    public AbstractCFAEdge (String rawStatement)
    {
        this.predecessor = null;
        this.successor = null;
        
        this.rawStatement = rawStatement;
    }
    
    public void initialize (CFANode predecessor, CFANode successor)
    {
        setPredecessor (predecessor);
        setSuccessor (successor);
    }
      
    public CFANode getPredecessor ()
    {
        return predecessor;
    }
    
    public void setPredecessor (CFANode predecessor) throws CFAGenerationRuntimeException
    {
        if (this.predecessor != null)
            this.predecessor.removeLeavingEdge (this);
        
        this.predecessor = predecessor;
        if (this.predecessor != null)
        {
            if (this.isJumpEdge ())
            {
                if (predecessor.hasJumpEdgeLeaving ())
                {
                    System.out.println ("Warning: Should not have multiple jump edges leaving at line: " + predecessor.getLineNumber ());
                    return;
                }
                
                // This edge is to be the only edge leaving the predecessor
                int numLeavingEdges = predecessor.getNumLeavingEdges ();
                for (int idx = numLeavingEdges - 1; idx >= 0; idx--)
                {
                    CFAEdge removedEdge = predecessor.getLeavingEdge (idx);
                    CFANode nullNode = new CFANode (predecessor.getLineNumber ());
                    removedEdge.setPredecessor (nullNode);
                }               
            }
            
            if (predecessor.hasJumpEdgeLeaving ())
            {
                // TODO: Do nothing? Or add null node?
                CFANode nullNode = new CFANode (predecessor.getLineNumber ());
                this.predecessor = nullNode;
            }
            
            this.predecessor.addLeavingEdge (this);
        }
    }
    
    public CFANode getSuccessor ()
    {
        return successor;
    }
    
    public void setSuccessor (CFANode successor) throws CFAGenerationRuntimeException
    {
        if (this.successor != null)
            this.successor.removeEnteringEdge (this);
        
        this.successor = successor;
        if (this.successor != null)
            this.successor.addEnteringEdge (this);
    }

    public String getRawStatement ()
    {
        return rawStatement;
    }
    
    public boolean isJumpEdge ()
    {
        return false;
    }
    
    public boolean equals (Object other)
    {
        if (!(other instanceof AbstractCFAEdge))
            return false;
        
        AbstractCFAEdge otherEdge = (AbstractCFAEdge) other;
        
        if ((otherEdge.predecessor != this.predecessor) ||
            (otherEdge.successor != this.successor))
            return false;
        
        return true;
    }
    
    public String toString() {
        return "(" + getPredecessor() + " -{" + 
                getRawStatement().replaceAll("\n", " ") + 
               "}-> " + getSuccessor() + ")";
    }
}
