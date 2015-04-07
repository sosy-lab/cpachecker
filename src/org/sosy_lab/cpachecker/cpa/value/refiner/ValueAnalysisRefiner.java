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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.SortingPathExtractor;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refiner.ErrorPathClassifier;
import org.sosy_lab.cpachecker.util.refiner.GenericRefiner;
import org.sosy_lab.cpachecker.util.refiner.PathExtractor;
import org.sosy_lab.cpachecker.util.refiner.StrongestPostOperator;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisRefiner
    extends GenericRefiner<ValueAnalysisState, ValueAnalysisInformation, ValueAnalysisInterpolant> {

  private final ValueAnalysisFeasibilityChecker checker;

  private ValueAnalysisConcreteErrorPathAllocator concreteErrorPathAllocator;

  public static ValueAnalysisRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    final ValueAnalysisFeasibilityChecker checker =
        new ValueAnalysisFeasibilityChecker(logger, cfa, config);

    final StrongestPostOperator<ValueAnalysisState> strongestPostOp =
        new ValueAnalysisStrongestPostOperator(logger, cfa);

    final ErrorPathClassifier pathClassifier = new ErrorPathClassifier(cfa.getVarClassification(),
                                                                       cfa.getLoopStructure());

    ValueAnalysisRefiner refiner = new ValueAnalysisRefiner(
        checker,
        strongestPostOp,
        new SortingPathExtractor(checker,
                                 pathClassifier,
                                 logger, config),
        pathClassifier,
        config,
        logger,
        valueAnalysisCpa.getShutdownNotifier(),
        cfa);

    return refiner;
  }

  ValueAnalysisRefiner(
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final PathExtractor pPathExtractor,
      final ErrorPathClassifier pPathClassifier,
      final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {

    super(pFeasibilityChecker,
          new ValueAnalysisPathInterpolator(pFeasibilityChecker,
                                            pStrongestPostOperator,
                                            pPathClassifier,
                                            pConfig, pLogger, pShutdownNotifier, pCfa),
          ValueAnalysisInterpolantManager.getInstance(),
          pPathExtractor,
          ValueAnalysisCPA.class,
          pConfig,
          pLogger,
          pShutdownNotifier,
          pCfa);
          pConfig.inject(this);

    pConfig.inject(this);

    checker = pFeasibilityChecker;

    concreteErrorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(pLogger, pShutdownNotifier, pCfa.getMachineModel());
  }


  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InterruptedException
   * @throws CPAException
   */
  @Override
  protected Model createModel(ARGPath errorPath) throws InterruptedException, CPAException {
    return concreteErrorPathAllocator.allocateAssignmentsToPath(checker.evaluate(errorPath));
  }
}

