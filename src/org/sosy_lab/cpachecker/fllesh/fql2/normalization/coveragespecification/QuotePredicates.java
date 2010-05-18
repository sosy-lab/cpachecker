package org.sosy_lab.cpachecker.fllesh.fql2.normalization.coveragespecification;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.fllesh.fql2.normalization.CompositeFQLSpecificationRewriter;
import org.sosy_lab.cpachecker.fllesh.fql2.normalization.FQLSpecificationRewriter;
import org.sosy_lab.cpachecker.fllesh.fql2.normalization.pathpattern.IdentityRewriter;

public class QuotePredicates implements CoverageSpecificationRewriter {

  public static FQLSpecificationRewriter mFQLRewriter = new CompositeFQLSpecificationRewriter(new QuotePredicates(), IdentityRewriter.getInstance());
  
  public static FQLSpecificationRewriter getFQLSpecificationRewriter() {
    return mFQLRewriter;
  }
  
  public static QuotePredicates mInstance = new QuotePredicates();
  
  public static QuotePredicates getRewriter() {
    return mInstance;
  }
  
  private Visitor mVisitor = new Visitor();
  
  @Override
  public CoverageSpecification rewrite(CoverageSpecification pSpecification) {
    return pSpecification.accept(mVisitor);
  }
  
  private class Visitor implements ASTVisitor<CoverageSpecification> {

    @Override
    public Concatenation visit(Concatenation pConcatenation) {
      CoverageSpecification lFirstSubspecification = pConcatenation.getFirstSubspecification();
      CoverageSpecification lSecondSubspecification = pConcatenation.getSecondSubspecification();
      
      CoverageSpecification lNewFirstSubspecification = lFirstSubspecification.accept(this);
      CoverageSpecification lNewSecondSubspecification = lSecondSubspecification.accept(this);
      
      if (lNewFirstSubspecification.equals(lFirstSubspecification) && lNewSecondSubspecification.equals(lSecondSubspecification)) {
        return pConcatenation;
      }
      else {
        return new Concatenation(lNewFirstSubspecification, lNewSecondSubspecification);
      }
    }

    @Override
    public Quotation visit(Quotation pQuotation) {
      return pQuotation;
    }

    @Override
    public Union visit(Union pUnion) {
      CoverageSpecification lFirstSubspecification = pUnion.getFirstSubspecification();
      CoverageSpecification lSecondSubspecification = pUnion.getSecondSubspecification();
      
      CoverageSpecification lNewFirstSubspecification = lFirstSubspecification.accept(this);
      CoverageSpecification lNewSecondSubspecification = lSecondSubspecification.accept(this);
      
      if (lNewFirstSubspecification.equals(lFirstSubspecification) && lNewSecondSubspecification.equals(lSecondSubspecification)) {
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
    public Quotation visit(Predicate pPredicate) {
      return new Quotation(pPredicate);
    }
    
  }

}
