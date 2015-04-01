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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier.PrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refiner.GenericRefiner;
import org.sosy_lab.cpachecker.util.refiner.StrongestPostOperator;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisRefiner
    extends GenericRefiner<ValueAnalysisState, ValueAnalysisInformation, ValueAnalysisInterpolant> {

  @Option(
      secure = true,
      description = "heuristic to sort targets based on the quality of interpolants deriveable from them")
  private boolean itpSortedTargets = false;

  private final ValueAnalysisFeasibilityChecker checker;

  private ValueAnalysisConcreteErrorPathAllocator concreteErrorPathAllocator;

  private ErrorPathClassifier classifier;

  /**
   * keep log of feasible targets that were already found
   */
  private final Set<ARGState> feasibleTargets = new HashSet<>();

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

    ValueAnalysisRefiner refiner = new ValueAnalysisRefiner(
        checker,
        strongestPostOp,
        config,
        logger,
        valueAnalysisCpa.getShutdownNotifier(),
        cfa);

    return refiner;
  }

  ValueAnalysisRefiner(
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,

      final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {

    super(pFeasibilityChecker,
          new ValueAnalysisPathInterpolator(pFeasibilityChecker,
                                            pStrongestPostOperator,
                                            pConfig, pLogger, pShutdownNotifier, pCfa),
          ValueAnalysisInterpolantManager.getInstance(),
          ValueAnalysisCPA.class,
          pConfig,
          pLogger,
          pShutdownNotifier,
          pCfa);
          pConfig.inject(this);

    pConfig.inject(this);

    checker = pFeasibilityChecker;
    classifier    = new ErrorPathClassifier(pCfa.getVarClassification(), pCfa.getLoopStructure());

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

  /**
   * This method returns an unsorted, non-empty collection of target states
   * found during the analysis.
   *
   * @param pReached the set of reached states
   * @return the target states
   * @throws RefinementFailedException
   */
  @Override
  protected Collection<ARGState> getTargetStates(final ARGReachedSet pReached) throws RefinementFailedException {

    // sort the list, to either favor shorter paths or better interpolants
    Comparator<ARGState> comparator = new Comparator<ARGState>() {
      @Override
      public int compare(ARGState target1, ARGState target2) {
        try {
          ARGPath path1 = ARGUtils.getOnePathTo(target1);
          ARGPath path2 = ARGUtils.getOnePathTo(target2);

          if(itpSortedTargets) {
            List<ARGPath> prefixes1 = checker.getInfeasilbePrefixes(path1);
            List<ARGPath> prefixes2 = checker.getInfeasilbePrefixes(path2);

            int score1 = classifier.obtainScoreForPrefixes(prefixes1, PrefixPreference.DOMAIN_BEST_BOUNDED);
            int score2 = classifier.obtainScoreForPrefixes(prefixes2, PrefixPreference.DOMAIN_BEST_BOUNDED);

            if(score1 == score2) {
              return 0;
            }

            else if(score1 < score2) {
              return -1;
            }

            else {
              return 1;
            }
          }

          else {
            return path1.size() - path2.size();
          }
        } catch (CPAException | InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    };

    Collection<ARGState> targets = super.getTargetStates(pReached);

    List<ARGState> sortedTargets = FluentIterable.from(targets)
        .filter(Predicates.not(Predicates.in(feasibleTargets))).toSortedList(comparator);

    return sortedTargets;
  }
}

