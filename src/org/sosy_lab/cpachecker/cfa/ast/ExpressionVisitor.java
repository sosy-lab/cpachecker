package org.sosy_lab.cpachecker.cfa.ast;

public interface ExpressionVisitor<R, X extends Exception> {

  R visit(IASTArraySubscriptExpression pIastArraySubscriptExpression) throws X;

  R visit(IASTBinaryExpression pIastBinaryExpression) throws X;

  R visit(IASTCastExpression pIastCastExpression) throws X;

  R visit(IASTFieldReference pIastFieldReference) throws X;

  R visit(IASTIdExpression pIastIdExpression) throws X;

  R visit(IASTLiteralExpression pIastLiteralExpression) throws X;

  R visit(IASTTypeIdExpression pIastTypeIdExpression) throws X;

  R visit(IASTUnaryExpression pIastUnaryExpression) throws X;

}
