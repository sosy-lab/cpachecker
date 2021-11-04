// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.util.CPAs;

/** This class does not actually apply a refinement. It only exports all new counterexamples. */
// TODO can we delete this class?
public class SMGBasicRefiner implements Refiner {

  private final LogManager logger;
  private final ARGCPA argCpa;
  private static Set<ARGState> cache;

  private SMGBasicRefiner(LogManager pLogger, ARGCPA pArgCpa) {
    logger = pLogger;
    argCpa = pArgCpa;
  }

  public static final SMGBasicRefiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SMGBasicRefiner.class);
    SMGCPA smgCpa = CPAs.retrieveCPAOrFail(pCpa, SMGCPA.class, SMGBasicRefiner.class);

    cache = new HashSet<>();

    return new SMGBasicRefiner(smgCpa.getLogger(), argCpa);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws InterruptedException {
    final Map<ARGState, CounterexampleInfo> counterexamples =
        argCpa.getARGExporter().getAllCounterexamples(pReached);

    logger.log(Level.FINEST, "Filtering new SMG counterexample.");
    for (Map.Entry<ARGState, CounterexampleInfo> cex : counterexamples.entrySet()) {
      final ARGState lastSate = cex.getKey();
      if (cache.add(lastSate)) {
        argCpa.getARGExporter().exportCounterexampleOnTheFly(lastSate, cex.getValue());
      }
    }
    logger.log(Level.FINEST, "SMG counterexample has been exported.");
    return false;
  }
}
