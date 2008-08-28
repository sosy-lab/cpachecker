package cpaplugin.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;


public class DeclarationEdge extends AbstractCFAEdge 
{
    private IASTDeclarator[] declarators;
    private IASTDeclSpecifier specifier;
    
    public DeclarationEdge (String rawStatement,
                            IASTDeclarator[] declarators,
                            IASTDeclSpecifier specifier)
    {
        super (rawStatement);
        this.declarators = declarators;
        this.specifier = specifier;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.DeclarationEdge;
    }
    
    public IASTDeclarator[] getDeclarators ()
    {
        return declarators;
    }
    
    public IASTDeclSpecifier getDeclSpecifier()
    {
        return specifier;
    }
}
