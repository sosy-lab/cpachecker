// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ExecutionAlgorithm implements Algorithm {

  private final ConfigurableProgramAnalysis cpa;
  private final Algorithm algorithm;

  public ExecutionAlgorithm(ConfigurableProgramAnalysis pCpa, Algorithm pAlgorithm) {
    cpa = pCpa;
    algorithm = pAlgorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus run = algorithm.run(reachedSet);
    if (run == AlgorithmStatus.SOUND_AND_PRECISE) {
      return run;
    }
    boolean hasTarget = false;
    for (AbstractState state : reachedSet) {
      if (state instanceof Targetable target) {
        if (target.isTarget()) {
          hasTarget = true;
          break;
        }
      }
    }
    if (run.isSound() && !hasTarget) {
      return run;
    }
    if (run.isPrecise() && hasTarget) {
      return run;
    }
    return run;
  }
}
