package cfa.objectmodel.c;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;


public class MultiDeclarationEdge extends AbstractCFAEdge
{
    private List<IASTDeclarator[]> declarators;
    private List<String> rawStatements;
    
    private boolean jumpEdge;
    
    public MultiDeclarationEdge (String rawStatement,
                              List<IASTDeclarator[]> declarators,
                              List<String> rawStatements)
    {
        super (rawStatement);
        this.jumpEdge = false;
        
        if (declarators == null)
            this.declarators = new ArrayList<IASTDeclarator[]> ();
        else
            this.declarators = declarators;
        
        if (rawStatements == null)
            this.rawStatements = new ArrayList<String> ();
        else
            this.rawStatements = rawStatements;
    }
    
    public void setIsJumpEdge (boolean jumpEdge)
    {
        this.jumpEdge = jumpEdge;
    }
    
    @Override
    public boolean isJumpEdge ()
    {
        return jumpEdge;
    }

    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.MultiDeclarationEdge;
    }
    
    public List<IASTDeclarator[]> getDeclarators ()
    {
        return declarators;
    }
    
    public List<String> getRawStatements ()
    {
        return rawStatements;
    }
    
    @Override
    public String getRawStatement ()
    {
        StringBuilder builder = new StringBuilder ();
        
        for (String sig : rawStatements)
        {
            builder.append (sig).append ("\\n");
        }
        
        return builder.toString ();
    }
}
