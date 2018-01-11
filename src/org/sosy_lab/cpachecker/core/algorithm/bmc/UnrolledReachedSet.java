/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class UnrolledReachedSet {

  private final Algorithm algorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final Set<CFANode> loopHeads;

  private final ReachedSet reachedSet;

  private final EnsureK ensureK;

  private @Nullable Set<Object> containedLoopBoundKeys = null;

  private int k = -1;

  private AlgorithmStatus lastStatus = AlgorithmStatus.SOUND_AND_PRECISE;

  public UnrolledReachedSet(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Set<CFANode> pLoopHeads,
      ReachedSet pReachedSet,
      EnsureK pEnsureK) {
    algorithm = Objects.requireNonNull(pAlgorithm);
    cpa = Objects.requireNonNull(pCPA);
    loopHeads = Objects.requireNonNull(pLoopHeads);
    reachedSet = Objects.requireNonNull(pReachedSet);
    ensureK = Objects.requireNonNull(pEnsureK);
  }

  public ReachedSet getReachedSet() {
    return reachedSet;
  }

  public int getCurrentMaxK() {
    return k;
  }

  public int getDesiredK() {
    LoopBoundCPA loopBoundCPA = CPAs.retrieveCPA(cpa, LoopBoundCPA.class);
    return loopBoundCPA == null ? Integer.MAX_VALUE : loopBoundCPA.getMaxLoopIterations();
  }

  public void setDesiredK(int pK) {
    LoopBoundCPA loopBoundCPA = CPAs.retrieveCPA(cpa, LoopBoundCPA.class);
    if (loopBoundCPA != null) {
      loopBoundCPA.setMaxLoopIterations(pK);
    }
  }

  public AlgorithmStatus ensureK() throws InterruptedException, CPAException {
    if (getDesiredK() > k) {
      lastStatus = ensureK.ensureK(algorithm, cpa, reachedSet);
      k = getDesiredK();
      containedLoopBoundKeys = null;
    }
    return lastStatus;
  }

  public Set<Object> getContainedLoopBoundKeys() {
    if (containedLoopBoundKeys == null) {
      getContainedLoopBoundKeys(k);
    }

    return containedLoopBoundKeys;
  }

  public Set<Object> getContainedLoopBoundKeys(int pK) {
    if (pK == k && containedLoopBoundKeys != null) {
      return containedLoopBoundKeys;
    }
    Set<Object> containedLoopBoundKeysK =
        AbstractStates.filterLocations(reachedSet, loopHeads)
            .transform(s -> AbstractStates.extractStateByType(s, LoopIterationReportingState.class))
            .filter(ls -> ls.getDeepestIteration() <= pK)
            .transform(LoopIterationReportingState::getPartitionKey)
            .toSet();
    if (pK == k) {
      containedLoopBoundKeys = containedLoopBoundKeysK;
    }
    return containedLoopBoundKeysK;
  }

  interface EnsureK {

    AlgorithmStatus ensureK(
        Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA, ReachedSet pReachedSet)
        throws InterruptedException, CPAException;
  }
}
