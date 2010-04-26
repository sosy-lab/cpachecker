package org.sosy_lab.cpachecker.fllesh.ecp.reduced;

public class Atom implements Pattern {

  private String mIdentifier;
  
  public Atom(String pIdentifier) {
    mIdentifier = pIdentifier;
  }
  
  public String getIdentifier() {
    return mIdentifier;
  }
  
  @Override
  public String toString() {
    return mIdentifier;
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
}
