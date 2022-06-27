// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchOtherwise;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStateTypes;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIACombinerCPA;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIACombinerState;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIATransition;
import org.sosy_lab.cpachecker.cpa.giacombiner.NotPresentGIAState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class GIACombinerGenerator {


  private final ConfigurableProgramAnalysis cpa;

  public GIACombinerGenerator(ConfigurableProgramAnalysis pCpa) {
this.cpa = pCpa;
  }

  int produceGIA4ARG(
      Appendable pOutput, UnmodifiableReachedSet pReached)
      throws IOException, InterruptedException {
    final AbstractState firstState = pReached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      pOutput.append("Cannot dump assumption as automaton if ARGCPA is not used.");
      return 0;
    }
    if (AbstractStates.extractStateByType(firstState, GIACombinerState.class) == null) {
      throw new InterruptedException("Cannot dump combined GIA if no GIACombinerState is present.");
    }

    //Check if the shortcut was taken (only a single node is present)
    @Nullable GIACombinerState firstGIAState =
        AbstractStates.extractStateByType(firstState, GIACombinerState.class);
    if (firstGIAState.getStateOfAutomaton1() instanceof NotPresentGIAState && firstGIAState.getStateOfAutomaton2() instanceof  NotPresentGIAState){
      @Nullable GIACombinerCPA combinerCPA = CPAs.retrieveCPA(cpa, GIACombinerCPA.class);
      if (combinerCPA != null && combinerCPA.getPathToOnlyAutomaton().isPresent()){
        pOutput.append(Files.readString(combinerCPA.getPathToOnlyAutomaton().orElseThrow()));

      return 0;}
    }


    Set<GIACombinerState> statesPresent =
        pReached.stream()
            .map(as -> AbstractStates.extractStateByType(as, GIACombinerState.class))
            .filter(as -> as != null)
            .collect(ImmutableSet.toImmutableSet());
    final ARGState pArgRoot = (ARGState) pReached.getFirstState();
    Set<GIAARGStateEdge<GIACombinerState>> relevantEdges = computeRelevantEdges(pArgRoot, pReached);

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
        pOutput,
        pArgRoot,
        relevantEdges,
        false,
        targetStates,
        nonTargetStates,
        unknownStates
    );
  }

  private Set<GIAARGStateEdge<GIACombinerState>> computeRelevantEdges(
      ARGState firstState, UnmodifiableReachedSet pReached) {

    Set<GIAARGStateEdge<GIACombinerState>> edges = new HashSet<>();
    List<GIACombinerState> toProcess =
        Lists.newArrayList(AbstractStates.extractStateByType(firstState, GIACombinerState.class));
    Map<GIACombinerState, Boolean> argStateIsCovered = new HashMap<>();
    pReached.stream()
        .filter(as -> AbstractStates.extractStateByType(as, GIACombinerState.class) != null
        &&  AbstractStates.extractStateByType(as, ARGState.class) != null)
        .forEach(
            as ->
                argStateIsCovered.put(
                    AbstractStates.extractStateByType(as, GIACombinerState.class),
                    Objects.requireNonNull(AbstractStates.extractStateByType(as, ARGState.class)).isCovered()));

    while (!toProcess.isEmpty()) {

      GIACombinerState currentState = toProcess.remove(0);
      // Check if there is only a single successor and this successor has the same inner states.
      // Then, search for the next successor state having either two outgoing edges or  different
      // inner states

      if (currentState.getSuccessors().size() == 1 && noAssumptions(currentState.getSuccessors())) {
        GIACombinerState nextState = getNextState(currentState.getSuccessors());
        if (!(nextState.getStateOfAutomaton1().equals(currentState.getStateOfAutomaton1())
            && nextState.getStateOfAutomaton2().equals(currentState.getStateOfAutomaton2()))) {
          addEdges(edges, toProcess, currentState, argStateIsCovered);
          continue;
        }
        while (nextState.getSuccessors().size() == 1
            && nextState.getStateOfAutomaton1().equals(currentState.getStateOfAutomaton1())
            && nextState.getStateOfAutomaton2().equals(currentState.getStateOfAutomaton2())
            && noAssumptions(currentState.getSuccessors())
        && ! argStateIsCovered.get(getNextState(nextState.getSuccessors()))
        ) {
          nextState = getNextState(nextState.getSuccessors());
        }
        for (Entry<GIATransition, GIACombinerState> edge : nextState.getSuccessors().entrySet()) {

          edges.add(new GIAARGStateEdge<>(currentState, edge));
          if (!(edge.getKey().getTrigger() instanceof MatchOtherwise)){
            toProcess.add(edge.getValue());
          }
        }

      } else {

        addEdges(edges, toProcess, currentState, argStateIsCovered);
      }
    }

    return edges;
  }

  private void addEdges(
      Set<GIAARGStateEdge<GIACombinerState>> pEdges,
      List<GIACombinerState> pToProcess,
      GIACombinerState pCurrentState,
      Map<GIACombinerState, Boolean> pArgStateIsCovered) {

    for (Entry<GIATransition, GIACombinerState> edge :
        pCurrentState
            .getSuccessors()
            .entrySet()) {
      if (!pArgStateIsCovered.get(edge.getValue())) {
        pEdges.add(new GIAARGStateEdge<>(pCurrentState, edge));
        if (!(edge.getKey().getTrigger() instanceof MatchOtherwise)) {
          pToProcess.add(edge.getValue());
        }
      }
    }
  }

  private boolean noAssumptions(Map<GIATransition, GIACombinerState> pSuccessors) {
    assert pSuccessors.size() == 1;
    return Lists.newArrayList(pSuccessors.keySet()).get(0).getAssumptions().isEmpty();
  }

  private GIACombinerState getNextState(Map<GIATransition, GIACombinerState> pSuccessors) {
    assert pSuccessors.size() == 1;
    return Lists.newArrayList(pSuccessors.values()).get(0);
  }
}
