/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
 *
 *        Refiner                  ARGBasedRefiner                     RefinementStrategy
 *           ^                           ^                                     ^
 *           |                           |                                     |
 * AbstractARGBasedRefiner               |                     PredicateAbstractionRefinementStrategy
 *           ^                           |                                     ^
 *           |                           |                                     |
 *     BAMBasedRefiner    --->    PredicateCPARefiner  --->   BAMPredicateAbstractionRefinementStrategy
 *
 * Here ^ means inheritance and -> means reference.
 *
 * BAMPredicateRefiner is only used for encapsulating this and providing the static factory method.
 */
public abstract class BAMPredicateRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return BAMBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

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
            config, logger, predicateCpa, solver, predicateCpa.getPredicateManager());

    return new PredicateCPARefinerFactory(pCpa)
        .setBlockFormulaStrategy(blockFormulaStrategy)
        .create(strategy);
  }
}
