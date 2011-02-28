package org.sosy_lab.cpachecker.cfa.ast;

public class IASTFunctionCallExpression extends IASTExpression {

  private final IASTExpression functionName;
  private final IASTExpression parameters;

  public IASTFunctionCallExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pFunctionName, final IASTExpression pParameters) {
    super(pRawSignature, pFileLocation, pType);
    functionName = pFunctionName;
    parameters = pParameters;
  }

  public IASTExpression getFunctionNameExpression() {
    return functionName;
  }

  public IASTExpression getParameterExpression() {
    return parameters;
  }

  @Override
  public IASTNode[] getChildren() {
    if (parameters == null) {
      return new IASTNode[] { functionName };
    } else {
      return new IASTNode[] { functionName, parameters };
    }
  }
}
