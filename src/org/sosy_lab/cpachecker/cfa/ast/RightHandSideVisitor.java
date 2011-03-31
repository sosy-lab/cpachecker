package org.sosy_lab.cpachecker.cfa.ast;

public interface RightHandSideVisitor<R, X extends Exception> extends ExpressionVisitor<R, X> {

  R visit(IASTFunctionCallExpression pIastFunctionCallExpression) throws X;

}
