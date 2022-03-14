// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcher;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;

/**
 * Implements a boolean expression that evaluates and returns a <code>MaybeBoolean</code> value when
 * <code>eval()</code> is called. The Expression can be evaluated multiple times.
 */
interface AutomatonBoolExpr extends AutomatonExpression<Boolean> {
  ResultValue<Boolean> CONST_TRUE = new ResultValue<>(Boolean.TRUE);
  ResultValue<Boolean> CONST_FALSE = new ResultValue<>(Boolean.FALSE);

  @Override
  ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;

  enum MatchProgramExit implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getCfaEdge().getSuccessor().getNumLeavingEdges() == 0) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return "PROGRAM-EXIT";
    }
  }

  public static class CheckCoversLines implements AutomatonBoolExpr {
    private final ImmutableSet<Integer> linesToCover;

    public CheckCoversLines(Set<Integer> pSet) {
      linesToCover = ImmutableSet.copyOf(pSet);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getAbstractStates().isEmpty()) {
        return new ResultValue<>("No CPA elements available", "AutomatonBoolExpr.CheckCoversLines");
      }

      CFAEdge edge = pArgs.getCfaEdge();
      if (!CoverageData.coversLine(edge)) {
        return CONST_FALSE;
      }
      if (linesToCover.contains(edge.getFileLocation().getStartingLineInOrigin())) {
        return CONST_TRUE;
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "COVERS_LINES(" + Joiner.on(' ').join(linesToCover) + ")";
    }

    @Override
    public int hashCode() {
      return linesToCover.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof CheckCoversLines
          && linesToCover.equals(((CheckCoversLines) o).linesToCover);
    }
  }

  enum MatchProgramEntry implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFAEdge edge = pArgs.getCfaEdge();
      CFANode predecessor = edge.getPredecessor();
      if (predecessor instanceof FunctionEntryNode && predecessor.getNumEnteringEdges() == 0) {
        return CONST_TRUE;
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "PROGRAM-ENTRY";
    }
  }

  enum MatchLoopStart implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getCfaEdge().getSuccessor().isLoopStart()) {
        return CONST_TRUE;
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "LOOP-START";
    }
  }

  class MatchSuccessor implements AutomatonBoolExpr {

    private final ImmutableSet<CFANode> acceptedNodes;

    private MatchSuccessor(ImmutableSet<CFANode> pAcceptedNodes) {
      acceptedNodes = pAcceptedNodes;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (acceptedNodes.contains(pArgs.getCfaEdge().getSuccessor())) {
        return CONST_TRUE;
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "SUCCESSOR IN " + acceptedNodes;
    }

    @Override
    public int hashCode() {
      return acceptedNodes.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchSuccessor
          && acceptedNodes.equals(((MatchSuccessor) o).acceptedNodes);
    }

    static AutomatonBoolExpr of(CFANode pAcceptedNode) {
      return new MatchSuccessor(ImmutableSet.of(pAcceptedNode));
    }

    static AutomatonBoolExpr of(Set<CFANode> pAcceptedNodes) {
      return new MatchSuccessor(ImmutableSet.copyOf(pAcceptedNodes));
    }
  }

  class EpsilonMatch implements AutomatonBoolExpr {

    private final AutomatonBoolExpr expr;

    private final boolean forward;

    private final boolean continueAtBranching;

    private EpsilonMatch(AutomatonBoolExpr pExpr, boolean pForward, boolean pContinueAtBranching) {
      Preconditions.checkArgument(!(pExpr instanceof EpsilonMatch));
      expr = Objects.requireNonNull(pExpr);
      forward = pForward;
      continueAtBranching = pContinueAtBranching;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      ResultValue<Boolean> eval = expr.eval(pArgs);
      if (Boolean.TRUE.equals(eval.getValue())) {
        return eval;
      }
      CFAEdge edge = pArgs.getCfaEdge();
      CFATraversal traversal = CFATraversal.dfs().ignoreSummaryEdges();
      final CFANode startNode;
      if (forward) {
        startNode = edge.getSuccessor();
      } else {
        traversal = traversal.backwards();
        startNode = edge.getPredecessor();
      }
      class EpsilonMatchVisitor implements CFAVisitor {

        private ResultValue<Boolean> evaluation;

        private CPATransferException transferException;

        public EpsilonMatchVisitor(ResultValue<Boolean> pEvaluation) {
          evaluation = pEvaluation;
        }

        @Override
        public TraversalProcess visitEdge(CFAEdge pEdge) {
          AutomatonExpressionArguments args =
              new AutomatonExpressionArguments(
                  pArgs.getState(),
                  pArgs.getAutomatonVariables(),
                  pArgs.getAbstractStates(),
                  pEdge,
                  pArgs.getLogger());
          try {
            evaluation = expr.eval(args);
          } catch (CPATransferException e) {
            transferException = e;
            return TraversalProcess.ABORT;
          }
          if (Boolean.TRUE.equals(evaluation.getValue())) {
            return TraversalProcess.ABORT;
          }
          return AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge)
              ? TraversalProcess.CONTINUE
              : TraversalProcess.SKIP;
        }

        @Override
        public TraversalProcess visitNode(CFANode pNode) {
          if (continueAtBranching) {
            return TraversalProcess.CONTINUE;
          }
          if (forward && pNode.getNumLeavingEdges() < 2) {
            return TraversalProcess.CONTINUE;
          } else if (!forward && pNode.getNumEnteringEdges() < 2) {
            return TraversalProcess.CONTINUE;
          }
          return TraversalProcess.SKIP;
        }
      }
      EpsilonMatchVisitor epsilonMatchVisitor = new EpsilonMatchVisitor(eval);
      traversal.traverse(startNode, epsilonMatchVisitor);
      if (epsilonMatchVisitor.transferException != null) {
        throw epsilonMatchVisitor.transferException;
      }
      return epsilonMatchVisitor.evaluation;
    }

    @Override
    public String toString() {
      if (!continueAtBranching) {
        return (forward ? "~>" : "<~") + expr;
      }
      return (forward ? "~>>" : "<<~") + expr;
    }

    @Override
    public int hashCode() {
      return Objects.hash(expr, forward, continueAtBranching);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof EpsilonMatch) {
        EpsilonMatch other = (EpsilonMatch) o;
        return expr.equals(other.expr)
            && forward == other.forward
            && continueAtBranching == other.continueAtBranching;
      }
      return false;
    }

    static AutomatonBoolExpr forwardEpsilonMatch(
        AutomatonBoolExpr pExpr, boolean pContinueAtBranching) {
      return of(pExpr, true, pContinueAtBranching);
    }

    static AutomatonBoolExpr backwardEpsilonMatch(
        AutomatonBoolExpr pExpr, boolean pContinueAtBranching) {
      return of(pExpr, false, pContinueAtBranching);
    }

    private static AutomatonBoolExpr of(
        AutomatonBoolExpr pExpr, boolean pForward, boolean pContinueAtBranching) {
      if (pExpr instanceof EpsilonMatch && ((EpsilonMatch) pExpr).forward == pForward) {
        return pExpr;
      }
      return new EpsilonMatch(pExpr, pForward, pContinueAtBranching);
    }
  }

  static class MatchFunctionCallStatement implements AutomatonBoolExpr {

    private final String functionName;

    MatchFunctionCallStatement(String pFunctionName) {
      functionName = pFunctionName;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge instanceof AStatementEdge) {
        AStatement statement = ((AStatementEdge) edge).getStatement();
        if (statement instanceof AFunctionCall) {
          AFunctionCall functionCall = (AFunctionCall) statement;
          AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          if (functionCallExpression.getFunctionNameExpression() instanceof AIdExpression) {
            AIdExpression idExpression =
                (AIdExpression) functionCallExpression.getFunctionNameExpression();
            if (idExpression.getDeclaration().getOrigName().equals(functionName)) {
              return CONST_TRUE;
            }
          }
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH FUNCTION CALL STATEMENT \"" + functionName + "\"";
    }

    @Override
    public int hashCode() {
      return functionName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchFunctionCallStatement
          && functionName.equals(((MatchFunctionCallStatement) o).functionName);
    }
  }

  static class MatchFunctionCall implements AutomatonBoolExpr {

    private final String functionName;

    MatchFunctionCall(String pFunctionName) {
      functionName = pFunctionName;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFAEdge edge = pArgs.getCfaEdge();

      // check cases like direct function calls and main-entry.
      if (edge.getSuccessor().getFunction().getOrigName().equals(functionName)) {
        if (edge instanceof FunctionCallEdge || AutomatonGraphmlCommon.isMainFunctionEntry(edge)) {
          return CONST_TRUE;
        }
      }

      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH FUNCTIONCALL \"" + functionName + "\"";
    }

    @Override
    public int hashCode() {
      return functionName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchFunctionCall
          && functionName.equals(((MatchFunctionCall) o).functionName);
    }
  }

  static class MatchFunctionPointerAssumeCase implements AutomatonBoolExpr {

    private final MatchAssumeCase matchAssumeCase;

    private final MatchFunctionCall matchFunctionCall;

    public MatchFunctionPointerAssumeCase(
        MatchAssumeCase pMatchAssumeCase, MatchFunctionCall pMatchFunctionCall) {
      matchAssumeCase = pMatchAssumeCase;
      matchFunctionCall = pMatchFunctionCall;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      ResultValue<Boolean> assumeMatches = matchAssumeCase.eval(pArgs);
      if (assumeMatches.canNotEvaluate() || !assumeMatches.getValue()) {
        return assumeMatches;
      }
      CFAEdge edge = pArgs.getCfaEdge();
      AssumeEdge assumeEdge = (AssumeEdge) edge;
      if (!assumeEdge.getTruthAssumption()) {
        assumeEdge = CFAUtils.getComplimentaryAssumeEdge(assumeEdge);
      }
      FluentIterable<FunctionCallEdge> pointerCallEdges =
          CFAUtils.leavingEdges(assumeEdge.getSuccessor())
              .filter(e -> e.getFileLocation().equals(edge.getFileLocation()))
              .filter(FunctionCallEdge.class);
      for (CFAEdge pointerCallEdge : pointerCallEdges) {
        AutomatonExpressionArguments args =
            new AutomatonExpressionArguments(
                pArgs.getState(),
                pArgs.getAutomatonVariables(),
                pArgs.getAbstractStates(),
                pointerCallEdge,
                pArgs.getLogger());
        return matchFunctionCall.eval(args);
      }
      return CONST_FALSE;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof MatchFunctionPointerAssumeCase) {
        MatchFunctionPointerAssumeCase other = (MatchFunctionPointerAssumeCase) pOther;
        return matchAssumeCase.equals(other.matchAssumeCase)
            && matchFunctionCall.equals(other.matchFunctionCall);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(matchAssumeCase, matchFunctionCall);
    }

    @Override
    public String toString() {
      return "MATCH FP-CALL("
          + matchFunctionCall.functionName
          + ") BRANCHING CASE "
          + matchAssumeCase.matchPositiveCase;
    }
  }

  static class MatchFunctionExit implements AutomatonBoolExpr {

    private final String functionName;

    MatchFunctionExit(String pFunctionName) {
      functionName = pFunctionName;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge instanceof FunctionReturnEdge) {
        FunctionReturnEdge returnEdge = (FunctionReturnEdge) edge;
        if (returnEdge.getPredecessor().getFunction().getOrigName().equals(functionName)) {
          return CONST_TRUE;
        }
      } else if (edge instanceof AReturnStatementEdge) {
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getSuccessor().getFunction().getOrigName().equals(functionName)) {
          return CONST_TRUE;
        }
      } else if (edge instanceof BlankEdge) {
        CFANode succ = edge.getSuccessor();
        if (succ instanceof FunctionExitNode
            && succ.getNumLeavingEdges() == 0
            && succ.getFunction().getOrigName().equals(functionName)) {
          assert "default return".equals(edge.getDescription());
          return CONST_TRUE;
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH FUNCTION EXIT \"" + functionName + "\"";
    }

    @Override
    public int hashCode() {
      return functionName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchFunctionExit
          && functionName.equals(((MatchFunctionExit) o).functionName);
    }
  }

  /**
   * Implements a match on the label after the current CFAEdge. The eval method returns false if
   * there is no label following the CFAEdge.
   */
  static class MatchLabelExact implements AutomatonBoolExpr {

    private final String label;

    public MatchLabelExact(String pLabel) {
      label = checkNotNull(pLabel);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFANode successorNode = pArgs.getCfaEdge().getSuccessor();
      if (successorNode instanceof CFALabelNode
          && label.equals(((CFALabelNode) successorNode).getLabel())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return "MATCH LABEL \"" + label + "\"";
    }

    @Override
    public int hashCode() {
      return label.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchLabelExact && label.equals(((MatchLabelExact) o).label);
    }
  }

  /**
   * Implements a regex match on the label after the current CFAEdge. The eval method returns false
   * if there is no label following the CFAEdge. (".*" in java-regex means "any characters")
   */
  static class MatchLabelRegEx implements AutomatonBoolExpr {

    private final Pattern pattern;

    public MatchLabelRegEx(String pPattern) {
      pattern = Pattern.compile(pPattern);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFANode successorNode = pArgs.getCfaEdge().getSuccessor();
      if (successorNode instanceof CFALabelNode) {
        String label = ((CFALabelNode) successorNode).getLabel();
        if (pattern.matcher(label).matches()) {
          return CONST_TRUE;
        } else {
          return CONST_FALSE;
        }
      } else {
        return CONST_FALSE;
        // return new ResultValue<>("cannot evaluate if the CFAEdge is not a CLabelNode",
        // "MatchLabelRegEx.eval(..)");
      }
    }

    @Override
    public String toString() {
      return "MATCH LABEL [" + pattern + "]";
    }

    @Override
    public int hashCode() {
      return pattern.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchLabelRegEx && pattern.equals(((MatchLabelRegEx) o).pattern);
    }
  }

  /**
   * This is a efficient implementation of the ASTComparison (it caches the generated ASTs for the
   * pattern). It also displays error messages if the AST contains problems/errors. The AST
   * Comparison evaluates the pattern (coming from the Automaton Definition) and the C-Statement on
   * the CFA Edge to ASTs and compares these with a Tree comparison algorithm.
   */
  static class MatchCFAEdgeASTComparison implements AutomatonBoolExpr {

    private final ASTMatcher patternAST;

    public MatchCFAEdgeASTComparison(ASTMatcher pPatternAST) {
      patternAST = pPatternAST;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws UnrecognizedCFAEdgeException {
      Optional<?> ast = Optional.empty();
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        // Ignore this edge, FunctionReturnEdge will be taken instead.
        return CONST_FALSE;
      } else if (edge.getEdgeType().equals(CFAEdgeType.FunctionReturnEdge)) {
        ast = Optional.of(((FunctionReturnEdge) edge).getSummaryEdge().getExpression());
      } else {
        ast = edge.getRawAST();
      }
      if (ast.isPresent()) {
        if (!(ast.get() instanceof CAstNode)) {
          throw new UnrecognizedCFAEdgeException(pArgs.getCfaEdge());
        }
        // some edges do not have an AST node attached to them, e.g. BlankEdges
        if (patternAST.matches((CAstNode) ast.get(), pArgs)) {
          return CONST_TRUE;
        } else {
          return CONST_FALSE;
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH {"
          + patternAST.toString().replaceAll(AutomatonASTComparator.JOKER_EXPR + "\\d+", "\\$?")
          + "}";
    }
  }

  static class MatchCFAEdgeRegEx implements AutomatonBoolExpr {

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
    public String toString() {
      return "MATCH [" + pattern + "]";
    }

    @Override
    public int hashCode() {
      return pattern.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchCFAEdgeRegEx && pattern.equals(((MatchCFAEdgeRegEx) o).pattern);
    }
  }

  static class MatchCFAEdgeNodes implements AutomatonBoolExpr {

    private final int predecessorNodeNumber;
    private final int successorNodeNumber;

    public MatchCFAEdgeNodes(CFAEdge pEdge) {
      this(pEdge.getPredecessor().getNodeNumber(), pEdge.getSuccessor().getNodeNumber());
    }

    public MatchCFAEdgeNodes(int pPredecessorNodeNumber, int pSuccessorNodeNumber) {
      predecessorNodeNumber = pPredecessorNodeNumber;
      successorNodeNumber = pSuccessorNodeNumber;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (predecessorNodeNumber == pArgs.getCfaEdge().getPredecessor().getNodeNumber()
          && successorNodeNumber == pArgs.getCfaEdge().getSuccessor().getNodeNumber()) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return "MATCH TRANSITION [" + predecessorNodeNumber + " -> " + successorNodeNumber + "]";
    }

    @Override
    public int hashCode() {
      return Objects.hash(predecessorNodeNumber, successorNodeNumber);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MatchCFAEdgeNodes
          && predecessorNodeNumber == ((MatchCFAEdgeNodes) obj).predecessorNodeNumber
          && successorNodeNumber == ((MatchCFAEdgeNodes) obj).successorNodeNumber;
    }
  }

  static class MatchCFAEdgeExact implements AutomatonBoolExpr {

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
    public String toString() {
      return "MATCH \"" + pattern + "\"";
    }

    @Override
    public int hashCode() {
      return pattern.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchCFAEdgeExact && pattern.equals(((MatchCFAEdgeExact) o).pattern);
    }
  }

  enum MatchJavaAssert implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
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
  }

  static class MatchAssumeCase implements AutomatonBoolExpr {

    private final boolean matchPositiveCase;

    public MatchAssumeCase(boolean pMatchPositiveCase) {
      matchPositiveCase = pMatchPositiveCase;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge instanceof AssumeEdge) {
        AssumeEdge a = (AssumeEdge) pArgs.getCfaEdge();
        boolean actualBranchInSource = a.getTruthAssumption() != a.isSwapped();
        if (matchPositiveCase == actualBranchInSource) {
          return CONST_TRUE;
        }
      }
      if (matchPositiveCase && AutomatonGraphmlCommon.treatAsWhileTrue(edge)) {
        return CONST_TRUE;
      }

      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH ASSUME CASE " + matchPositiveCase;
    }

    @Override
    public int hashCode() {
      return matchPositiveCase ? 1 : 0;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchAssumeCase
          && matchPositiveCase == ((MatchAssumeCase) o).matchPositiveCase;
    }
  }

  static class MatchAllSuccessorEdgesBoolExpr implements AutomatonBoolExpr {

    private final AutomatonBoolExpr operandExpression;

    MatchAllSuccessorEdgesBoolExpr(AutomatonBoolExpr pOperandExpression) {
      Preconditions.checkNotNull(pOperandExpression);
      operandExpression = pOperandExpression;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      if (pArgs.getCfaEdge().getSuccessor().getNumLeavingEdges() == 0) {
        return CONST_TRUE;
      }
      ResultValue<Boolean> result = null;
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pArgs.getCfaEdge().getSuccessor())) {
        result =
            operandExpression.eval(
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

    @Override
    public int hashCode() {
      return operandExpression.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchAllSuccessorEdgesBoolExpr
          && operandExpression.equals(((MatchAllSuccessorEdgesBoolExpr) o).operandExpression);
    }
  }

  static class MatchAnySuccessorEdgesBoolExpr implements AutomatonBoolExpr {

    private final AutomatonBoolExpr operandExpression;

    MatchAnySuccessorEdgesBoolExpr(AutomatonBoolExpr pOperandExpression) {
      Preconditions.checkNotNull(pOperandExpression);
      operandExpression = pOperandExpression;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      Iterable<CFAEdge> leavingEdges = CFAUtils.leavingEdges(edge.getSuccessor());
      if (edge instanceof FunctionCallEdge) {
        FunctionCallEdge callEdge = (FunctionCallEdge) edge;
        if (callEdge.getSummaryEdge() != null) {
          FunctionSummaryEdge summaryEdge = callEdge.getSummaryEdge();
          AFunctionCall call = summaryEdge.getExpression();
          if (call instanceof AFunctionCallAssignmentStatement) {
            Iterable<? extends CFAEdge> potentialFurtherMatches =
                CFAUtils.enteringEdges(summaryEdge.getSuccessor())
                    .filter(
                        e ->
                            (e instanceof AStatementEdge
                                    && call.equals(((AStatementEdge) e).getStatement()))
                                || (e instanceof FunctionReturnEdge
                                    && summaryEdge.equals(
                                        ((FunctionReturnEdge) e).getSummaryEdge())));
            leavingEdges = Iterables.concat(leavingEdges, potentialFurtherMatches);
          }
        }
      }
      if (Iterables.isEmpty(leavingEdges)) {
        return CONST_FALSE;
      }

      leavingEdges = skipSplitDeclarationEdges(leavingEdges, pArgs);

      ResultValue<Boolean> result = null;
      for (CFAEdge successorEdge : leavingEdges) {
        result =
            operandExpression.eval(
                new AutomatonExpressionArguments(
                    pArgs.getState(),
                    pArgs.getAutomatonVariables(),
                    pArgs.getAbstractStates(),
                    successorEdge,
                    pArgs.getLogger()));
        if (!result.canNotEvaluate() && result.getValue()) {
          return result;
        }
      }
      assert result != null;
      return result;
    }

    private Collection<CFAEdge> skipSplitDeclarationEdges(
        Iterable<CFAEdge> pEdges, AutomatonExpressionArguments pArgs) throws CPATransferException {
      List<CFAEdge> edges = new ArrayList<>();
      for (CFAEdge edge : pEdges) {
        if (CONST_TRUE.equals(
            MatchSplitDeclaration.INSTANCE.eval(
                new AutomatonExpressionArguments(
                    pArgs.getState(),
                    pArgs.getAutomatonVariables(),
                    pArgs.getAbstractStates(),
                    edge,
                    pArgs.getLogger())))) {
          edges.addAll(
              skipSplitDeclarationEdges(CFAUtils.leavingEdges(edge.getSuccessor()), pArgs));
        } else {
          edges.add(edge);
        }
      }
      return edges;
    }

    @Override
    public String toString() {
      return String.format("MATCH EXISTS SUCCESSOR EDGE (%s)", operandExpression);
    }

    @Override
    public int hashCode() {
      return operandExpression.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MatchAnySuccessorEdgesBoolExpr
          && operandExpression.equals(((MatchAnySuccessorEdgesBoolExpr) o).operandExpression);
    }
  }

  enum MatchSplitDeclaration implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      CFAEdge edge = pArgs.getCfaEdge();
      return AutomatonGraphmlCommon.isSplitDeclaration(edge) ? CONST_TRUE : CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH SPLIT DECLARATION";
    }
  }

  static class MatchLocationDescriptor implements AutomatonBoolExpr {

    private final FunctionEntryNode mainEntry;

    private final java.util.function.Predicate<FileLocation> matchDescriptor;

    public MatchLocationDescriptor(
        FunctionEntryNode pMainEntry, java.util.function.Predicate<FileLocation> pDescriptor) {
      Preconditions.checkNotNull(pDescriptor);

      mainEntry = pMainEntry;
      matchDescriptor = pDescriptor;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return eval(pArgs.getCfaEdge()) ? CONST_TRUE : CONST_FALSE;
    }

    protected boolean eval(CFAEdge edge) {
      return AutomatonGraphmlCommon.getFileLocationsFromCfaEdge(edge, mainEntry).stream()
          .anyMatch(matchDescriptor);
    }

    @Override
    public String toString() {
      return "MATCH " + matchDescriptor;
    }

    @Override
    public int hashCode() {
      return Objects.hash(mainEntry, matchDescriptor);
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof MatchLocationDescriptor) {
        MatchLocationDescriptor other = (MatchLocationDescriptor) pOther;
        return mainEntry.equals(other.mainEntry) && matchDescriptor.equals(other.matchDescriptor);
      }
      return false;
    }
  }

  /**
   * Sends a query string to all available AbstractStates. Returns TRUE if one Element returned
   * TRUE; Returns FALSE if all Elements returned either FALSE or an InvalidQueryException. Returns
   * MAYBE if no Element is available or the Variables could not be replaced.
   */
  public static class ALLCPAQuery implements AutomatonBoolExpr {
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
          return new ResultValue<>(
              "Failed to modify queryString \"" + queryString + "\"",
              "AutomatonBoolExpr.ALLCPAQuery");
        }
        int exceptionFreeCallCount = 0;
        for (AbstractState ae : pArgs.getAbstractStates()) {
          if (ae instanceof AbstractQueryableState) {
            exceptionFreeCallCount = exceptionFreeCallCount + 1;
            AbstractQueryableState aqe = (AbstractQueryableState) ae;
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof Boolean) {
                if ((Boolean) result) {
                  pArgs
                      .getLogger()
                      .log(
                          Level.FINER,
                          "CPA-Check succeeded: ModifiedCheckString: \"%s\" CPAElement: (%s)"
                              + " \"%s\"",
                          modifiedQueryString,
                          aqe.getCPAName(),
                          aqe);
                  return CONST_TRUE;
                }
              }
            } catch (InvalidQueryException e) {
              exceptionFreeCallCount = exceptionFreeCallCount - 1;
            }
          }
        }
        if (exceptionFreeCallCount == 0) {
          // No CPA feels responsible => returning CONST_FALSE would not be right here
          return new ResultValue<>(
              "None of the states sees \"" + modifiedQueryString + "\" as a valid query!",
              "AutomatonBoolExpr.ALLCPAQuery");
        } else {
          // At least one CPA considers the query valid, but none answered with true
          return CONST_FALSE;
        }
      }
    }

    @Override
    public String toString() {
      return "CHECK(\"" + queryString + "\")";
    }

    @Override
    public int hashCode() {
      return queryString.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ALLCPAQuery && queryString.equals(((ALLCPAQuery) o).queryString);
    }
  }

  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the
   * query-Result.
   */
  static class CPAQuery implements AutomatonBoolExpr {
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
        return new ResultValue<>(
            "Failed to modify queryString \"" + queryString + "\"", "AutomatonBoolExpr.CPAQuery");
      }

      LogManager logger = pArgs.getLogger();
      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof Boolean) {
                if ((Boolean) result) {
                  if (logger.wouldBeLogged(Level.FINER)) {
                    String message =
                        "CPA-Check succeeded: ModifiedCheckString: \""
                            + modifiedQueryString
                            + "\" CPAElement: ("
                            + aqe.getCPAName()
                            + ") \""
                            + aqe
                            + "\"";
                    logger.log(Level.FINER, message);
                  }
                  return CONST_TRUE;
                } else {
                  if (logger.wouldBeLogged(Level.FINER)) {
                    String message =
                        "CPA-Check failed: ModifiedCheckString: \""
                            + modifiedQueryString
                            + "\" CPAElement: ("
                            + aqe.getCPAName()
                            + ") \""
                            + aqe
                            + "\"";
                    logger.log(Level.FINER, message);
                  }
                  return CONST_FALSE;
                }
              } else {
                logger.log(
                    Level.WARNING,
                    "Automaton got a non-Boolean value during Query of the "
                        + cpaName
                        + " CPA on Edge "
                        + pArgs.getCfaEdge().getDescription()
                        + ". Assuming FALSE.");
                return CONST_FALSE;
              }
            } catch (InvalidQueryException e) {
              logger.logException(
                  Level.WARNING,
                  e,
                  "Automaton encountered an Exception during Query of the "
                      + cpaName
                      + " CPA on Edge "
                      + pArgs.getCfaEdge().getDescription());
              return CONST_FALSE;
            }
          }
        }
      }
      return new ResultValue<>(
          "No State of CPA \"" + cpaName + "\" was found!", "AutomatonBoolExpr.CPAQuery");
    }

    @Override
    public String toString() {
      return "CHECK(" + cpaName + ", \"" + queryString + "\")";
    }

    @Override
    public int hashCode() {
      return Objects.hash(cpaName, queryString);
    }

    @Override
    public boolean equals(Object pOther) {
      if (pOther instanceof CPAQuery) {
        CPAQuery other = (CPAQuery) pOther;
        return cpaName.equals(other.cpaName) && queryString.equals(other.queryString);
      }
      return false;
    }
  }

  enum CheckAllCpasForTargetState implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      if (pArgs.getAbstractStates().isEmpty()) {
        return new ResultValue<>(
            "No CPA elements available", "AutomatonBoolExpr.CheckAllCpasForTargetState");
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
  }

  /** Constant for true. */
  AutomatonBoolExpr TRUE =
      new AutomatonBoolExpr() {
        @Override
        public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
          return CONST_TRUE;
        }

        @Override
        public String toString() {
          return "TRUE";
        }
      };

  /** Constant for false. */
  AutomatonBoolExpr FALSE =
      new AutomatonBoolExpr() {
        @Override
        public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
          return CONST_FALSE;
        }

        @Override
        public String toString() {
          return "FALSE";
        }
      };

  abstract static class IntBinaryTest implements AutomatonBoolExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;
    private final BiFunction<Integer, Integer, Boolean> op;
    private final String repr;

    private IntBinaryTest(
        AutomatonIntExpr pA,
        AutomatonIntExpr pB,
        BiFunction<Integer, Integer, Boolean> pOp,
        String pRepr) {
      a = pA;
      b = pB;
      op = pOp;
      repr = pRepr;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      ResultValue<Integer> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return new ResultValue<>(resA);
      }
      ResultValue<Integer> resB = b.eval(pArgs);
      if (resB.canNotEvaluate()) {
        return new ResultValue<>(resB);
      }
      if (op.apply(resA.getValue(), resB.getValue())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return String.format("(%s %s %s)", a, repr, b);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, repr);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof IntBinaryTest) {
        IntBinaryTest other = (IntBinaryTest) o;
        return a.equals(other.a) && b.equals(other.b) && repr.equals(other.repr);
      }
      return false;
    }
  }

  /** Tests the equality of the values of two instances of {@link AutomatonIntExpr}. */
  static class IntEqTest extends IntBinaryTest {
    public IntEqTest(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      super(pA, pB, (a, b) -> a.equals(b), "==");
    }
  }

  /** Tests whether two instances of {@link AutomatonIntExpr} evaluate to different integers. */
  static class IntNotEqTest extends IntBinaryTest {
    public IntNotEqTest(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      super(pA, pB, (a, b) -> !a.equals(b), "!=");
    }
  }

  /** Computes the disjunction of two {@link AutomatonBoolExpr} (lazy evaluation). */
  static class Or extends BoolBinaryTest {
    public Or(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB, null, "||");
    }

    public @Override ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      /* OR:
       * True  || _ -> True
       * _ || True -> True
       * false || false -> false
       * every other combination returns the result that can not evaluate
       */
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        ResultValue<Boolean> resB = b.eval(pArgs);
        if (!resB.canNotEvaluate() && resB.getValue()) {
          return resB;
        } else {
          return resA;
        }
      } else {
        if (resA.getValue()) {
          return resA;
        } else {
          ResultValue<Boolean> resB = b.eval(pArgs);
          if (resB.canNotEvaluate()) {
            return resB;
          }
          if (resB.getValue()) {
            return resB;
          } else {
            return resA;
          }
        }
      }
    }
  }

  /** Computes the conjunction of two {@link AutomatonBoolExpr} (lazy evaluation). */
  static class And extends BoolBinaryTest {
    public And(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB, null, "&&");
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      /* AND:
       * false && _ -> false
       * _ && false -> false
       * true && true -> true
       * every other combination returns the result that can not evaluate
       */
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        ResultValue<Boolean> resB = b.eval(pArgs);
        if (!resB.canNotEvaluate() && !resB.getValue()) {
          return resB;
        } else {
          return resA;
        }
      } else {
        if (!resA.getValue()) {
          return resA;
        } else {
          ResultValue<Boolean> resB = b.eval(pArgs);
          if (resB.canNotEvaluate()) {
            return resB;
          }
          if (!resB.getValue()) {
            return resB;
          } else {
            return resA;
          }
        }
      }
    }
  }

  /**
   * Negates the result of a {@link AutomatonBoolExpr}. If the result is MAYBE it is returned
   * unchanged.
   */
  static class Negation implements AutomatonBoolExpr {

    private final AutomatonBoolExpr a;

    public Negation(AutomatonBoolExpr pA) {
      a = pA;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      if (resA.getValue()) {
        return CONST_FALSE;
      } else {
        return CONST_TRUE;
      }
    }

    @Override
    public String toString() {
      return "!" + a;
    }

    public AutomatonBoolExpr getA() {
      return a;
    }

    @Override
    public int hashCode() {
      return a.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Negation && a.equals(((Negation) o).a);
    }
  }

  abstract static class BoolBinaryTest implements AutomatonBoolExpr {

    protected final AutomatonBoolExpr a;
    protected final AutomatonBoolExpr b;
    private final String repr;

    // Operator can be NULL if overridden in subclass
    private final @Nullable BiFunction<Boolean, Boolean, Boolean> op;

    private BoolBinaryTest(
        AutomatonBoolExpr pA,
        AutomatonBoolExpr pB,
        BiFunction<Boolean, Boolean, Boolean> pOp,
        String pRepr) {
      a = pA;
      b = pB;
      op = pOp;
      repr = pRepr;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      ResultValue<Boolean> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      ResultValue<Boolean> resB = b.eval(pArgs);
      if (resB.canNotEvaluate()) {
        return resB;
      }
      if (op.apply(resA.getValue(), resB.getValue())) {
        return CONST_TRUE;
      } else {
        return CONST_FALSE;
      }
    }

    @Override
    public String toString() {
      return "(" + a + " " + repr + " " + b + ")";
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, repr);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof BoolBinaryTest) {
        BoolBinaryTest other = (BoolBinaryTest) o;
        return a.equals(other.a) && b.equals(other.b) && repr.equals(other.repr);
      }
      return false;
    }
  }

  /** Boolean Equality */
  static class BoolEqTest extends BoolBinaryTest {
    public BoolEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB, (a, b) -> a.equals(b), "==");
    }
  }

  /** Boolean != */
  static class BoolNotEqTest extends BoolBinaryTest {
    public BoolNotEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      super(pA, pB, (a, b) -> !a.equals(b), "!=");
    }
  }
}
