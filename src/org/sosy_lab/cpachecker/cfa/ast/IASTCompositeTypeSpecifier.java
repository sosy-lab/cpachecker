package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class IASTCompositeTypeSpecifier extends IASTDeclSpecifier
    implements org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier {

  private final int key;
  private final List<IASTDeclaration> members;
  private final IASTName name;

  public IASTCompositeTypeSpecifier(String pRawSignature,
      org.sosy_lab.cpachecker.cfa.ast.IASTFileLocation pFileLocation,
      int pStorageClass, boolean pConst, boolean pInline, boolean pVolatile,
      int pKey, List<IASTDeclaration> pMembers, IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline, pVolatile);
    key = pKey;
    members = ImmutableList.copyOf(pMembers);
    name = pName;
  }

  @Override
  @Deprecated
  public int getRoleForName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addDeclaration(org.eclipse.cdt.core.dom.ast.IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTDeclaration[] getDeclarations(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addMemberDeclaration(org.eclipse.cdt.core.dom.ast.IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
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
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setKey(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  @Deprecated
  public IASTCompositeTypeSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
