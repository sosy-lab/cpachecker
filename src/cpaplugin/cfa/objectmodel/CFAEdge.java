package cpaplugin.cfa.objectmodel;



public interface CFAEdge 
{
    public CFAEdgeType getEdgeType ();
       
    public CFANode getPredecessor ();
    public CFANode getSuccessor ();
    
    // Needed by the CFA simplifier.  It is discouraged that these are used
    public void setPredecessor (CFANode predecessor);
    public void setSuccessor (CFANode successor);
    
    public String getRawStatement ();
    
    public boolean isJumpEdge ();
    
    public void initialize (CFANode predecessor, CFANode successor);    
}
