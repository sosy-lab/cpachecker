package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;
import com.google.common.collect.ImmutableList;

public final class IASTSimpleDeclaration extends IASTDeclaration {

  private final IASTDeclSpecifier    specifier;
  private final List<IASTDeclarator> declarators;

  public IASTSimpleDeclaration(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTDeclSpecifier pSpecifier,
      final List<IASTDeclarator> pDeclarators) {
    super(pRawSignature, pFileLocation);
    specifier = pSpecifier;
    declarators = ImmutableList.copyOf(pDeclarators);
  }

  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  public IASTDeclarator[] getDeclarators() {
    return declarators.toArray(new IASTDeclarator[declarators.size()]);
  }

  @Override
  public IASTNode[] getChildren(){
    // children of this node are all declarators and the specifier
    final IASTNode[] children = declarators.toArray(new IASTDeclarator[declarators.size()+1]);
    children[declarators.size()] = specifier;
    return children;
  }
}
