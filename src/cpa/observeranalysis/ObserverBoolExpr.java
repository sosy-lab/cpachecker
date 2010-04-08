package cpa.observeranalysis;

import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import cfa.objectmodel.CFALabelNode;
import cfa.objectmodel.CFANode;

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
  static enum MaybeBoolean {TRUE, FALSE, MAYBE;
    static MaybeBoolean valueOf(boolean pB) {
      if (pB) return TRUE;
      else return FALSE;
    }
  }
  
  private ObserverBoolExpr() {} //nobody can use this
  abstract MaybeBoolean eval(ObserverExpressionArguments pArgs);
  
  /**
   * Implements a regex match on the label after the current CFAEdge.
   * The eval method returns false if there is no label following the CFAEdge.
   * (".*" in java-regex means "any characters")
   * @author rhein
   */
  static class MatchLabelRegEx extends ObserverBoolExpr {
    String pattern;
    public MatchLabelRegEx(String pPattern) {
      super();
      pattern = pPattern;
    }
    @Override
    MaybeBoolean eval(ObserverExpressionArguments pArgs) {
      CFANode successorNode = pArgs.getCfaEdge().getSuccessor();
      if (successorNode instanceof CFALabelNode) {
        String label = ((CFALabelNode)successorNode).getLabel(); 
        if (label.toLowerCase().matches(pattern)) {
          return MaybeBoolean.TRUE;
        } else {
          return MaybeBoolean.FALSE;
        } 
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
  
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
  static class IntEqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public IntEqTest(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
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
  static class IntNotEqTest extends ObserverBoolExpr {
    ObserverIntExpr a, b;
    public IntNotEqTest(ObserverIntExpr pA, ObserverIntExpr pB) {this.a = pA; this.b = pB;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
      if (a.eval(pArgs) != b.eval(pArgs)) {
        return MaybeBoolean.TRUE;
      } else {
        return MaybeBoolean.FALSE;
      }
    }
  }
  /** Computes the disjunction of two {@link ObserverBoolExpr} (lazy evaluation).
   * @author rhein
   */
  static class Or extends ObserverBoolExpr {
    ObserverBoolExpr a, b;
    public Or(ObserverBoolExpr pA, ObserverBoolExpr pB) {this.a = pA; this.b = pB;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
      MaybeBoolean resultA = a.eval(pArgs);
      if (resultA == MaybeBoolean.TRUE) {
        return MaybeBoolean.TRUE;
      } else {
        MaybeBoolean resultB = b.eval(pArgs);
        if (resultB == MaybeBoolean.TRUE)  return MaybeBoolean.TRUE;
        if (resultB == MaybeBoolean.FALSE) return resultA;
        return resultB; // in this case resultB==MAYBE
      }
    }
  }
  /** Computes the conjunction of two {@link ObserverBoolExpr} (lazy evaluation).
   * @author rhein
   */
  static class And extends ObserverBoolExpr {
    ObserverBoolExpr a, b;
    public And(ObserverBoolExpr pA, ObserverBoolExpr pB) {this.a = pA; this.b = pB;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
      MaybeBoolean resultA = a.eval(pArgs);
      if (resultA == MaybeBoolean.FALSE) {
        return MaybeBoolean.FALSE;
      } else {
        MaybeBoolean resultB = b.eval(pArgs);
        if (resultB == MaybeBoolean.FALSE)  return MaybeBoolean.FALSE;
        if (resultB == MaybeBoolean.TRUE) return resultA;
        return resultB; // in this case resultB==MAYBE
      }
    }
  }
  /**
   * Negates the result of a {@link ObserverBoolExpr}. If the result is MAYBE it is returned unchanged.
   * @author rhein
   */
  static class Negation extends ObserverBoolExpr {
    ObserverBoolExpr a;
    public Negation(ObserverBoolExpr pA) {this.a = pA;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
      MaybeBoolean resultA = a.eval(pArgs);
      switch (resultA) {
      case TRUE: return MaybeBoolean.FALSE; 
      case FALSE: return MaybeBoolean.TRUE;
      default: return MaybeBoolean.MAYBE;
      }
    }
  }
  /**
   * Boolean Equality
   * @author rhein
   */
  static class BoolEqTest extends ObserverBoolExpr {
    ObserverBoolExpr a, b;
    public BoolEqTest(ObserverBoolExpr pA, ObserverBoolExpr pB) {this.a = pA; this.b = pB;}
    @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
      MaybeBoolean resultA = a.eval(pArgs);
      MaybeBoolean resultB = b.eval(pArgs);
      if (resultA == MaybeBoolean.MAYBE || resultB == MaybeBoolean.MAYBE) {
        return MaybeBoolean.MAYBE;
      } else {
        return MaybeBoolean.valueOf(resultA.equals(resultB));
      }
    }
  }
  /**
   * Boolean !=
   * @author rhein
   */
    static class BoolNotEqTest extends ObserverBoolExpr {
      ObserverBoolExpr a, b;
      public BoolNotEqTest(ObserverBoolExpr pA, ObserverBoolExpr pB) {this.a = pA; this.b = pB;}
      @Override MaybeBoolean eval(ObserverExpressionArguments pArgs) { 
        MaybeBoolean resultA = a.eval(pArgs);
        MaybeBoolean resultB = b.eval(pArgs);
        if (resultA == MaybeBoolean.MAYBE || resultB == MaybeBoolean.MAYBE) {
          return MaybeBoolean.MAYBE;
        } else {
          return MaybeBoolean.valueOf(! resultA.equals(resultB));
        }
      }
    }
}
