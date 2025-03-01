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
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcWriteStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignmentSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqValidator {

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
      checkReturnValueAssignmentLabelPc(entry.getValue());
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
        if (statement.getTargetPc().isPresent()) {
          targetPcs.add(statement.getTargetPc().orElseThrow());
        }
      }
      rPcMap.put(caseClause.label.value, targetPcs.build());
    }
    return rPcMap.buildOrThrow();
  }

  private static void checkLabelPcAsTargetPc(
      int pLabelPc, ImmutableSet<Integer> pAllTargetPc, int pThreadId, LogManager pLogger) {

    // exclude INIT_PC, it is (often) not present as a target pc
    if (pLabelPc != Sequentialization.INIT_PC) {
      if (!pAllTargetPc.contains(pLabelPc)) {
        String message =
            "label pc " + pLabelPc + " does not exist as target pc in thread " + pThreadId;
        pLogger.log(Level.SEVERE, message);
        MPORAlgorithm.fail(message);
      }
    }
  }

  private static void checkTargetPcsAsLabelPc(
      ImmutableSet<Integer> pTargetPcs,
      ImmutableSet<Integer> pLabelPcs,
      int pThreadId,
      LogManager pLogger) {

    for (int targetPc : pTargetPcs) {
      // exclude EXIT_PC, it is never present as a label pc
      if (targetPc != Sequentialization.EXIT_PC) {
        if (!pLabelPcs.contains(targetPc)) {
          String message =
              "target pc " + targetPc + " does not exist as label pc in thread " + pThreadId;
          pLogger.log(Level.SEVERE, message);
          MPORAlgorithm.fail(message);
        }
      }
    }
  }

  /**
   * Ensures that the label {@code pc} in {@link SeqReturnValueAssignmentSwitchStatement} are at
   * some point assigned to the respective {@code RETURN_PC} in {@code pCaseClauses}.
   */
  private static void checkReturnValueAssignmentLabelPc(ImmutableList<SeqCaseClause> pCaseClauses) {

    // extract returnPcWrites and map variables to assigned values
    ImmutableList<SeqReturnPcWriteStatement> returnPcWrites =
        extractStatements(pCaseClauses, SeqReturnPcWriteStatement.class);
    ImmutableMultimap<CIdExpression, Integer> returnPcWriteMap =
        getReturnPcWriteMap(returnPcWrites);

    // extract returnValueAssignments (i.e. switch statements)
    ImmutableList<SeqReturnValueAssignmentSwitchStatement> switchStatements =
        extractStatements(pCaseClauses, SeqReturnValueAssignmentSwitchStatement.class);

    // for each switch statement, ensure that each label is a variable in a return pc write
    for (SeqReturnValueAssignmentSwitchStatement switchStatement : switchStatements) {
      // we switch over the return_pc: switch(return_pc) ...
      CIdExpression switchExpression = switchStatement.getReturnPc();
      Verify.verify(
          returnPcWriteMap.containsKey(switchExpression),
          "return value assignment switch expression %s is not a written RETURN_PC in pCaseClauses",
          switchExpression.toASTString());
      for (FunctionReturnValueAssignment assignment : switchStatement.assignments) {
        CIdExpression assignmentReturnPc = assignment.returnPcWrite.variable;
        int assignmentLabel = assignment.returnPcWrite.value;
        //
        Verify.verify(
            returnPcWriteMap.get(assignmentReturnPc).contains(assignmentLabel),
            "the return pc %s is never assigned the label pc %s",
            assignmentReturnPc.toASTString(),
            assignmentLabel);
      }
    }
  }

  private static ImmutableMultimap<CIdExpression, Integer> getReturnPcWriteMap(
      ImmutableList<SeqReturnPcWriteStatement> pReturnPcWrites) {

    ImmutableMultimap.Builder<CIdExpression, Integer> rMap = ImmutableMultimap.builder();
    for (SeqReturnPcWriteStatement returnPcWrite : pReturnPcWrites) {
      Verify.verify(
          returnPcWrite.getTargetPc().isPresent(), "return pc write does not have a target pc");
      rMap.put(returnPcWrite.getReturnPcVariable(), returnPcWrite.getTargetPc().orElseThrow());
    }
    return rMap.build();
  }

  private static <T extends SeqCaseBlockStatement> ImmutableList<T> extractStatements(
      ImmutableList<SeqCaseClause> pCaseClauses, Class<T> pStatementClass) {

    ImmutableList.Builder<T> rStatements = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        if (pStatementClass.isInstance(statement)) {
          rStatements.add(pStatementClass.cast(statement));
        }
      }
    }
    return rStatements.build();
  }
}
