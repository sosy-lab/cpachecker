package org.sosy_lab.cpachecker.cfa.ast;

public interface StatementVisitor<R, X extends Exception> extends RightHandSideVisitor<R, X> {

  R visit(IASTExpressionStatement pIastExpressionStatement) throws X;

  R visit(IASTExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws X;

  R visit(IASTFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws X;

  R visit(IASTFunctionCallStatement pIastFunctionCallStatement) throws X;

}
