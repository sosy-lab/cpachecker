// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.base.Functions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
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
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SampleUnrollingAlgorithm {

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final ConfigurableProgramAnalysis cpa;

  public SampleUnrollingAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      CFA pCfa,
      Specification pSpecification)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    logger = pLogger.withComponentName("SampleUnrollingAlgorithm");
    reachedSetFactory = new ReachedSetFactory(pConfig, logger);

    // TODO: Build CPA here: We want a location+value+predicate analysis with standard wrappers.
    //  For this we can add an option `unrollingConfig` (similar to e.g. overflow config) and use
    //  the config file specified via this option here.
    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            pConfig, logger, pShutdownManager.getNotifier(), AggregatedReachedSets.empty());
    cpa = coreComponents.createCPA(pCfa, pSpecification);
    GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
  }

  public Set<Sample> unrollSample(Sample initialSample, Loop loop)
      throws CPAException, InterruptedException {
    AbstractState initialState = makeInitialState(initialSample);
    Precision initialPrecision = makeInitialPrecision(initialSample);
    ReachedSet reachedSet = reachedSetFactory.create(cpa);
    reachedSet.add(initialState, initialPrecision);
    logger.log(Level.FINER, "Unrolling sample...");

    SampleTreeNode sampleTreeRoot = run(reachedSet, initialSample, loop);

    ImmutableSet.Builder<Sample> builder = ImmutableSet.builder();
    Queue<SampleTreeNode> waitlist = new ArrayDeque<>();
    waitlist.add(sampleTreeRoot);
    while (!waitlist.isEmpty()) {
      SampleTreeNode node = waitlist.poll();
      builder.add(node.getSample());
      waitlist.addAll(node.getChildren());
    }
    return builder.build();
  }

  private AbstractState makeInitialState(Sample sample) throws InterruptedException {
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

    return cpa.getInitialPrecision(sample.getLocation(), StateSpacePartition.getDefaultPartition());
  }

  private SampleTreeNode run(ReachedSet reachedSet, Sample initialSample, Loop loop)
      throws CPAException, InterruptedException {
    TransferRelation transferRelation = cpa.getTransferRelation();
    PrecisionAdjustment precisionAdjustment = cpa.getPrecisionAdjustment();
    Set<MemoryLocation> relevantVariables = initialSample.getVariableValues().keySet();

    AbstractState first = reachedSet.getFirstState();
    assert initialSample.getLocation().equals(getLocationForState(first));
    SampleTreeNode root = new SampleTreeNode(initialSample);
    Map<AbstractState, SampleTreeNode> nodes = new HashMap<>();
    nodes.put(first, root);

    // All samples obtained via unrolling have the same class as the initial sample
    SampleClass sampleClass = initialSample.getSampleClass();

    while (reachedSet.hasWaitingState()) {
      AbstractState state = reachedSet.popFromWaitlist();
      Precision precision = reachedSet.getPrecision(state);
      SampleTreeNode node = nodes.get(state);
      Sample sample = node.getSample();
      for (AbstractState successor : transferRelation.getAbstractSuccessors(state, precision)) {
        // Update successor via precision adjustment
        Optional<PrecisionAdjustmentResult> precAdjustmentOptional =
            precisionAdjustment.prec(
                successor, precision, reachedSet, Functions.identity(), successor);
        if (precAdjustmentOptional.isEmpty()) {
          continue;
        }
        PrecisionAdjustmentResult precAdjustmentResult = precAdjustmentOptional.orElseThrow();
        successor = precAdjustmentResult.abstractState();
        Precision successorPrecision = precAdjustmentResult.precision();

        // Build sample for successor state
        ValueAnalysisState valueSuccessor =
            AbstractStates.extractStateByType(successor, ValueAnalysisState.class);
        assert valueSuccessor != null;
        ImmutableMap.Builder<MemoryLocation, ValueAndType> builder = ImmutableMap.builder();
        for (MemoryLocation memoryLocation :
            valueSuccessor.createInterpolant().getMemoryLocations()) {
          if (relevantVariables.contains(memoryLocation)) {
            builder.put(memoryLocation, valueSuccessor.getValueAndTypeFor(memoryLocation));
          }
        }
        Sample successorSample =
            new Sample(builder.buildOrThrow(), getLocationForState(successor), sample, sampleClass);

        // Handle successor
        if (!successorSample.getVariableValues().equals(sample.getVariableValues())
            || !successorSample.getLocation().equals(sample.getLocation())) {
          SampleTreeNode nextNode = new SampleTreeNode(successorSample);
          boolean isRepeated = root.contains(nextNode);
          node.addChild(nextNode);
          nodes.put(successor, nextNode);
          if (!isRepeated && isStateInLoop(successor, loop)) {
            reachedSet.add(successor, successorPrecision);
          }
        } else if (isStateInLoop(successor, loop)) {
          nodes.put(successor, node);
          reachedSet.add(successor, successorPrecision);
        }
      }
    }

    return root;
  }

  private CFANode getLocationForState(AbstractState pState) {
    return AbstractStates.extractLocation(pState);
  }

  private boolean isStateInLoop(AbstractState pState, Loop pLoop) {
    CFANode location = getLocationForState(pState);
    return pLoop.getLoopNodes().contains(location);
  }

  private static class SampleTreeNode {

    private final Sample sample;
    private final Set<SampleTreeNode> children;

    private SampleTreeNode(Sample pSample) {
      sample = pSample;
      children = new HashSet<>();
    }

    private Sample getSample() {
      return sample;
    }

    private Set<SampleTreeNode> getChildren() {
      return children;
    }

    private void addChild(SampleTreeNode child) {
      children.add(child);
    }

    private boolean contains(SampleTreeNode node) {
      if (equals(node)) {
        return true;
      }
      return children.stream().anyMatch(child -> child.contains(node));
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof SampleTreeNode)) {
        return false;
      }
      SampleTreeNode that = (SampleTreeNode) pO;
      return sample.getVariableValues().equals(that.sample.getVariableValues())
          && sample.getLocation().equals(that.sample.getLocation());
    }

    @Override
    public int hashCode() {
      return Objects.hash(sample);
    }
  }
}
