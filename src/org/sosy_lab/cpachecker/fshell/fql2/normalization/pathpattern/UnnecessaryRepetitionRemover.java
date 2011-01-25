package org.sosy_lab.cpachecker.fshell.fql2.normalization.pathpattern;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPatternVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Concatenation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Union;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.CompositeFQLSpecificationRewriter;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.FQLSpecificationRewriter;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.coveragespecification.CoverageSpecificationRewriter;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.coveragespecification.PatternRewriter;

public class UnnecessaryRepetitionRemover implements PathPatternRewriter {

  private Visitor mVisitor;
  private static CoverageSpecificationRewriter mRewriter = new PatternRewriter(new UnnecessaryRepetitionRemover());
  private static UnnecessaryRepetitionRemover mRemover = new UnnecessaryRepetitionRemover();
  private static FQLSpecificationRewriter mSpecRewriter = new CompositeFQLSpecificationRewriter(mRewriter, mRemover);
  
  public UnnecessaryRepetitionRemover() {
    mVisitor = new Visitor();
  }
  
  @Override
  public PathPattern rewrite(PathPattern pPattern) {
    return pPattern.accept(mVisitor);
  }
  
  public static FQLSpecificationRewriter getFQLSpecificationRewriter() {
    return mSpecRewriter;
  }
  
  public static CoverageSpecificationRewriter getSpecificationRewriter() {
    return mRewriter;
  }
  
  public static UnnecessaryRepetitionRemover getInstance() {
    return mRemover; 
  }
  
  private static class Visitor implements PathPatternVisitor<PathPattern> {
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
    public PathPattern visit(Repetition pRepetition) {
      PathPattern lSubpattern = pRepetition.getSubpattern();
      
      PathPattern lNewSubpattern = lSubpattern.accept(this);
      
      if (lNewSubpattern instanceof Repetition) {
        return lNewSubpattern;
      }
      else {
        return pRepetition;
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
