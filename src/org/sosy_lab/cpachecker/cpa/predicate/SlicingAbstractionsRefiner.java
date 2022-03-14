// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * This is Refiner for Slicing Abstractions like in the papers: "Slicing Abstractions"
 * (doi:10.1007/978-3-540-75698-9_2) "Splitting via Interpolants" (doi:10.1007/978-3-642-27940-9_13)
 */
public class SlicingAbstractionsRefiner implements Refiner, StatisticsProvider {

  private final ARGBasedRefiner refiner;
  private final ARGCPA argCpa;

  public SlicingAbstractionsRefiner(ARGBasedRefiner pRefiner, ARGCPA pCpa) {
    refiner = pRefiner;
    argCpa = pCpa;
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    ARGCPA argCpa = CPAs.retrieveCPA(pCpa, ARGCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          SlicingAbstractionsRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    RefinementStrategy strategy =
        new SlicingAbstractionsStrategy(predicateCpa, predicateCpa.getConfiguration());

    PredicateCPARefinerFactory factory = new PredicateCPARefinerFactory(pCpa);
    ARGBasedRefiner refiner = factory.create(strategy);
    return new SlicingAbstractionsRefiner(refiner, argCpa);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    CounterexampleInfo counterexample = null;

    while (true) {
      Optional<AbstractState> optionalTargetState =
          pReached.stream().filter(AbstractStates::isTargetState).findFirst();
      if (optionalTargetState.isPresent()) {
        AbstractState targetState = optionalTargetState.orElseThrow();
        ARGPath errorPath = ARGUtils.getShortestPathTo((ARGState) targetState);
        ARGReachedSet reached = new ARGReachedSet(pReached, argCpa);
        counterexample = refiner.performRefinementForPath(reached, errorPath);
        if (!counterexample.isSpurious()) {
          ((ARGState) targetState).addCounterexampleInformation(counterexample);
          return false;
        } else {
          if (!SlicingAbstractionsUtils.checkProgress(pReached, errorPath)) {
            throw new RefinementFailedException(Reason.RepeatedCounterexample, errorPath);
          }
        }
      } else {
        break;
      }
    }
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (refiner instanceof StatisticsProvider) {
      ((StatisticsProvider) refiner).collectStatistics(pStatsCollection);
    }
  }
}
