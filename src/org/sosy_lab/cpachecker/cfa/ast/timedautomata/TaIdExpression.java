package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public class TaIdExpression extends AbstractExpression {

    private static final long serialVersionUID = -5739778913828599965L;
    private final String name;

    public TaIdExpression(FileLocation pFileLocation, final String pName) {
        super(pFileLocation, null);
        name = pName;
    }

    @Override
    public <R, R1 extends R, R2 extends R, X1 extends Exception, X2 extends Exception, V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>>
            R accept_(final V pV) throws X1, X2 {
        return null;
    }

    @Override
    public String toASTString(final boolean pQualified) {
        return name;
    }

    @Override
    public <R, R1 extends R, R2 extends R, X1 extends Exception, X2 extends Exception, V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2>>
            R accept_(final V pV) throws X1, X2 {
        return null;
    }

}