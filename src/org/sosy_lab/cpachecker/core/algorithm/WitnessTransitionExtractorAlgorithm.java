// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTracker;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTracker.TracingInformation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWithMetaData;
import org.sosy_lab.cpachecker.cpa.automaton.GraphMLTransition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultUtil;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class WitnessTransitionExtractorAlgorithm
    implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final StatTimer timer = new StatTimer("Total time for finding edges");

  /**
   * Simulates a fault localization algorithm that reduces the edges in the counterexample to the
   * edges contained in a given witness.
   *
   * @param pAlgorithm arbitrary algorithm that validates a witness
   */
  public WitnessTransitionExtractorAlgorithm(Algorithm pAlgorithm) {
    algorithm = pAlgorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    // take the ARGPath and find all CFAEdges along the path where WitnessAutomatonState changed!
    AlgorithmStatus status = algorithm.run(reachedSet);
    Map<AutomatonState, ARGState> stateMap = new HashMap<>();
    for (AbstractState abstractState : reachedSet) {
      stateMap.put(witnessStateFromARGState((ARGState) abstractState), (ARGState) abstractState);
    }
    Set<GraphMLTransition> allTransitions = findAllTransitionsOfAutomaton((ARGState) Objects.requireNonNull(
        reachedSet.getFirstState()));
    Multimap<ARGState, TracingInformation> information = ArrayListMultimap.create();
    for (TracingInformation coveredTransition :
        AutomatonTracker.getInstance().getCoveredTransitions()) {
      information.put(stateMap.get(coveredTransition.getFrom()), coveredTransition);
    }
    timer.start();
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));
    for (CounterexampleInfo info : counterExamples) {
      Set<ARGState> statesOnPath = info.getTargetPath().getStateSet();
      List<Pair<ARGState, ARGState>> waitlist = new ArrayList<>();
      Set<ARGState> finished = new HashSet<>();
      finished.add(info.getRootState());
      waitlist.add(Pair.of(info.getRootState(), info.getRootState()));
      Set<CFAEdge> relevantEdges = new HashSet<>();
      while (!waitlist.isEmpty()) {
        Pair<ARGState, ARGState> next = waitlist.remove(0);
        ARGState first = next.getFirstNotNull();
        ARGState second = next.getSecondNotNull();
        AutomatonState secondAutomaton = witnessStateFromARGState(second);
        if (!witnessStateFromARGState(first).equals(secondAutomaton)) {
          CFAEdge transition = java.util.Objects.requireNonNull(first.getEdgeToChild(second));
          relevantEdges.add(transition);
        }
        finished.add(second);
        for (ARGState child : second.getChildren()) {
          if (!finished.contains(child) && statesOnPath.contains(child)) {
            waitlist.add(Pair.of(second, child));
          }
        }
      }
      Fault witnessFault = FaultUtil.fromEdges(relevantEdges);
      witnessFault.addInfo(new AppendixFaultInfo(information, allTransitions));
      List<Fault> singleFault =
          ImmutableList.of(witnessFault);

      // adapt counterexample
      new FaultLocalizationInfo(singleFault, info).apply();
    }
    timer.stop();
    return status;
  }

  private static AutomatonState witnessStateFromARGState(ARGState state) {
    CompositeState compositeState = (CompositeState) state.getWrappedState();
    Preconditions.checkNotNull(compositeState);
    return FluentIterable.from(compositeState.getWrappedStates())
        .filter(AutomatonState.class)
        .filter(
            automatonState ->
                automatonState
                    .getOwningAutomatonName()
                    .equals(AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME))
        .first()
        .toJavaUtil()
        .orElseThrow(
            () ->
                new AssertionError(
                    WitnessTransitionExtractorAlgorithm.class.getSimpleName()
                        + " cannot find a witness automaton in "
                        + compositeState));
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out)
        .put(timer);
  }

  @Override
  public @Nullable String getName() {
    return "Witness Extractor";
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }

  private static Set<GraphMLTransition> findAllTransitionsOfAutomaton(ARGState pState) {
    AutomatonWithMetaData automaton = (AutomatonWithMetaData) witnessStateFromARGState(pState).getOwningAutomaton();
    return ImmutableSet.copyOf(automaton.getTransitions().values());
  }

  public static class AppendixFaultInfo extends FaultInfo {

    private final Multimap<ARGState, TracingInformation> information;
    private final Set<GraphMLTransition> transitions;

    protected AppendixFaultInfo(Multimap<ARGState, TracingInformation> pInformation, Set<GraphMLTransition> pTransitions) {
      super(InfoType.REASON);
      transitions = pTransitions;
      information = pInformation;
    }

    public Multimap<ARGState, TracingInformation> getInformation() {
      return information;
    }

    public Set<GraphMLTransition> getTransitions() {
      return transitions;
    }
  }
}
