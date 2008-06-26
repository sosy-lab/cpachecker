package cpaplugin.cfa.objectmodel.c;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;


public class FunctionCallEdge extends AbstractCFAEdge 
{
    public FunctionCallEdge (String rawStatement)
    {
        super (rawStatement);
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.FunctionCallEdge;
    }
}
