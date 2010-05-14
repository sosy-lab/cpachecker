/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.util.logging.Level;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement.EvaluationReturnValue;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Implements a boolean expression that evaluates and returns a <code>MaybeBoolean</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 * @author rhein
 */
abstract class AutomatonBoolExpr {

  /**
   * @author rhein
   * This is a extension of the boolean data type. It also contains a dont-know-value (MAYBE).
   */
  static enum MaybeBoolean {TRUE, FALSE, MAYBE;
    static MaybeBoolean valueOf(boolean pBool) {
      return pBool ? TRUE : FALSE;
    }
  }

  private AutomatonBoolExpr() {} //nobody can use this

  public abstract MaybeBoolean eval(AutomatonExpressionArguments pArgs);


  /**
   * Implements a regex match on the label after the current CFAEdge.
   * The eval method returns false if there is no label following the CFAEdge.
   * (".*" in java-regex means "any characters")
   * @author rhein
   */
  static class MatchLabelRegEx extends AutomatonBoolExpr {

    private final Pattern pattern;

    public MatchLabelRegEx(String pPattern) {
      pattern = Pattern.compile(pPattern);
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      CFANode successorNode = pArgs.getCfaEdge().getSuccessor();
      if (successorNode instanceof CFALabelNode) {
        String label = ((CFALabelNode)successorNode).getLabel().toLowerCase();
        return MaybeBoolean.valueOf(pattern.matcher(label).matches());

      } else {
        return MaybeBoolean.FALSE;
      }
    }

    @Override
    public String toString() {
      return "MATCH LABEL [" + pattern + "]";
    }
  }


  /**
   * This is a efficient implementation of the ASTComparison (it caches the generated ASTs for the pattern).
   * It also displays error messages if the AST contains problems/errors.
   * The AST Comparison evaluates the pattern (coming from the Automaton Definition) and the C-Statement on the CFA Edge to ASTs and compares these with a Tree comparison algorithm.
   * @author rhein
   */
  static class MatchCFAEdgeASTComparison extends AutomatonBoolExpr {

    private final IASTNode patternAST;

    public MatchCFAEdgeASTComparison(String pPattern) throws InvalidAutomatonException {
      this.patternAST = AutomatonASTComparator.generatePatternAST(pPattern);
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {

      IASTNode ast = pArgs.getCfaEdge().getRawAST();
      //AutomatonASTComparator.printAST(ast);
      //AutomatonASTComparator.printAST(patternAST);
      if (ast != null) {
        // some edges do not have an AST node attached to them, e.g. BlankEdges
        return MaybeBoolean.valueOf(AutomatonASTComparator.compareASTs(ast, patternAST, pArgs));
      }

      return MaybeBoolean.FALSE;
    }

    @Override
    public String toString() {
      return "MATCH {" + patternAST.getRawSignature() + "}";
    }
  }


  static class MatchCFAEdgeRegEx extends AutomatonBoolExpr {

    private final Pattern pattern;

    public MatchCFAEdgeRegEx(String pPattern) {
      pattern = Pattern.compile(pPattern);
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      return MaybeBoolean.valueOf(
          pattern.matcher(pArgs.getCfaEdge().getRawStatement()).matches());
    }

    @Override
    public String toString() {
      return "MATCH [" + pattern + "]";
    }
  }


  static class MatchCFAEdgeExact extends AutomatonBoolExpr {

    private final String pattern;

    public MatchCFAEdgeExact(String pPattern) {
      pattern = pPattern;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      return MaybeBoolean.valueOf(
          pArgs.getCfaEdge().getRawStatement().equals(pattern));
    }

    @Override
    public String toString() {
      return "MATCH \"" + pattern + "\"";
    }
  }

  /**
   * Sends a query string to all available AbstractElements.
   * Returns TRUE if one Element returned TRUE;
   * Returns FALSE if all Elements returned either FALSE or an InvalidQueryException.
   * Returns MAYBE if no Element is available or the Variables could not be replaced.
   * @author rhein
   */
  public static class ALLCPAQuery extends AutomatonBoolExpr {
    private final String queryString;
    
    public ALLCPAQuery(String pString) {
      queryString = pString;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getAbstractElements().isEmpty()) {
        return MaybeBoolean.MAYBE;
      } else {
        // replace transition variables
        String modifiedQueryString = pArgs.replaceVariables(queryString);
        if (modifiedQueryString == null) {
          return MaybeBoolean.MAYBE;
        }
        for (AbstractElement ae : pArgs.getAbstractElements()) {
          if (ae instanceof AbstractQueryableElement) {
            AbstractQueryableElement aqe = (AbstractQueryableElement) ae;
            try {
              EvaluationReturnValue<? extends Object> result = aqe.evaluateProperty(modifiedQueryString);
              if (result.getValueType().equals(Boolean.class)) {
                if (((Boolean)result.getValue()).booleanValue()) {
                  String message = "CPA-Check succeeded: ModifiedCheckString: \"" + 
                  modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                  aqe.toString() + "\"";
                  pArgs.getLogger().log(Level.FINER, message);
                  return MaybeBoolean.TRUE;
                }
              }
            } catch (InvalidQueryException e) {
              // do nothing;
            }
          }
        }
        return MaybeBoolean.FALSE;
      }
    }
  }
  /**
   * Sends a query-String to an <code>AbstractElement</code> of another analysis and returns the query-Result.
   * @author rhein
   */
  static class CPAQuery extends AutomatonBoolExpr {
    private final String cpaName;
    private final String queryString;

