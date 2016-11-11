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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
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

/**
 * Implements a boolean expression that evaluates and returns a <code>MaybeBoolean</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 */
interface AutomatonBoolExpr extends AutomatonExpression {
  static final ResultValue<Boolean> CONST_TRUE = new ResultValue<>(Boolean.TRUE);
  static final ResultValue<Boolean> CONST_FALSE = new ResultValue<>(Boolean.FALSE);

  @Override
  abstract ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;

  static enum MatchProgramExit implements AutomatonBoolExpr {

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

  static enum MatchProgramEntry implements AutomatonBoolExpr {

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
    public String toString() {
      return "PROGRAM-ENTRY";
    }

  }

  static enum MatchLoopStart implements AutomatonBoolExpr {
    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      CFANode successor = edge.getSuccessor();
      if (successor.isLoopStart()) {
        return AutomatonBoolExpr.CONST_TRUE;
      }
      return AutomatonBoolExpr.CONST_FALSE;
    }

    @Override
    public String toString() {
      return "LOOP-START";
    }
  }

  class MatchSuccessor implements AutomatonBoolExpr {

    private final Set<CFANode> acceptedNodes;

    private MatchSuccessor(Set<CFANode> pAcceptedNodes) {
      this.acceptedNodes = pAcceptedNodes;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      CFANode successor = edge.getSuccessor();
      if (acceptedNodes.contains(successor)) {
        return AutomatonBoolExpr.CONST_TRUE;
      }
      return AutomatonBoolExpr.CONST_FALSE;
    }

    @Override
    public String toString() {
      return "SUCCESSOR IN " + acceptedNodes;
    }

    static AutomatonBoolExpr of(CFANode pAcceptedNode) {
      return new MatchSuccessor(Collections.singleton(pAcceptedNode));
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
          this.evaluation = pEvaluation;
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
          return AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge) ? TraversalProcess.CONTINUE : TraversalProcess.SKIP;
        }

        @Override
        public TraversalProcess visitNode(CFANode pNode) {
          return continueAtBranching || pNode.getNumEnteringEdges() < 2 ? TraversalProcess.CONTINUE : TraversalProcess.SKIP;
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

    static AutomatonBoolExpr forwardEpsilonMatch(AutomatonBoolExpr pExpr, boolean pContinueAtBranching) {
      return of(pExpr, true, pContinueAtBranching);
    }

    static AutomatonBoolExpr backwardEpsilonMatch(AutomatonBoolExpr pExpr, boolean pContinueAtBranching) {
      return of(pExpr, false, pContinueAtBranching);
    }

    private static AutomatonBoolExpr of(AutomatonBoolExpr pExpr, boolean pForward, boolean pContinueAtBranching) {
      if (pExpr instanceof EpsilonMatch && ((EpsilonMatch) pExpr).forward == pForward) {
        return pExpr;
      }
      return new EpsilonMatch(pExpr, pForward, pContinueAtBranching);
    }

  }

  static class MatchFunctionCall implements AutomatonBoolExpr {

    private final String functionName;

    MatchFunctionCall(String pFunctionName) {
      this.functionName = pFunctionName;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge instanceof AStatementEdge) {
        AStatement statement = ((AStatementEdge) edge).getStatement();
        if (statement instanceof AFunctionCall) {
          AFunctionCall functionCall = (AFunctionCall) statement;
          AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          if (functionCallExpression.getFunctionNameExpression() instanceof AIdExpression) {
            AIdExpression idExpression = (AIdExpression) functionCallExpression.getFunctionNameExpression();
            if (idExpression.getName().equals(functionName)) {
              return CONST_TRUE;
            }
          }
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH FUNCTION CALL";
    }

  }

  /**
   * Implements a match on the label after the current CFAEdge.
   * The eval method returns false if there is no label following the CFAEdge.
   */
  static class MatchLabelExact implements AutomatonBoolExpr {

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
    public String toString() {
      return "MATCH LABEL \"" + label + "\"";
    }
  }
  /**
   * Implements a regex match on the label after the current CFAEdge.
   * The eval method returns false if there is no label following the CFAEdge.
   * (".*" in java-regex means "any characters")
   */
  static class MatchLabelRegEx implements AutomatonBoolExpr {

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
    public String toString() {
      return "MATCH LABEL [" + pattern + "]";
    }
  }


  /**
   * This is a efficient implementation of the ASTComparison (it caches the generated ASTs for the pattern).
   * It also displays error messages if the AST contains problems/errors.
   * The AST Comparison evaluates the pattern (coming from the Automaton Definition) and the C-Statement on the CFA Edge to ASTs and compares these with a Tree comparison algorithm.
   */
  static class MatchCFAEdgeASTComparison implements AutomatonBoolExpr {

    private final ASTMatcher patternAST;

