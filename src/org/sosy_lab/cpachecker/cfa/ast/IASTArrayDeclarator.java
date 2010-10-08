package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public final class IASTArrayDeclarator extends IASTDeclarator implements
    org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator {

  private final List<IASTArrayModifier> modifiers;
  
  public IASTArrayDeclarator(String pRawSignature,
      IASTFileLocation pFileLocation, IASTInitializer pInitializer,
      IASTName pName, IASTDeclarator pNestedDeclarator,
      List<IASTPointerOperator> pPointerOperators,
      List<IASTArrayModifier> pModifiers) {
    super(pRawSignature, pFileLocation, pInitializer, pName, pNestedDeclarator,
        pPointerOperators);
    modifiers = pModifiers;
  }

  @Override
  @Deprecated
  public void addArrayModifier(org.eclipse.cdt.core.dom.ast.IASTArrayModifier pArg0) {
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
}
