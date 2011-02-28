package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTDeclaration extends IASTNode {

  public IASTDeclaration(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }
}
