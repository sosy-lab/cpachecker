package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public class TaLiteralValueExpression extends AIntegerLiteralExpression {

    private static final long serialVersionUID = 5518559880924665980L;

    public TaLiteralValueExpression(FileLocation pFileLocation, BigInteger pValue) {
        super(pFileLocation, null, pValue);
    }

    @Override
    public <R, R1 extends R, R2 extends R, X1 extends Exception, X2 extends Exception, V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>>
            R accept_(V pV) throws X1, X2 {
        return null;
    }

    @Override
    public <R, R1 extends R, R2 extends R, X1 extends Exception, X2 extends Exception, V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2>>
            R accept_(V pV) throws X1, X2 {
        return null;
    }

}