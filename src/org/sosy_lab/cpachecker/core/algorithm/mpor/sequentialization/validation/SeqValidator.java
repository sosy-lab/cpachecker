// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqValidator {

  /**
   * Returns {@code pCaseClauses} as is or throws an {@link AssertionError} if:
   *
   * <ul>
   *   <li>not all target {@code pc} (e.g. {@code 42} in {@code pc[0] = 42;} are present as origin
   *       {@code pc} (e.g. {@code case 42:})
   *   <li>not all origin {@code pc} are also target {@code pc} somewhere in the thread simulation
   * </ul>
   *
   * Every sequentialization needs to fulfill this property, otherwise it is faulty.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> validateCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses, LogManager pLogger) {

    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      // create the map of originPc n (e.g. case n) to target pc(s) m (e.g. pc[i] = m)
      ImmutableMap<Integer, ImmutableSet<Integer>> pcMap = getPcMap(entry.getValue());
      // ImmutableSet<Integer> allTargetPcs =
      // pcMap.values().stream().flatMap(Set::stream).collect(ImmutableSet.toImmutableSet());
      for (var pcEntry : pcMap.entrySet()) {
        // checkOriginPcAsTargetPc(pcEntry.getKey(), allTargetPcs, thread.id, pLogger);
        checkTargetPcsAsOriginPc(pcEntry.getValue(), pcMap.keySet(), thread.id, pLogger);
      }
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

  private static void checkOriginPcAsTargetPc(
      int pOriginPc, ImmutableSet<Integer> pAllTargetPc, int pThreadId, LogManager pLogger) {

    // exclude INIT_PC, it is (often) not present as a target pc
    if (pOriginPc != SeqUtil.INIT_PC) {
      if (!pAllTargetPc.contains(pOriginPc)) {
        String message =
            "origin pc " + pOriginPc + " does not exist as target pc in thread " + pThreadId;
        pLogger.log(Level.SEVERE, message);
        MPORAlgorithm.fail(message);
      }
    }
  }

  private static void checkTargetPcsAsOriginPc(
      ImmutableSet<Integer> pTargetPcs,
      ImmutableSet<Integer> pOriginPcs,
      int pThreadId,
      org.sosy_lab.common.log.LogManager pLogger) {

    for (int targetPc : pTargetPcs) {
      // exclude EXIT_PC, it is never present as an origin pc
      if (targetPc != SeqUtil.EXIT_PC) {
        if (!pOriginPcs.contains(targetPc)) {
          String message =
              "target pc " + targetPc + " does not exist as origin pc in thread " + pThreadId;
          pLogger.log(Level.SEVERE, message);
          MPORAlgorithm.fail(message);
        }
      }
    }
  }
}
