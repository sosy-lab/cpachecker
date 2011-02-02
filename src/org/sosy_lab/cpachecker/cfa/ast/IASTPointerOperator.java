package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTPointerOperator extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTPointerOperator {

  public IASTPointerOperator(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }
  
  @Override
  @Deprecated
  public IASTPointerOperator copy() {
    throw new UnsupportedOperationException();
  }

}
