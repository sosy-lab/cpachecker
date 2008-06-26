package cpaplugin.cfa.objectmodel.c;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;


public class ReturnEdge extends AbstractCFAEdge 
{
    public ReturnEdge (String rawStatement)
    {
        super (rawStatement);
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.ReturnEdge;
    }
}
