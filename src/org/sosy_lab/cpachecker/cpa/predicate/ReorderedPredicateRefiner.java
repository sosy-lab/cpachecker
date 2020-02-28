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
import org.sosy_lab.cpachecker.util.refinement.DelegatingARGBasedRefiner;

public abstract class ReorderedPredicateRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, ReorderedPredicateRefiner.class);

    LogManager logger = predicateCpa.getLogger();

    RefinementStrategy localStrategy =
        new PredicateAbstractionRefinementStrategy(
            predicateCpa.getConfiguration(),
            logger,
            predicateCpa.getPredicateManager(),
            predicateCpa.getSolver());

    PredicateCPARefinerFactory localFactory = new PredicateCPARefinerFactory(predicateCpa);

    ARGBasedRefiner localRefiner =
        localFactory.create(localStrategy, predicateCpa.getImprecisePathFormulaManager());

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder = configBuilder.copyFrom(predicateCpa.getConfiguration());
    configBuilder.setOption("cpa.predicate.refinement.recomputeBlockFormulas", "true");
    Configuration newConfig = configBuilder.build();

    RefinementStrategy globalStrategy =
        new ThreadModularRefinementStrategy(
            predicateCpa.getConfiguration(),
            logger,
            predicateCpa.getPredicateManager(),
            predicateCpa.getSolver());

    PredicateCPARefinerFactory globalFactory =
        new PredicateCPARefinerFactory(predicateCpa, newConfig);
    ARGBasedRefiner globalRefiner =
        globalFactory.create(globalStrategy, predicateCpa.getPathFormulaManager());

    // first value analysis refiner, then predicate analysis refiner
    return AbstractARGBasedRefiner
        .forARGBasedRefiner(new DelegatingARGBasedRefiner(logger, localRefiner, globalRefiner), cpa);
  }

}
