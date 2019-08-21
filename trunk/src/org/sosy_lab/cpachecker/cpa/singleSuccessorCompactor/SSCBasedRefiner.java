/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;

public class SSCBasedRefiner extends AbstractARGBasedRefiner {

  private final SingleSuccessorCompactorCPA sscCpa;

  private SSCBasedRefiner(
      ARGBasedRefiner pRefiner,
      ARGCPA pArgCpa,
      SingleSuccessorCompactorCPA pSscCpa,
      LogManager pLogger) {
    super(pRefiner, pArgCpa, pLogger);
    sscCpa = pSscCpa;
  }

  /**
   * creates a dummy instance of this refiner, that does nothing but report found error paths.
   *
   * @see AbstractARGBasedRefiner#create an identical approach, just with/without SSC.
   */
  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return SSCBasedRefiner.forARGBasedRefiner(
        (rs, path) -> CounterexampleInfo.feasibleImprecise(path), pCpa);
  }

  /**
   * Create a {@link Refiner} instance that supports SSC from a {@link ARGBasedRefiner} instance.
   */
  public static Refiner forARGBasedRefiner(
      final ARGBasedRefiner pRefiner, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    checkArgument(
        !(pRefiner instanceof Refiner),
        "ARGBasedRefiners may not implement Refiner, choose between these two!");

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SSCBasedRefiner.class);
    SingleSuccessorCompactorCPA sscCpa =
        CPAs.retrieveCPAOrFail(pCpa, SingleSuccessorCompactorCPA.class, SSCBasedRefiner.class);
    return new SSCBasedRefiner(pRefiner, argCpa, sscCpa, argCpa.getLogger());
  }

  @Override
  protected final CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    checkArgument(
        !(pReached instanceof SSCReachedSet),
        "Wrapping of SSC-based refiners inside SSC-based refiners is not allowed.");
    checkArgument(pPath instanceof SSCPath, "SSCPath required for SSCReachedSet.");
    assert pPath.size() > 0;

    // wrap the original reached-set to have a valid "view" on all reached states.
    return super.performRefinementForPath(new SSCReachedSet(pReached, (SSCPath)pPath), pPath);
  }

  @Override
  protected final ARGPath computePath(ARGState pLastElement, ARGReachedSet pMainReachedSet)
      throws InterruptedException, CPATransferException {
    assert pMainReachedSet.asReachedSet().contains(pLastElement)
        : "targetState must be in mainReachedSet.";

    ARGPath shortPath = ARGUtils.getOnePathTo(pLastElement);
    // info: never access fullPath from shortPath
    return new SSCPath(sscCpa, pMainReachedSet, shortPath.asStatesList());
  }
}
