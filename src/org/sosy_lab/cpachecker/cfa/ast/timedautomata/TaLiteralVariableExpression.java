package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public class TaLiteralVariableExpression extends ALiteralExpression implements TaVariableExpression {

    private final boolean value;

    public TaLiteralVariableExpression(FileLocation pFileLocation, boolean pValue) {
        super(pFileLocation, null);
        value = pValue;
    }

    private static final long serialVersionUID = 7281222079411685920L;

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

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String toASTString() {
        return value ? "TRUE" : "FALSE";
    }

}