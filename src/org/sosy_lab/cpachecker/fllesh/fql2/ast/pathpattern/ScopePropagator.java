package org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Compose;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;

public class ScopePropagator implements ASTVisitor<PathPattern> {

  Filter mFilter;
  
  public ScopePropagator(Filter pFilter) {
    mFilter = pFilter;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  @Override
  public Concatenation visit(Concatenation pConcatenation) {
    PathPattern lFirstSubpattern = pConcatenation.getFirstSubpattern();
    PathPattern lSecondSubpattern = pConcatenation.getSecondSubpattern();
    
    PathPattern lNewFirstSubpattern = lFirstSubpattern.accept(this);
    PathPattern lNewSecondSubpattern = lSecondSubpattern.accept(this);
    
    if (lFirstSubpattern.equals(lNewFirstSubpattern) && lSecondSubpattern.equals(lNewSecondSubpattern)) {
      return pConcatenation;
    }
    else {
      return new Concatenation(lNewFirstSubpattern, lNewSecondSubpattern);
    }
  }

  @Override
  public Repetition visit(Repetition pRepetition) {
    PathPattern lSubpattern = pRepetition.getSubpattern();
    
    PathPattern lNewSubpattern = lSubpattern.accept(this);
    
    if (lSubpattern.equals(lNewSubpattern)) {
      return pRepetition;
    }
    else {
      return new Repetition(lNewSubpattern);
    }
  }

  @Override
  public Union visit(Union pUnion) {
    PathPattern lFirstSubpattern = pUnion.getFirstSubpattern();
    PathPattern lSecondSubpattern = pUnion.getSecondSubpattern();
    
    PathPattern lNewFirstSubpattern = lFirstSubpattern.accept(this);
    PathPattern lNewSecondSubpattern = lSecondSubpattern.accept(this);
    
    if (lFirstSubpattern.equals(lNewFirstSubpattern) && lSecondSubpattern.equals(lNewSecondSubpattern)) {
      return pUnion;
    }
    else {
      return new Union(lNewFirstSubpattern, lNewSecondSubpattern);
    }
  }

  @Override
  public Edges visit(Edges pEdges) {
    Filter lFilter = pEdges.getFilter();
    
    return new Edges(new Compose(lFilter, getFilter()));
  }

  @Override
  public Nodes visit(Nodes pNodes) {
    Filter lFilter = pNodes.getFilter();
    
    return new Nodes(new Compose(lFilter, getFilter()));
  }

  @Override
  public Paths visit(Paths pPaths) {
    Filter lFilter = pPaths.getFilter();
    int lBound = pPaths.getBound();
    
    return new Paths(new Compose(lFilter, getFilter()), lBound);
  }

  @Override
  public Predicate visit(Predicate pPredicate) {
    return pPredicate;
  }

}
