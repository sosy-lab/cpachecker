/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;

import com.google.common.base.Optional;

public abstract class PredicateRefiner implements Refiner {

  public static PredicateCPARefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(PredicateRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();
    Solver solver = predicateCpa.getSolver();
    PredicateStaticRefiner staticRefiner = predicateCpa.getStaticRefiner();
    MachineModel machineModel = predicateCpa.getMachineModel();
    Optional<LoopStructure> loopStructure = predicateCpa.getCfa().getLoopStructure();
    Optional<VariableClassification> variableClassification = predicateCpa.getCfa().getVarClassification();

    InterpolationManager manager = new InterpolationManager(
        pfmgr,
        solver,
        loopStructure,
        variableClassification,
        config,
        predicateCpa.getShutdownNotifier(),
        logger);

    PathChecker pathChecker = new PathChecker(
        config,
        logger,
        predicateCpa.getShutdownNotifier(),
        machineModel,
        pfmgr,
        solver);

    PrefixProvider prefixProvider = new PredicateBasedPrefixProvider(config, logger, solver, pfmgr);

    RefinementStrategy strategy = new PredicateAbstractionRefinementStrategy(
        config,
        logger,
        predicateCpa.getShutdownNotifier(),
        predicateCpa.getPredicateManager(),
        staticRefiner,
        solver);

    return new PredicateCPARefiner(
        config,
        logger,
        pCpa,
        manager,
        pathChecker,
        prefixProvider,
        pfmgr,
        strategy,
        solver,
        predicateCpa.getAssumesStore(),
        predicateCpa.getCfa());
  }
}
