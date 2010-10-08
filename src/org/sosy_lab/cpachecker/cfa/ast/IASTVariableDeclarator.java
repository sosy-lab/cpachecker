package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public final class IASTVariableDeclarator extends IASTDeclarator {

  public IASTVariableDeclarator(String pRawSignature,
      IASTFileLocation pFileLocation, IASTInitializer pInitializer,
      IASTName pName, IASTDeclarator pNestedDeclarator,
      List<IASTPointerOperator> pPointerOperators) {
    super(pRawSignature, pFileLocation, pInitializer, pName, pNestedDeclarator, pPointerOperators);
  }

}
