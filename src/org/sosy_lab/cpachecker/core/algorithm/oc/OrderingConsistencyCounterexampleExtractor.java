// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
      // how far the path has walked each thread instance's exploration DAG; the event-free steps
      // between two events of a thread (local assignments and branches) are taken from that walk,
      // so that the witness also pins down local nondeterministic choices
      Map<Integer, Optional<ARGState>> walkPositions = new HashMap<>();
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
        if (event.id() == terminalEvent.id()
            && endsAtOwnEdge(pAlgorithm, event, chainToEdgeIndex)) {
          // the overflow happens when its edge executes: the path ends there
          int end = chainToEdgeIndex.get(parentChain(pAlgorithm, event)) + 1;
          edges.subList(end, edges.size()).clear();
          edgeInstances.subList(end, edgeInstances.size()).clear();
          edgeCreatedInstances.subList(end, edgeCreatedInstances.size()).clear();
          edgeAssumptions.subList(end, edgeAssumptions.size()).clear();
          edges.set(end - 1, event.edge());
          break;
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
          if (index == null) {
            Optional<ARGState> position =
                walkPositions.computeIfAbsent(
                    event.instanceId(), instance -> threadRoot(pReachedSet, instance));
            List<CFAEdge> steps = new ArrayList<>();
            Optional<ARGState> next = position.flatMap(start -> walkTo(start, edge, model, steps));
            if (next.isPresent()) {
              for (CFAEdge local : steps) {
                edges.add(local);
                edgeInstances.add(event.instanceId());
                edgeCreatedInstances.add(MemoryEvent.NO_INSTANCE);
                edgeAssumptions.add(new ArrayList<>());
              }
              walkPositions.put(event.instanceId(), next);
            }
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
      // a branch-condition read is shared by both assume edges of the branch, so the state must
      // also sit after the terminal edge, where the path's last edge leads
      CFANode terminalSuccessor = edges.isEmpty() ? null : edges.getLast().getSuccessor();
      ARGState target = null;
      for (AbstractState reached : pReachedSet) {
        OrderingConsistencyState ocState =
            AbstractStates.extractStateByType(reached, OrderingConsistencyState.class);
        if (reached instanceof ARGState argState
            && ocState != null
            && ocState.getLastEventIds().contains(terminalChainId)
            && Iterables.contains(AbstractStates.extractLocations(reached), terminalSuccessor)) {
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

  /**
   * Whether the error event of an overflow follows the accesses of its own edge, which are already
   * on the path.
   */
  private static boolean endsAtOwnEdge(
      OrderingConsistencyAlgorithm pAlgorithm,
      MemoryEvent pEvent,
      Map<Integer, Integer> pChainToEdgeIndex) {
    if (pEvent.kind() != EventKind.ERROR
        || pEvent.edge() == null
        || pEvent.poParentId() == MemoryEvent.NO_EVENT) {
      return false;
    }
    MemoryEvent parent = pAlgorithm.getOcCpa().getRegistry().getEvent(pEvent.poParentId());
    return parent.edge() == pEvent.edge()
        && pChainToEdgeIndex.containsKey(parentChain(pAlgorithm, pEvent));
  }

  private static int parentChain(OrderingConsistencyAlgorithm pAlgorithm, MemoryEvent pEvent) {
    return pAlgorithm.getOcCpa().getRegistry().chainTerminalEventId(pEvent.poParentId());
  }

  /** The unique root of a thread instance's exploration DAG, if it has one. */
  private static Optional<ARGState> threadRoot(ReachedSet pReachedSet, int pInstance) {
    ARGState root = null;
    for (AbstractState reached : pReachedSet) {
      OrderingConsistencyState ocState =
          AbstractStates.extractStateByType(reached, OrderingConsistencyState.class);
      if (reached instanceof ARGState argState
          && argState.getParents().isEmpty()
          && ocState != null
          && ocState.getThreadInstanceId() == pInstance) {
        if (root != null) {
          return Optional.empty();
        }
        root = argState;
      }
    }
    return Optional.ofNullable(root);
  }

  /**
   * Walks a thread instance's exploration DAG from {@code pStart} to the next execution of {@code
   * pTarget}, collecting the edges taken before it into {@code pSteps}, and returns the state after
   * {@code pTarget}. Where the exploration branched, the walk follows the successor whose guard
   * holds in the model and from which {@code pTarget} is reachable; it fails (empty result) where
   * this does not decide the successor. Edges are compared by identity: a synthetic edge (e.g. a
   * thread-handle write) equals the CFA edge it was derived from, but is no step of the walk.
   */
  private static Optional<ARGState> walkTo(
      ARGState pStart, CFAEdge pTarget, Model pModel, List<CFAEdge> pSteps) {
    ARGState current = pStart;
    while (true) {
      List<ARGState> children = liveChildren(current);
      if (children.size() > 1) {
        children.removeIf(
            child ->
                !Boolean.TRUE.equals(
                    pModel.evaluate(
                        AbstractStates.extractStateByType(child, OrderingConsistencyState.class)
                            .getGuard())));
      }
      if (children.size() > 1) {
        ARGState parent = current;
        children.removeIf(
            child ->
                indexOfIdentical(parent.getEdgesToChild(child), pTarget) < 0
                    && !reaches(child, pTarget));
      }
      if (children.size() != 1) {
        return Optional.empty();
      }
      ARGState next = children.getFirst();
      List<CFAEdge> step = current.getEdgesToChild(next);
      int target = indexOfIdentical(step, pTarget);
      if (target >= 0) {
        pSteps.addAll(step.subList(0, target));
        return Optional.of(next);
      }
      pSteps.addAll(step);
      current = next;
    }
  }

  /** Whether {@code pEdge} is taken anywhere below {@code pState}. */
  private static boolean reaches(ARGState pState, CFAEdge pEdge) {
    Set<ARGState> visited = new HashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.push(pState);
    while (!waitlist.isEmpty()) {
      ARGState state = waitlist.pop();
      for (ARGState child : liveChildren(state)) {
        if (indexOfIdentical(state.getEdgesToChild(child), pEdge) >= 0) {
          return true;
        }
        if (visited.add(child)) {
          waitlist.push(child);
        }
      }
    }
    return false;
  }

  private static List<ARGState> liveChildren(ARGState pState) {
    List<ARGState> children = new ArrayList<>();
    for (ARGState child : pState.getChildren()) {
      if (!child.isCovered() && !child.isDestroyed()) {
        children.add(child);
      }
    }
    return children;
  }

  private static int indexOfIdentical(List<CFAEdge> pEdges, CFAEdge pEdge) {
    for (int i = 0; i < pEdges.size(); i++) {
      if (pEdges.get(i) == pEdge) {
        return i;
      }
    }
    return -1;
  }
}
