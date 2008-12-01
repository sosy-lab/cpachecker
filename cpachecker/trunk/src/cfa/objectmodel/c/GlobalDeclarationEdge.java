package cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import cfa.objectmodel.c.DeclarationEdge;


/**
 * An edge to store declarations for global variables. These are different
 * from standard declarations, in that they can have also an initializer
 * for the declared variable
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class GlobalDeclarationEdge extends DeclarationEdge {

    public GlobalDeclarationEdge(String rawStatement,
            IASTDeclarator[] declarators, IASTDeclSpecifier specifier) {
        super(rawStatement, declarators, specifier);
    }

}
