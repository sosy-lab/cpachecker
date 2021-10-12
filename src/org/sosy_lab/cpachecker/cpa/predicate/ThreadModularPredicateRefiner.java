/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public abstract class ThreadModularPredicateRefiner implements Refiner {

  @SuppressWarnings("resource")
  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, PredicateGlobalRefiner.class);
    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    Solver solver = predicateCpa.getSolver();

    GlobalRefinementStrategy strategy =
        new PredicateAbstractionGlobalRefinementStrategy(
            config,
            logger,
            predicateCpa.getPredicateManager(),
            solver);

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder = configBuilder.copyFrom(predicateCpa.getConfiguration());
    configBuilder.setOption("cpa.predicate.refinement.recomputeBlockFormulas", "true");
    Configuration newConfig = configBuilder.build();

    PredicateCPARefinerFactory factory = new PredicateCPARefinerFactory(pCpa, newConfig);
    ARGBasedRefiner refiner = factory.create(strategy);

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new ThreadModularCPARefiner(
        logger,
        strategy,
        config,
            refiner),
        pCpa);
  }
}
