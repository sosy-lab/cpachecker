package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public class IASTInitializerList extends IASTInitializer implements
    org.eclipse.cdt.core.dom.ast.IASTInitializerList {

  private final List<IASTInitializer> initializerList;

  public IASTInitializerList(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final List<IASTInitializer> pInitializerList) {
    super(pRawSignature, pFileLocation);
    initializerList = pInitializerList;
  }

  @Override
  public IASTInitializer[] getInitializers() {
    return initializerList.toArray(new IASTInitializer[initializerList.size()]);
  }

  @Override
  public IASTNode[] getChildren() {
    return getInitializers();
  }

  @Override
  @Deprecated
  public IASTInitializerList copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addInitializer(
      final org.eclipse.cdt.core.dom.ast.IASTInitializer pArg0) {
    throw new UnsupportedOperationException();
  }
}