    public CPAQuery(String pCPAName, String pQuery) {
      cpaName = pCPAName;
      queryString = pQuery;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      // replace transition variables
      String modifiedQueryString = pArgs.replaceVariables(queryString);
      if (modifiedQueryString == null) {
        return MaybeBoolean.MAYBE;
      }

      for (AbstractElement ae : pArgs.getAbstractElements()) {
        if (ae instanceof AbstractQueryableElement) {
          AbstractQueryableElement aqe = (AbstractQueryableElement) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              EvaluationReturnValue<? extends Object> result = aqe.evaluateProperty(modifiedQueryString);
              if (result.getValueType().equals(Boolean.class)) {
                if (((Boolean)result.getValue()).booleanValue()) {
                  String message = "CPA-Check succeeded: ModifiedCheckString: \"" + 
                  modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                  aqe.toString() + "\"";
                  pArgs.getLogger().log(Level.FINER, message);
                  return MaybeBoolean.TRUE;
                } else {
                  String message = "CPA-Check failed: ModifiedCheckString: \"" + 
                  modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                  aqe.toString() + "\"";
                  pArgs.getLogger().log(Level.FINER, message);
                  return MaybeBoolean.FALSE;
                }
              } else {
                pArgs.getLogger().log(Level.WARNING,
                    "Automaton got a non-Boolean value during Query of the "
                    + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getRawStatement() + 
                    ". Assuming FALSE.");
                return MaybeBoolean.FALSE;
              }
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e,
                  "Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getRawStatement());
              return MaybeBoolean.FALSE;
            }
          }
        }
      }
      return MaybeBoolean.MAYBE; // the necessary CPA-State was not found
    }
    
    @Override
    public String toString() {
      return "CHECK(" + cpaName + "(\"" + queryString + "\"))";
    }
  }


  /** Constant for true.
   * @author rhein
   */
  static class True extends AutomatonBoolExpr {
    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      return MaybeBoolean.TRUE;
    }

    @Override
    public String toString() {
      return "TRUE";
    }
  }


  /** Constant for false.
   * @author rhein
   */
  static class False extends AutomatonBoolExpr {
    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      return MaybeBoolean.FALSE;
    }

    @Override
    public String toString() {
      return "FALSE";
    }
  }


  /** Tests the equality of the values of two instances of {@link AutomatonIntExpr}.
   * @author rhein
   */
  static class IntEqTest extends AutomatonBoolExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public IntEqTest(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }
    
    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      if (! (a.canEvaluateOn(pArgs) && b.canEvaluateOn(pArgs))) {
        return MaybeBoolean.MAYBE;
      } else {
        return MaybeBoolean.valueOf(a.eval(pArgs) == b.eval(pArgs));
      }
    }

    @Override
    public String toString() {
      return a + " == " + b;
    }
  }


  /** Tests whether two instances of {@link AutomatonIntExpr} evaluate to different integers.
   * @author rhein
   */
  static class IntNotEqTest extends AutomatonBoolExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public IntNotEqTest(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    public @Override MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      if (! (a.canEvaluateOn(pArgs) && b.canEvaluateOn(pArgs))) {
        return MaybeBoolean.MAYBE;
      } else {
        return MaybeBoolean.valueOf(a.eval(pArgs) != b.eval(pArgs));
      }
    }

    @Override
    public String toString() {
      return a + " != " + b;
    }
  }


  /** Computes the disjunction of two {@link AutomatonBoolExpr} (lazy evaluation).
   * @author rhein
   */
  static class Or extends AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public Or(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    public @Override MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      /* OR:
       * True  || _ -> True
       * _ || True -> True
       * false || false -> false
       * every other combination returns MAYBE
       */
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

    @Override
    public String toString() {
      return "(" + a + " || " + b + ")";
    }
  }


  /** Computes the conjunction of two {@link AutomatonBoolExpr} (lazy evaluation).
   * @author rhein
   */
  static class And extends AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public And(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      /* AND:
       * false && _ -> false
       * _ && false -> false
       * true && true -> true
       * every other combination returns MAYBE
       */
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

    @Override
    public String toString() {
      return "(" + a + " && " + b + ")";
    }
  }


  /**
   * Negates the result of a {@link AutomatonBoolExpr}. If the result is MAYBE it is returned unchanged.
   * @author rhein
   */
  static class Negation extends AutomatonBoolExpr {

    private final AutomatonBoolExpr a;

    public Negation(AutomatonBoolExpr pA) {
      this.a = pA;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      MaybeBoolean resultA = a.eval(pArgs);
      switch (resultA) {
      case TRUE: return MaybeBoolean.FALSE;
      case FALSE: return MaybeBoolean.TRUE;
      default: return MaybeBoolean.MAYBE;
      }
    }

    @Override
    public String toString() {
      return "!" + a;
    }
  }


  /**
   * Boolean Equality
   * @author rhein
   */
  static class BoolEqTest extends AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public BoolEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      MaybeBoolean resultA = a.eval(pArgs);
      MaybeBoolean resultB = b.eval(pArgs);
      if (resultA == MaybeBoolean.MAYBE || resultB == MaybeBoolean.MAYBE) {
        return MaybeBoolean.MAYBE;
      } else {
        return MaybeBoolean.valueOf(resultA.equals(resultB));
      }
    }

    @Override
    public String toString() {
      return a + " == " + b;
    }
  }


  /**
   * Boolean !=
   * @author rhein
   */
  static class BoolNotEqTest extends AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public BoolNotEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public MaybeBoolean eval(AutomatonExpressionArguments pArgs) {
      MaybeBoolean resultA = a.eval(pArgs);
      MaybeBoolean resultB = b.eval(pArgs);
      if (resultA == MaybeBoolean.MAYBE || resultB == MaybeBoolean.MAYBE) {
        return MaybeBoolean.MAYBE;
      } else {
        return MaybeBoolean.valueOf(! resultA.equals(resultB));
      }
    }

    @Override
    public String toString() {
      return a + " != " + b;
    }
  }
}
