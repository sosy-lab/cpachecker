package org.sosy_lab.cpachecker.cfa.ast;

/**
 * Fake class, should not be used by CPAs.
 */
public class IComplexType extends IType {

  private final String name;
  
  public IComplexType(final String pName) {
    name = pName;
  }

  public String getName() {
    return name;
  }
}
