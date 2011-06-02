package org.sosy_lab.cpachecker.fshell.fql2.normalization.coveragespecification;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.pathpattern.PathPatternRewriter;

public class PatternRewriter implements CoverageSpecificationRewriter {
  
  private PathPatternRewriter mRewriter;
  private Visitor mVisitor;
  
  public PatternRewriter(PathPatternRewriter pRewriter) {
    mRewriter = pRewriter;
    mVisitor = new Visitor();
  }
  
  @Override
  public CoverageSpecification rewrite(CoverageSpecification pSpecification) {
    return pSpecification.accept(mVisitor);
  }

  private class Visitor implements CoverageSpecificationVisitor<CoverageSpecification> {

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
      
      PathPattern lNewPattern = mRewriter.rewrite(lPattern);
      
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
      return pEdges;
    }

    @Override
    public Nodes visit(Nodes pNodes) {
      return pNodes;
    }

    @Override
    public Paths visit(Paths pPaths) {
      return pPaths;
    }

    @Override
    public Predicate visit(Predicate pPredicate) {
      return pPredicate;
    }

  }
  
}

