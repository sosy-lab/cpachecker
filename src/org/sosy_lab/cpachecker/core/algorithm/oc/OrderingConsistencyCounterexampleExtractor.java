// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.oc.EventKind;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/** Builds and attaches the synthetic counterexample path for an ordering-consistency violation. */
final class OrderingConsistencyCounterexampleExtractor {

  private OrderingConsistencyCounterexampleExtractor() {}

  /**
   * Builds a synthetic {@link ARGPath} for an interleaving of the whole execution. The path crosses
   * thread trees and thus has no counterpart in the forest-shaped reached set. It is attached as a
   * {@link CounterexampleInfo} to the reached target state so the existing counterexample
   * infrastructure can export and report it.
   */
  static void attachCounterexample(
      OrderingConsistencyAlgorithm pAlgorithm,
      ReachedSet pReachedSet,
      ProverEnvironment pProver,
      OcEncoder pEncoder)
      throws SolverException {
    try (Model model = pProver.getModel()) {
      List<CFAEdge> edges = new ArrayList<>();
      // the thread instance that executes each edge, and the instance its step creates (if any),
      // so that the violation witness can label every waypoint with the thread it belongs to
      List<Integer> edgeInstances = new ArrayList<>();
      List<Integer> edgeCreatedInstances = new ArrayList<>();
      List<List<AExpressionStatement>> edgeAssumptions = new ArrayList<>();
      // the event that ends the counterexample path (the reached error for unreach-call, the later
      // of the two racing accesses for no-data-race); its reached state becomes the CEX target
      MemoryEvent terminalEvent = pAlgorithm.terminalViolationEvent(model, pEncoder);
      if (terminalEvent == null) {
        return;
      }
      CFAEdge previousEdge = null;
      Map<Integer, Integer> chainToEdgeIndex = new HashMap<>();
      // after the earlier of two racing accesses only the thread of the later one runs, so that the
      // earlier access is the last step of its thread on the path
      MemoryEvent earlierAccess = null;
      if (pAlgorithm.isDataRaceTarget()) {
        OcEncoder.RacePair race = pAlgorithm.findRace(model, pEncoder);
        earlierAccess = race.access1().id() == terminalEvent.id() ? race.access2() : race.access1();
      }
      boolean earlierAccessPassed = false;
      for (MemoryEvent event : pAlgorithm.orderedViolationEvents(model, pEncoder)) {
        if (earlierAccessPassed
            && earlierAccess != null
            && event.instanceId() != terminalEvent.instanceId()
            && pAlgorithm.getOcCpa().getRegistry().chainTerminalEventId(event.id())
                != pAlgorithm.getOcCpa().getRegistry().chainTerminalEventId(earlierAccess.id())) {
          continue;
        }
        if (earlierAccess != null && event.id() == earlierAccess.id()) {
          earlierAccessPassed = true;
        }
        if (event.edge() != null) {
          CFAEdge edge = pAlgorithm.displayEdge(event, model);
          // collapse events of one execution of an edge, which other threads' events may separate:
          // one statement can produce several events (e.g. a[k]=5 is a read of k and a write of
          // a[k]), and a shared branch-condition read can be tagged on several reads of the same
          // assume edge
          int chain = pAlgorithm.getOcCpa().getRegistry().chainTerminalEventId(event.id());
          Integer index = chainToEdgeIndex.get(chain);
          if (index == null
              && edge.equals(previousEdge)
              && event.instanceId() == edgeInstances.getLast()
              && !java.util.EnumSet.of(EventKind.READ, EventKind.WRITE, EventKind.CREATE)
                  .contains(event.kind())) {
            index = edges.size() - 1;
          }
          if (index == null || event.id() == terminalEvent.id()) {
            edges.add(edge);
            edgeInstances.add(event.instanceId());
            edgeCreatedInstances.add(OrderingConsistencyAlgorithm.createdInstanceOf(event));
            edgeAssumptions.add(new ArrayList<>());
            previousEdge = edge;
            index = edges.size() - 1;
          } else if (edgeCreatedInstances.get(index) == MemoryEvent.NO_INSTANCE) {
            // one statement can produce several events; whichever of them is the create is the
            // one that names the new thread
            edgeCreatedInstances.set(index, OrderingConsistencyAlgorithm.createdInstanceOf(event));
          }
          chainToEdgeIndex.put(chain, index);
          OrderingConsistencyAlgorithm.assumptionFor(event, edge, model)
              .ifPresent(edgeAssumptions.get(index)::add);
        }
        if (event.id() == terminalEvent.id()) {
          break; // the execution stops at the violating event
        }
      }
      if (pAlgorithm.isDataRaceTarget()) {
        // the exploration marks every syntactically reachable reach_error state as a target; those
        // are irrelevant to the data-race property and would otherwise leak into the verdict. Drop
        // them here, then mark the actual racing state below as the sole target.
        TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
      }
      // the reached state that carries the terminal event; it must be the last state of the path so
      // CounterexampleInfo accepts it (getTargetState().isTarget()). The terminal event need not
      // itself be a state's lastEventIds: several accesses chained on one edge (e.g. both operands
      // of `a != b`) share one successor state, and only the last of the chain is recorded there
      // (see OcExplorationRegistry#chainTerminalEventId).
      int terminalChainId =
          pAlgorithm.getOcCpa().getRegistry().chainTerminalEventId(terminalEvent.id());
      ARGState target = null;
      for (AbstractState reached : pReachedSet) {
        OrderingConsistencyState ocState =
            AbstractStates.extractStateByType(reached, OrderingConsistencyState.class);
        if (reached instanceof ARGState argState
            && ocState != null
            && ocState.getLastEventIds().contains(terminalChainId)) {
          if (pAlgorithm.isDataRaceTarget()) {
            // no exploration-time target exists for a race; mark this reached state so the verdict
            // is FALSE (the reached set now contains a genuine target with a proper location)
            ocState.markRaceTarget();
          }
          target = argState;
          break;
        }
      }
      if (target == null || edges.isEmpty()) {
        return;
      }
      // a fresh linear chain of location states ending at the real target. Each state carries the
      // successor node of its incoming edge, so the full-path iterator (which advances a state
      // whenever the current edge's successor is that state's location) stays in lockstep with the
      // edge list — even across thread-jumps, where the edges are not CFA-connected. The real
      // target already sits at the terminal edge's successor, matching the last edge. It is only
      // appended to the state list, never parent-linked, so the reached-set ARG is left untouched.
      List<ARGState> states = new ArrayList<>();
      ARGState previous =
          new ARGState(
              pAlgorithm
                  .getOcCpa()
                  .counterexampleStateFor(
                      edges.getFirst().getPredecessor(),
                      edgeInstances.getFirst(),
                      MemoryEvent.NO_INSTANCE),
              null);
      states.add(previous);
      for (int i = 1; i < edges.size(); i++) {
        // the state after an edge carries that edge's thread, which is what the witness export
        // consults for the waypoint it builds from the edge
        ARGState next =
            new ARGState(
                pAlgorithm
                    .getOcCpa()
                    .counterexampleStateFor(
                        edges.get(i - 1).getSuccessor(),
                        edgeInstances.get(i - 1),
                        edgeCreatedInstances.get(i - 1)),
                previous);
        states.add(next);
        previous = next;
      }
      states.add(target);
      ARGPath path = new ARGPath(states, ImmutableList.copyOf(edges));
      ImmutableList.Builder<CFAEdgeWithAssumptions> assumptions = ImmutableList.builder();
      for (int i = 0; i < edges.size(); i++) {
        assumptions.add(
            new CFAEdgeWithAssumptions(
                edges.get(i), ImmutableList.copyOf(edgeAssumptions.get(i)), ""));
      }
      target.addCounterexampleInformation(
          CounterexampleInfo.feasiblePrecise(
              path, CFAPathWithAssumptions.from(assumptions.build())));
      // every syntactically reachable error state is a target, and the exporter would derive a
      // second counterexample from one, starting at its thread root and naming no thread instances
      for (AbstractState reached : pReachedSet) {
        OrderingConsistencyState other =
            AbstractStates.extractStateByType(reached, OrderingConsistencyState.class);
        if (other != null && reached != target) {
          other.clearTarget();
        }
      }
    }
  }
}
