package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFunctionCallExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression {

  private final IASTExpression functionName;
  private final IASTExpression parameters;
  
  public IASTFunctionCallExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      IASTExpression pFunctionName, IASTExpression pParameters) {
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
  @Deprecated
  public void setFunctionNameExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setParameterExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTFunctionCallExpression copy() {
    throw new UnsupportedOperationException();
  }
}
