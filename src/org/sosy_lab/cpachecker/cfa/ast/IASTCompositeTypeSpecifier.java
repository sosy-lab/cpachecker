package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class IASTCompositeTypeSpecifier extends IASTDeclSpecifier {

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

  public int getKey() {
    return key;
  }

  public IASTDeclaration[] getMembers() {
    return members.toArray(new IASTDeclaration[members.size()]);
  }

  public IASTName getName() {
    return name;
  }

  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children =
        members.toArray(new IASTDeclaration[members.size() + 1]);
    children[members.size()] = name;
    return children;
  }

  public static final int k_struct = 1;
  public static final int k_union  = 2;
}
