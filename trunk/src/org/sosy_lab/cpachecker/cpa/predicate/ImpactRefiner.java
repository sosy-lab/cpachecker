// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class ImpactRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, ImpactRefiner.class);
    RefinementStrategy strategy =
        new ImpactRefinementStrategy(
            predicateCpa.getConfiguration(),
            predicateCpa.getSolver(),
            predicateCpa.getPredicateManager());

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new PredicateCPARefinerFactory(pCpa).create(strategy), pCpa);
  }
}
