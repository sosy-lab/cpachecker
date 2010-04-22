package fllesh.fql2.ast;

import fllesh.fql.frontend.ast.filter.Filter;
import fllesh.fql2.ast.coveragespecification.ASTVisitor;

public class Edges implements Atom {

  private Filter mFilter;
  
  public Edges(Filter pFilter) {
    mFilter = pFilter;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  @Override
  public String toString() {
    return "EDGES(" + mFilter.toString() + ")";
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <T> T accept(fllesh.fql2.ast.pathpattern.ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
}
