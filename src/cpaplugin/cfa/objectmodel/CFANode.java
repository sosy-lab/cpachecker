package cpaplugin.cfa.objectmodel;

import java.util.ArrayList;
import java.util.List;


public class CFANode
{
    private int lineNumber;
    protected List<CFAEdge> leavingEdges;
    protected List<CFAEdge> enteringEdges;
    private int nodeNumber;
    
    private static int nextNodeNumber = 0;
       
    public CFANode (int lineNumber)
    {
        this.lineNumber = lineNumber;
        this.nodeNumber = nextNodeNumber++;
        leavingEdges = new ArrayList<CFAEdge>();
        enteringEdges = new ArrayList<CFAEdge> ();
    }
      
    public int getLineNumber ()
    {
        return lineNumber;
    }
    
    public int getNodeNumber ()
    {
        return nodeNumber;
    }
        
    public void addLeavingEdge (CFAEdge newLeavingEdge)
    {
        leavingEdges.add (newLeavingEdge);
    }
    
    public boolean removeLeavingEdge (CFAEdge edge)
    {
        return leavingEdges.remove (edge);
    }
    
    public int getNumLeavingEdges ()
    {
        return leavingEdges.size ();
    }
    
    public CFAEdge getLeavingEdge (int index)
    {
        return leavingEdges.get (index);
    }
    
    public void addEnteringEdge (CFAEdge enteringEdge)
    {
        enteringEdges.add (enteringEdge);
    }
    
    public boolean removeEnteringEdge (CFAEdge edge)
    {
        return enteringEdges.remove (edge);
    }
    
    public int getNumEnteringEdges ()
    {
        return enteringEdges.size ();
    }
    
    public CFAEdge getEnteringEdge (int index)
    {
        return enteringEdges.get (index);
    }
    
    public boolean hasEdgeTo (CFANode other)
    {
        boolean hasEdge = false;
        for (CFAEdge edge : leavingEdges)
        {
            if (edge.getSuccessor () == other)
            {
                hasEdge = true;
                break;
            }
        }
        
        return hasEdge;
    }
    
    public boolean hasJumpEdgeLeaving ()
    {
        for (CFAEdge edge : leavingEdges)
        {
            if (edge.isJumpEdge ())
                return true;
        }
        return false;
    }
}
