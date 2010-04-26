package org.sosy_lab.cpachecker.fllesh.fql2.ast;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.ASTVisitor;

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
  public <T> T accept(org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
}
