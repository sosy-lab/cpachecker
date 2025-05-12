// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class SeqValidator {

  /**
   * Checks whether CPAchecker can parse the output file {@code pSequentializationPath}.
   *
   * <p>Only use this method if {@link MPOROptions#inputTypeDeclarations} is disabled, because using
   * preprocessors on source code (i.e. {@code String}s) is not allowed and we need to run it on the
   * output file.
   */
  public static void validateProgramParsing(
      Path pSequentializationPath,
      MPOROptions pOptions,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws InvalidConfigurationException, ParserException, InterruptedException, IOException {

    checkArgument(
        !pOptions.inputTypeDeclarations,
        "can only validate source code if inputTypeDeclaration is disabled");
    // validate that seq can be parsed and cfa created -> code compiles
    CFACreator creator =
        new CFACreator(
            // we use a preprocessor so that missing type declarations do not cause an error
            Configuration.builder().setOption("parser.usePreprocessor", "true").build(),
            pLogger,
            pShutdownNotifier);
    List<String> files = ImmutableList.of(pSequentializationPath.toString());
    Verify.verify(creator.parseFileAndCreateCFA(files) != null);
  }

  /**
   * Returns {@code pSequentialization} as is if CPAchecker can parse it, reports an error
   * otherwise.
   *
   * <p>Only use this method if {@link MPOROptions#inputTypeDeclarations} is enabled, because using
   * preprocessors on source code (i.e. {@code String}s) is not allowed.
   */
  public static String validateProgramParsing(
      String pSequentialization,
      MPOROptions pOptions,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws InvalidConfigurationException, ParserException, InterruptedException {

    checkArgument(
        pOptions.inputTypeDeclarations,
        "can only validate source code if inputTypeDeclaration is enabled");
    // validate that seq can be parsed and cfa created -> code compiles
    CFACreator creator =
        new CFACreator(
            // we use a preprocessor so that missing type declarations do not cause an error
            Configuration.builder().build(), pLogger, pShutdownNotifier);
    Verify.verify(creator.parseSourceAndCreateCFA(pSequentialization) != null);
    return pSequentialization;
  }

  /**
   * Returns {@code pCaseClauses} as is or throws an {@link AssertionError} if:
   *
   * <ul>
   *   <li>not all origin {@code pc} are also target {@code pc} somewhere in the thread simulation,
   *       except {@link Sequentialization#INIT_PC}
   *   <li>not all target {@code pc} (e.g. {@code 42} in {@code pc[0] = 42;} are present as origin
   *       {@code pc} (e.g. {@code case 42:}), except {@link Sequentialization#EXIT_PC}
   * </ul>
   *
   * Every sequentialization needs to fulfill this property, otherwise it is faulty.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>>
      validateCaseClauses(
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
          LogManager pLogger) {

    // TODO validate that if there is a ThreadJoin, MutexLock etc. that it MUST be the
    //  first statement in the clause so that total strict orders can be enforced

    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> caseClauses = entry.getValue();
      // create the map of originPc to target pc (e.g. case n, pc[i] = m -> {n : m})
      ImmutableMap<Integer, ImmutableSet<Integer>> pcMap = getPcMap(caseClauses);
      ImmutableMap<Integer, SeqThreadStatementClause> labelCaseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(caseClauses);
      ImmutableSet<Integer> allTargetPcs =
          pcMap.values().stream().flatMap(Set::stream).collect(ImmutableSet.toImmutableSet());
      for (var pcEntry : pcMap.entrySet()) {
        checkLabelPcAsTargetPc(pcEntry.getKey(), allTargetPcs, labelCaseMap, thread.id, pLogger);
        checkTargetPcAsLabelPc(pcEntry.getValue(), pcMap.keySet(), thread.id, pLogger);
      }
    }
    return pCaseClauses;
  }

  /** Maps origin pcs n in {@code case n} to the set of target pcs m {@code pc[t_id] = m}. */
  private static ImmutableMap<Integer, ImmutableSet<Integer>> getPcMap(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, ImmutableSet<Integer>> rPcMap = ImmutableMap.builder();
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableSet.Builder<Integer> targetPcs = ImmutableSet.builder();
      for (SeqThreadStatement statement : caseClause.getAllStatements()) {
        targetPcs.addAll(SeqThreadStatementClauseUtil.collectAllIntegerTargetPc(statement));
      }
      rPcMap.put(caseClause.labelNumber, targetPcs.build());
    }
    return rPcMap.buildOrThrow();
  }

  private static void checkLabelPcAsTargetPc(
      int pLabelPc,
      ImmutableSet<Integer> pAllTargetPc,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelCaseMap,
      int pThreadId,
      LogManager pLogger)
      throws IllegalArgumentException {

    // exclude INIT_PC, it is (often) not present as a target pc
    if (pLabelPc != Sequentialization.INIT_PC) {
      // check if label is a target pc anywhere in this threads switch statement
      if (!pAllTargetPc.contains(pLabelPc)) {
        // check if the labels case clause is a loop head -> it is targeted with goto, not target pc
        SeqThreadStatementClause caseClause = pLabelCaseMap.get(pLabelPc);
        assert caseClause != null;
        SeqThreadStatement firstStatement = caseClause.block.getFirstStatement();
        // TODO test if this can be removed now?
        if (SeqThreadStatementUtil.startsInAtomicBlock(firstStatement)) {
          return; // for statements in atomic blocks, the label pcs may not be targets due to gotos
        }
        handleValidationException(
            String.format(
                "label pc %s does not exist as target pc in thread %s", pLabelPc, pThreadId),
            pLogger);
      }
    }
  }

  private static void checkTargetPcAsLabelPc(
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

  private static void handleValidationException(String pMessage, LogManager pLogger) {
    pLogger.log(Level.SEVERE, pMessage);
    throw new IllegalArgumentException(pMessage);
  }
}
