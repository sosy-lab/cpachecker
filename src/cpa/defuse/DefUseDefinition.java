package cpa.defuse;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.defuse.DefUseDefinition;

public class DefUseDefinition implements AbstractElement
{
    private String variableName;
    private CFAEdge assigningEdge;
    
    public DefUseDefinition (String variableName, CFAEdge assigningEdge)
    {
    	//System.out.println("DefUseDefinition: " + variableName + " + "+ assigningEdge.getPredecessor().getNodeNumber());
        this.variableName = variableName;
        this.assigningEdge = assigningEdge;
    }
    
    public String getVariableName ()
    {
        return variableName;
    }
    
    public CFAEdge getAssigningEdge ()
    {
        return assigningEdge;
    }
    
    public int hashCode ()
    {
        return variableName.hashCode ();
    }
    
    public boolean equals (Object other) 
    {
        if (!(other instanceof DefUseDefinition))
            return false;
        
        DefUseDefinition otherDef = (DefUseDefinition) other;
        if (!otherDef.variableName.equals (this.variableName))
            return false;
        
        if (this.assigningEdge == null && otherDef.assigningEdge == null)
            return true;
        
        if ((this.assigningEdge == null && otherDef.assigningEdge != null) ||
            (this.assigningEdge != null && otherDef.assigningEdge == null))
            return false;
        
        if ((otherDef.assigningEdge.getPredecessor ().getNodeNumber () != this.assigningEdge.getPredecessor ().getNodeNumber ()) ||
            (otherDef.assigningEdge.getSuccessor ().getNodeNumber () != this.assigningEdge.getSuccessor ().getNodeNumber ()))
            return false;
            
        return true;
    }
}
