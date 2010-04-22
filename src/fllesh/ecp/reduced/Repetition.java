package fllesh.ecp.reduced;

/**
 * P*
 */
public class Repetition implements Pattern {

  private Pattern mSubpattern;
  
  public Repetition(Pattern pSubpattern) {
    mSubpattern = pSubpattern;
  }
  
  public Pattern getSubpattern() {
    return mSubpattern;
  }
  
  @Override
  public String toString() {
    return "(" + mSubpattern.toString() + ")*";
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
