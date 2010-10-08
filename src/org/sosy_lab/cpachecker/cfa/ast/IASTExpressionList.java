package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class IASTExpressionList extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTExpressionList {

  private final List<IASTExpression> expressions;
  
  public IASTExpressionList(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      List<IASTExpression> pExpressions) {
    super(pRawSignature, pFileLocation, pType);
    expressions = ImmutableList.copyOf(pExpressions);
  }

  @Override
  @Deprecated
  public void addExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTExpression[] getExpressions() {
    return expressions.toArray(new IASTExpression[expressions.size()]);
  }

  @Override
  @Deprecated
  public IASTExpressionList copy() {
    throw new UnsupportedOperationException();
  }
}
