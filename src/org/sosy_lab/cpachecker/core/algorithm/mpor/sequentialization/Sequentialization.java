// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

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
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function.SeqReachErrorFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnPcRetrieval;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnPcStorage;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadLocksMutex;
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

  public static final String inputReachErrorDummy =
      buildReachErrorCall(SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__PRETTY_FUNCTION__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final String outputReachErrorDummy =
      buildReachErrorCall(
                  SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__SEQUENTIALIZATION_ERROR__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  protected final int threadCount;

  public Sequentialization(int pThreadCount) {
    threadCount = pThreadCount;
  }

  /** Generates and returns the sequentialized program. */
  public String generateProgram(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      MPOROptions pOptions,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    StringBuilder rProgram = new StringBuilder();
    ImmutableSet<MPORThread> threads = pSubstitutions.keySet();

    // add all original program declarations that are not substituted
    rProgram.append(SeqComment.UNCHANGED_DECLARATIONS);
    for (MPORThread thread : threads) {
      rProgram.append(createNonVarDecString(thread));
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add all var substitute declarations in the order global - local - params - return_pc
    MPORThread mainThread = MPORAlgorithm.getMainThread(threads);
    rProgram.append(createGlobalVarString(Objects.requireNonNull(pSubstitutions.get(mainThread))));
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createLocalVarString(entry.getKey().id, entry.getValue()));
    }
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createParamVarString(entry.getKey().id, entry.getValue()));
    }
    rProgram.append(SeqComment.RETURN_PCS);
    ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> returnPcVars =
        mapReturnPcVars(threads);
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
    ThreadVars threadVars = buildThreadVars(threads, subEdges);
    for (CIdExpression threadVar : threadVars.getIdExpressions()) {
      assert threadVar.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDec = (CVariableDeclaration) threadVar.getDeclaration();
      rProgram.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add all custom function declarations
    rProgram.append(SeqComment.CUSTOM_FUNCTION_DECLARATIONS);
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rProgram.append(SeqFunctionDeclaration.ASSERT_FAIL.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram
        .append(SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString())
        .append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.ABORT.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.REACH_ERROR.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.ASSUME.toASTString()).append(SeqSyntax.NEWLINE);
    // main should always be duplicate
    rProgram
        .append(SeqFunctionDeclaration.MAIN.toASTString())
        .append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));

    // add non main() methods
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    SeqAssumeFunction assume = new SeqAssumeFunction();
    rProgram.append(reachError.toASTString()).append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));
    rProgram.append(assume.toASTString()).append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));

    // create pruned (i.e. only non-blank) cases statements in main method
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses =
        mapCaseClauses(pSubstitutions, subEdges, returnPcVars, threadVars);
    assert validCaseClauses(caseClauses, pLogger);
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> prunedCaseClauses =
        pruneCaseClauses(caseClauses);
    assert validCaseClauses(prunedCaseClauses, pLogger);
    // optional: include loop invariant assertions over thread variables
    Optional<ImmutableList<SeqLogicalAndExpression>> loopInvariants =
        pOptions.addLoopInvariants
            ? Optional.of(createLoopInvariants(pSubstitutions.keySet(), threadVars))
            : Optional.empty();
    // optional: include POR assumptions
    Optional<ImmutableList<SeqFunctionCallExpression>> porAssumptions =
        pOptions.addPOR ? Optional.of(createPORAssumptions(prunedCaseClauses)) : Optional.empty();
    SeqMainFunction mainMethod =
        new SeqMainFunction(
            threadCount,
            pOptions,
            loopInvariants,
            createThreadSimulationAssumptions(pSubstitutions.keySet(), threadVars),
            porAssumptions,
            prunedCaseClauses);
    rProgram.append(mainMethod.toASTString());

    return rProgram.toString();
  }

  // TODO problem: methods such as pthread_cancel allow the termination of another thread.
  //  so if thread i waits for a mutex or a thread, then another thread can cancel i
  //  and the invariants will not hold
  //  -> once we support intermediary thread terminations, remove these invariants
  private ImmutableList<SeqLogicalAndExpression> createLoopInvariants(
      ImmutableSet<MPORThread> pThreads, ThreadVars pThreadVars) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqLogicalAndExpression> rAssertions = ImmutableList.builder();
    ImmutableMap<Integer, CBinaryExpression> pcNotExitPcExpressions =
        mapPcNotExitPcExpressions(pThreads);
    // add assertions over locks: ti_locks_m && pc[i] == -1) ==> assert_fail
    for (var lockedEntry : pThreadVars.locks.entrySet()) {
      CBinaryExpression pcNotExitPc = pcNotExitPcExpressions.get(lockedEntry.getKey().id);
      for (ThreadLocksMutex locks : lockedEntry.getValue().values()) {
        rAssertions.add(new SeqLogicalAndExpression(locks.idExpression, pcNotExitPc));
      }
    }
    // add assertions over joins: tj_joins_ti && pc[j] == -1 ==> assert_fail
    for (var joinsEntry : pThreadVars.joins.entrySet()) {
      CBinaryExpression pcNotExitPc = pcNotExitPcExpressions.get(joinsEntry.getKey().id);
      for (ThreadJoinsThread joins : joinsEntry.getValue().values()) {
        rAssertions.add(new SeqLogicalAndExpression(joins.idExpression, pcNotExitPc));
      }
    }
    return rAssertions.build();
  }

  /**
   * Creates assume function calls to handle total strict orders (TSOs) induced by thread
   * simulations and pthread methods inside the sequentialization.
   */
  private ImmutableList<SeqFunctionCallExpression> createThreadSimulationAssumptions(
      ImmutableSet<MPORThread> pThreads, ThreadVars pThreadVars) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();
    ImmutableMap<Integer, CBinaryExpression> nextThreadNotIdExpressions =
        mapNextThreadNotIdExpressions(pThreads);
    ImmutableMap<Integer, CBinaryExpression> pcNotExitPcExpressions =
        mapPcNotExitPcExpressions(pThreads);
    // TODO create separate functions for better overview here
    // add assumptions over mutexes: assume(!(m_locked && ti_awaits_m) || next_thread != i);
    for (var lockedEntry : pThreadVars.locked.entrySet()) {
      CIdExpression pthreadMutexT = lockedEntry.getKey();
      MutexLocked locked = lockedEntry.getValue();
      // search for the awaits variable corresponding to pthreadMutexT
      for (var awaitsEntry : pThreadVars.locks.entrySet()) {
        MPORThread thread = awaitsEntry.getKey();
        for (var awaitsValue : awaitsEntry.getValue().entrySet()) {
          if (pthreadMutexT.equals(awaitsValue.getKey())) {
            ThreadLocksMutex awaits = awaitsValue.getValue();
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
    // add assumptions over joins: assume(!(pc[i] != -1 && tj_joins_ti) || next_thread != j);
    for (var join : pThreadVars.joins.entrySet()) {
      MPORThread jThread = join.getKey();
      for (var joinValue : join.getValue().entrySet()) {
        MPORThread iThread = joinValue.getKey();
        ThreadJoinsThread joinVar = joinValue.getValue();
        SeqLogicalNotExpression notActiveAndJoins =
            new SeqLogicalNotExpression(
                new SeqLogicalAndExpression(
                    pcNotExitPcExpressions.get(iThread.id), joinVar.idExpression));
        CToSeqExpression nextThreadNotId =
            new CToSeqExpression(nextThreadNotIdExpressions.get(jThread.id));
        SeqLogicalOrExpression assumption =
            new SeqLogicalOrExpression(notActiveAndJoins, nextThreadNotId);
        SeqFunctionCallExpression assumeCall =
            new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
        rAssumptions.add(assumeCall);
      }
    }
    // add atomic assumptions: assume(!(atomic_in_use && ti_begins_atomic) || next_thread != i);
    for (var entry : pThreadVars.begins.entrySet()) {
      assert pThreadVars.atomicInUse.isPresent();
      MPORThread thread = entry.getKey();
      ThreadBeginsAtomic begins = entry.getValue();
      SeqLogicalNotExpression notAtomicInUseAndBegins =
          new SeqLogicalNotExpression(
              new SeqLogicalAndExpression(
                  pThreadVars.atomicInUse.orElseThrow().idExpression, begins.idExpression));
      CToSeqExpression nextThreadNotId =
          new CToSeqExpression(nextThreadNotIdExpressions.get(thread.id));
      SeqLogicalOrExpression assumption =
          new SeqLogicalOrExpression(notAtomicInUseAndBegins, nextThreadNotId);
      SeqFunctionCallExpression assumeCall =
          new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
      rAssumptions.add(assumeCall);
    }
    return rAssumptions.build();
  }

  /**
   * Returns {@code true} if all targetPc (e.g. {@code pc[i] = 42;}) except {@code -1} in
   * pCaseClauses are present as an originPc (e.g. {@code case 42:}), false otherwise. <br>
   * Every sequentialization needs to fulfill this property, otherwise it is faulty.
   */
  private boolean validCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses, LogManager pLogger) {

    for (ImmutableList<SeqCaseClause> caseClauses : pCaseClauses.values()) {
      // create the map of originPc n (e.g. case n) to target pc(s) m (e.g. pc[i] = m)
      ImmutableMap<Integer, ImmutableSet<Integer>> pcMap = getPcMap(caseClauses);
      // check if each targetPc is also present as an origin pc
      for (var pcEntry : pcMap.entrySet()) {
        for (int targetPc : pcEntry.getValue()) {
          // exclude EXIT_PC, it is never present as a pc
          if (targetPc != SeqUtil.EXIT_PC) {
            if (!pcMap.containsKey(targetPc)) {
              pLogger.log(Level.SEVERE, "targetPc %s does not exist as an origin pc", targetPc);
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  /** Maps origin pcs n in {@code case n} to the set of target pcs m {@code pc[t_id] = m}. */
  private @NonNull ImmutableMap<Integer, ImmutableSet<Integer>> getPcMap(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, ImmutableSet<Integer>> pcMapBuilder = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableSet.Builder<Integer> targetPcs = ImmutableSet.builder();
      for (SeqCaseBlockStatement stmt : caseClause.caseBlock.statements) {
        if (stmt.getTargetPc().isPresent()) {
          targetPcs.add(stmt.getTargetPc().orElseThrow());
        }
      }
      pcMapBuilder.put(caseClause.caseLabel.value, targetPcs.build());
    }
    return pcMapBuilder.buildOrThrow();
  }

  private ImmutableList<SeqFunctionCallExpression> createPORAssumptions(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pPrunedCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();
    for (var entry : pPrunedCaseClauses.entrySet()) {
      int threadId = entry.getKey().id;
      for (SeqCaseClause caseClause : entry.getValue()) {
        if (!caseClause.isGlobal && caseClause.alwaysUpdatesPc()) {
          rAssumptions.add(SeqUtil.createPORAssumption(threadId, caseClause.caseLabel.value));
        }
      }
    }
    return rAssumptions.build();
  }

  /** Maps thread ids {@code i} to {@code pc[i] != i} expressions. */
  private ImmutableMap<Integer, CBinaryExpression> mapPcNotExitPcExpressions(
      ImmutableSet<MPORThread> pThreads) throws UnrecognizedCodeException {

    ImmutableMap.Builder<Integer, CBinaryExpression> rExpressions = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      CBinaryExpression nextThreadNotId =
          SeqBinaryExpression.buildBinaryExpression(
              SeqExpressions.getPcExpression(thread.id),
              SeqIntegerLiteralExpression.INT_EXIT_PC,
              BinaryOperator.NOT_EQUALS);
      rExpressions.put(thread.id, nextThreadNotId);
    }
    return rExpressions.buildOrThrow();
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
      ThreadVars pThreadVars)
      throws UnrecognizedCodeException {

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

  /**
   * Extracts {@link SeqCaseClause}s that are not {@link SeqBlankStatement}s from pCaseClauses and
   * updates the {@code pc} accordingly.
   */
  private ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pruneCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> pruned = ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      ImmutableList<SeqCaseClause> caseClauses = entry.getValue();
      ImmutableMap<Integer, SeqCaseClause> caseLabelValueMap =
          mapCaseLabelValueToCaseClauses(caseClauses);
      // map from case label pruned pcs to their new pcs after step 1 pruning
      Map<Integer, Integer> prunePcs = new HashMap<>();
      ImmutableList.Builder<SeqCaseClause> prune1 = ImmutableList.builder();

      Set<SeqCaseClause> prunable = new HashSet<>();
      Set<Long> newIds = new HashSet<>();

      // step 1: recursively prune by executing chains of blank cases until a non-blank is found
      for (SeqCaseClause caseClause : caseClauses) {
        if (!prunable.contains(caseClause)) {
          if (caseClause.isPrunable()) {
            int pc = caseClause.caseLabel.value;
            Optional<SeqCaseClause> optNonBlank =
                findNonBlankCaseClause(caseLabelValueMap, caseClause, prunable, caseClause);
            if (optNonBlank.isPresent()) {
              SeqCaseClause nonBlank = optNonBlank.orElseThrow();
              int nonBlankPc = nonBlank.caseLabel.value;
              if (prunePcs.containsKey(nonBlankPc)) {
                // a nonBlank may be reachable through multiple blank paths
                // -> reference the first clone to prevent duplication of cases
                prunePcs.put(pc, prunePcs.get(nonBlankPc));
              } else {
                prune1.add(nonBlank.cloneWithCaseLabel(caseClause.caseLabel));
                newIds.add(nonBlank.id);
                prunePcs.put(nonBlankPc, pc);
              }
            } else {
              // no non-blank found: path leads to thread exit node -> entire path is blank
              assert caseClause.caseBlock.statements.size() == 1;
              int targetPc = caseClause.caseBlock.statements.get(0).getTargetPc().orElseThrow();
              assert targetPc == SeqUtil.EXIT_PC;
              prunePcs.put(pc, targetPc);
            }
          } else if (!newIds.contains(caseClause.id)) {
            prune1.add(caseClause);
            newIds.add(caseClause.id);
          }
        }
      }
      // step 2: ensure all targetPcs point to a valid pc after pruning
      ImmutableList.Builder<SeqCaseClause> prune2 = ImmutableList.builder();
      for (SeqCaseClause caseClause : prune1.build()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStmts = ImmutableList.builder();
        for (SeqCaseBlockStatement stmt : caseClause.caseBlock.statements) {
          Optional<Integer> targetPc = stmt.getTargetPc();
          // if the statement targets a pruned pc, clone it with the new target pc
          if (targetPc.isPresent() && prunePcs.containsKey(targetPc.orElseThrow())) {
            int newTargetPc = prunePcs.get(targetPc.orElseThrow());
            newStmts.add(stmt.cloneWithTargetPc(newTargetPc));
          } else {
            // otherwise, add unchanged statement
            newStmts.add(stmt);
          }
        }
        prune2.add(
            caseClause.cloneWithCaseBlock(new SeqCaseBlock(newStmts.build(), Terminator.CONTINUE)));
      }
      pruned.put(entry.getKey(), prune2.build());
    }
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> rPruned = pruned.buildOrThrow();
    assert !isPrunable(rPruned);
    return rPruned;
  }

  /**
   * Returns {@code true} if any {@link SeqCaseClause} of any thread can be pruned, i.e. contains
   * only blank statements, and {@code false} otherwise.
   */
  private boolean isPrunable(ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    for (ImmutableList<SeqCaseClause> caseClauses : pCaseClauses.values()) {
      for (SeqCaseClause caseClause : caseClauses) {
        if (caseClause.isPrunable()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * A helper mapping {@link SeqCaseClause}s to their {@link SeqCaseLabel} values, which are always
   * {@code int} values in the sequentialization.
   */
  private ImmutableMap<Integer, SeqCaseClause> mapCaseLabelValueToCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, SeqCaseClause> rOriginPcs = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      rOriginPcs.put(caseClause.caseLabel.value, caseClause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /**
   * Returns the first {@link SeqCaseClause} in the {@code pc} chain that has no {@link
   * SeqBlankStatement}. If pInit reaches a threads termination through only blank statement,
   * returns {@link Optional#empty()}.
   */
  private Optional<SeqCaseClause> findNonBlankCaseClause(
      final ImmutableMap<Integer, SeqCaseClause> pCaseLabelValueMap,
      final SeqCaseClause pInit,
      Set<SeqCaseClause> pPruned,
      SeqCaseClause pCurrent) {

    for (SeqCaseBlockStatement stmt : pCurrent.caseBlock.statements) {
      if (pCurrent.isPrunable()) {
        pPruned.add(pCurrent);
        SeqBlankStatement blank = (SeqBlankStatement) stmt;
        SeqCaseClause nextCaseClause = pCaseLabelValueMap.get(blank.getTargetPc().orElseThrow());
        if (nextCaseClause == null) {
          // this is only reachable if blank is a threads exit -> no successors
          return Optional.empty();
        }
        // do not visit exit nodes of the threads cfa
        if (!nextCaseClause.caseBlock.statements.isEmpty()) {
          return findNonBlankCaseClause(pCaseLabelValueMap, pInit, pPruned, nextCaseClause);
        }
      }
      // otherwise break recursion -> non-blank case found
      return Optional.of(pCurrent);
    }
    throw new IllegalArgumentException("pCurrent statements cannot be empty");
  }

  // FunctionVars ================================================================================

  /**
   * Maps {@link CFunctionDeclaration}s to {@code return_pc} {@link CIdExpression}s for all threads.
   *
   * <p>E.g. the function {@code fib} in thread 0 is mapped to the expression of {@code int
   * __return_pc_t0_fib}.
   */
  private ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
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
  private ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
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
  private ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
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
  private ImmutableMap<ThreadEdge, FunctionReturnPcStorage> mapReturnPcStorages(
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
  private ImmutableMap<ThreadNode, FunctionReturnPcRetrieval> mapReturnPcRetrievals(
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

  private ImmutableMap<CIdExpression, MutexLocked> mapMutexLockedVars(
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

  private ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>>
      mapThreadAwaitsMutexVars(
          ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<CIdExpression, ThreadLocksMutex> awaitVars = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert pSubEdges.containsKey(threadEdge);
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        if (PthreadFuncType.callsPthreadFunc(sub.cfaEdge, PthreadFuncType.PTHREAD_MUTEX_LOCK)) {
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
          // multiple lock calls within one thread to the same mutex are possible -> only need one
          if (!awaitVars.containsKey(pthreadMutexT)) {
            String varName =
                SeqNameBuilder.buildThreadLocksMutexName(thread.id, pthreadMutexT.getName());
            CIdExpression awaits = SeqIdExpression.buildIntIdExpr(varName, SeqInitializer.INT_0);
            awaitVars.put(pthreadMutexT, new ThreadLocksMutex(awaits));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(awaitVars));
    }
    return rVars.buildOrThrow();
  }

  private ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>>
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

  private ImmutableMap<MPORThread, ThreadBeginsAtomic> mapThreadBeginsAtomicVars(
      ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ThreadBeginsAtomic> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.__VERIFIER_ATOMIC_BEGIN)) {
          String varName = SeqNameBuilder.buildThreadBeginsAtomicName(thread.id);
          CIdExpression begin = SeqIdExpression.buildIntIdExpr(varName, SeqInitializer.INT_0);
          rVars.put(thread, new ThreadBeginsAtomic(begin));
          break; // only need one call to atomic_begin -> break inner loop
        }
      }
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

  /**
   * Returns the {@link CFunctionCallExpression} of {@code reach_error("{pFile}", {pLine},
   * "{pFunction}")}
   */
  public static CFunctionCallExpression buildReachErrorCall(
      String pFile, int pLine, String pFunction) {
    CStringLiteralExpression file =
        SeqStringLiteralExpression.buildStringLiteralExpr(SeqUtil.wrapInQuotationMarks(pFile));
    CIntegerLiteralExpression line = SeqIntegerLiteralExpression.buildIntLiteralExpr(pLine);
    CStringLiteralExpression function =
        SeqStringLiteralExpression.buildStringLiteralExpr(SeqUtil.wrapInQuotationMarks(pFunction));
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        SeqVoidType.VOID,
        SeqIdExpression.REACH_ERROR,
        ImmutableList.of(file, line, function),
        SeqFunctionDeclaration.REACH_ERROR);
  }

  private ThreadVars buildThreadVars(
      ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    return new ThreadVars(
        mapMutexLockedVars(pThreads, pSubEdges),
        mapThreadAwaitsMutexVars(pThreads, pSubEdges),
        mapThreadJoinsThreadVars(pThreads),
        mapThreadBeginsAtomicVars(pThreads));
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
}
