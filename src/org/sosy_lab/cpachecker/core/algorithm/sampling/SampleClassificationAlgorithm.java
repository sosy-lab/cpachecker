// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SampleClassificationAlgorithm {

  private final ConfigurableProgramAnalysis cpa;
  private final AlgorithmFactory algorithmFactory;
  private final ReachedSetFactory reachedSetFactory;

  public SampleClassificationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      CFA pCfa,
      Specification pSpecification)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            pConfig, pLogger, pShutdownManager.getNotifier(), AggregatedReachedSets.empty());
    cpa = coreComponents.createCPA(pCfa, pSpecification);
    algorithmFactory =
        new CPAAlgorithmFactory(cpa, pLogger, pConfig, pShutdownManager.getNotifier());
    reachedSetFactory = new ReachedSetFactory(pConfig, pLogger);
  }

  public SampleClass run(Sample sample, CFANode entryNode)
      throws CPAException, InterruptedException {
    // Run first analysis: Check whether sample is reachable from the initial state
    ReachedSet reachedSet = reachedSetFactory.create(cpa);
    AbstractState initialState =
        cpa.getInitialState(entryNode, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(entryNode, StateSpacePartition.getDefaultPartition());
    reachedSet.add(initialState, initialPrecision);

    Algorithm algorithm = algorithmFactory.newInstance();
    AlgorithmStatus status = algorithm.run(reachedSet);

    Set<MemoryLocation> relevantVariables = sample.getVariableValues().keySet();
    for (AbstractState state : reachedSet) {
      if (sample.getLocation().equals(AbstractStates.extractLocation(state))
          && sample
              .getVariableValues()
              .equals(SampleUtils.getValuesAndTypesFromAbstractState(state, relevantVariables))) {
        // Sample is reachable from the initial state
        if (status.isSound()) {
          return SampleClass.POSITIVE;
        }
        return SampleClass.UNKNOWN;
      }
    }

    // Run second analysis: Check whether an error state is reachable from the sample
    reachedSet = reachedSetFactory.create(cpa);
    initialState = makeInitialState(sample);
    initialPrecision = makeInitialPrecision(sample);
    reachedSet.add(initialState, initialPrecision);

    algorithm = algorithmFactory.newInstance();
    status = algorithm.run(reachedSet);

    if (reachedSet.wasTargetReached() && status.isSound()) {
      return SampleClass.NEGATIVE;
    }
    return SampleClass.UNKNOWN;
  }

  private AbstractState makeInitialState(Sample sample) throws InterruptedException {
    // Get initial state
    AbstractState initialState =
        cpa.getInitialState(sample.getLocation(), StateSpacePartition.getDefaultPartition());

    // Initialize value analysis with values from sample
    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(initialState, ValueAnalysisState.class);
    for (Entry<MemoryLocation, ValueAndType> assignment : sample.getVariableValues().entrySet()) {
      MemoryLocation variable = assignment.getKey();
      ValueAndType valueAndType = assignment.getValue();
      valueState.assignConstant(variable, valueAndType.getValue(), valueAndType.getType());
    }

    return initialState;
  }

  private Precision makeInitialPrecision(Sample sample) throws InterruptedException {
    // Initialize precision of value analysis
    Multimap<CFANode, MemoryLocation> valuePrecisionIncrement = HashMultimap.create();
    for (MemoryLocation variable : sample.getVariableValues().keySet()) {
      valuePrecisionIncrement.put(sample.getLocation(), variable);
    }
    ValueAnalysisCPA valueCPA = CPAs.retrieveCPA(cpa, ValueAnalysisCPA.class);
    valueCPA.incrementPrecision(valuePrecisionIncrement);

    // Get initial precision
    return cpa.getInitialPrecision(sample.getLocation(), StateSpacePartition.getDefaultPartition());
  }
}
