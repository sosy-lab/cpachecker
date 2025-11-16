// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterministicSimulationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqFunctionBuilder {

  public static ImmutableList<SeqThreadSimulationFunction> buildThreadSimulationFunctions(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (!pOptions.loopUnrolling()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<SeqThreadSimulationFunction> rFunctions = ImmutableList.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(pClauses.keySet(), thread);
      String threadSimulation =
          NondeterministicSimulationUtil.buildSingleThreadSimulationByNondeterminismSource(
              pOptions, pGhostElements, thread, otherThreads, pClauses, pUtils);
      rFunctions.add(new SeqThreadSimulationFunction(threadSimulation, thread));
    }
    return rFunctions.build();
  }

  public static SeqThreadSimulationFunction getMainThreadSimulationFunction(
      ImmutableList<SeqThreadSimulationFunction> pFunctions) {

    for (SeqThreadSimulationFunction function : pFunctions) {
      if (function.thread.isMain()) {
        return function;
      }
    }
    throw new IllegalArgumentException("could not find main thread in pFunctions");
  }
}
