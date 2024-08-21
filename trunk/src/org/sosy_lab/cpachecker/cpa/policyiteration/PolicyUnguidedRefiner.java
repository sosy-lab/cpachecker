// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.Iterables;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * Unguided precision refiner: increase the number of generated templates at each refinement stage.
 */
@Options(prefix = "cpa.lpi")
public class PolicyUnguidedRefiner implements Refiner {

  @Option(
      secure = true,
      description =
          "Number of refinements after which the unrolling depth is increased."
              + "Set to -1 to never increase the depth.")
  private int unrollingRefinementThreshold = 2;

  private final PolicyCPA policyCPA;
  private final LoopBoundCPA loopBoundCPA;
  private final LogManager logger;

  private int refinementsPerformed = 0;

  public PolicyUnguidedRefiner(
      PolicyCPA pPolicyCPA, LoopBoundCPA pLoopstackCPA, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    policyCPA = pPolicyCPA;
    loopBoundCPA = pLoopstackCPA;
    logger = pLogger;
  }

  public static PolicyUnguidedRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    PolicyCPA policyCPA =
        CPAs.retrieveCPAOrFail(pCpa, PolicyCPA.class, PolicyUnguidedRefiner.class);
    LoopBoundCPA loopBoundCPA = CPAs.retrieveCPA(pCpa, LoopBoundCPA.class);

    return new PolicyUnguidedRefiner(
        policyCPA, loopBoundCPA, policyCPA.getLogger(), policyCPA.getConfig());
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    boolean out = policyCPA.adjustPrecision();
    if (out) {

      if (unrollingRefinementThreshold != -1
          && refinementsPerformed == unrollingRefinementThreshold
          && loopBoundCPA != null) {
        loopBoundCPA.incrementLoopIterationsBeforeAbstraction();
        logger.log(Level.INFO, "LPI Refinement: increasing unrolling bound.");
      }

      // Keep performing refinement.
      forceRestart(pReached);
      refinementsPerformed++;
      return true;
    } else {

      // No more tricks up our sleeves, do not clear the reached set.
      return false;
    }
  }

  private void forceRestart(ReachedSet reached) throws InterruptedException {
    ARGState firstChild =
        Iterables.getOnlyElement(((ARGState) reached.getFirstState()).getChildren());

    new ARGReachedSet(reached).removeSubtree(firstChild);
  }
}
