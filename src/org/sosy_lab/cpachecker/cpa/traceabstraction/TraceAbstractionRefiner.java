// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class TraceAbstractionRefiner implements Refiner {

  public static Refiner create(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {
    ARGBasedRefiner refiner = createRefiner(pCpa, pLogger, pNotifier);
    return AbstractARGBasedRefiner.forARGBasedRefiner(refiner, pCpa);
  }

  @SuppressWarnings("resource")
  private static ARGBasedRefiner createRefiner(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {

    TraceAbstractionCPA taCpa =
        CPAs.retrieveCPAOrFail(pCpa, TraceAbstractionCPA.class, TraceAbstractionRefiner.class);
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, TraceAbstractionRefiner.class);

    RefinementStrategy strategy =
        new TraceAbstractionRefinementStrategy(
            predicateCpa.getConfiguration(),
            pLogger,
            pNotifier,
            taCpa.getInterpolationSequenceStorage(),
            predicateCpa.getPredicateManager(),
            predicateCpa.getSolver());

    return new PredicateCPARefinerFactory(pCpa).create(strategy);
  }
}
