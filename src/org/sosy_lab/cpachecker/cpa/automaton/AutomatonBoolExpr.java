/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.TrinaryEqualable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcherProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.SourceLocationMapper;
import org.sosy_lab.cpachecker.util.SourceLocationMapper.LocationDescriptor;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Implements a boolean expression that evaluates and returns a <code>MaybeBoolean</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 */
interface AutomatonBoolExpr extends AutomatonExpression, TrinaryEqualable {

  ResultValue<Boolean> CONST_TRUE = new ResultValue<>(Boolean.TRUE);
  ResultValue<Boolean> CONST_FALSE = new ResultValue<>(Boolean.FALSE);

  abstract class AbstractAutomatonBoolExpr implements AutomatonBoolExpr {

    @Override
    public Equality equalityTo(Object pOther) {
      return this.equals(pOther)
          ? Equality.EQUAL
          : Equality.UNKNOWN;
    }
  }

  @Override
  ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;

  final class MatchProgramExit extends AbstractAutomatonBoolExpr {

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getCfaEdge().getSuccessor().getNumLeavingEdges() == 0) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchProgramExit
          ? Equality.EQUAL
          : Equality.UNKNOWN; // Also other matches might match a program exit
    }

  }

  enum MatchProgramEntry implements AutomatonBoolExpr {

    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      CFANode predecessor = edge.getPredecessor();
      if (predecessor instanceof FunctionEntryNode
          && predecessor.getNumEnteringEdges() == 0) {
        return AutomatonBoolExpr.CONST_TRUE;
      }
      return AutomatonBoolExpr.CONST_FALSE;
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchProgramEntry
          ? Equality.EQUAL
          : Equality.UNKNOWN; // Also other matches might match a program exit
    }

  }

  /**
   * Implements a match on the label after the current CFAEdge.
   * The eval method returns false if there is no label following the CFAEdge.
   */
  final class MatchLabelExact extends AbstractAutomatonBoolExpr {

    private final String label;

    public MatchLabelExact(String pLabel) {
      label = checkNotNull(pLabel);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFANode successorNode = pArgs.getCfaEdge().getSuccessor();
      if (successorNode instanceof CLabelNode) {
        if (label.equals(((CLabelNode)successorNode).getLabel())) {
          return CONST_TRUE;
        } else {
          return CONST_FALSE;
        }
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchLabelExact
          ? (this.label.equals(((MatchLabelExact)pOther).label)
                ? Equality.EQUAL
                : Equality.UNEQUAL)
          : Equality.UNKNOWN; // Also other matchers might match a program exit
    }

    @Override
    public String toString() {
      return "MATCH LABEL \"" + label + "\"";
    }
  }

  /**
   * Implements a regex match on the label after the current CFAEdge.
   * The eval method returns false if there is no label following the CFAEdge.
   * (".*" in java-regex means "any characters")
   */
  final class MatchLabelRegEx extends AbstractAutomatonBoolExpr {

    private final Pattern pattern;

    public MatchLabelRegEx(String pPattern) {
      pattern = Pattern.compile(pPattern);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFANode successorNode = pArgs.getCfaEdge().getSuccessor();
      if (successorNode instanceof CLabelNode) {
        String label = ((CLabelNode)successorNode).getLabel();
        if (pattern.matcher(label).matches()) {
          return CONST_TRUE;
        } else {
          return CONST_FALSE;
        }
      } else {
        return CONST_FALSE;
        //return new ResultValue<>("cannot evaluate if the CFAEdge is not a CLabelNode", "MatchLabelRegEx.eval(..)");
      }
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchLabelRegEx
          ? (this.pattern.pattern().equals(((MatchLabelRegEx)pOther).pattern.pattern())
                ? Equality.EQUAL
                : Equality.UNKNOWN)
          : Equality.UNKNOWN; // Also other matchers might match a program exit
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
   */
  final class MatchCFAEdgeASTComparison extends AbstractAutomatonBoolExpr {

    public enum CallableMatchMode { NONE, CALL, RETURN }

    private final ASTMatcherProvider patternAST;
    private final CallableMatchMode callableMatchMode;

    public MatchCFAEdgeASTComparison(ASTMatcherProvider pAstMatcherProvider, CallableMatchMode pCallMatchMode) {
      this.patternAST = pAstMatcherProvider;
      this.callableMatchMode = pCallMatchMode;
    }

    private boolean isFunctionCall(CFAEdge pEdge) {

      if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        return true;
      }

      if (pEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
        if (pEdge instanceof AStatementEdge) {
          if (((AStatementEdge) pEdge).getStatement() instanceof AFunctionCall) {
            return true;
          }
        }
      }

      return false;
    }

    private boolean isReturnCall(final CFAEdge pCFAEdge) {
      if (pCFAEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        return true;
      }

      if (pCFAEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
        if (pCFAEdge instanceof AStatementEdge) {
          if (((AStatementEdge) pCFAEdge).getStatement() instanceof
              AFunctionCallAssignmentStatement) {
            return true;
          }
        }
      }

      return false;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws UnrecognizedCFAEdgeException {
      final Optional<?> ast;

      switch (callableMatchMode) {
      case CALL:
        if (isFunctionCall(pArgs.getCfaEdge())) {
          ast = pArgs.getCfaEdge().getRawAST();
        } else {
          return CONST_FALSE;
        }
        break;
      case RETURN:
        if (isReturnCall(pArgs.getCfaEdge())) {
          // Match the function summary edge of the call!!
          ast = pArgs.getCfaEdge().getRawAST();
        } else {
          return CONST_FALSE;
        }
        break;
      default:
        ast = pArgs.getCfaEdge().getRawAST();
      }

      if (ast.isPresent()) {
        if (!(ast.get() instanceof CAstNode)) {
          throw new UnrecognizedCFAEdgeException(pArgs.getCfaEdge());
        }
        // some edges do not have an AST node attached to them, e.g. BlankEdges
        if (patternAST.getMatcher().matches((CAstNode)ast.get(), pArgs)) {
          return CONST_TRUE;
        } else {
          return CONST_FALSE;
        }
      }
      return CONST_FALSE;
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchCFAEdgeASTComparison
          ? (this.patternAST.getPatternString().equals(((MatchCFAEdgeASTComparison)pOther).patternAST.getPatternString())
                ? Equality.EQUAL
                : Equality.UNKNOWN)
          : Equality.UNKNOWN; // Also other matchers might match the represented edge
    }

    @Override
    public String toString() {
      return "MATCH {" + patternAST + "}";
    }
  }


  final class MatchCFAEdgeRegEx extends AbstractAutomatonBoolExpr {

    private final Pattern pattern;

    public MatchCFAEdgeRegEx(String pPattern) {
      pattern = Pattern.compile(pPattern);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pattern.matcher(pArgs.getCfaEdge().getRawStatement()).matches()) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchCFAEdgeRegEx
          ? (this.pattern.pattern().equals(((MatchCFAEdgeRegEx)pOther).pattern.pattern())
                ? Equality.EQUAL
                : Equality.UNKNOWN)
          : Equality.UNKNOWN; // Also other matchers might match similar edges
    }

    @Override
    public String toString() {
      return "MATCH [" + pattern + "]";
    }
  }


  final class MatchCFAEdgeExact extends AbstractAutomatonBoolExpr {

    private final String pattern;

    public MatchCFAEdgeExact(String pPattern) {
      pattern = pPattern;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getCfaEdge().getRawStatement().equals(pattern)) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchCFAEdgeExact
          ? (this.pattern.equals(((MatchCFAEdgeExact)pOther).pattern)
                ? Equality.EQUAL
                : Equality.UNEQUAL)
          : Equality.UNKNOWN; // Also other matchers might match a similar set of edges
    }


    @Override
    public String toString() {
      return "MATCH \"" + pattern + "\"";
    }
  }

  final class MatchJavaAssert extends AbstractAutomatonBoolExpr {

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge instanceof BlankEdge && edge.getDescription().equals("assert fail")) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return "MATCH ASSERT";
    }
  }

  enum MatchAssumeEdge implements AutomatonBoolExpr {

    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return pArgs.getCfaEdge() instanceof AssumeEdge ? CONST_TRUE : CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH ASSUME EDGE";
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof MatchAssumeEdge
          ? Equality.EQUAL
          : Equality.UNKNOWN;
    }

  }

  class MatchAssumeCase extends AbstractAutomatonBoolExpr {

    private final boolean matchPositiveCase;

    public MatchAssumeCase(boolean pMatchPositiveCase) {
      matchPositiveCase = pMatchPositiveCase;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getCfaEdge() instanceof AssumeEdge) {
        AssumeEdge a = (AssumeEdge) pArgs.getCfaEdge();
        if (matchPositiveCase == a.getTruthAssumption()) {
          return CONST_TRUE;
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH ASSUME CASE " + matchPositiveCase;
    }
  }

  class MatchAllSuccessorEdgesBoolExpr extends AbstractAutomatonBoolExpr {

    private final AutomatonBoolExpr operandExpression;

    MatchAllSuccessorEdgesBoolExpr(AutomatonBoolExpr pOperandExpression) {
      Preconditions.checkNotNull(pOperandExpression);
      operandExpression = pOperandExpression;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      if (pArgs.getCfaEdge().getSuccessor().getNumLeavingEdges() == 0) {
        return CONST_TRUE;
      }
      ResultValue<Boolean> result = null;
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pArgs.getCfaEdge().getSuccessor())) {
        result = operandExpression.eval(
            new AutomatonExpressionArguments(
                pArgs.getState(),
                pArgs.getAutomatonVariables(),
                pArgs.getAbstractStates(),
                cfaEdge,
                pArgs.getLogger()));
        if (result.canNotEvaluate() || !result.getValue()) {
          return result;
        }
      }
      assert result != null;
      return result;
    }

    @Override
    public String toString() {
      return String.format("MATCH FORALL SUCCESSOR EDGES (%s)", operandExpression);
    }

  }

  class MatchAnySuccessorEdgesBoolExpr extends AbstractAutomatonBoolExpr {

    private final AutomatonBoolExpr operandExpression;

    MatchAnySuccessorEdgesBoolExpr(AutomatonBoolExpr pOperandExpression) {
      Preconditions.checkNotNull(pOperandExpression);
      operandExpression = pOperandExpression;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      if (pArgs.getCfaEdge().getSuccessor().getNumLeavingEdges() == 0) {
        return CONST_FALSE;
      }
      ResultValue<Boolean> result = null;
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pArgs.getCfaEdge().getSuccessor())) {
        result = operandExpression.eval(
            new AutomatonExpressionArguments(
                pArgs.getState(),
                pArgs.getAutomatonVariables(),
                pArgs.getAbstractStates(),
                cfaEdge,
                pArgs.getLogger()));
        if (!result.canNotEvaluate() && result.getValue()) {
          return result;
        }
      }
      assert result != null;
      return result;
    }

    @Override
    public String toString() {
      return String.format("MATCH EXISTS SUCCESSOR EDGE (%s)", operandExpression);
    }

  }

  interface OnRelevantEdgesBoolExpr extends AutomatonBoolExpr {

    // Marker interface

  }

  enum MatchPathRelevantEdgesBoolExpr implements OnRelevantEdgesBoolExpr {

    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return AutomatonGraphmlCommon.handleAsEpsilonEdge(pArgs.getCfaEdge()) ? CONST_FALSE : CONST_TRUE;
    }

    @Override
    public String toString() {
      return "MATCH PATH RELEVANT EDGE";
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return this.equals(pOther)
          ? Equality.EQUAL
          : Equality.UNKNOWN;
    }

  }

  class MatchLocationDescriptor extends AbstractAutomatonBoolExpr {

    private final LocationDescriptor matchDescriptor;

    public MatchLocationDescriptor(LocationDescriptor pOriginDescriptor) {
      Preconditions.checkNotNull(pOriginDescriptor);

      this.matchDescriptor = pOriginDescriptor;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return eval(pArgs.getCfaEdge()) ? CONST_TRUE : CONST_FALSE;
    }

    protected boolean eval(CFAEdge edge) {
      Set<FileLocation> fileLocs = SourceLocationMapper.getFileLocationsFromCfaEdge(edge);
      for (FileLocation l: fileLocs) {
        if (matchDescriptor.matches(l)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "MATCH " + matchDescriptor;
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return this.equals(pOther)
          ? Equality.EQUAL
          : Equality.UNKNOWN;
    }

  }

  /**
   * Sends a query string to all available AbstractStates.
   * Returns TRUE if one Element returned TRUE;
   * Returns FALSE if all Elements returned either FALSE or an InvalidQueryException.
   * Returns MAYBE if no Element is available or the Variables could not be replaced.
   */
  class ALLCPAQuery extends AbstractAutomatonBoolExpr {
    private final String queryString;

    public ALLCPAQuery(String pString) {
      queryString = pString;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getAbstractStates().isEmpty()) {
        return new ResultValue<>("No CPA elements available", "AutomatonBoolExpr.ALLCPAQuery");
      } else {
        // replace transition variables
        String modifiedQueryString = pArgs.replaceVariables(queryString);
        if (modifiedQueryString == null) {
          return new ResultValue<>("Failed to modify queryString \"" + queryString + "\"", "AutomatonBoolExpr.ALLCPAQuery");
        }
        for (AbstractState ae : pArgs.getAbstractStates()) {
          if (ae instanceof AbstractQueryableState) {
            AbstractQueryableState aqe = (AbstractQueryableState) ae;
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof Boolean) {
                if (((Boolean)result).booleanValue()) {
                  if (pArgs.getLogger().wouldBeLogged(Level.FINER)) {
                    String message = "CPA-Check succeeded: ModifiedCheckString: \"" +
                    modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                    aqe.toString() + "\"";
                    pArgs.getLogger().log(Level.FINER, message);
                  }
                  return CONST_TRUE;
                }
              }
            } catch (InvalidQueryException e) {
              // do nothing;
            }
          }
        }
        return CONST_FALSE;
      }
    }
  }
  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the query-Result.
   */
  class CPAQuery extends AbstractAutomatonBoolExpr {
    private final String cpaName;
    private final String queryString;

    public CPAQuery(String pCPAName, String pQuery) {
      cpaName = pCPAName;
      queryString = pQuery;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      // replace transition variables
      String modifiedQueryString = pArgs.replaceVariables(queryString);
      if (modifiedQueryString == null) {
        return new ResultValue<>("Failed to modify queryString \"" + queryString + "\"", "AutomatonBoolExpr.CPAQuery");
      }

      LogManager logger = pArgs.getLogger();
      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof Boolean) {
                if (((Boolean)result).booleanValue()) {
                  if (logger.wouldBeLogged(Level.FINER)) {
                    String message = "CPA-Check succeeded: ModifiedCheckString: \"" +
                    modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                    aqe.toString() + "\"";
                    logger.log(Level.FINER, message);
                  }
                  return CONST_TRUE;
                } else {
                  if (logger.wouldBeLogged(Level.FINER)) {
                    String message = "CPA-Check failed: ModifiedCheckString: \"" +
                    modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                    aqe.toString() + "\"";
                    logger.log(Level.FINER, message);
                  }
                  return CONST_FALSE;
                }
              } else {
                logger.log(Level.WARNING,
                    "Automaton got a non-Boolean value during Query of the "
                    + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
                    ". Assuming FALSE.");
                return CONST_FALSE;
              }
            } catch (InvalidQueryException e) {
              logger.logException(Level.WARNING, e,
                  "Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription());
              return CONST_FALSE;
            }
          }
        }
      }
      return new ResultValue<>("No State of CPA \"" + cpaName + "\" was found!", "AutomatonBoolExpr.CPAQuery");
    }

    @Override
    public String toString() {
      return "CHECK(" + cpaName + "(\"" + queryString + "\"))";
    }
  }

  enum CheckAllCpasForTargetState implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      if (pArgs.getAbstractStates().isEmpty()) {
        return new ResultValue<>("No CPA elements available", "AutomatonBoolExpr.CheckAllCpasForTargetState");
      } else {
        for (AbstractState ae : pArgs.getAbstractStates()) {
          if (AbstractStates.isTargetState(ae)) {
            return CONST_TRUE;
          }
        }
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return "CHECK(IS_TARGET_STATE)";
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return this.equals(pOther)
          ? Equality.EQUAL
          : Equality.UNKNOWN;
    }
  }

  /** Constant for true.
   */
  AutomatonBoolExpr TRUE = new AbstractAutomatonBoolExpr() {
    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return CONST_TRUE;
    }

    @Override
    public String toString() {
      return "TRUE";
    }
  };

  /** Constant for false.
   */
  AutomatonBoolExpr FALSE = new AbstractAutomatonBoolExpr() {
    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "FALSE";
    }
  };


  /** Tests the equality of the values of two instances of {@link AutomatonIntExpr}.
   */
  class IntEqTest extends AbstractAutomatonBoolExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public IntEqTest(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      ResultValue<Integer> resA = a.eval(pArgs);
      ResultValue<Integer> resB = b.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return new ResultValue<>(resA);
      }
      if (resB.canNotEvaluate()) {
        return new ResultValue<>(resB);
      }
      if (resA.getValue().equals(resB.getValue())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return a + " == " + b;
    }
  }


  /** Tests whether two instances of {@link AutomatonIntExpr} evaluate to different integers.
   */
  class IntNotEqTest extends AbstractAutomatonBoolExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public IntNotEqTest(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      ResultValue<Integer> resA = a.eval(pArgs);
      ResultValue<Integer> resB = b.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return new ResultValue<>(resA);
      }
      if (resB.canNotEvaluate()) {
        return new ResultValue<>(resB);
      }
      if (! resA.getValue().equals(resB.getValue())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return a + " != " + b;
    }
  }

  abstract class BinaryAutomatonBoolExpr extends AbstractAutomatonBoolExpr {

    protected final AutomatonBoolExpr a;
    protected final AutomatonBoolExpr b;

    public BinaryAutomatonBoolExpr(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    public AutomatonBoolExpr getA() {
      return a;
    }

    public AutomatonBoolExpr getB() {
      return b;
    }

    @Override
    public Equality equalityTo(Object pOther) {
      if (!(pOther instanceof BinaryAutomatonBoolExpr)) {
        return Equality.UNKNOWN;
      }

      BinaryAutomatonBoolExpr other = (BinaryAutomatonBoolExpr) pOther;

      if (other.a.equalityTo(this.a) != Equality.EQUAL) {
        return Equality.UNKNOWN;
      }

      if (other.b.equalityTo(this.b) != Equality.EQUAL) {
        return Equality.UNKNOWN;
      }

      return Equality.EQUAL;
    }
  }


  /** Computes the disjunction of two {@link AutomatonBoolExpr} (lazy evaluation).
   */
  final class Or extends BinaryAutomatonBoolExpr {

    public Or(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB);
    }

    public @Override ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      /* OR:
       * True  || _ -> True
       * _ || True -> True
       * false || false -> false
       * every other combination returns the result that can not evaluate
       */
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        ResultValue<Boolean> resB = b.eval(pArgs);
        if ((!resB.canNotEvaluate()) && resB.getValue().equals(Boolean.TRUE)) {
          return resB;
        } else {
          return resA;
        }
      } else {
        if (resA.getValue().equals(Boolean.TRUE)) {
          return resA;
        } else {
          ResultValue<Boolean> resB = b.eval(pArgs);
          if (resB.canNotEvaluate()) {
            return resB;
          }
          if (resB.getValue().equals(Boolean.TRUE)) {
            return resB;
          } else {
            return resA;
          }
        }
      }
    }

    @Override
    public String toString() {
      return "(" + a + " || " + b + ")";
    }

  }


  /** Computes the conjunction of two {@link AutomatonBoolExpr} (lazy evaluation).
   */
  final class And extends BinaryAutomatonBoolExpr {

    public And(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      /* AND:
       * false && _ -> false
       * _ && false -> false
       * true && true -> true
       * every other combination returns the result that can not evaluate
       */
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        ResultValue<Boolean> resB = b.eval(pArgs);
        if ((! resB.canNotEvaluate()) && resB.getValue().equals(Boolean.FALSE)) {
          return resB;
        } else {
          return resA;
        }
      } else {
        if (resA.getValue().equals(Boolean.FALSE)) {
          return resA;
        } else {
          ResultValue<Boolean> resB = b.eval(pArgs);
          if (resB.canNotEvaluate()) {
            return resB;
          }
          if (resB.getValue().equals(Boolean.FALSE)) {
            return resB;
          } else {
            return resA;
          }
        }
      }
    }

    @Override
    public String toString() {
      return "(" + a + " && " + b + ")";
    }

  }


  /**
   * Negates the result of a {@link AutomatonBoolExpr}. If the result is MAYBE it is returned unchanged.
   */
  final class Negation extends AbstractAutomatonBoolExpr {

    private final AutomatonBoolExpr a;

    public Negation(AutomatonBoolExpr pA) {
      this.a = pA;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      if (resA.getValue().equals(Boolean.TRUE)) {
        return CONST_FALSE;
      } else {
        return CONST_TRUE;
      }
    }

    @Override
    public Equality equalityTo(Object pOther) {
      return pOther instanceof Negation
          ? this.a.equalityTo(((Negation) pOther).a)
          : Equality.UNKNOWN;
    }

    @Override
    public String toString() {
      return "!" + a;
    }

    public AutomatonBoolExpr getA() {
      return a;
    }
  }


  /**
   * Boolean Equality
   */
  final class BoolEqTest extends BinaryAutomatonBoolExpr {

    public BoolEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      ResultValue<Boolean> resB = b.eval(pArgs);
      if (resB.canNotEvaluate()) {
        return resB;
      }
      if (resA.getValue().equals(resB.getValue())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return a + " == " + b;
    }
  }


  /**
   * Boolean !=
   */
  final class BoolNotEqTest extends BinaryAutomatonBoolExpr {

    public BoolNotEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      ResultValue<Boolean> resB = b.eval(pArgs);
      if (resB.canNotEvaluate()) {
        return resB;
      }
      if (! resA.getValue().equals(resB.getValue())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return a + " != " + b;
    }
  }
}
