package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;
import com.google.common.collect.ImmutableList;

public final class IASTEnumerationSpecifier extends IASTDeclSpecifier {

  private final List<IASTEnumerator> enumerators;
  private final IASTName             name;

  public IASTEnumerationSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final List<IASTEnumerator> pEnumerators, final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName;
  }

  public IASTEnumerator[] getEnumerators() {
    return enumerators.toArray(new IASTEnumerator[enumerators.size()]);
  }

  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children1 = super.getChildren();
    final IASTNode[] children2 = getEnumerators();
    IASTNode[] allChildren=new IASTNode[children1.length + children2.length];
    System.arraycopy(children1, 0, allChildren, 0, children1.length);
    System.arraycopy(children2, 0, allChildren, children1.length, children2.length);
    return allChildren;
  }

  public IASTName getName() {
    return name;
  }

  public static final class IASTEnumerator extends IASTNode {

    private final IASTName       name;
    private final IASTExpression value;

    public IASTEnumerator(final String pRawSignature,
        final IASTFileLocation pFileLocation, final IASTName pName,
        final IASTExpression pValue) {
      super(pRawSignature, pFileLocation);
      name = pName;
      value = pValue;
    }

    public IASTName getName() {
      return name;
    }

    public IASTExpression getValue() {
      return value;
    }

    @Override
    public IASTNode[] getChildren(){
      return new IASTNode[] {value};
    }
  }
}
