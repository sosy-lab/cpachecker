// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.AccessType;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.SideEffectKind;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class UnseqBehaviorAnalysisTransferRelation
    extends ForwardingTransferRelation<
        UnseqBehaviorAnalysisState, UnseqBehaviorAnalysisState, Precision> {

  private final LogManager logger;

  public UnseqBehaviorAnalysisTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleStatementEdge(
      CStatementEdge statementEdge, CStatement stat) throws UnrecognizedCodeException {

    UnseqBehaviorAnalysisState newState = Objects.requireNonNull(state);
    Map<String, Set<SideEffectInfo>> mergedSideEffects =
        deepCopySideEffects(newState.getSideEffectsInFun());
    Set<ConflictPair> mergedConflicts = new HashSet<>(newState.getDetectedConflicts());

    if (stat instanceof CExpressionAssignmentStatement exprAssign) {
      CExpression lhsExpr = exprAssign.getLeftHandSide();
      CExpression rhsExpr = exprAssign.getRightHandSide();

      // if functioncall true, then record side effects inside it
      if (lhsExpr instanceof CIdExpression||
          (lhsExpr instanceof CPointerExpression pointerExpr && pointerExpr.getOperand() instanceof CIdExpression)) {
        mergeSideEffects(
            mergedSideEffects,
            recordSideEffectsIfInFunctionCall(lhsExpr, statementEdge, AccessType.WRITE, newState));
        mergeSideEffects(
            mergedSideEffects,
            recordSideEffectsIfInFunctionCall(rhsExpr, statementEdge, AccessType.READ, newState));
      } else if (lhsExpr instanceof CPointerExpression pointerExpr) {

        logger.logf(
            Level.WARNING,
            """
            PointerCPA cannot resolve pointer arithmetic expressions like '%s' in statement '%s' (%s).
            As a result, the side effect will be recorded on the pointer base as if there were no offset.
            This may lead to incorrect alias resolution and missed or spurious conflict reports.
            """,
            pointerExpr.toASTString(),
            statementEdge.getCode(),
            statementEdge.getFileLocation());

        mergeSideEffects(
            mergedSideEffects,
            recordSideEffectsIfInFunctionCall(lhsExpr, statementEdge, AccessType.READ, newState));
        mergeSideEffects(
            mergedSideEffects,
            recordSideEffectsIfInFunctionCall(rhsExpr, statementEdge, AccessType.READ, newState));

        if (pointerExpr.getOperand()
            instanceof
            CBinaryExpression binaryExpr) { // to detect unseq behavior like *(f() + x) = 3;
          mergeConflicts(
              mergedConflicts,
              detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState));
        }

        // to detect unseq behavior like *f() = g(); and return *f() = g();
        mergeConflicts(
            mergedConflicts,
            detectAssignmentStatementConflicts(lhsExpr, rhsExpr, statementEdge, newState));
      }

      // check if there exists unsequenced behavior and cause conflict
      // to detect unseq behavior like y = (f() + g()) + x;
      if (rhsExpr instanceof CBinaryExpression binaryExpr) {
        mergeConflicts(
            mergedConflicts,
            detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState));
      }

    } else if (stat
        instanceof CExpressionStatement exStat) { // to detect unseq behavior like (f() + g()) + x;
      CExpression expr = exStat.getExpression();

      mergeSideEffects(
          mergedSideEffects,
          recordSideEffectsIfInFunctionCall(expr, statementEdge, AccessType.READ, newState));

      if (expr instanceof CBinaryExpression binaryExpr) {
        mergeConflicts(
            mergedConflicts,
            detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState));
      }
    }

    return new UnseqBehaviorAnalysisState(
        UnseqUtils.toImmutableSideEffectsMap(mergedSideEffects),
        newState.getCalledFunctionStack(),
        ImmutableSet.copyOf(mergedConflicts),
        newState.getTmpToOriginalExprMap(),
        this.logger);
  }

  @Override
  protected UnseqBehaviorAnalysisState handleDeclarationEdge(
      CDeclarationEdge declarationEdge, CDeclaration declaration) throws UnrecognizedCodeException {

    UnseqBehaviorAnalysisState newState = Objects.requireNonNull(state);
    Map<String, Set<SideEffectInfo>> mergedSideEffects =
        deepCopySideEffects(newState.getSideEffectsInFun());
    Set<ConflictPair> mergedConflicts = new HashSet<>(newState.getDetectedConflicts());

    if (declaration instanceof CVariableDeclaration varDecl) {
      if (varDecl.getInitializer() instanceof CInitializerExpression init) {
        CExpression initExpr = init.getExpression();
        // if functioncall true, then record side effects rhs
        mergeSideEffects(
            mergedSideEffects,
            recordSideEffectsIfInFunctionCall(
                initExpr, declarationEdge, AccessType.READ, newState));

        if (initExpr
            instanceof
            CBinaryExpression
                binaryExpr) { // to detect unseq behavior like int y = (f() + g()) + x;
          mergeConflicts(
              mergedConflicts,
              detectConflictsInUnsequencedBinaryExprs(binaryExpr, declarationEdge, newState));
        }
      }
    }

    return new UnseqBehaviorAnalysisState(
        UnseqUtils.toImmutableSideEffectsMap(mergedSideEffects),
        newState.getCalledFunctionStack(),
        ImmutableSet.copyOf(mergedConflicts),
        newState.getTmpToOriginalExprMap(),
        this.logger);
  }

  @Override
  protected UnseqBehaviorAnalysisState handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {

    UnseqBehaviorAnalysisState newState = Objects.requireNonNull(state);
    Map<String, Set<SideEffectInfo>> mergedSideEffects =
        deepCopySideEffects(newState.getSideEffectsInFun());
    Set<ConflictPair> mergedConflicts = new HashSet<>(newState.getDetectedConflicts());
    Deque<String> mergedFunctionCallStack = new ArrayDeque<>(state.getCalledFunctionStack());
    mergedFunctionCallStack.push(calledFunctionName);

    for (CExpression argument : arguments) {
      mergeSideEffects(
          mergedSideEffects,
          recordSideEffectsIfInFunctionCall(argument, callEdge, AccessType.READ, newState));
      // to detect unseq behavior inside single argument
      if (argument instanceof CBinaryExpression binaryExpr) {
        mergeConflicts(
            mergedConflicts,
            detectConflictsInUnsequencedBinaryExprs(binaryExpr, callEdge, newState));
      }
    }

    return new UnseqBehaviorAnalysisState(
        UnseqUtils.toImmutableSideEffectsMap(mergedSideEffects),
        ImmutableList.copyOf(mergedFunctionCallStack),
        ImmutableSet.copyOf(mergedConflicts),
        newState.getTmpToOriginalExprMap(),
        this.logger);
  }

  @Override
  protected UnseqBehaviorAnalysisState handleFunctionReturnEdge(
      CFunctionReturnEdge funReturnEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = Objects.requireNonNull(state);
    Set<ConflictPair> mergedConflicts = new HashSet<>(newState.getDetectedConflicts());
    Deque<String> mergedFunctionCallStack = new ArrayDeque<>(newState.getCalledFunctionStack());
    Map<String, CRightHandSide> mergedTmpToOriginalExprMap =
        new HashMap<>(newState.getTmpToOriginalExprMap());
    mergedFunctionCallStack.pop();

    ExpressionBehaviorVisitor visitor =
        new ExpressionBehaviorVisitor(newState, funReturnEdge, AccessType.READ, logger);

    if (summaryExpr instanceof CFunctionCallAssignmentStatement assignStmt) {
      CExpression lhs = assignStmt.getLeftHandSide();
      CFunctionCallExpression rhs = assignStmt.getRightHandSide();

      // to detect unseq behavior between arguments,like int c = foo(f1(), f2());
      // and return foo(f1(), f2());
      ExpressionAnalysisSummary summary = rhs.accept(visitor);
      mergeConflicts(
          mergedConflicts,
          detectCrossArgumentConflicts(
              summary.getSideEffectsPerSubExpr(), funReturnEdge, newState));

      if (lhs instanceof CIdExpression tmpVar) { // map tmp name and function name
        String tmpName = tmpVar.getDeclaration().getQualifiedName();
        String simpleName = tmpVar.getName();
        if (simpleName.startsWith("__CPAchecker_TMP_")) {
          mergedTmpToOriginalExprMap.put(tmpName, rhs);

          logger.logf(
              Level.INFO,
              "[TmpMapping] Map tmp variable '%s' to function call '%s' (Caller='%s')",
              tmpName,
              rhs.toQualifiedASTString(),
              callerFunctionName);
        }
      } else if (lhs
          instanceof
          CPointerExpression
              pointerExpr) { // to detect unseq behavior *f()=g(); and return *f()=g();
        mergeConflicts(
            mergedConflicts,
            detectAssignmentStatementConflicts(
                pointerExpr, summaryExpr.getFunctionCallExpression(), funReturnEdge, newState));
      }
    }
    return new UnseqBehaviorAnalysisState(
        newState.getSideEffectsInFun(),
        ImmutableList.copyOf(mergedFunctionCallStack),
        ImmutableSet.copyOf(mergedConflicts),
        ImmutableMap.copyOf(mergedTmpToOriginalExprMap),
        this.logger);
  }

  @Override
  protected UnseqBehaviorAnalysisState handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = Objects.requireNonNull(state);
    Map<String, Set<SideEffectInfo>> mergedSideEffects =
        deepCopySideEffects(newState.getSideEffectsInFun());
    Set<ConflictPair> mergedConflicts = new HashSet<>(newState.getDetectedConflicts());

    Optional<CExpression> expressionOptional = returnEdge.getExpression();

    if (expressionOptional.isPresent()) {
      CExpression returnExpr = expressionOptional.orElseThrow();
      mergeSideEffects(
          mergedSideEffects,
          recordSideEffectsIfInFunctionCall(returnExpr, returnEdge, AccessType.READ, newState));

      if (returnExpr
          instanceof
          CBinaryExpression
              returnBinExpr) { // to detect unseq behavior like return (f() + g()) + x;
        mergeConflicts(
            mergedConflicts,
            detectConflictsInUnsequencedBinaryExprs(returnBinExpr, returnEdge, newState));
      }
    }

    return new UnseqBehaviorAnalysisState(
        UnseqUtils.toImmutableSideEffectsMap(mergedSideEffects),
        newState.getCalledFunctionStack(),
        ImmutableSet.copyOf(mergedConflicts),
        newState.getTmpToOriginalExprMap(),
        this.logger);
  }

  @Override
  protected UnseqBehaviorAnalysisState handleAssumption(
      CAssumeEdge assumeEdge, CExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = Objects.requireNonNull(state);
    Map<String, Set<SideEffectInfo>> mergedSideEffects =
        deepCopySideEffects(newState.getSideEffectsInFun());
    Set<ConflictPair> mergedConflicts = new HashSet<>(newState.getDetectedConflicts());

    mergeSideEffects(
        mergedSideEffects,
        recordSideEffectsIfInFunctionCall(expression, assumeEdge, AccessType.READ, newState));

    if (expression
        instanceof
        CBinaryExpression binExpr) { // to detect unsequenced behavior like if (f()+g() != 0){}
      mergeConflicts(
          mergedConflicts, detectConflictsInUnsequencedBinaryExprs(binExpr, assumeEdge, newState));
    }

    return new UnseqBehaviorAnalysisState(
        UnseqUtils.toImmutableSideEffectsMap(mergedSideEffects),
        newState.getCalledFunctionStack(),
        ImmutableSet.copyOf(mergedConflicts),
        newState.getTmpToOriginalExprMap(),
        this.logger);
  }

  @Override
  protected UnseqBehaviorAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    return Objects.requireNonNull(state);
  }

  /**
   * Analyzes the given expression for side effects if currently inside a function call.
   *
   * <p>If any side effects are detected, they are associated with the current function and
   * propagated to all callers in the function call stack.
   *
   * @param expr the expression to analyze
   * @param edge the CFA edge where the expression appears
   * @param accessType the type of access (READ or WRITE)
   * @param pState the current abstract state
   * @return a new {@code Map<String, Set<SideEffectInfo>>} containing updated side-effect
   *     information
   * @throws UnrecognizedCodeException if the expression contains unsupported constructs
   */
  private Map<String, Set<SideEffectInfo>> recordSideEffectsIfInFunctionCall(
      CExpression expr, CFAEdge edge, AccessType accessType, UnseqBehaviorAnalysisState pState)
      throws UnrecognizedCodeException {

    Map<String, Set<SideEffectInfo>> newSideEffectsInFun =
        deepCopySideEffects(pState.getSideEffectsInFun());
    Deque<String> mergedFunctionCallStack = new ArrayDeque<>(pState.getCalledFunctionStack());

    if (!pState.isInsideFunctionCall()) {
      return newSideEffectsInFun;
    }

    ExpressionBehaviorVisitor visitor =
        new ExpressionBehaviorVisitor(pState, edge, accessType, logger);
    ExpressionAnalysisSummary summary = expr.accept(visitor);
    Set<SideEffectInfo> effects = summary.getSideEffects();

    if (!effects.isEmpty()) {
      String currentFunction = mergedFunctionCallStack.peek();

      // Add side effects to the entry of the currently called function
      newSideEffectsInFun.computeIfAbsent(currentFunction, k -> new HashSet<>()).addAll(effects);

      // Propagate side effects upwards to all callers in the call stack (excluding the top, i.e.,
      // current function)
      boolean skipTop = true;
      for (String caller : mergedFunctionCallStack) {
        if (skipTop) {
          skipTop = false;
          continue;
        }
        newSideEffectsInFun.computeIfAbsent(caller, k -> new HashSet<>()).addAll(effects);
      }

      logger.logf(
          Level.INFO,
          "[CollectSideEffect] Function='%s', Expr='%s', NewEffects=%s, ALlEffects=%s",
          currentFunction,
          UnseqUtils.replaceTmpInExpression(expr, pState),
          effects,
          newSideEffectsInFun);
    }

    return newSideEffectsInFun;
  }

  /**
   * Detects unsequenced side-effect conflicts in nested binary expressions within the given
   * expression.
   *
   * <p>This method visits the input expression and extracts all sub-expressions marked as
   * unsequenced binary expressions (e.g., those using operators such as {@code +}, {@code *},
   * etc.). For each such binary expression, it analyzes the left and right operands for side
   * effects and identifies potential memory access conflicts.
   *
   * @param expr the compound expression possibly containing unsequenced binary sub-expressions
   * @param edge the CFA edge where the expression occurs
   * @param pState the current analysis state used for tracking side effects and logging
   * @return a set of {@link ConflictPair} instances indicating unsequenced memory access conflicts
   * @throws UnrecognizedCodeException if the expression contains unsupported constructs
   */
  private Set<ConflictPair> detectConflictsInUnsequencedBinaryExprs(
      CExpression expr, CFAEdge edge, UnseqBehaviorAnalysisState pState)
      throws UnrecognizedCodeException {

    Set<ConflictPair> newConflicts = new HashSet<>(pState.getDetectedConflicts());
    ExpressionBehaviorVisitor visitor =
        new ExpressionBehaviorVisitor(pState, edge, AccessType.READ, logger);
    ExpressionAnalysisSummary summary = expr.accept(visitor);

    for (CBinaryExpression unseqExpr : summary.getUnsequencedBinaryExprs()) {
      CExpression left = unseqExpr.getOperand1();
      CExpression right = unseqExpr.getOperand2();

      ExpressionAnalysisSummary leftSummary = left.accept(visitor);
      ExpressionAnalysisSummary rightSummary = right.accept(visitor);

      Set<SideEffectInfo> leftEffects = leftSummary.getSideEffects();
      Set<SideEffectInfo> rightEffects = rightSummary.getSideEffects();

      logger.logf(
          Level.INFO,
          "[BinaryExprConflicts] Detected: (%s) ⊕ (%s) → Left Side Effects: %s → Right Side"
              + " Effects: %s",
          UnseqUtils.replaceTmpInExpression(left, pState),
          UnseqUtils.replaceTmpInExpression(right, pState),
          leftEffects,
          rightEffects);

      Set<ConflictPair> conflicts =
          getUnsequencedConflicts(leftEffects, rightEffects, edge, left, right, pState);
      if (!conflicts.isEmpty()) {
        newConflicts.addAll(conflicts);
      }
    }
    return newConflicts;
  }

  /**
   * Detects unsequenced side-effect conflicts between pairs of function call arguments.
   *
   * <p>For each distinct pair of expressions in {@code sideEffectsPerSubExpr}, this method checks
   * if their associated side effects are unsequenced and potentially conflicting. If so, {@link
   * ConflictPair} instances are created and added to the result set.
   *
   * @param sideEffectsPerSubExpr a mapping from each argument expression to its associated side
   *     effects
   * @param edge the CFA edge at which the potential unsequenced conflict occurs
   * @param pState the current analysis state providing access to TMP mappings and logging
   * @return a new {@link Set} of {@link ConflictPair} instances representing all detected
   *     conflicts, including those already present in {@code pState}
   */
  private Set<ConflictPair> detectCrossArgumentConflicts(
      Map<CRightHandSide, Set<SideEffectInfo>> sideEffectsPerSubExpr,
      CFAEdge edge,
      UnseqBehaviorAnalysisState pState) {

    Set<ConflictPair> newConflicts = new HashSet<>(pState.getDetectedConflicts());
    List<CRightHandSide> exprs = new ArrayList<>(sideEffectsPerSubExpr.keySet());

    for (int i = 0; i < exprs.size(); i++) {
      for (int j = i + 1; j < exprs.size(); j++) {
        CRightHandSide expr1 = exprs.get(i);
        CRightHandSide expr2 = exprs.get(j);

        Set<SideEffectInfo> effects1 = sideEffectsPerSubExpr.getOrDefault(expr1, ImmutableSet.of());
        Set<SideEffectInfo> effects2 = sideEffectsPerSubExpr.getOrDefault(expr2, ImmutableSet.of());

        logger.logf(
            Level.INFO,
            "[CrossArgumentConflicts] Detected: Argument 1: (%s) ⊕ Argument 2: (%s) → Argument"
                + " 1 Side Effects: %s → Argument 2 Side Effects: %s",
            UnseqUtils.replaceTmpInExpression(expr1, pState),
            UnseqUtils.replaceTmpInExpression(expr2, pState),
            effects2,
            effects1);

        Set<ConflictPair> conflicts =
            getUnsequencedConflicts(effects1, effects2, edge, expr1, expr2, pState);
        if (!conflicts.isEmpty()) {
          newConflicts.addAll(conflicts);
        }
      }
    }
    return newConflicts;
  }

  /**
   * Detects potential unsequenced side-effect conflicts between the left-hand side (LHS) and
   * right-hand side (RHS) of an assignment statement.
   *
   * <p>It evaluates both expressions using {@link ExpressionBehaviorVisitor}, collects their side
   * effects, and analyzes them for conflicting access to the same memory location.
   *
   * @param lhsExpr the left-hand side expression of the assignment
   * @param rhsExpr the right-hand side expression of the assignment
   * @param edge the CFA edge representing the assignment
   * @param pState the current abstract analysis state, used for context and logging
   * @return a {@link Set} of {@link ConflictPair} objects representing detected conflicts,
   *     including those already present in {@code pState}
   * @throws UnrecognizedCodeException if the expression contains unrecognized constructs
   */
  private Set<ConflictPair> detectAssignmentStatementConflicts(
      CExpression lhsExpr, CRightHandSide rhsExpr, CFAEdge edge, UnseqBehaviorAnalysisState pState)
      throws UnrecognizedCodeException {

    Set<ConflictPair> newConflicts = new HashSet<>(pState.getDetectedConflicts());
    ExpressionBehaviorVisitor visitor =
        new ExpressionBehaviorVisitor(pState, edge, AccessType.READ, logger);
    ExpressionAnalysisSummary lhsSummary = lhsExpr.accept(visitor);
    ExpressionAnalysisSummary rhsSummary = rhsExpr.accept(visitor);

    logger.logf(
        Level.INFO,
        "[AssignmentConflict] Detected: LHS: (%s) ⊕ RHS: (%s) → LHS Side Effects: %s → RHS Side"
            + " Effects: %s",
        UnseqUtils.replaceTmpInExpression(lhsExpr, pState),
        UnseqUtils.replaceTmpInExpression(rhsExpr, pState),
        lhsSummary.getSideEffects(),
        rhsSummary.getSideEffects());

    Set<ConflictPair> conflicts =
        getUnsequencedConflicts(
            lhsSummary.getSideEffects(),
            rhsSummary.getSideEffects(),
            edge,
            lhsExpr,
            rhsExpr,
            pState);

    if (!conflicts.isEmpty()) {
      newConflicts.addAll(conflicts);
    }
    return newConflicts;
  }

  /** Find conflict pairs from two sets of side effects. */
  private Set<ConflictPair> getUnsequencedConflicts(
      Set<SideEffectInfo> op1Effects,
      Set<SideEffectInfo> op2Effects,
      CFAEdge edge,
      CRightHandSide op1Expr,
      CRightHandSide op2Expr,
      UnseqBehaviorAnalysisState pState) {

    Set<ConflictPair> result = new HashSet<>();
    for (SideEffectInfo s1 : op1Effects) {
      for (SideEffectInfo s2 : op2Effects) {
        if (conflictOnSameLocation(s1, s2)) {
          result.add(new ConflictPair(s1, s2, edge, op1Expr, op2Expr));

          logger.logf(
              Level.INFO,
              "[Conflict] Unsequenced conflict detected at %s: '%s' vs '%s' on location '%s'"
                  + " (access: %s / %s)",
              edge.getFileLocation(),
              UnseqUtils.replaceTmpInExpression(op1Expr, pState),
              UnseqUtils.replaceTmpInExpression(op2Expr, pState),
              s1.memoryLocation(),
              s1.accessType(),
              s2.accessType());
        }
      }
    }
    return result;
  }

  private boolean conflictOnSameLocation(
      SideEffectInfo sideEffectInfo1, SideEffectInfo sideEffectInfo2) {
    return sideEffectInfo1.memoryLocation().equals(sideEffectInfo2.memoryLocation())
        && (sideEffectInfo1.isWrite() || sideEffectInfo2.isWrite());
  }

  private static Map<String, Set<SideEffectInfo>> deepCopySideEffects(
      Map<String, ImmutableSet<SideEffectInfo>> original) {

    Map<String, Set<SideEffectInfo>> copy = new HashMap<>();

    for (Map.Entry<String, ImmutableSet<SideEffectInfo>> entry : original.entrySet()) {
      copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
    }

    return copy;
  }

  private void mergeSideEffects(
      Map<String, Set<SideEffectInfo>> base, Map<String, Set<SideEffectInfo>> addition) {
    for (Map.Entry<String, Set<SideEffectInfo>> entry : addition.entrySet()) {
      base.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
    }
  }

  private void mergeConflicts(Set<ConflictPair> base, Set<ConflictPair> addition) {
    if (addition == null || addition.isEmpty()) {
      return;
    }
    base.addAll(addition);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement,
      Iterable<AbstractState> pOtherElements,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws UnrecognizedCodeException {

    if (!(pElement instanceof UnseqBehaviorAnalysisState unseqState)) {
      return Collections.singleton(pElement);
    }

    Optional<PointerState> pointerStateOpt = FluentIterable.from(pOtherElements)
        .filter(PointerState.class)
        .first()
        .toJavaUtil();

    if (pointerStateOpt.isEmpty()) {
      return Collections.singleton(pElement);
    }

    PointerState pointerState = pointerStateOpt.orElseThrow();
    if (!pointerState.getPointsToMap().isEmpty()) {
      logger.logf(Level.INFO, "PointerCPA pointsToMap: %s", pointerState.getPointsToMap());
    }
    UnseqBehaviorAnalysisState result = unseqState;

    Set<SideEffectInfo> pointerEffects = unseqState.getAllPointerSideEffects();
    if (pointerEffects.isEmpty()) {
      return Collections.singleton(pElement);
    }

    for (SideEffectInfo se : pointerEffects) {
      if (!se.isUnresolvedPointer()) {
        continue; //skip already resolved or irrelevant
      }

      MemoryLocation pointer = se.memoryLocation();
      LocationSet pointees = pointerState.getPointsToSet(pointer);

      if (!pointees.isTop() && !pointees.isBot()) {
        Iterable<MemoryLocation> resolvedTargets =
            PointerTransferRelation.toNormalSet(pointerState, pointees);

        logger.logf(Level.INFO,
            "Replacing pointer memory %s with resolved target(s): %s at edge: %s",
            pointer,
            resolvedTargets,
            se.cfaEdge().getCode());

        Set<SideEffectInfo> resolvedEffects = new HashSet<>();
        for (MemoryLocation target : resolvedTargets) {
          resolvedEffects.add(new SideEffectInfo(
              target,
              se.accessType(),
              se.cfaEdge(),
              SideEffectKind.POINTER_DEREFERENCE_RESOLVED));
        }
        result = result.replaceSideEffectBatch(se, resolvedEffects);
      } else {
        logger.logf(Level.WARNING, "Could not resolve alias for: %s ", pointer.getExtendedQualifiedName());
      }
    }

    return Collections.singleton(result);
  }
}
