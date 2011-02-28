package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IBindingType extends IType implements IBinding {

  private final String name;
  
  public IBindingType(final String pName) {
    name = pName;
  }

  @Override
  public String getName() {
    return name;
  }
}
