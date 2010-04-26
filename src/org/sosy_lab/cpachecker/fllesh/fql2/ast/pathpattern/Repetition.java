package org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern;

public class Repetition implements PathPattern {

  private PathPattern mSubpattern;
  
  public Repetition(PathPattern pSubpattern) {
    mSubpattern = pSubpattern;
  }
  
  public PathPattern getSubpattern() {
    return mSubpattern;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return "(" + mSubpattern.toString() + ")*";
  }
  
}
