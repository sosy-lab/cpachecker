package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTProblem;

public class IASTProblemStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTProblemStatement {

  public IASTProblemStatement(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  @Override
  public IASTProblem getProblem() {
      throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setProblem(IASTProblem pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTProblemStatement copy() {
    throw new UnsupportedOperationException();
  }
}
