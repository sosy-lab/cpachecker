// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnPcRetrieval;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnPcStorage;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output.SequentializationWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadActive;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadAwaitsMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  // TODO create LineOfCode class with multiple constructors where we define tab amount, semicolon
  //  curly left / right brackets, newlines, etc.
  public static final int TAB_SIZE = 2;

  /**
   * The call to {@code __assert_fail("0", "{output_file_name}", -1,
   * "__SEQUENTIALIZATION_ERROR__");}. The {@code -1} is adjusted to the actual line of code by
   * {@link SequentializationWriter}.
   */
  private static String seqError = null;

  protected final int threadCount;

  public Sequentialization(int pThreadCount) {
    threadCount = pThreadCount;
  }

  /** Generates and returns the sequentialized program. */
  public String generateProgram(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions)
      throws UnrecognizedCodeException {

    StringBuilder rProgram = new StringBuilder();

    // add all original program declarations that are not substituted
    rProgram.append(SeqComment.UNCHANGED_DECLARATIONS);
    for (MPORThread thread : pSubstitutions.keySet()) {
      rProgram.append(createNonVarDecString(thread));
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add all var substitute declarations in the order global - local - params - return_pc
    MPORThread mainThread = MPORAlgorithm.getMainThread(pSubstitutions.keySet());
    rProgram.append(createGlobalVarString(Objects.requireNonNull(pSubstitutions.get(mainThread))));
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createLocalVarString(entry.getKey().id, entry.getValue()));
    }
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createParamVarString(entry.getKey().id, entry.getValue()));
    }
    rProgram.append(SeqComment.RETURN_PCS);
    ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> returnPcVars =
        mapReturnPcVars(pSubstitutions.keySet());
    for (ImmutableMap<CFunctionDeclaration, CIdExpression> map : returnPcVars.values()) {
      for (CIdExpression returnPc : map.values()) {
        rProgram.append(returnPc.getDeclaration().toASTString()).append(SeqSyntax.NEWLINE);
      }
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add thread simulation vars
    rProgram.append(SeqComment.THREAD_SIMULATION);
    ImmutableMap<ThreadEdge, SubstituteEdge> subEdges =
        SubstituteBuilder.substituteEdges(pSubstitutions);
    ThreadVars threadVars = buildThreadVars(pSubstitutions.keySet(), subEdges);
    for (CIdExpression threadVar : threadVars.getIdExpressions()) {
      assert threadVar.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDec = (CVariableDeclaration) threadVar.getDeclaration();
      rProgram.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add all custom function declarations
    rProgram.append(SeqComment.CUSTOM_FUNCTION_DECLARATIONS);
    // abort, assert, nondet_int may be duplicate depending on the input program
    rProgram
        .append(SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString())
        .append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.ABORT.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.ASSERT_FAIL.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.ASSUME.toASTString()).append(SeqSyntax.NEWLINE);
    // main should always be duplicate
    rProgram.append(SeqFunctionDeclaration.MAIN.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqSyntax.NEWLINE);

    // add non main() methods
    SeqAssumeFunction assume = new SeqAssumeFunction();
    rProgram.append(assume.toASTString()).append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));

    // create thread simulation assumptions
    ImmutableList<SeqFunctionCallExpression> assumptions =
        createThreadSimulationAssumptions(threadVars);
    // TODO we also need to prune: update targetPc to -1 if we reach a thread exit node
    // create pruned (i.e. only non-empty) cases statements
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses =
        mapCaseClauses(pSubstitutions, subEdges, returnPcVars, threadVars);
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> prunedCaseClauses =
        pruneCaseClauses(caseClauses);
    SeqMainFunction mainMethod = new SeqMainFunction(threadCount, assumptions, prunedCaseClauses);
    rProgram.append(mainMethod.toASTString());

    return rProgram.toString();
  }

  /**
   * Creates assume function calls to handle total strict orders (TSOs) induced by thread
   * simulations and pthread methods inside the sequentialization.
   */
  private ImmutableList<SeqFunctionCallExpression> createThreadSimulationAssumptions(
      ThreadVars pThreadVars) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();
    ImmutableMap<Integer, CBinaryExpression> nextThreadNotIdExpressions =
        mapNextThreadNotIdExpressions(pThreadVars.active.keySet());
    // add assumptions over active vars: assume(ti_active || next_thread != i);
    for (var entry : pThreadVars.active.entrySet()) {
      MPORThread thread = entry.getKey();
      ThreadActive active = entry.getValue();
      SeqLogicalOrExpression assumption =
          new SeqLogicalOrExpression(
              active.idExpression, nextThreadNotIdExpressions.get(thread.id));
      SeqFunctionCallExpression assumeCall =
          new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
      rAssumptions.add(assumeCall);
    }
    // add assumptions over mutexes: assume(!(m_locked && ti_awaits_m) || next_thread != i);
    for (var lockedEntry : pThreadVars.locked.entrySet()) {
      CIdExpression pthreadMutexT = lockedEntry.getKey();
      MutexLocked locked = lockedEntry.getValue();
      // search for the awaits variable corresponding to pthreadMutexT
      for (var awaitsEntry : pThreadVars.awaits.entrySet()) {
        MPORThread thread = awaitsEntry.getKey();
        for (var awaitsValue : awaitsEntry.getValue().entrySet()) {
          if (pthreadMutexT.equals(awaitsValue.getKey())) {
            ThreadAwaitsMutex awaits = awaitsValue.getValue();
            SeqLogicalNotExpression notLockedAndAwaits =
                new SeqLogicalNotExpression(
                    new SeqLogicalAndExpression(locked.idExpression, awaits.idExpression));
            CToSeqExpression nextThreadNotId =
                new CToSeqExpression(nextThreadNotIdExpressions.get(thread.id));
            SeqLogicalOrExpression assumption =
                new SeqLogicalOrExpression(notLockedAndAwaits, nextThreadNotId);
            SeqFunctionCallExpression assumeCall =
                new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
            rAssumptions.add(assumeCall);
          }
        }
      }
    }
    // add assumptions over joins: assume(!(ti_active && tj_joins_ti) || next_thread != j);
    for (var join : pThreadVars.joins.entrySet()) {
      MPORThread jThread = join.getKey();
      for (var joinValue : join.getValue().entrySet()) {
        MPORThread iThread = joinValue.getKey();
        ThreadJoinsThread joinVar = joinValue.getValue();
        ThreadActive iThreadActive = pThreadVars.active.get(iThread);
        assert iThreadActive != null;
        SeqLogicalNotExpression notActiveAndJoins =
            new SeqLogicalNotExpression(
                new SeqLogicalAndExpression(iThreadActive.idExpression, joinVar.idExpression));
        CToSeqExpression nextThreadNotId =
            new CToSeqExpression(nextThreadNotIdExpressions.get(jThread.id));
        SeqLogicalOrExpression assumption =
            new SeqLogicalOrExpression(notActiveAndJoins, nextThreadNotId);
        SeqFunctionCallExpression assumeCall =
            new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
        rAssumptions.add(assumeCall);
      }
    }
    return rAssumptions.build();
  }

  /** Maps thread ids {@code i} to {@code next_thread != i} expressions. */
  private ImmutableMap<Integer, CBinaryExpression> mapNextThreadNotIdExpressions(
      ImmutableSet<MPORThread> pThreads) throws UnrecognizedCodeException {

    ImmutableMap.Builder<Integer, CBinaryExpression> rExpressions = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      CIntegerLiteralExpression threadId =
          SeqIntegerLiteralExpression.buildIntLiteralExpr(thread.id);
      CBinaryExpression nextThreadNotId =
          SeqBinaryExpression.buildBinaryExpression(
              SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.NOT_EQUALS);
      rExpressions.put(thread.id, nextThreadNotId);
    }
    return rExpressions.buildOrThrow();
  }

  /** Maps threads to the case clauses they potentially execute. */
  private ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> mapCaseClauses(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> pReturnPcVars,
      ThreadVars pThreadVars) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rCaseClauses =
        ImmutableMap.builder();
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();

      ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();

      Set<ThreadNode> coveredNodes = new HashSet<>();

      for (ThreadNode threadNode : thread.cfa.threadNodes) {
        if (!coveredNodes.contains(threadNode)) {
          SeqCaseClause caseClause =
              SeqUtil.createCaseFromThreadNode(
                  thread,
                  pSubstitutions.keySet(),
                  coveredNodes,
                  threadNode,
                  pSubEdges,
                  buildFunctionVars(thread, substitution, pSubEdges, pReturnPcVars),
                  pThreadVars);
          if (caseClause != null) {
            caseClauses.add(caseClause);
          }
        }
      }

      rCaseClauses.put(thread, caseClauses.build());
    }
    return rCaseClauses.buildOrThrow();
  }

  /** Prunes case clauses that contain only {@link SeqBlankStatement}s. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pruneCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rPruned = ImmutableMap.builder();

    for (var entry : pCaseClauses.entrySet()) {
      ImmutableList<SeqCaseClause> caseClauses = entry.getValue();
      ImmutableMap<Integer, SeqCaseClause> caseLabelValueMap =
          mapCaseLabelValueToCaseClauses(caseClauses);
      ImmutableList.Builder<SeqCaseClause> pruned = ImmutableList.builder();

      Set<SeqCaseClause> skipped = new HashSet<>();
      Set<Long> newCaseClauseIds = new HashSet<>();

      for (SeqCaseClause caseClause : caseClauses) {
        if (!skipped.contains(caseClause)) {
          if (caseClause.isPrunable()) {
            SeqCaseClause prunedCaseClause =
                handleCaseClausePrune(caseLabelValueMap, caseClause, skipped, caseClause);
            pruned.add(prunedCaseClause);
            newCaseClauseIds.add(prunedCaseClause.id);
          } else if (!newCaseClauseIds.contains(caseClause.id)) {
            pruned.add(caseClause);
            newCaseClauseIds.add(caseClause.id);
          }
        }
      }
      rPruned.put(entry.getKey(), pruned.build());
    }
    return rPruned.buildOrThrow();
  }

  private static ImmutableMap<Integer, SeqCaseClause> mapCaseLabelValueToCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, SeqCaseClause> rOriginPcs = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      rOriginPcs.put(caseClause.caseLabel.value, caseClause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /**
   * Recursively skips case clauses that are empty (i.e. contain only {@link SeqBlankStatement}).
   */
  private static SeqCaseClause handleCaseClausePrune(
      final ImmutableMap<Integer, SeqCaseClause> pCaseLabelValueMap,
      final SeqCaseClause pInit,
      Set<SeqCaseClause> pSkipped,
      SeqCaseClause pCurrent) {

    for (SeqCaseBlockStatement stmt : pCurrent.caseBlock.statements) {
      if (pCurrent.isPrunable()) {
        pSkipped.add(pCurrent);
        SeqBlankStatement blank = (SeqBlankStatement) stmt;
        SeqCaseClause nextCaseClause = pCaseLabelValueMap.get(blank.targetPc);
        // TODO nextCaseClause null -> targetPc is EXIT_PC, needs to be handled separately
        if (nextCaseClause == null) {
          return pInit;
        }
        // do not visit exit nodes of the threads cfa
        if (!nextCaseClause.caseBlock.statements.isEmpty()) {
          return handleCaseClausePrune(pCaseLabelValueMap, pInit, pSkipped, nextCaseClause);
        }
      }
      // otherwise break recursion -> non-prunable case found
      return pCurrent.cloneWithCaseLabel(pInit.caseLabel);
    }
    throw new IllegalArgumentException("pCurrent statements are empty");
  }

  // FunctionVars ================================================================================

  /**
   * Maps {@link CFunctionDeclaration}s to {@code return_pc} {@link CIdExpression}s for all threads.
   *
   * <p>E.g. the function {@code fib} in thread 0 is mapped to the expression of {@code int
   * __return_pc_t0_fib}.
   */
  private static ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
      mapReturnPcVars(ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      ImmutableMap.Builder<CFunctionDeclaration, CIdExpression> returnPc = ImmutableMap.builder();
      for (CFunctionDeclaration function : thread.cfa.calledFuncs) {
        CVariableDeclaration varDec =
            SeqVariableDeclaration.buildReturnPcVarDec(thread.id, function.getName());
        returnPc.put(function, SeqIdExpression.buildIdExpr(varDec));
      }
      rVars.put(thread, returnPc.buildOrThrow());
    }
    return rVars.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CFunctionCallEdge} to a list of
   * {@link FunctionParameterAssignment}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution vars are declared in {@link
   * CSimpleDeclarationSubstitution#paramSubs}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      mapParameterAssignments(
          MPORThread pThread,
          ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
          CSimpleDeclarationSubstitution pSub) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<FunctionParameterAssignment>> rAssigns =
        ImmutableMap.builder();

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      SubstituteEdge sub = pSubEdges.get(threadEdge);
      assert sub != null;
      if (sub.cfaEdge instanceof CFunctionCallEdge funcCall) {

        ImmutableList.Builder<FunctionParameterAssignment> assigns = ImmutableList.builder();
        List<CParameterDeclaration> paramDecs =
            funcCall.getSuccessor().getFunctionDefinition().getParameters();

        // for each parameter, assign the param substitute to the param expression in funcCall
        for (int i = 0; i < paramDecs.size(); i++) {
          CParameterDeclaration paramDec = paramDecs.get(i);
          assert pSub.paramSubs != null;
          CExpression paramExpr =
              funcCall.getFunctionCallExpression().getParameterExpressions().get(i);
          CIdExpression paramSub = pSub.paramSubs.get(paramDec);
          assert paramSub != null;
          FunctionParameterAssignment parameterAssignment =
              new FunctionParameterAssignment(
                  SeqExpressions.buildExprAssignStmt(paramSub, paramExpr));
          assigns.add(parameterAssignment);
        }
        rAssigns.put(threadEdge, assigns.build());
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to {@link
   * FunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>Note that {@code main} functions and start routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      mapReturnValueAssignments(
          MPORThread pThread,
          ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
          ImmutableMap<ThreadEdge, FunctionReturnPcStorage> pReturnPcStorages) {

    ImmutableMap.Builder<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>> rRetStmts =
        ImmutableMap.builder();
    for (ThreadEdge aThreadEdge : pThread.cfa.threadEdges) {
      SubstituteEdge aSub = pSubEdges.get(aThreadEdge);
      assert aSub != null;

      if (aSub.cfaEdge instanceof CReturnStatementEdge returnStmtEdge) {
        ImmutableSet.Builder<FunctionReturnValueAssignment> assigns = ImmutableSet.builder();
        for (ThreadEdge bThreadEdge : pThread.cfa.threadEdges) {
          SubstituteEdge bSub = pSubEdges.get(bThreadEdge);
          assert bSub != null;

          if (bSub.cfaEdge instanceof CFunctionSummaryEdge funcSumm) {
            // if the summary edge is of the form value = func(); (i.e. an assignment)
            if (funcSumm.getExpression() instanceof CFunctionCallAssignmentStatement assignStmt) {
              AFunctionDeclaration aFuncDec = returnStmtEdge.getSuccessor().getFunction();
              AFunctionType aFunc = aFuncDec.getType();
              AFunctionType bFunc = funcSumm.getFunctionEntry().getFunction().getType();
              if (aFunc.equals(bFunc)) {
                assert aFuncDec instanceof CFunctionDeclaration;
                FunctionReturnValueAssignment assign =
                    new FunctionReturnValueAssignment(
                        pReturnPcStorages.get(bThreadEdge),
                        assignStmt.getLeftHandSide(),
                        returnStmtEdge.getExpression().orElseThrow());
                assigns.add(assign);
              }
            }
          }
        }
        rRetStmts.put(aThreadEdge, assigns.build());
      }
    }
    return rRetStmts.buildOrThrow();
  }

  // TODO the major problem here is that if we assign a pc that is pruned later, the assignment
  //  results the case not being matched because the origin pc is pruned --> the program stops.
  //  before pruning, we should assert that the pc assigned does not point to a blank statement
  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CFunctionSummaryEdge}s to {@link
   * FunctionReturnPcStorage}s.
   *
   * <p>E.g. a {@link CFunctionSummaryEdge} going from pc 5 to 10 for the function {@code fib} in
   * thread 0 is mapped to the storage with the assignment {@code __return_pc_t0_fib = 10;}.
   */
  private static ImmutableMap<ThreadEdge, FunctionReturnPcStorage> mapReturnPcStorages(
      MPORThread pThread, ImmutableMap<CFunctionDeclaration, CIdExpression> pReturnPcVars) {

    ImmutableMap.Builder<ThreadEdge, FunctionReturnPcStorage> rAssigns = ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummary) {
        CFunctionDeclaration function = funcSummary.getFunctionEntry().getFunctionDefinition();
        CIdExpression returnPc = pReturnPcVars.get(function);
        assert returnPc != null;
        FunctionReturnPcStorage returnPcStorage =
            new FunctionReturnPcStorage(returnPc, threadEdge.getSuccessor().pc);
        rAssigns.put(threadEdge, returnPcStorage);
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadNode}s whose {@link CFANode} is a {@link FunctionExitNode} to {@link
   * FunctionReturnPcRetrieval}s.
   *
   * <p>E.g. a {@link FunctionExitNode} for the function {@code fib} in thread 0 is mapped to the
   * assignment {@code pc[0] = __return_pc_t0_fib;}.
   */
  private static ImmutableMap<ThreadNode, FunctionReturnPcRetrieval> mapReturnPcRetrievals(
      MPORThread pThread, ImmutableMap<CFunctionDeclaration, CIdExpression> pReturnPcVars) {

    Map<ThreadNode, FunctionReturnPcRetrieval> rAssigns = new HashMap<>();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummary) {
        Optional<FunctionExitNode> funcExitNode = funcSummary.getFunctionEntry().getExitNode();
        if (funcExitNode.isPresent()) {
          CFunctionDeclaration function = funcSummary.getFunctionEntry().getFunctionDefinition();
          CIdExpression returnPc = pReturnPcVars.get(function);
          ThreadNode threadNode = pThread.cfa.getThreadNodeByCfaNode(funcExitNode.orElseThrow());
          if (!rAssigns.containsKey(threadNode)) {
            FunctionReturnPcRetrieval returnPcRetrieval =
                new FunctionReturnPcRetrieval(pThread.id, returnPc);
            rAssigns.put(threadNode, returnPcRetrieval);
          }
        }
      }
    }
    return ImmutableMap.copyOf(rAssigns);
  }

  // ThreadVars ==================================================================================

  /**
   * Creates and returns a list of {@code __t{thread_id}_active} variables, indexed by their {@link
   * MPORThread#id}.
   */
  private static ImmutableMap<MPORThread, ThreadActive> mapThreadActiveVars(
      ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ThreadActive> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      String varName = SeqNameBuilder.buildThreadActiveName(thread.id);
      // main thread -> init active var to 1, otherwise 0
      CInitializer initializer = thread.isMain() ? SeqInitializer.INT_1 : SeqInitializer.INT_0;
      CIdExpression activeVar = SeqIdExpression.buildIntIdExpr(varName, initializer);
      rVars.put(thread, new ThreadActive(activeVar));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<CIdExpression, MutexLocked> mapMutexLockedVars(
      ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    ImmutableMap.Builder<CIdExpression, MutexLocked> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert pSubEdges.containsKey(threadEdge);
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        // TODO mutexes can also be init with pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
        if (PthreadFuncType.callsPthreadFunc(sub.cfaEdge, PthreadFuncType.PTHREAD_MUTEX_INIT)) {
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
          CIdExpression subPthreadMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
          String varName = SeqNameBuilder.buildMutexLockedName(subPthreadMutexT.getName());
          CIdExpression mutexLocked = SeqIdExpression.buildIntIdExpr(varName, SeqInitializer.INT_0);
          rVars.put(pthreadMutexT, new MutexLocked(mutexLocked));
        }
      }
    }
    // if the same mutex is init twice (i.e. undefined behavior), this throws an exception
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadAwaitsMutex>>
      mapThreadAwaitsMutexVars(
          ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CIdExpression, ThreadAwaitsMutex>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<CIdExpression, ThreadAwaitsMutex> awaitVars = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert pSubEdges.containsKey(threadEdge);
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        if (PthreadFuncType.callsPthreadFunc(sub.cfaEdge, PthreadFuncType.PTHREAD_MUTEX_LOCK)) {
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
          // multiple lock calls within one thread to the same mutex are possible -> only need one
          if (!awaitVars.containsKey(pthreadMutexT)) {
            String varName =
                SeqNameBuilder.buildThreadAwaitsMutexName(thread.id, pthreadMutexT.getName());
            CIdExpression awaits = SeqIdExpression.buildIntIdExpr(varName, SeqInitializer.INT_0);
            awaitVars.put(pthreadMutexT, new ThreadAwaitsMutex(awaits));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(awaitVars));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>>
      mapThreadJoinsThreadVars(ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<MPORThread, ThreadJoinsThread> targetThreads = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_JOIN)) {
          CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
          MPORThread targetThread = PthreadUtil.extractThread(pThreads, cfaEdge);

          // multiple join calls within one thread to the same thread are possible -> only need one
          if (!targetThreads.containsKey(targetThread)) {
            String varName = SeqNameBuilder.buildThreadJoinsThreadName(thread.id, targetThread.id);
            CIdExpression joins = SeqIdExpression.buildIntIdExpr(varName, SeqInitializer.INT_0);
            targetThreads.put(targetThread, new ThreadJoinsThread(joins));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(targetThreads));
    }
    return rVars.buildOrThrow();
  }

  // String Creators =============================================================================

  private String createNonVarDecString(MPORThread pThread) {
    StringBuilder rDecs = new StringBuilder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CDeclarationEdge decEdge) {
        CDeclaration dec = decEdge.getDeclaration();
        if (!(dec instanceof CVariableDeclaration)) {
          assert pThread.isMain(); // test if only main thread declares non-vars, e.g. functions
          rDecs.append(threadEdge.cfaEdge.getCode()).append(SeqSyntax.NEWLINE);
        }
      }
    }
    return rDecs.toString();
  }

  private String createGlobalVarString(CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.GLOBAL_VARIABLES);
    assert pSubstitution.globalVarSubs != null;
    for (CIdExpression globalVar : pSubstitution.globalVarSubs.values()) {
      rDecs.append(globalVar.getDeclaration().toASTString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createLocalVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createLocalVarsComment(pThreadId));
    for (CIdExpression localVar : pSubstitution.localVarSubs.values()) {
      CVariableDeclaration varDec = pSubstitution.castIdExprDec(localVar.getDeclaration());
      if (!SeqUtil.isConstCPAcheckerTMP(varDec)) {
        rDecs.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
      }
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createParamVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createParamVarsComment(pThreadId));
    assert pSubstitution.paramSubs != null;
    for (CIdExpression param : pSubstitution.paramSubs.values()) {
      rDecs.append(param.getDeclaration().toASTString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  // Helpers for better Overview =================================================================

  private ThreadVars buildThreadVars(
      ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    return new ThreadVars(
        mapThreadActiveVars(pThreads),
        mapMutexLockedVars(pThreads, pSubEdges),
        mapThreadAwaitsMutexVars(pThreads, pSubEdges),
        mapThreadJoinsThreadVars(pThreads));
  }

  private FunctionVars buildFunctionVars(
      MPORThread pThread,
      CSimpleDeclarationSubstitution pSubstitution,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> pReturnPcVars) {

    ImmutableMap<ThreadEdge, FunctionReturnPcStorage> returnPcStorages =
        mapReturnPcStorages(pThread, pReturnPcVars.get(pThread));
    return new FunctionVars(
        mapParameterAssignments(pThread, pSubEdges, pSubstitution),
        mapReturnValueAssignments(pThread, pSubEdges, returnPcStorages),
        returnPcStorages,
        mapReturnPcRetrievals(pThread, pReturnPcVars.get(pThread)));
  }

  // Static Variable Setters / Getters ===========================================================

  public static String getSeqError() {
    checkArgument(seqError != null, "seqError was not initialized yet");
    return seqError;
  }

  public static void setSeqError(String pSeqError) {
    checkNotNull(pSeqError);
    checkArgument(seqError == null, "seqError was initialized already");
    seqError = pSeqError;
  }
}
