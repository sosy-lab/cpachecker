package cpaplugin.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;


public class StatementEdge extends AbstractCFAEdge 
{
    private IASTExpression expression;
    private boolean jumpEdge;
    
    public StatementEdge (String rawStatement,
                              IASTExpression expression)
    {
        super (rawStatement);
        this.expression = expression;
        this.jumpEdge = false;
    }
    
    public void setIsJumpEdge (boolean jumpEdge)
    {
        this.jumpEdge = jumpEdge;
    }
    
    public boolean isJumpEdge ()
    {
        return jumpEdge;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.StatementEdge;
    }
    
    public IASTExpression getExpression ()
    {
        return expression;
    }
}
