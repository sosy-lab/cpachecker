package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public abstract class IASTFunctionDeclarator extends IASTDeclarator implements
    org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator {

  public IASTFunctionDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTInitializer pInitializer,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointerOperator> pPointerOperators) {
    super(pRawSignature, pFileLocation, pInitializer, pName, pNestedDeclarator,
        pPointerOperators);
  }

  @Override
  @Deprecated
  public IASTFunctionDeclarator copy() {
    throw new UnsupportedOperationException();
  }
}
