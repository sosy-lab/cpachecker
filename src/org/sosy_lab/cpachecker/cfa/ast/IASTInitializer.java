package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTInitializer extends IASTNode {

  public IASTInitializer(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }
}
