package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;

public class DummyIASTFunctionCallExpression extends IASTFunctionCallExpression{

  public DummyIASTFunctionCallExpression(final IASTExpression pFunctionNameExpression,
      final IASTExpression pParameterExpression) {
    super (null, null, null, pFunctionNameExpression, pParameterExpression);
  }
  
  @Override
  public String getRawSignature() {
    return getFunctionNameExpression().getRawSignature() + "(" +
    getParameterExpression() != null ? "[]" : getParameterExpression().getRawSignature() + ");";
  }

  @Override
  public String toString() {
    return this.getRawSignature();
  }
}
