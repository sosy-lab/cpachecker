package org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern;

public class Concatenation implements PathPattern {

  private PathPattern mFirstSubpattern;
  private PathPattern mSecondSubpattern;
  
  public Concatenation(PathPattern pFirstSubpattern, PathPattern pSecondSubpattern) {
    mFirstSubpattern = pFirstSubpattern;
    mSecondSubpattern = pSecondSubpattern;
  }
  
  public PathPattern getFirstSubpattern() {
    return mFirstSubpattern;
  }
  
  public PathPattern getSecondSubpattern() {
    return mSecondSubpattern;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "(" + mFirstSubpattern.toString() + "." + mSecondSubpattern.toString() + ")";
  }

}
