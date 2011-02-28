package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTNode {

  private final String           rawSignature;
  private final IASTFileLocation fileLocation;

  public IASTNode(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    rawSignature = pRawSignature;
    fileLocation = pFileLocation;
  }

  public abstract IASTNode[] getChildren();

  public IASTFileLocation getFileLocation() {
    return fileLocation;
  }

  public String getRawSignature() {
    return rawSignature;
  }
}
