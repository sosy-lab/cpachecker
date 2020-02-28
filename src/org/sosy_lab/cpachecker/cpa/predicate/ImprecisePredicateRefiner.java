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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public abstract class ImprecisePredicateRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, ReorderedPredicateRefiner.class);

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder = configBuilder.copyFrom(predicateCpa.getConfiguration());
    configBuilder.setOption("cpa.predicate.refinement.recomputeBlockFormulas", "true");
    configBuilder.setOption("cpa.predicate.useHavocAbstraction", "true");
    Configuration newConfig = configBuilder.build();

    PathFormulaManager pMngr = predicateCpa.createPathFormulaManager(newConfig);

    RefinementStrategy strategy =
        new PredicateAbstractionRefinementStrategy(
            predicateCpa.getConfiguration(),
            predicateCpa.getLogger(),
            predicateCpa.getPredicateManager(),
            predicateCpa.getSolver());

    PredicateCPARefinerFactory globalFactory =
        new PredicateCPARefinerFactory(predicateCpa, newConfig);
    return AbstractARGBasedRefiner.forARGBasedRefiner(
        globalFactory.create(strategy, pMngr),
        cpa);
  }

}
