// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcWriteStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignmentSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqValidator {

  // TODO add a method that checks if the final sequentialization can be compiled
  //  similar to unit tests. but add an option to disable. default is enabled
  //  then we can remove the compile test entirely from unit tests

  /**
   * Returns {@code pCaseClauses} as is or throws an {@link AssertionError} if:
   *
   * <ul>
   *   <li>not all target {@code pc} (e.g. {@code 42} in {@code pc[0] = 42;} are present as origin
   *       {@code pc} (e.g. {@code case 42:}), except {@link Sequentialization#EXIT_PC}
   *   <li>not all origin {@code pc} are also target {@code pc} somewhere in the thread simulation,
   *       except {@link Sequentialization#INIT_PC}
   * </ul>
   *
   * Every sequentialization needs to fulfill this property, otherwise it is faulty.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> validateCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses, LogManager pLogger) {

    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      // create the map of originPc to target pc (e.g. case n, pc[i] = m -> {n : m})
      ImmutableMap<Integer, ImmutableSet<Integer>> pcMap = getPcMap(entry.getValue());
      ImmutableSet<Integer> allTargetPcs =
          pcMap.values().stream().flatMap(Set::stream).collect(ImmutableSet.toImmutableSet());
      for (var pcEntry : pcMap.entrySet()) {
        checkLabelPcAsTargetPc(pcEntry.getKey(), allTargetPcs, thread.id, pLogger);
        checkTargetPcsAsLabelPc(pcEntry.getValue(), pcMap.keySet(), thread.id, pLogger);
      }
      checkReturnValueAssignmentLabelPc(entry.getValue(), pLogger);
    }
    return pCaseClauses;
  }

  /** Maps origin pcs n in {@code case n} to the set of target pcs m {@code pc[t_id] = m}. */
  private static ImmutableMap<Integer, ImmutableSet<Integer>> getPcMap(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, ImmutableSet<Integer>> rPcMap = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableSet.Builder<Integer> targetPcs = ImmutableSet.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        Optional<CExpression> targetPcExpression = statement.getTargetPcExpression();
        if (targetPcExpression.isPresent()) {
          if (targetPcExpression.orElseThrow() instanceof CIntegerLiteralExpression intExpression) {
            targetPcs.add(intExpression.getValue().intValue());
          }
        } else if (statement.getTargetPc().isPresent()) {
          targetPcs.add(statement.getTargetPc().orElseThrow());
        }
      }
      rPcMap.put(caseClause.label.value, targetPcs.build());
    }
    return rPcMap.buildOrThrow();
  }

  private static void checkLabelPcAsTargetPc(
      int pLabelPc, ImmutableSet<Integer> pAllTargetPc, int pThreadId, LogManager pLogger)
      throws IllegalArgumentException {

    // exclude INIT_PC, it is (often) not present as a target pc
    if (pLabelPc != Sequentialization.INIT_PC) {
      if (!pAllTargetPc.contains(pLabelPc)) {
        handleValidationException(
            String.format(
                "label pc %s does not exist as target pc in thread %s", pLabelPc, pThreadId),
            pLogger);
      }
    }
  }

  private static void checkTargetPcsAsLabelPc(
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

  /**
   * Ensures that the label {@code pc} in {@link SeqReturnValueAssignmentSwitchStatement} are at
   * some point assigned to the respective {@code RETURN_PC} in {@code pCaseClauses}.
   */
  private static void checkReturnValueAssignmentLabelPc(
      ImmutableList<SeqCaseClause> pCaseClauses, LogManager pLogger) {

    // extract returnPcWrites and map variables to assigned values
    ImmutableList<SeqReturnPcWriteStatement> returnPcWrites =
        SeqCaseClauseUtil.extractStatements(pCaseClauses, SeqReturnPcWriteStatement.class);
    ImmutableMultimap<CIdExpression, Integer> returnPcWriteMap =
        getReturnPcWriteMap(returnPcWrites);

    // extract returnValueAssignments (i.e. switch statements)
    ImmutableList<SeqReturnValueAssignmentSwitchStatement> switchStatements =
        SeqCaseClauseUtil.extractStatements(
            pCaseClauses, SeqReturnValueAssignmentSwitchStatement.class);

    // for each switch statement, ensure that each label is a variable in a return pc write
    for (SeqReturnValueAssignmentSwitchStatement switchStatement : switchStatements) {
      // we switch over the return_pc: switch(return_pc) ...
      CIdExpression switchExpression = switchStatement.getReturnPc();
      if (!returnPcWriteMap.containsKey(switchExpression)) {
        handleValidationException(
            String.format(
                "return value assignment switch expression %s is not a written RETURN_PC in"
                    + " pCaseClauses",
                switchExpression.toASTString()),
            pLogger);
      }
      for (SeqCaseClause caseClause : switchStatement.caseClauses) {
        int label = caseClause.label.value;
        if (!returnPcWriteMap.get(switchExpression).contains(label)) {
          handleValidationException(
              String.format(
                  "the return pc %s is never assigned the label pc %s",
                  switchExpression.toASTString(), label),
              pLogger);
        }
      }
    }
  }

  /**
   * Maps the variables {@code RETURN_PC} to their assigned {@code int} values based on {@code
   * pReturnPcWrites}.
   */
  private static ImmutableSetMultimap<CIdExpression, Integer> getReturnPcWriteMap(
      ImmutableList<SeqReturnPcWriteStatement> pReturnPcWrites) {

    ImmutableSetMultimap.Builder<CIdExpression, Integer> rMap = ImmutableSetMultimap.builder();
    for (SeqReturnPcWriteStatement returnPcWrite : pReturnPcWrites) {
      Optional<Integer> targetPc = returnPcWrite.getTargetPc();
      Optional<CExpression> targetPcExpression = returnPcWrite.getTargetPcExpression();
      // we want to ensure that we never write return_pc = return_pc
      Verify.verify(
          targetPc.isPresent()
              || targetPcExpression.orElseThrow() instanceof CIntegerLiteralExpression,
          "either int targetPc must pe present or targetPcExpression must be integer expression");
      CIdExpression returnPcVariable = returnPcWrite.getReturnPcVariable();
      if (targetPc.isPresent()) {
        rMap.put(returnPcVariable, targetPc.orElseThrow());
      } else {
        CIntegerLiteralExpression intExpression =
            (CIntegerLiteralExpression) targetPcExpression.orElseThrow();
        rMap.put(returnPcVariable, intExpression.getValue().intValue());
      }
    }
    return rMap.build();
  }

  private static void handleValidationException(String pMessage, LogManager pLogger) {
    pLogger.log(Level.SEVERE, pMessage);
    throw new IllegalArgumentException(pMessage);
  }
}
