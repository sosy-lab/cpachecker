package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public abstract class IASTDeclarator extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTDeclarator {

  private final IASTInitializer           initializer;
  private final IASTName                  name;
  private final IASTDeclarator            nestedDeclarator;
  private final List<IASTPointerOperator> pointerOperators;

  public IASTDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTInitializer pInitializer,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointerOperator> pPointerOperators) {
    super(pRawSignature, pFileLocation);
    initializer = pInitializer;
    name = pName;
    nestedDeclarator = pNestedDeclarator;
    pointerOperators = ImmutableList.copyOf(pPointerOperators);
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addPointerOperator(
      final org.eclipse.cdt.core.dom.ast.IASTPointerOperator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTInitializer getInitializer() {
    return initializer;
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  public IASTDeclarator getNestedDeclarator() {
    return nestedDeclarator;
  }

  @Override
  public IASTPointerOperator[] getPointerOperators() {
    return pointerOperators.toArray(new IASTPointerOperator[pointerOperators
        .size()]);
  }

  @Override
  @Deprecated
  public void setInitializer(
      final org.eclipse.cdt.core.dom.ast.IASTInitializer pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setNestedDeclarator(
      final org.eclipse.cdt.core.dom.ast.IASTDeclarator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTDeclarator copy() {
    throw new UnsupportedOperationException();
  }
}
