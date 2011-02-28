package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public class IASTInitializerList extends IASTInitializer {

  private final List<IASTInitializer> initializerList;

  public IASTInitializerList(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final List<IASTInitializer> pInitializerList) {
    super(pRawSignature, pFileLocation);
    initializerList = pInitializerList;
  }

  public IASTInitializer[] getInitializers() {
    return initializerList.toArray(new IASTInitializer[initializerList.size()]);
  }

  @Override
  public IASTNode[] getChildren() {
    return getInitializers();
  }
}
