// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnesses;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

/**
 * Adapts location summaries to the standard YAML witness exporters. Correctness states are
 * independent snapshots, without parent links. A violation has only the edge identifying the
 * target, not the execution that led there. Neither representation is used to export counterexample
 * traces.
 */
public class LocationWitnessExporter extends ARGStatistics {

  private final LocationStateFactory locations;
  private final ReachedSetFactory reachedSetFactory;
  private final LocationStatesCollector collector;

  public LocationWitnessExporter(
      Configuration pConfig,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    this(pConfig, pLogger, pCpa, pSpecification, pCfa, new LocationStatesCollector());
  }

  private LocationWitnessExporter(
      Configuration pConfig,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa,
      Specification pSpecification,
      CFA pCfa,
      LocationStatesCollector pCollector)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa, pSpecification, pCfa, pCollector);
    collector = pCollector;
    locations = new LocationStateFactory(pCfa, AnalysisDirection.FORWARD, pConfig);
    reachedSetFactory = new ReachedSetFactory(pConfig, pLogger);
  }

  public ReachedSet createReachedSet() {
    return reachedSetFactory.create(cpa);
  }

  /** Prepare an exhausted reached set containing only a root and the recorded invariant states. */
  public void prepareCorrectnessWitness(
      ReachedSet pReached,
      Multimap<CFANode, ImmutableList<ExpressionTreeReportingState>> pInvariants) {
    pReached.clear();
    collector.states.clear();
    pReached.add(
        new ARGState(locations.getState(cfa.getMainFunction()), null),
        SingletonPrecision.getInstance());
    for (var entry : pInvariants.entries()) {
      ARGState snapshot =
          new ARGState(
              new CompositeState(
                  ImmutableList.<AbstractState>builder()
                      .add(locations.getState(entry.getKey()))
                      .addAll(entry.getValue())
                      .build()),
              null);
      pReached.add(snapshot, SingletonPrecision.getInstance());
      collector.states.put(entry.getKey(), snapshot);
    }
    // Use add() to also populate indexes of partitioned reached sets, then mark them exhausted.
    pReached.clearWaitlist();
  }

  /** Prepare a target marker with its source location, without recovering any preceding path. */
  public void prepareViolationWitness(
      ReachedSet pReached, CFAEdge pEdge, DummyTargetState pTarget) {
    pReached.clear();
    collector.states.clear();
    ARGState root = new ARGState(locations.getState(pEdge.getPredecessor()), null);
    ARGState target =
        new ARGState(
            new CompositeState(ImmutableList.of(locations.getState(pEdge.getSuccessor()), pTarget)),
            root);
    target.addCounterexampleInformation(
        CounterexampleInfo.feasibleImprecise(
            new ARGPath(ImmutableList.of(root, target), ImmutableList.of(pEdge))));
    pReached.add(root, SingletonPrecision.getInstance());
    pReached.add(target, SingletonPrecision.getInstance());
    pReached.clearWaitlist();
  }

  /** Collect from the location map, because the snapshots deliberately have no ARG edges. */
  private static class LocationStatesCollector implements RelevantArgStatesCollector {

    private final Multimap<CFANode, ARGState> states = LinkedHashMultimap.create();

    @Override
    public CollectedARGStates getRelevantStates(ARGState pRootState) {
      ImmutableListMultimap.Builder<CFANode, ARGState> loops = ImmutableListMultimap.builder();
      ImmutableListMultimap.Builder<CFANode, ARGState> calls = ImmutableListMultimap.builder();
      ImmutableListMultimap.Builder<FunctionEntryNode, ARGState> entries =
          ImmutableListMultimap.builder();
      for (var entry : states.entries()) {
        CFANode node = entry.getKey();
        if (node.isLoopStart()) {
          loops.put(node, entry.getValue());
        }
        if (node.getNumLeavingEdges() == 1 && node.getLeavingEdge(0) instanceof FunctionCallEdge) {
          calls.put(node, entry.getValue());
        }
        if (node instanceof FunctionEntryNode functionEntry) {
          entries.put(functionEntry, entry.getValue());
        }
      }
      return new CollectedARGStates(
          loops.build(), calls.build(), entries.build(), ImmutableListMultimap.of());
    }
  }
}
