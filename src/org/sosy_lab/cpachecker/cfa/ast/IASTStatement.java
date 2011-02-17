package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTStatement extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTStatement {

  public IASTStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public abstract IASTStatement copy();
}
