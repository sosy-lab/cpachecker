package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public final class IASTStandardFunctionDeclarator extends
    IASTFunctionDeclarator implements
    org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator {

  private final List<IASTParameterDeclaration> parameters;
  private final boolean takesVarArgs;
  
  public IASTStandardFunctionDeclarator(String pRawSignature,
      IASTFileLocation pFileLocation, IASTInitializer pInitializer,
      IASTName pName, IASTDeclarator pNestedDeclarator,
      List<IASTPointerOperator> pPointerOperators,
      List<IASTParameterDeclaration> pParameters,
      boolean pTakesVarArgs) {
    super(pRawSignature, pFileLocation, pInitializer, pName, pNestedDeclarator,
        pPointerOperators);
    parameters = pParameters;
    takesVarArgs = pTakesVarArgs;
  }

  @Override
  @Deprecated
  public void addParameterDeclaration(org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getFunctionScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTParameterDeclaration[] getParameters() {
    return parameters.toArray(new IASTParameterDeclaration[parameters.size()]);
  }

  @Override
  @Deprecated
  public void setVarArgs(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean takesVarArgs() {
    return takesVarArgs;
  }

  @Override
  @Deprecated
  public IASTStandardFunctionDeclarator copy() {
    throw new UnsupportedOperationException();
  }
}
