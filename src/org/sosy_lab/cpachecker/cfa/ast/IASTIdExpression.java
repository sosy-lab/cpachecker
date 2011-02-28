package org.sosy_lab.cpachecker.cfa.ast;

public class IASTIdExpression extends IASTExpression {

  private final IASTName name;

  public IASTIdExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTName pName) {
    super(pRawSignature, pFileLocation, pType);
    name = pName;
  }

  public IASTName getName() {
    return name;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {name};
  }
}
