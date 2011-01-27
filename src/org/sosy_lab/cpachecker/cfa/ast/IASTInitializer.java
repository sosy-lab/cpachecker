package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTInitializer extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTInitializer {

  public IASTInitializer(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTInitializer copy() {
    throw new UnsupportedOperationException();
  }
}
