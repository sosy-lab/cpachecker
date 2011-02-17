package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class IASTCompositeTypeSpecifier extends IASTDeclSpecifier
    implements org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier {

  private final int                   key;
  private final List<IASTDeclaration> members;
  private final IASTName              name;

  public IASTCompositeTypeSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final int pKey, final List<IASTDeclaration> pMembers, final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    key = pKey;
    members = ImmutableList.copyOf(pMembers);
    name = pName;
  }

  @Override
  public int getKey() {
    return key;
  }

  @Override
  public IASTDeclaration[] getMembers() {
    return members.toArray(new IASTDeclaration[members.size()]);
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  public IASTNode[] getChildren(){
    final IASTNode[] children = members.toArray(new IASTDeclaration[members.size() + 1]);
    children[members.size()] = name;
    return children;
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addDeclaration(
      final org.eclipse.cdt.core.dom.ast.IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTDeclaration[] getDeclarations(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addMemberDeclaration(
      final org.eclipse.cdt.core.dom.ast.IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setKey(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTCompositeTypeSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
