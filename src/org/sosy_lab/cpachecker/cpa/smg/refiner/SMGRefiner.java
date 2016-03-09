/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class SMGRefiner extends GenericRefiner<SMGState, SMGInterpolant> {

  private final Set<ControlAutomatonCPA> automatonCpas;
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;
  private final SMGFeasibilityChecker checker;

  public static final SMGRefiner create(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {

    final ARGCPA argCpa = retrieveCPA(pCpa, ARGCPA.class);
    final SMGCPA smgCpa = retrieveCPA(pCpa, SMGCPA.class);
    Set<ControlAutomatonCPA> automatonCpas = CPAs.asIterable(pCpa).filter(ControlAutomatonCPA.class).toSet();

    smgCpa.injectRefinablePrecision();

    final LogManager logger = smgCpa.getLogger();
    final Configuration config = smgCpa.getConfiguration();
    final CFA cfa = smgCpa.getCFA();

    final StrongestPostOperator<SMGState> strongestPostOp =
        new SMGStrongestPostOperator(logger, config, cfa);

    SMGState initialState = smgCpa.getInitialState(cfa.getMainFunction());

    final SMGFeasibilityChecker checker =
        new SMGFeasibilityChecker(strongestPostOp, logger, cfa, config, initialState);

    SMGState emptyState = smgCpa.getInitialState(cfa.getMainFunction());

    final GenericPrefixProvider<SMGState> prefixProvider =
        new SMGPrefixProvider(logger, cfa, config, emptyState);

    final SMGInterpolantManager smgInterpolantManager = new SMGInterpolantManager(smgCpa.getMachineModel(), logger, cfa);

    return new SMGRefiner(argCpa,
        checker,
        strongestPostOp,
        new PathExtractor(logger, config),
        prefixProvider,
        config,
        logger,
        smgCpa.getShutdownNotifier(),
        cfa,
        smgCpa,
        smgInterpolantManager, automatonCpas);

  }

  SMGRefiner(ARGCPA pArgCpa, SMGFeasibilityChecker pChecker, StrongestPostOperator<SMGState> pStrongestPostOp,
      PathExtractor pPathExtractor, GenericPrefixProvider<SMGState> pPrefixProvider, Configuration pConfig,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa, SMGCPA pSMGCPA,
      InterpolantManager<SMGState, SMGInterpolant> pSmgInterpolantManager, Set<ControlAutomatonCPA> pAutomatonCpas) throws InvalidConfigurationException {
    super(pArgCpa, pChecker,
        new SMGPathInterpolator(pChecker,
            pStrongestPostOp,
            pPrefixProvider,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            pSMGCPA,
            pSmgInterpolantManager,
            pAutomatonCpas),
        pSmgInterpolantManager,
        pPathExtractor,
        pConfig,
        pLogger);
    automatonCpas = pAutomatonCpas;
    checker = pChecker;

  }

  private VariableTrackingPrecision mergeValuePrecisionsForSubgraph(
      final ARGState pRefinementRoot,
      final ARGReachedSet pReached
  ) {
    // get all unique precisions from the subtree
    Set<VariableTrackingPrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(extractValuePrecision(pReached, descendant));
    }

    // join all unique precisions into a single precision
    VariableTrackingPrecision mergedPrecision = Iterables.getLast(uniquePrecisions);
    for (VariableTrackingPrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.join(precision);
    }

    return mergedPrecision;
  }

  private Collection<ARGState> getNonCoveredStatesInSubgraph(ARGState pRoot) {
    Collection<ARGState> subgraph = new HashSet<>();
    for (ARGState state : pRoot.getSubgraph()) {
      if (!state.isCovered()) {
        subgraph.add(state);
      }
    }
    return subgraph;
  }

  private VariableTrackingPrecision extractValuePrecision(final ARGReachedSet pReached,
      ARGState state) {
    return (VariableTrackingPrecision) Precisions
        .asIterable(pReached.asReachedSet().getPrecision(state))
        .filter(VariableTrackingPrecision.isMatchingCPAClass(SMGCPA.class))
        .get(0);
  }

  @Override
  public boolean isErrorPathFeasible(ARGPath pErrorPath) throws CPAException, InterruptedException {
    return checker.isFeasible(pErrorPath, automatonCpas);
  }

  @Override
  protected void refineUsingInterpolants(ARGReachedSet pReached,
      InterpolationTree<SMGState, SMGInterpolant> pInterpolationTree) {

    Map<ARGState, List<Precision>> refinementInformation = new HashMap<>();
    Collection<ARGState> refinementRoots = pInterpolationTree.obtainRefinementRoots(restartStrategy);

    for(ARGState root : refinementRoots) {

      // merge the value precisions of the subtree, and refine it
      Precision precision = mergeValuePrecisionsForSubgraph(root, pReached)
          .withIncrement(pInterpolationTree.extractPrecisionIncrement(root));

      refinementInformation.put(root, Collections.singletonList(precision));
    }

    for (Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {

      Predicate<? super Precision> precisionType = VariableTrackingPrecision.isMatchingCPAClass(SMGCPA.class);
      List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(1);
      precisionTypes.add(precisionType);
      pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
    }


  }

  @Override
  protected void printAdditionalStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {

  }
}