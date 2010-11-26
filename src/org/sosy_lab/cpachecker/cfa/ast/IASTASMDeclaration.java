package org.sosy_lab.cpachecker.cfa.ast;

public class IASTASMDeclaration extends IASTDeclaration implements
    org.eclipse.cdt.core.dom.ast.IASTASMDeclaration {

  public IASTASMDeclaration(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  @Override
  public String getAssembly() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setAssembly(String pArg0) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  @Deprecated
  public IASTASMDeclaration copy() {
    throw new UnsupportedOperationException();
  }
}