    public MatchCFAEdgeASTComparison(ASTMatcher pPatternAST) {
      this.patternAST = pPatternAST;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws UnrecognizedCFAEdgeException {
      Optional<?> ast = pArgs.getCfaEdge().getRawAST();
      if (ast.isPresent()) {
        if (!(ast.get() instanceof CAstNode)) {
          throw new UnrecognizedCFAEdgeException(pArgs.getCfaEdge());
        }
        // some edges do not have an AST node attached to them, e.g. BlankEdges
        if (patternAST.matches((CAstNode)ast.get(), pArgs)) {
          return CONST_TRUE;
        } else {
          return CONST_FALSE;
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH {" + patternAST + "}";
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
  }

  static class MatchJavaAssert implements AutomatonBoolExpr {

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

  static enum MatchAssumeEdge implements AutomatonBoolExpr {

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

  static class MatchAllSuccessorEdgesBoolExpr implements AutomatonBoolExpr {

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

  static class MatchAnySuccessorEdgesBoolExpr implements AutomatonBoolExpr {

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

  static interface OnRelevantEdgesBoolExpr extends AutomatonBoolExpr {

    // Marker interface

  }

  static enum MatchPathRelevantEdgesBoolExpr implements OnRelevantEdgesBoolExpr {

    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return AutomatonGraphmlCommon.handleAsEpsilonEdge(pArgs.getCfaEdge()) ? CONST_FALSE : CONST_TRUE;
    }

    @Override
    public String toString() {
      return "MATCH PATH RELEVANT EDGE";
    }

  }

  static enum MatchSplitDeclaration implements AutomatonBoolExpr {

    INSTANCE;

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      CFAEdge edge = pArgs.getCfaEdge();
      if (edge instanceof ADeclarationEdge) {
        ADeclarationEdge declEdge = (ADeclarationEdge) edge;
        ADeclaration decl = declEdge.getDeclaration();
        if (decl instanceof AFunctionDeclaration) {
          return CONST_FALSE;
        } else if (decl instanceof CTypeDeclaration) {
          return CONST_FALSE;
        } else if (decl instanceof AVariableDeclaration) {
          AVariableDeclaration varDecl = (AVariableDeclaration) decl;
          CFANode successor = edge.getSuccessor();
          Iterator<CFAEdge> leavingEdges = CFAUtils.allLeavingEdges(successor).iterator();
          if (!leavingEdges.hasNext()) {
            return CONST_FALSE;
          }
          CFAEdge successorEdge = leavingEdges.next();
          if (leavingEdges.hasNext()) {
            return CONST_FALSE;
          }
          if (successorEdge instanceof AStatementEdge) {
            AStatementEdge statementEdge = (AStatementEdge) successorEdge;
            if (statementEdge.getFileLocation().equals(edge.getFileLocation())
                && statementEdge.getStatement() instanceof AAssignment) {
              AAssignment assignment = (AAssignment) statementEdge.getStatement();
              ALeftHandSide leftHandSide = assignment.getLeftHandSide();
              if (leftHandSide instanceof AIdExpression) {
                AIdExpression lhs = (AIdExpression) leftHandSide;
                if (lhs.getDeclaration() != null && lhs.getDeclaration().equals(varDecl)) {
                  return CONST_TRUE;
                }
              }
            }
          }
        }
      }
      return CONST_FALSE;
    }

    @Override
    public String toString() {
      return "MATCH SPLIT DECLARATION";
    }

  }

  static class MatchLocationDescriptor implements AutomatonBoolExpr {

    private final Predicate<FileLocation> matchDescriptor;

    public MatchLocationDescriptor(Predicate<FileLocation> pOriginDescriptor) {
      Preconditions.checkNotNull(pOriginDescriptor);

      this.matchDescriptor = pOriginDescriptor;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) {
      return eval(pArgs.getCfaEdge()) ? CONST_TRUE : CONST_FALSE;
    }

    protected boolean eval(CFAEdge edge) {
      return Iterables.any(CFAUtils.getFileLocationsFromCfaEdge(edge), matchDescriptor);
    }

    @Override
    public String toString() {
      return "MATCH " + matchDescriptor;
    }

  }

  /**
   * Sends a query string to all available AbstractStates.
   * Returns TRUE if one Element returned TRUE;
   * Returns FALSE if all Elements returned either FALSE or an InvalidQueryException.
   * Returns MAYBE if no Element is available or the Variables could not be replaced.
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
          return new ResultValue<>("Failed to modify queryString \"" + queryString + "\"", "AutomatonBoolExpr.ALLCPAQuery");
        }
        for (AbstractState ae : pArgs.getAbstractStates()) {
          if (ae instanceof AbstractQueryableState) {
            AbstractQueryableState aqe = (AbstractQueryableState) ae;
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof Boolean) {
                if (((Boolean)result).booleanValue()) {
                  pArgs
                      .getLogger()
                      .log(
                          Level.FINER,
                          "CPA-Check succeeded: ModifiedCheckString: \"%s\" CPAElement: (%s) \"%s\"",
                          modifiedQueryString,
                          aqe.getCPAName(),
                          aqe);
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

  static enum CheckAllCpasForTargetState implements AutomatonBoolExpr {
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
  }

  /** Constant for true.
   */
  static AutomatonBoolExpr TRUE = new AutomatonBoolExpr() {
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
  static AutomatonBoolExpr FALSE = new AutomatonBoolExpr() {
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
  static class IntEqTest implements AutomatonBoolExpr {

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
  static class IntNotEqTest implements AutomatonBoolExpr {

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


  /** Computes the disjunction of two {@link AutomatonBoolExpr} (lazy evaluation).
   */
  static class Or implements AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public Or(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
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

    public AutomatonBoolExpr getA() {
      return a;
    }

    public AutomatonBoolExpr getB() {
      return b;
    }
  }


  /** Computes the conjunction of two {@link AutomatonBoolExpr} (lazy evaluation).
   */
  static class And implements AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public And(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
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

    public AutomatonBoolExpr getA() {
      return a;
    }

    public AutomatonBoolExpr getB() {
      return b;
    }
  }


  /**
   * Negates the result of a {@link AutomatonBoolExpr}. If the result is MAYBE it is returned unchanged.
   */
  static class Negation implements AutomatonBoolExpr {

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
  static class BoolEqTest implements AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public BoolEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
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
  static class BoolNotEqTest implements AutomatonBoolExpr {

    private final AutomatonBoolExpr a;
    private final AutomatonBoolExpr b;

    public BoolNotEqTest(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
      this.a = pA;
      this.b = pB;
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
