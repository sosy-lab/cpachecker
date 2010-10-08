package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class IASTSimpleDeclaration extends IASTDeclaration implements
    org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration {

  private final IASTDeclSpecifier specifier;
  private final List<IASTDeclarator> declarators; 
  
  public IASTSimpleDeclaration(String pRawSignature,
      IASTFileLocation pFileLocation,
      IASTDeclSpecifier pSpecifier, List<IASTDeclarator> pDeclarators) {
    super(pRawSignature, pFileLocation);
    specifier = pSpecifier;
    declarators = ImmutableList.copyOf(pDeclarators);
  }

  @Override
  @Deprecated
  public void addDeclarator(org.eclipse.cdt.core.dom.ast.IASTDeclarator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  @Override
  public IASTDeclarator[] getDeclarators() {
    return declarators.toArray(new IASTDeclarator[declarators.size()]);
  }

  @Override
  @Deprecated
  public void setDeclSpecifier(org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTSimpleDeclaration copy() {
    throw new UnsupportedOperationException();
  }
}
