package cpaplugin.cpa.domains.defuse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class DefUseElement implements AbstractElement
{
    private List<DefUseDefinition> definitions;
    
    public DefUseElement (List<DefUseDefinition> definitions)
    {
        this.definitions = definitions;
        
        if (this.definitions == null)
            this.definitions = new ArrayList<DefUseDefinition> ();
    }
    
    public DefUseElement clone ()
    {
        DefUseElement newElement = new DefUseElement (null);
        for (DefUseDefinition def : definitions)
            newElement.definitions.add (def);
        
        return newElement;
    }
    
    public int getNumDefinitions ()
    {
        return definitions.size ();
    }
    
    public Iterator<DefUseDefinition> getIterator ()
    {
        return definitions.iterator ();
    }
    
    public DefUseDefinition getDefinition (int index)
    {
        return definitions.get (index);
    }
    
    public boolean containsDefinition (DefUseDefinition def)
    {
        return definitions.contains (def);
    }
    
    public void update (DefUseDefinition def)
    {
        String testVarName = def.getVariableName ();
        for (int defIdx = definitions.size () - 1; defIdx >= 0; defIdx--)
        {
            DefUseDefinition otherDef = definitions.get (defIdx);
            if (otherDef.getVariableName ().equals (testVarName))
                definitions.remove (defIdx);
        }
        
        definitions.add (def);
    }
    
    public boolean equals (Object other)
    {
        if (this == other)
            return true;
        
        if (!(other instanceof DefUseElement))
            return false;
        
        DefUseElement otherDefUse = (DefUseElement) other;
        if (otherDefUse.definitions.size () != this.definitions.size ())
            return false;
        
        for (DefUseDefinition def : definitions)
        {
            if (!otherDefUse.definitions.contains (def))
                return false;
        }
        
        return true;
    }
    
    public String toString ()
    {       
        StringBuilder builder = new StringBuilder ();
        builder.append ('{');
        
        boolean hasAny = false;
        for (DefUseDefinition def : definitions)
        {
            CFAEdge assigningEdge = def.getAssigningEdge ();
            builder.append ('(').append (def.getVariableName ()).append(", ");
            
            if (assigningEdge != null)
                builder.append(assigningEdge.getPredecessor ().getNodeNumber ());
            else
                builder.append (0);
            
            builder.append (", ");
            
            if (assigningEdge != null)
                builder.append (assigningEdge.getSuccessor ().getNodeNumber ());
            else
                builder.append (0);
            
            builder.append("), ");
            hasAny = true;
        }
        
        if (hasAny)
            builder.replace (builder.length () - 2, builder.length (), "}");
        else
            builder.append ('}');
        
        return builder.toString ();
    }
}
