package cpaplugin.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;


public class DeclarationEdge extends AbstractCFAEdge 
{
    private IASTDeclarator[] declarators;
    
    public DeclarationEdge (String rawStatement,
                              IASTDeclarator[] declarators)
    {
        super (rawStatement);
        this.declarators = declarators;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.DeclarationEdge;
    }
    
    public IASTDeclarator[] getDeclarators ()
    {
        return declarators;
    }
}
