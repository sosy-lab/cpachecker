package org.sosy_lab.cpachecker.fllesh.ecp.reduced;

/**
 * P + P
 */
public class Union implements Pattern {

  private Pattern mElement1;
  private Pattern mElement2;
  
  public Union(Pattern pElement1, Pattern pElement2) {
    mElement1 = pElement1;
    mElement2 = pElement2;
  }
  
  public Pattern getFirstSubpattern() {
    return mElement1;
  }
  
  public Pattern getSecondSubpattern() {
    return mElement2;
  }
  
  @Override
  public String toString() {
    return "(" + mElement1.toString() + ") + (" + mElement2.toString() + ")";
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
