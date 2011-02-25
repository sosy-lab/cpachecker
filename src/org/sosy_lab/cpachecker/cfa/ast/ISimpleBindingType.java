package org.sosy_lab.cpachecker.cfa.ast;

/**
 * Fake class, should not be used by CPAs.
 */
class ISimpleBindingType extends IBindingType {

  public ISimpleBindingType(String pName) {
    super(pName);
  }

  @Override
  public boolean isSameType(final org.eclipse.cdt.core.dom.ast.IType other) {
    throw new UnsupportedOperationException();
  }
}
