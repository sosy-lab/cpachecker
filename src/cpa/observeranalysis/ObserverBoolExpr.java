package cpa.observeranalysis;

import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractQueryableElement;
import exceptions.InvalidQueryException;

/**
 * Implements a boolean expression that evaluates and returns a <code>MaybeBoolean</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 * @author rhein
 */
abstract class ObserverBoolExpr {
  
  /**
   * @author rhein
   * This is a extension of the boolean data type. It also contains a dont-know-value (MAYBE).
   */
  static enum MaybeBoolean {TRUE, FALSE, MAYBE}
  
  private ObserverBoolExpr() {} //nobody can use this
  abstract MaybeBoolean eval(ObserverExpressionArguments pArgs);
  
  /**
   * This is a efficient implementation of the ASTComparison (it caches the generated ASTs for the pattern).
   * It also displays error messages if the AST contains problems/errors.
   * The AST Comparison evaluates the pattern (coming from the Automaton Definition) and the C-Statement on the CFA Edge to ASTs and compares these with a Tree comparison algorithm.
   * @author rhein
   */
  static class MatchCFAEdgeASTComparison extends ObserverBoolExpr {
    private IASTTranslationUnit patternAST;
    public MatchCFAEdgeASTComparison(String pPattern) {
      this.patternAST = ObserverASTComparator.generatePatternAST(pPattern);
      String problem = ObserverASTComparator.ASTcontatinsProblems(patternAST);
      if (problem != null) {
        System.out.println("The AST generated for \"" + pPattern + "\" contains the following problem: " + problem);
      }
    }
    @Override
    MaybeBoolean eval(ObserverExpressionArguments pArgs) {
      if (ObserverASTComparator.generateAndCompareASTs(pArgs.getCfaEdge().getRawStatement(), patternAST)) {
        return MaybeBoolean.TRUE;
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
  
  static class MatchCFAEdgeRegEx extends ObserverBoolExpr {
    String pattern;
    public MatchCFAEdgeRegEx(String pPattern) {
      super();
      pattern = pPattern;
    }
    @Override
    MaybeBoolean eval(ObserverExpressionArguments pArgs) {
      if (pArgs.getCfaEdge().getRawStatement().matches(pattern)) {
        return MaybeBoolean.TRUE;
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
  static class MatchCFAEdgeExact extends ObserverBoolExpr {
    String pattern;
    MatchCFAEdgeExact(String pPattern) {
      super();
      pattern = pPattern;
    }
    @Override
    MaybeBoolean eval(ObserverExpressionArguments pArgs) {
      if (pArgs.getCfaEdge().getRawStatement().equals(pattern)) {
        return MaybeBoolean.TRUE;
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
  
  /**
   * Sends a query-String to an <code>AbstractElement</code> of another analysis and returns the query-Result.  
   * @author rhein
   */
  static class CPAQuery extends ObserverBoolExpr {
    String cPAName, queryString;
    CPAQuery(String pCPAName, String pQuery) {
      super();
      cPAName = pCPAName;
      queryString = pQuery;
    }
    @Override
    MaybeBoolean eval(ObserverExpressionArguments pArgs) {
       for (AbstractElement ae : pArgs.getAbstractElements()) {
         if (ae instanceof AbstractQueryableElement) {
          AbstractQueryableElement aqe = (AbstractQueryableElement) ae;
          if (aqe.getCPAName().equals(cPAName)) {
            try {
              if (aqe.checkProperty(queryString)) {
                return MaybeBoolean.TRUE;
              } else {
                return MaybeBoolean.FALSE;
              }
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e, 
                  "ObserverAutomaton encountered an Exception during Query of the " 
                  + cPAName + " CPA on Edge " + pArgs.getCfaEdge().getRawStatement());
              return MaybeBoolean.FALSE;
            }
          }
        }
      }
      return MaybeBoolean.MAYBE; // the necessary CPA-State was not found
    }
  }
  
  /** Constant for true.
   * @author rhein
   */
  static class True extends ObserverBoolExpr {
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) {return MaybeBoolean.TRUE;}
  }
  
  /** Constant for false.
   * @author rhein
   */
  static class False extends ObserverBoolExpr {
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) {return MaybeBoolean.FALSE;}
  }
  
  /** Tests the equality of the values of two instances of {@link ObserverIntExpr}.
   * @author rhein
   */
  static class EqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public EqTest(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) {
      if (a.eval(pArgs) == b.eval(pArgs)) {
        return MaybeBoolean.TRUE;
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
  /** Tests whether two instances of {@link ObserverIntExpr} evaluate to different integers.
   * @author rhein
   */
  static class NotEqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public NotEqTest(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
      if (a.eval(pArgs) != b.eval(pArgs)) {
        return MaybeBoolean.TRUE;
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
}
