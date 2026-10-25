// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.java_smt.api.SolverException;

/** Reconstructs a concrete schedule before delegating infeasible traces to predicate refinement. */
public final class ConcurrentPredicateRefiner implements ARGBasedRefiner, StatisticsProvider {
  private final ARGBasedRefiner delegate;
  private final ConcurrentPathChecker checker;

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(new ConcurrentPredicateRefiner(cpa), cpa);
  }

  private ConcurrentPredicateRefiner(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    PredicateCPA predicate =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, ConcurrentPredicateRefiner.class);
    ConcurrentCPA concurrent =
        CPAs.retrieveCPAOrFail(cpa, ConcurrentCPA.class, ConcurrentPredicateRefiner.class);
    ARGCPA arg = CPAs.retrieveCPAOrFail(cpa, ARGCPA.class, ConcurrentPredicateRefiner.class);
    checker =
        new ConcurrentPathChecker(predicate, concurrent.getTransferRelation(), arg.getLogger());
    delegate = PredicateRefiner.create0(cpa);
  }

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet reached, ARGPath path)
      throws CPAException, InterruptedException {
    try {
      Optional<ARGPath> schedule = checker.findSchedule(path);
      if (schedule.isPresent()) {
        return checker.checkSchedule(schedule.orElseThrow());
      }
    } catch (SolverException e) {
      throw new CPAException("Could not reconstruct a concurrent counterexample", e);
    }
    CounterexampleInfo refined = delegate.performRefinementForPath(reached, path);
    if (!refined.isSpurious()) {
      throw new CPAException("Predicate counterexample has no feasible concurrent schedule");
    }
    return refined;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statistics) {
    if (delegate instanceof StatisticsProvider provider) {
      provider.collectStatistics(statistics);
    }
  }
}
