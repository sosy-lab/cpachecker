// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class SeqValidator {

  // Program Parsing ===============================================================================

  /**
   * Returns {@code pSequentialization} as is if CPAchecker can parse it, reports an error
   * otherwise.
   *
   * <p>Only use this method if {@link MPOROptions#inputTypeDeclarations()} is enabled, because
   * using preprocessors on source code (i.e. {@code String}s) is not allowed.
   */
  public static String validateProgramParsing(
      String pSequentialization, SequentializationUtils pUtils)
      throws InvalidConfigurationException, ParserException, InterruptedException {

    // validate that seq can be parsed and cfa created -> code compiles
    CFACreator cfaCreator =
        MPORUtil.buildTestCfaCreator(pUtils.logger(), pUtils.shutdownNotifier());
    Verify.verify(cfaCreator.parseSourceAndCreateCFA(pSequentialization) != null);
    return pSequentialization;
  }

  // Clauses =======================================================================================

  // TODO validate that if there is a ThreadJoin, MutexLock etc. that it MUST be the
  //  first statement

  /**
   * Validates correctness properties of {@code pClauses} based on the options set in {@code
   * pOptions}.
   */
  public static void tryValidateClauses(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    tryValidateProgramCounters(pOptions, pClauses, pLogger);
    tryValidateNoBackwardGoto(pOptions, pClauses, pLogger);
    tryValidateNoBlankClauses(pOptions, pClauses, pLogger);
  }

  // Program Counter (pc) ==========================================================================

  /**
   * If enabled in {@code pOptions}, ensures that all {@code pc} writes are to a valid location
   * (except exit), and that all locations are at some point written to (except start).
   *
   * <p>Every sequentialization needs to fulfill this property, otherwise it is faulty.
   */
  public static void tryValidateProgramCounters(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    if (pOptions.validatePc()) {
      for (MPORThread thread : pClauses.keySet()) {
        ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
        // create the map of originPc to target pc (e.g. case n, pc[i] = m -> {n : m})
        ImmutableMap<Integer, ImmutableSet<Integer>> pcMap = getPcMap(clauses);
        ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
            SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
        ImmutableSet<Integer> allTargetPcs =
            pcMap.values().stream().flatMap(Set::stream).collect(ImmutableSet.toImmutableSet());
        for (var pcEntry : pcMap.entrySet()) {
          validateLabelPcAsTargetPc(
              pcEntry.getKey(), allTargetPcs, labelClauseMap, thread.id(), pLogger);
          validateTargetPcAsLabelPc(pcEntry.getValue(), pcMap.keySet(), thread.id(), pLogger);
        }
      }
    }
  }

  /** Maps origin pcs n in {@code case n} to the set of target pcs m {@code pc[t_id] = m}. */
  private static ImmutableMap<Integer, ImmutableSet<Integer>> getPcMap(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, ImmutableSet<Integer>> rPcMap = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableSet.Builder<Integer> targetPcs = ImmutableSet.builder();
      for (CSeqThreadStatement statement : clause.getAllStatements()) {
        targetPcs.addAll(SeqThreadStatementClauseUtil.collectAllIntegerTargetPc(statement));
      }
      rPcMap.put(clause.labelNumber, targetPcs.build());
    }
    return rPcMap.buildOrThrow();
  }

  private static void validateLabelPcAsTargetPc(
      int pLabelPc,
      ImmutableSet<Integer> pAllTargetPc,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      int pThreadId,
      LogManager pLogger)
      throws IllegalArgumentException {

    // exclude INIT_PC, it is (often) not present as a target pc
    if (pLabelPc != Sequentialization.INIT_PC) {
      // check if label is a target pc anywhere in this threads switch statement
      if (!pAllTargetPc.contains(pLabelPc)) {
        // check if the labels case clause is a loop head -> it is targeted with goto, not target pc
        SeqThreadStatementClause clause = pLabelClauseMap.get(pLabelPc);
        assert clause != null;
        handleValidationException(
            String.format(
                "label pc %s does not exist as target pc in thread %s", pLabelPc, pThreadId),
            pLogger);
      }
    }
  }

  private static void validateTargetPcAsLabelPc(
      ImmutableSet<Integer> pTargetPcs,
      ImmutableSet<Integer> pLabelPcs,
      int pThreadId,
      LogManager pLogger)
      throws IllegalArgumentException {

    for (int targetPc : pTargetPcs) {
      // exclude EXIT_PC, it is never present as a label pc
      if (targetPc != Sequentialization.EXIT_PC) {
        if (!pLabelPcs.contains(targetPc)) {
          handleValidationException(
              String.format(
                  "target pc %s does not exist as label pc in thread %s", targetPc, pThreadId),
              pLogger);
        }
      }
    }
  }

  // Block Reordering ==============================================================================

  /** Returns {@code true} if the two collections contain the exact same blocks, in any order. */
  public static void validateEqualBlocks(
      ImmutableCollection<SeqThreadStatementBlock> pBlocksA,
      ImmutableCollection<SeqThreadStatementBlock> pBlocksB,
      LogManager pLogger) {

    // short circuit: check for equal length
    if (pBlocksA.size() != pBlocksB.size()) {
      handleValidationException("pBlocksA and pBlocksB length differ", pLogger);
    }
    // otherwise check if B contains all elements from A
    for (SeqThreadStatementBlock blockA : pBlocksA) {
      if (!pBlocksB.contains(blockA)) {
        handleValidationException("pBlocksB does not contain all blocks from pBlocksA", pLogger);
      }
    }
  }

  /**
   * Validates that all {@code goto} statements inside a thread simulation target a forward
   * location.
   */
  public static void tryValidateNoBackwardGoto(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    if (pOptions.validateNoBackwardGoto()) {
      for (MPORThread thread : pClauses.keySet()) {
        ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
            SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses.get(thread));
        validateBlockLabelLessThanTargetLabel(pOptions, thread, labelBlockMap, pLogger);
      }
    }
  }

  private static void validateBlockLabelLessThanTargetLabel(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      LogManager pLogger) {

    for (SeqThreadStatementBlock block : pLabelBlockMap.values()) {
      for (CSeqThreadStatement statement : block.getStatements()) {
        if (statement.getTargetGoto().isPresent()) {
          int blockNumber = block.getLabel().number();
          int targetNumber = statement.getTargetGoto().orElseThrow().number();
          if (blockNumber > targetNumber) {
            SeqThreadStatementBlock targetBlock =
                Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
            // ignore backward jump, if it is to a loop start and enabled in options
            if (!targetBlock.isLoopStart() || pOptions.noBackwardLoopGoto()) {
              handleValidationException(
                  String.format(
                      "block number %s is greater than target number %s in thread %s",
                      blockNumber, targetNumber, pThread.id()),
                  pLogger);
            }
          }
        }
      }
    }
  }

  // No Blank Clauses ==============================================================================

  /** Checks that no clause is blank, when {@link MPOROptions#pruneEmptyStatements()} is enabled. */
  public static void tryValidateNoBlankClauses(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    if (pOptions.pruneEmptyStatements()) {
      for (MPORThread thread : pClauses.keySet()) {
        // ignore threads that have only one clause, they may be blank
        if (pClauses.get(thread).size() > 1) {
          for (SeqThreadStatementClause clause : pClauses.get(thread)) {
            if (!clause.isBlank()) {
              return;
            }
          }
          handleValidationException(
              String.format("thread %s contains a blank statement after pruning", thread.id()),
              pLogger);
        }
      }
    }
  }

  // Helper ========================================================================================

  private static void handleValidationException(String pMessage, LogManager pLogger) {
    pLogger.log(Level.SEVERE, pMessage);
    throw new IllegalArgumentException(pMessage);
  }
}
