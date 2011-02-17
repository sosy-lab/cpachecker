package org.sosy_lab.cpachecker.cfa.ast;

public class IASTFunctionCallExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression {

  private final IASTExpression functionName;
  private final IASTExpression parameters;

  public IASTFunctionCallExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pFunctionName, final IASTExpression pParameters) {
    super(pRawSignature, pFileLocation, pType);
    functionName = pFunctionName;
    parameters = pParameters;
  }

  @Override
  public IASTExpression getFunctionNameExpression() {
    return functionName;
  }

  @Override
  public IASTExpression getParameterExpression() {
    return parameters;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {functionName, parameters};
  }

  @Override
  @Deprecated
  public void setFunctionNameExpression(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setParameterExpression(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTFunctionCallExpression copy() {
    throw new UnsupportedOperationException();
  }
}
