// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.util;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

public class ReusableCoreComponents {
  private final CompositeCPA cpa;

  private final Algorithm algorithm;

  private final ReachedSet reachedSet;

  public ReusableCoreComponents(
      final CompositeCPA pCPA,
      final Algorithm pAlgorithm,
      final ReachedSet pReachedSet) {
    cpa = pCPA;
    
    algorithm = pAlgorithm;
    
    reachedSet = pReachedSet;
    reachedSet.clear();
  }

  public CompositeCPA getCpa() {
    return cpa;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public ReachedSet getReachedSet() {
    return reachedSet;
  }
}
