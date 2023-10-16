// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "sampling.unrolling")
public class SampleUnrollingAlgorithm {

  @Option(
      secure = true,
      description =
          "Whether intermediate samples (at locations different from the given initial sample)"
              + " should be collected")
  private boolean collectIntermediateSamples = true;

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final ConfigurableProgramAnalysis cpa;

  private record NodeUpdate(SampleTreeNode node, Sample previous) {}

  public SampleUnrollingAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      CFA pCfa,
      Specification pSpecification)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger.withComponentName("SampleUnrollingAlgorithm");
    reachedSetFactory = new ReachedSetFactory(pConfig, logger);

    // TODO: Build CPA here: We want a location+value+predicate analysis with standard wrappers.
    //  For this we can add an option `unrollingConfig` (similar to e.g. overflow config) and use
    //  the config file specified via this option here.
    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            pConfig, logger, pShutdownManager.getNotifier(), AggregatedReachedSets.empty());
    cpa = coreComponents.createCPA(pCfa, pSpecification);
    SerializationInfoStorage.storeSerializationInformation(cpa, pCfa);
  }

  public Set<Sample> unrollSample(Sample initialSample, Loop loop)
      throws CPAException, InterruptedException {
    AbstractState initialState = SampleUtils.makeInitialStateFromSample(cpa, initialSample);
    Precision initialPrecision = SampleUtils.makeInitialPrecisionFromSample(cpa, initialSample);
    ReachedSet reachedSet = reachedSetFactory.create(cpa);
    reachedSet.add(initialState, initialPrecision);
    logger.log(Level.FINER, "Unrolling sample...");

    SampleTreeNode sampleTreeRoot = run(reachedSet, initialSample, loop);

    // If target is reachable from initial sample, then all samples are negative
    boolean classifySamples =
        initialSample.getSampleClass() == SampleClass.UNKNOWN && reachedSet.wasTargetReached();

    ImmutableSet.Builder<Sample> builder = ImmutableSet.builder();
    Queue<NodeUpdate> waitlist = new ArrayDeque<>();
    waitlist.add(new NodeUpdate(sampleTreeRoot, null));
    while (!waitlist.isEmpty()) {
      NodeUpdate nodeUpdate = waitlist.poll();
      SampleTreeNode node = nodeUpdate.node();
      Sample previous = nodeUpdate.previous();
      if (node == SampleTreeNode.DUMMY) {
        continue;
      }
      Sample sample = node.getSample();
      if (classifySamples) {
        sample =
            new Sample(
                sample.getVariableValues(), sample.getLocation(), previous, SampleClass.NEGATIVE);
      }
      builder.add(sample);
      for (SampleTreeNode child : node.getChildren()) {
        waitlist.add(new NodeUpdate(child, sample));
      }
    }
    return builder.build();
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

        if (sample == null) {
          assert sampleClass == SampleClass.UNKNOWN;
          nodes.put(successor, SampleTreeNode.DUMMY);
          reachedSet.add(successor, successorPrecision);
          continue;
        }

        // Build sample for successor state
        Sample successorSample = null;
        boolean sampleChanged = false;
        CFANode successorLocation = getLocationForState(successor);
        if (collectIntermediateSamples || successorLocation.equals(initialSample.getLocation())) {
          Map<MemoryLocation, ValueAndType> successorValues =
              SampleUtils.getValuesAndTypesFromAbstractState(successor, relevantVariables);
          if (successorValues.size() != relevantVariables.size()) {
            logger.log(Level.WARNING, "Sample is missing values of some relevant variables.");
            // TODO: When encountering nondeterminism inside loop, pick any value and insert in
            //       ValueAnalysisState
            throw new UnsupportedCodeException("Nondeterminism in loop", null);
          }
          successorSample = new Sample(successorValues, successorLocation, sample, sampleClass);
          sampleChanged =
              !successorValues.equals(sample.getVariableValues())
                  || !successorLocation.equals(sample.getLocation());
        }

        // Handle successor
        if (sampleChanged) {
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
        } else if (sampleClass == SampleClass.UNKNOWN) {
          nodes.put(successor, SampleTreeNode.DUMMY);
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
    FluentIterable<CFANode> afterLoop =
        FluentIterable.from(pLoop.getOutgoingEdges()).transform(CFAEdge::getSuccessor);
    return !afterLoop.contains(location);
  }

  private static class SampleTreeNode {

    private static final SampleTreeNode DUMMY = new SampleTreeNode(null);

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

    private boolean contains(SampleTreeNode pNode) {
      Queue<SampleTreeNode> waitlist = new ArrayDeque<>();
      waitlist.add(this);
      while (!waitlist.isEmpty()) {
        SampleTreeNode node = waitlist.poll();
        if (node.equals(pNode)) {
          return true;
        }
        waitlist.addAll(node.getChildren());
      }
      return false;
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
