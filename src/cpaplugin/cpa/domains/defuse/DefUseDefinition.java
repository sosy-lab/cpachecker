package cpaplugin.cpa.domains.defuse;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class DefUseDefinition implements AbstractElement
{
    private String variableName;
    private CFAEdge assigningEdge;
    
    public DefUseDefinition (String variableName, CFAEdge assigningEdge)
    {
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
