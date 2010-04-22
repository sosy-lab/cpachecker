package fllesh.fql2.ast.coveragespecification;

public class Quotation implements CoverageSpecification {

  private CoverageSpecification mSubspecification;
  
  public Quotation(CoverageSpecification pSubspecification) {
    mSubspecification = pSubspecification;
  }
  
  public CoverageSpecification getSubspecification() {
    return mSubspecification;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "\"(" + mSubspecification.toString() + ")\"";
  }

}
