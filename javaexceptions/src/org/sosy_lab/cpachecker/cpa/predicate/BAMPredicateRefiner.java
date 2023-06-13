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
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.bam.BAMBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * This is a small extension of {@link PredicateCPARefiner} that supplies it with a special
 * {@link BlockFormulaStrategy} and {@link RefinementStrategy} so that it respects BAM.
 *
 * So the hierarchy is as follows:
 * <pre>{@code
 *        Refiner                  ARGBasedRefiner                     RefinementStrategy
 *           ^                           ^                                     ^
 *           |                           |                                     |
 * AbstractARGBasedRefiner               |                     PredicateAbstractionRefinementStrategy
 *           ^                           |                                     ^
 *           |                           |                                     |
 *     BAMBasedRefiner    --->    PredicateCPARefiner  --->   BAMPredicateAbstractionRefinementStrategy
 *}
 * Here ^ means inheritance and -> means reference.
 *
 * BAMPredicateRefiner is only used for encapsulating this and providing the static factory method.
 */
public abstract class BAMPredicateRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return BAMBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

  @SuppressWarnings("resource")
  public static ARGBasedRefiner create0(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    BAMPredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, BAMPredicateCPA.class, BAMPredicateRefiner.class);
    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    Solver solver = predicateCpa.getSolver();
    PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();

    BlockFormulaStrategy blockFormulaStrategy = new BAMBlockFormulaStrategy(pfmgr);

    RefinementStrategy strategy =
        new BAMPredicateAbstractionRefinementStrategy(
            config, logger, solver, predicateCpa.getPredicateManager());

    return new PredicateCPARefinerFactory(pCpa)
        .setBlockFormulaStrategy(blockFormulaStrategy)
        .create(strategy);
  }
}
