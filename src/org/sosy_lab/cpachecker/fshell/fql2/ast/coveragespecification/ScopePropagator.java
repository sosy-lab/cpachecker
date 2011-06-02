package org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Filter;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;

public class ScopePropagator implements CoverageSpecificationVisitor<CoverageSpecification> {

  private org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.ScopePropagator mPatternScopePropagator;

  public ScopePropagator(Filter pFilter) {
    mPatternScopePropagator = new org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.ScopePropagator(pFilter);
  }

  public Filter getFilter() {
    return mPatternScopePropagator.getFilter();
  }

  @Override
  public Concatenation visit(Concatenation pConcatenation) {
    CoverageSpecification lFirstSubspecification = pConcatenation.getFirstSubspecification();
    CoverageSpecification lSecondSubspecification = pConcatenation.getSecondSubspecification();

    CoverageSpecification lNewFirstSubspecification = lFirstSubspecification.accept(this);
    CoverageSpecification lNewSecondSubspecification = lSecondSubspecification.accept(this);

    if (lFirstSubspecification.equals(lNewFirstSubspecification) && lSecondSubspecification.equals(lNewSecondSubspecification)) {
      return pConcatenation;
    }
    else {
      return new Concatenation(lNewFirstSubspecification, lNewSecondSubspecification);
    }
  }

  @Override
  public Quotation visit(Quotation pQuotation) {
    PathPattern lPattern = pQuotation.getPathPattern();
    PathPattern lNewPattern = lPattern.accept(mPatternScopePropagator);

    if (lPattern.equals(lNewPattern)) {
      return pQuotation;
    }
    else {
      return new Quotation(lNewPattern);
    }
  }

  @Override
  public Union visit(Union pUnion) {
    CoverageSpecification lFirstSubspecification = pUnion.getFirstSubspecification();
    CoverageSpecification lSecondSubspecification = pUnion.getSecondSubspecification();

    CoverageSpecification lNewFirstSubspecification = lFirstSubspecification.accept(this);
    CoverageSpecification lNewSecondSubspecification = lSecondSubspecification.accept(this);

    if (lFirstSubspecification.equals(lNewFirstSubspecification) && lSecondSubspecification.equals(lNewSecondSubspecification)) {
      return pUnion;
    }
    else {
      return new Union(lNewFirstSubspecification, lNewSecondSubspecification);
    }
  }

  @Override
  public Edges visit(Edges pEdges) {
    return mPatternScopePropagator.visit(pEdges);
  }

  @Override
  public Nodes visit(Nodes pNodes) {
    return mPatternScopePropagator.visit(pNodes);
  }

  @Override
  public Paths visit(Paths pPaths) {
    return mPatternScopePropagator.visit(pPaths);
  }

  @Override
  public Predicate visit(Predicate pPredicate) {
    return mPatternScopePropagator.visit(pPredicate);
  }

}
