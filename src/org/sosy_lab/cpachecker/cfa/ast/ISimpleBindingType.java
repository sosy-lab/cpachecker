package org.sosy_lab.cpachecker.cfa.ast;

/**
 * Fake class, should not be used by CPAs.
 */
class ISimpleBindingType extends IBindingType {

  public ISimpleBindingType(final String pName) {
    super(pName);
  }

  @Override
  public boolean isSameType(final IType other) {
    throw new UnsupportedOperationException();
  }
}
