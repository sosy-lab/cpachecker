package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTStatement extends IASTNode {

  public IASTStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }
}
