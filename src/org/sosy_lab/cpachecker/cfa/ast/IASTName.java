package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTName extends IASTNode {

  private final IType type;
  
  public IASTName(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType) {
    super(pRawSignature, pFileLocation);
    type = pType;
  }

  /**
   * Return the type of the thing this name references.
   * Not fully implemented.
   * May return null if the parser did not find a binding.
   */
  public IType getType() {
    return type;
  }
  
  public char[] getSimpleID() {
    // TODO: is this really important? 
    // it is equal to toString() and getRawSignatue()
    return getRawSignature().toCharArray();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

  @Override
  public IASTNode[] getChildren(){
    // there are no children of this class
    return new IASTNode[0];
  }
}
