// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;

public class TerminationToSafetyUtils {

  public static void shareTheSolverBetweenCPAs(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    checkNotNull(pCpa);

    TerminationToReachCPA terminationCPA =
        CPAs.retrieveCPAOrFail(pCpa, TerminationToReachCPA.class, TerminationToSafetyUtils.class);
    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, TerminationToSafetyUtils.class);

    terminationCPA.setSolver(predicateCPA.getSolver());
  }
}
