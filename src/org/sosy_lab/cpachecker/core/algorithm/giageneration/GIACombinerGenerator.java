// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStateTypes;
import org.sosy_lab.cpachecker.cpa.giacombiner.AbstractGIAState;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIACombinerCPA;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIACombinerState;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIATransition;
import org.sosy_lab.cpachecker.cpa.giacombiner.NotPresentGIAState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;

public class GIACombinerGenerator {

  private final ConfigurableProgramAnalysis cpa;

  public GIACombinerGenerator(ConfigurableProgramAnalysis pCpa) {
    this.cpa = pCpa;
  }

  int produceGIA4ARG(Appendable pOutput, UnmodifiableReachedSet pReached)
      throws IOException, InterruptedException {
    final AbstractState firstState = pReached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      pOutput.append("Cannot dump assumption as automaton if ARGCPA is not used.");
      return 0;
    }
    if (AbstractStates.extractStateByType(firstState, GIACombinerState.class) == null) {
      throw new InterruptedException("Cannot dump combined GIA if no GIACombinerState is present.");
    }

    // Check if the shortcut was taken (only a single node is present)
    @Nullable GIACombinerState firstGIAState =
        AbstractStates.extractStateByType(firstState, GIACombinerState.class);
    if (firstGIAState.getStateOfAutomaton1() instanceof NotPresentGIAState
        && firstGIAState.getStateOfAutomaton2() instanceof NotPresentGIAState) {
      @Nullable GIACombinerCPA combinerCPA = CPAs.retrieveCPA(cpa, GIACombinerCPA.class);
      if (combinerCPA != null && combinerCPA.getPathToOnlyAutomaton().isPresent()) {
        pOutput.append(Files.readString(combinerCPA.getPathToOnlyAutomaton().orElseThrow()));

        return 0;
      }
    }

    Set<GIACombinerState> statesPresent =
        pReached.stream()
            .map(as -> AbstractStates.extractStateByType(as, GIACombinerState.class))
            .filter(as -> as != null)
            .collect(ImmutableSet.toImmutableSet());
    Set<GIAARGStateEdge<GIACombinerState>> relevantEdges = computeRelevantEdges(statesPresent);

    Set<GIACombinerState> targetStates =
        statesPresent.stream()
            .filter(s -> s.isPresent(AutomatonStateTypes.TARGET))
            .collect(ImmutableSet.toImmutableSet());
    Set<GIACombinerState> nonTargetStates =
        statesPresent.stream()
            .filter(s -> s.isPresent(AutomatonStateTypes.NON_TARGET))
            .collect(ImmutableSet.toImmutableSet());
    Set<GIACombinerState> unknownStates =
        statesPresent.stream()
            .filter(s -> s.isPresent(AutomatonStateTypes.UNKNOWN))
            .collect(ImmutableSet.toImmutableSet());
    GIAWriter<GIACombinerState> writer = new GIAWriter<>();
    return writer.writeGIA(
        pOutput, firstGIAState, relevantEdges, targetStates, nonTargetStates, unknownStates);
  }

  private Set<GIAARGStateEdge<GIACombinerState>> computeRelevantEdges(
      Set<GIACombinerState> pStatesPresent) {

    Set<CombinerTransition> transitionsSeen = new HashSet<>();
    Map<Pair<AbstractGIAState, AbstractGIAState>, GIACombinerState> statesUsedForTransitions =
        new HashMap<>();

    for (GIACombinerState current : pStatesPresent) {
      Pair<AbstractGIAState, AbstractGIAState> pairOfStates =
          Pair.of(current.getStateOfAutomaton1(), current.getStateOfAutomaton2());

      statesUsedForTransitions.putIfAbsent(pairOfStates, current);
      GIACombinerState source = statesUsedForTransitions.get(pairOfStates);

      for (Entry<GIATransition, GIACombinerState> edge : current.getSuccessors().entrySet()) {
        Pair<AbstractGIAState, AbstractGIAState> pairTarget =
            Pair.of(edge.getValue().getStateOfAutomaton1(), edge.getValue().getStateOfAutomaton2());
        statesUsedForTransitions.putIfAbsent(pairTarget, edge.getValue());
        GIACombinerState target = statesUsedForTransitions.get(pairTarget);
        CombinerTransition e = new CombinerTransition(source, edge.getKey(), target);
        transitionsSeen.add(e);
      }
    }
    return transitionsSeen.stream().map(t -> t.toEdge()).collect(ImmutableSet.toImmutableSet());
  }

  private static class CombinerTransition {
    GIACombinerState source;
    GIATransition transition;
    GIACombinerState target;

    public CombinerTransition(
        GIACombinerState pSource, GIATransition pTransition, GIACombinerState pTarget) {
      source = pSource;
      transition = pTransition;
      target = pTarget;
    }

    public GIAARGStateEdge<GIACombinerState> toEdge() {
      return new GIAARGStateEdge<>(source, new SimpleEntry<>(transition, target));
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof CombinerTransition)) {
        return false;
      }
      CombinerTransition that = (CombinerTransition) pO;
      return Objects.equals(source.getStateOfAutomaton1(), that.source.getStateOfAutomaton1())
          && Objects.equals(source.getStateOfAutomaton2(), that.source.getStateOfAutomaton2())
          && Objects.equals(transition.getTrigger(), that.transition.getTrigger())
          && Objects.equals(transition.getAssumptions(), that.transition.getAssumptions())
          && Objects.equals(target.getStateOfAutomaton1(), that.target.getStateOfAutomaton1())
          && Objects.equals(target.getStateOfAutomaton2(), that.target.getStateOfAutomaton2());
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          source.getStateOfAutomaton1(),
          source.getStateOfAutomaton2(),
          transition.getAssumptions(),
          transition.getTrigger(),
          target.getStateOfAutomaton1(),
          target.getStateOfAutomaton2());
    }

    @Override
    public String toString() {
      return source + "-" + transition.getTrigger().toString() + "->" + target;
    }
  }
}
