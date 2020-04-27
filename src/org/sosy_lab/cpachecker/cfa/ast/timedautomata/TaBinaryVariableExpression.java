package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public class TaBinaryVariableExpression extends ABinaryExpression implements TaVariableExpression {

    public TaBinaryVariableExpression(
            FileLocation pFileLocation,
            TaIdExpression pOperand1,
            TaLiteralValueExpression pOperand2,
            BinaryOperator pOperator) {
        super(pFileLocation, null, pOperand1, pOperand2, pOperator);
    }

    private static final long serialVersionUID = -2603510928604073505L;

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

    public enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
        LESS_THAN     ("<"),
        GREATER_THAN  (">"),
        LESS_EQUAL    ("<="),
        GREATER_EQUAL (">="),
        EQUALS        ("="),
        ;
    
        private final String op;
    
        BinaryOperator(String pOp) {
          op = pOp;
        }
    
        /**
         * Returns the string representation of this operator (e.g. "*", "+").
         */
        @Override
        public String getOperator() {
          return op;
        }
    }

    @Override
    public String toASTString() {
      return getOperand1().toASTString()
        + " "
        + getOperator().getOperator()
        + " "
        + getOperand2().toASTString();
    }

}