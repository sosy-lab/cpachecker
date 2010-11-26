package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTProblem extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTNode {

  public IASTProblem(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  public String getMessage() {
    throw new UnsupportedOperationException();
  }

}
