// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class RestrictedProgramDomainAlgorithm implements Algorithm {

  private final Algorithm innerAlgorithm;
  private final CFA cfa;

  public RestrictedProgramDomainAlgorithm(Algorithm pAlgorithm, CFA pCfa) {
    innerAlgorithm = pAlgorithm;
    cfa = pCfa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    if (cfa.getVarClassification().isPresent()) {
      if (cfa.getVarClassification().orElseThrow().hasRelevantNonIntAddVars()) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
    }

    return innerAlgorithm.run(pReachedSet);
  }
}
