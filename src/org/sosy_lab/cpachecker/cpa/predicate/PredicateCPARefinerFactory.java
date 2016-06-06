/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;

import javax.annotation.Nullable;

/**
 * Factory for {@link PredicateCPARefiner}, the base class for most refiners for the PredicateCPA.
 */
@Options(prefix = "cpa.predicate.refinement")
public class PredicateCPARefinerFactory {

  @Option(secure = true, description = "slice block formulas, experimental feature!")
  private boolean sliceBlockFormulas = false;

  @Option(
    secure = true,
    description = "use heuristic to extract predicates from the CFA statically on first refinement"
  )
  private boolean performInitialStaticRefinement = false;

  private final PredicateCPA predicateCpa;

  private @Nullable BlockFormulaStrategy blockFormulaStrategy = null;

  /**
   * Create a factory instance.
   * @param pCpa The CPA used for this whole analysis.
   * @throws InvalidConfigurationException
   *    If there is no PredicateCPA configured or if configuration is invalid.
   */
  @SuppressWarnings("options")
  public PredicateCPARefinerFactory(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    predicateCpa = CPAs.retrieveCPA(checkNotNull(pCpa), PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          PredicateCPARefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    predicateCpa.getConfiguration().inject(this);
  }

  /**
   * Ensure that {@link PredicateStaticRefiner} is not used.
   * This is mostly useful for configurations where static refinements do not make sense,
   * or a the predicate refiner is used as a helper for other refinements and should always
   * generate interpolants.
   * @return this
   * @throws InvalidConfigurationException If static refinements are enabled by the configuration.
   */
  public PredicateCPARefinerFactory forbidStaticRefinements() throws InvalidConfigurationException {
    if (performInitialStaticRefinement) {
      throw new InvalidConfigurationException(
          "Static refinement is not supported with the configured refiner, "
              + "please turn cpa.predicate.refinement.useStaticRefinement off.");
    }
    return this;
  }

  /**
   * Let the refiners created by this factory instance use the given {@link BlockFormulaStrategy}.
   * May be called only once, but does not need to be called
   * (in this case the configuration will determine the used BlockFormulaStrategy).
   * @return this
   */
  public PredicateCPARefinerFactory setBlockFormulaStrategy(
      BlockFormulaStrategy pBlockFormulaStrategy) {
    checkState(blockFormulaStrategy == null);
    blockFormulaStrategy = checkNotNull(pBlockFormulaStrategy);
    return this;
  }

  /**
   * Create a {@link PredicateCPARefiner}.
   * This factory can be reused afterwards.
   * @param pRefinementStrategy The refinement strategy to use.
   * @return A fresh instance.
   */
  public ARGBasedRefiner create(RefinementStrategy pRefinementStrategy)
      throws InvalidConfigurationException {
    checkNotNull(pRefinementStrategy);

    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    ShutdownNotifier shutdownNotifier = predicateCpa.getShutdownNotifier();
    Solver solver = predicateCpa.getSolver();
    PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();

    CFA cfa = predicateCpa.getCfa();
    MachineModel machineModel = cfa.getMachineModel();
    Optional<VariableClassification> variableClassification = cfa.getVarClassification();
    Optional<LoopStructure> loopStructure = cfa.getLoopStructure();

    PredicateAbstractionManager predAbsManager = predicateCpa.getPredicateManager();
    PredicateCPAInvariantsManager invariantsManager = predicateCpa.getInvariantsManager();

    PrefixProvider prefixProvider = predicateCpa.getPrefixProvider();
    PrefixSelector prefixSelector = new PrefixSelector(variableClassification, loopStructure);

    InterpolationManager interpolationManager =
        new InterpolationManager(
            pfmgr, solver, loopStructure, variableClassification, config, shutdownNotifier, logger);

    PathChecker pathChecker =
        new PathChecker(config, logger, shutdownNotifier, machineModel, pfmgr, solver);

    BlockFormulaStrategy bfs;
    if (blockFormulaStrategy != null) {
      if (sliceBlockFormulas) {
        throw new InvalidConfigurationException(
            "Block-formula slicing is not supported with this refiner, "
                + "please turn cpa.predicate.refinement.sliceBlockFormula off.");
      }
      bfs = blockFormulaStrategy;
    } else {
      bfs = sliceBlockFormulas ? new BlockFormulaSlicer(pfmgr) : new BlockFormulaStrategy();
    }

    ARGBasedRefiner refiner =
        new PredicateCPARefiner(
            config,
            logger,
            loopStructure,
            bfs,
            solver,
            pfmgr,
            interpolationManager,
            pathChecker,
            prefixProvider,
            prefixSelector,
            invariantsManager,
            pRefinementStrategy);

    if (performInitialStaticRefinement) {
      refiner =
          new PredicateStaticRefiner(
              config,
              logger,
              shutdownNotifier,
              solver,
              pfmgr,
              predAbsManager,
              bfs,
              interpolationManager,
              pathChecker,
              cfa,
              refiner);
    }

    return refiner;
  }
}
