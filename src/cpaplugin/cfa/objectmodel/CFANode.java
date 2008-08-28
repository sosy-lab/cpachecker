package cpaplugin.cfa.objectmodel;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;


public class CFANode
{
	private int lineNumber;
    protected List<CFAEdge> leavingEdges;
    protected List<CFAEdge> enteringEdges;
    private int nodeNumber;
    // is start node of a loop?
    private boolean isLoopStart = false;
    // in which function is that node?
    private String functionName;
    // list of summary edges
    protected CallToReturnEdge leavingSummaryEdge;
    protected CallToReturnEdge enteringSummaryEdge;
    
    private static int nextNodeNumber = 0;
       
    public CFANode (int lineNumber)
    {
        this.lineNumber = lineNumber;
        this.nodeNumber = nextNodeNumber++;
        leavingEdges = new ArrayList<CFAEdge>();
        enteringEdges = new ArrayList<CFAEdge> ();
        leavingSummaryEdge = null;
        enteringSummaryEdge = null;
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
    
    public void setLoopStart(){
    	isLoopStart = true;
    }
    
    public boolean isLoopStart(){
    	return isLoopStart;
    }
    
    public void setFunctionName(String fName){
    	functionName = fName;
    }
    
    public String getFunctionName(){
    	return functionName;
    }
    
    public void addEnteringSummaryEdge(CallToReturnEdge edge){
    	enteringSummaryEdge = edge;
    }
    
    public void addLeavingSummaryEdge(CallToReturnEdge edge){
    	leavingSummaryEdge = edge;
    }
    
    public CallToReturnEdge getEnteringSummaryEdge(){
    	return enteringSummaryEdge;
    }
    
    public CallToReturnEdge getLeavingSummaryEdge(){
    	return leavingSummaryEdge;
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof CFANode)) return false;
        return getNodeNumber() == ((CFANode)other).getNodeNumber();
    }
    
    public static int getFinalNumberOfNodes(){
    	return nextNodeNumber;
    }
    
    public int hashCode() {
        return getNodeNumber();
    }
    
    public String toString() {
        return "N" + getNodeNumber();
    }
}
