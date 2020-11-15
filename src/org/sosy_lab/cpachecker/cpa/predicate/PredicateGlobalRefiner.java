// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public abstract class PredicateGlobalRefiner implements Refiner {

  @SuppressWarnings("resource")
  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, PredicateGlobalRefiner.class);
    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    Solver solver = predicateCpa.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();

    GlobalRefinementStrategy strategy =
        new PredicateAbstractionGlobalRefinementStrategy(
            config, logger, predicateCpa.getPredicateManager(), solver);

    return new PredicateCPAGlobalRefiner(
        logger,
        fmgr,
        strategy,
        solver,
        CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, PredicateGlobalRefiner.class),
        config);
  }
}
