package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public final class IASTArrayDeclarator extends IASTDeclarator implements
    org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator {

  private final List<IASTArrayModifier> modifiers;

  public IASTArrayDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTInitializer pInitializer,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointerOperator> pPointerOperators,
      final List<IASTArrayModifier> pModifiers) {
    super(pRawSignature, pFileLocation, pInitializer, pName, pNestedDeclarator,
        pPointerOperators);
    modifiers = pModifiers;
  }

  @Override
  @Deprecated
  public void addArrayModifier(
      final org.eclipse.cdt.core.dom.ast.IASTArrayModifier pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTArrayModifier[] getArrayModifiers() {
    return modifiers.toArray(new IASTArrayModifier[modifiers.size()]);
  }

  @Override
  @Deprecated
  public IASTArrayDeclarator copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children1 = super.getChildren();
    final IASTNode[] children2 = getArrayModifiers();
    IASTNode[] allChildren=new IASTNode[children1.length + children2.length];
    System.arraycopy(children1, 0, allChildren, 0, children1.length);
    System.arraycopy(children2, 0, allChildren, children1.length, children2.length);
    return allChildren;
  }
}
