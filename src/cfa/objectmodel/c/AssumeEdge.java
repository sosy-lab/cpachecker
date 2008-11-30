package cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;



public class AssumeEdge extends AbstractCFAEdge 
{
    private boolean truthAssumption;
    private IASTExpression expression;
    
    public AssumeEdge (String rawStatement, 
                           IASTExpression expression,
                           boolean truthAssumption)
    {
        super (rawStatement);
        
        this.truthAssumption = truthAssumption;
        this.expression = expression;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.AssumeEdge;
    }
    
    public boolean getTruthAssumption ()
    {
        return truthAssumption;
    }
    
    public IASTExpression getExpression ()
    {
        return expression;
    }
    
    public String getRawStatement ()
    {
        return "[" + super.getRawStatement () + "]";
    }
}
