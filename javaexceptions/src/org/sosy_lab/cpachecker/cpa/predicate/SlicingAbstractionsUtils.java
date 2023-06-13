// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.dca.DCAState;
import org.sosy_lab.cpachecker.cpa.overflow.OverflowState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.slab.EdgeSet;
import org.sosy_lab.cpachecker.cpa.slab.SLARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Utility class for Slicing Abstractions like in the papers: "Slicing Abstractions"
 * (doi:10.1007/978-3-540-75698-9_2) "Splitting via Interpolants" (doi:10.1007/978-3-642-27940-9_13)
 */
public class SlicingAbstractionsUtils {

  private SlicingAbstractionsUtils() {}

  /**
   * Calculate parent abstraction states for a given abstraction state and the corresponding
   * non-abstraction states that lie between them.
   *
   * @param originState The (abstraction) state for which to calculate the incoming segment
   * @return A mapping of (abstraction) states to a list of (non-abstraction) states via which
   *     originState can be reached from the corresponding key
   */
  public static Map<ARGState, PersistentList<ARGState>> calculateIncomingSegments(
      ARGState originState) {
    checkArgument(isAbstractionState(originState));

    final Map<ARGState, PersistentList<ARGState>> result = new TreeMap<>();
    final List<ARGState> startAbstractionStates = calculateStartStates(originState);

    // This looks a bit expensive, but we cannot simply write this method like
    // calculateOutgoingSegments!
    // Because of the way we build our ARG, we can be sure that a block has only one starting
    // abstraction state, but there could be several abstraction states to end at.
    for (ARGState s : startAbstractionStates) {
      Map<ARGState, PersistentList<ARGState>> outgoing = calculateOutgoingSegments(s);
      if (outgoing.containsKey(originState)) {
        result.put(s, outgoing.get(originState));
      }
    }

    return result;
  }

  /**
   * This method can be seen as a generalized way of calculating the parent abstraction states of a
   * given abstraction state.
   *
   * @param originState The (abstraction) state for which to calculate the effective parent
   *     (abstraction) states
   * @return A list of (abstraction) states from which originState can be reached
   */
  public static List<ARGState> calculateStartStates(ARGState originState) {
    checkArgument(isAbstractionState(originState));
    final Set<ARGState> result = new HashSet<>();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    final Set<ARGState> reached = new HashSet<>();

    for (ARGState parent : originState.getParents()) {

      if (isAbstractionState(parent)) {
        result.add(parent);
        continue;
      }

      waitlist.add(parent);
      reached.clear();
      reached.add(parent);
      while (!waitlist.isEmpty()) {
        ARGState currentState = waitlist.pop();
        for (ARGState s : currentState.getParents()) {
          if (isAbstractionState(s)) {
            result.add(s);
            waitlist.clear();
            break;
          } else {
            if (!reached.contains(s)) {
              waitlist.add(s);
              reached.add(s);
            }
          }
        }
      }
    }
    return new ArrayList<>(result);
  }

  /**
   * Calculate child abstraction states for a given abstraction state and the corresponding
   * non-abstraction states that lie between them.
   *
   * @param originState The (abstraction) state from which to start
   * @return A mapping of (abstraction) states to a list of (non-abstraction) states which can be
   *     reached from originState
   */
  public static ImmutableMap<ARGState, PersistentList<ARGState>> calculateOutgoingSegments(
      ARGState originState) {
    // checkArgument(isAbstractionState(originState));

    // Used data structures:
    final Collection<ARGState> outgoingStates = originState.getChildren();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    final Map<ARGState, PersistentList<ARGState>> frontier = new TreeMap<>();
    final Map<ARGState, PersistentList<ARGState>> segmentMap = new TreeMap<>();
    final Collection<ARGState> reachableNonAbstractionStates = nonAbstractionReach(originState);

    // prepare initial state
    frontier.put(originState, PersistentLinkedList.of());
    for (ARGState startState : outgoingStates) {
      // we need to treat AbstractionStates differently!
      if (!isAbstractionState(startState)) {
        waitlist.add(startState);
      } else {
        segmentMap.put(startState, PersistentLinkedList.of());
      }
    }

    // search algorithm
    while (!waitlist.isEmpty()) {

      // get element from waitlist. Re-queue if we have not yet
      // explored all of its parents!
      ARGState currentState = waitlist.pop();
      if (!frontier
          .keySet()
          .containsAll(
              FluentIterable.from(currentState.getParents())
                  .filter(x -> reachableNonAbstractionStates.contains(x))
                  .toList())) {
        waitlist.add(currentState);
        continue;
      }

      // All parents have already been explored, let's
      // build the state list for this state:
      PersistentList<ARGState> currentStateList = null;
      Set<ARGState> currentStateSet = new HashSet<>();
      for (ARGState parent : currentState.getParents()) {
        if (!reachableNonAbstractionStates.contains(parent)) {
          continue;
        }
        PersistentList<ARGState> parentStateList = frontier.get(parent);
        if (currentStateList == null) {
          currentStateList = parentStateList;
          currentStateSet.addAll(parentStateList);
        } else {
          for (ARGState s : parentStateList.reversed()) {
            // checking containment in O(1) is crucial here:
            if (!currentStateSet.contains(s)) {
              currentStateList = currentStateList.with(s);
              currentStateSet.add(s);
            }
          }
        }
      }
      assert currentStateList != null;
      // Don't forget to add the currentState to its own list:
      currentStateList = currentStateList.with(currentState);
      // Now store the finished list for later use:
      frontier.put(currentState, currentStateList);

      // Put the children on the waitlist or - if the child is an
      // AbstractionState - add the currentStateList to the
      // segmentMap with the child as key
      for (ARGState child : currentState.getChildren()) {
        if (isAbstractionState(child)) {
          if (segmentMap.containsKey(child)) {
            PersistentList<ARGState> storedStateList = segmentMap.get(child);
            Set<ARGState> storedStateSet = new HashSet<>(storedStateList);
            for (ARGState s : currentStateList.reversed()) {
              if (!storedStateSet.contains(s)) {
                storedStateList = storedStateList.with(s);
              }
            }
            segmentMap.put(child, storedStateList);
          } else {
            segmentMap.put(child, currentStateList);
          }
        } else {
          if (!waitlist.contains(child)) {
            waitlist.add(child);
          }
        }
      }
    }

    // Now we need to reverse the segments so that they are in correct order:
    return ImmutableMap.copyOf(Maps.transformValues(segmentMap, segment -> segment.reversed()));
  }

  private static Collection<ARGState> nonAbstractionReach(ARGState pOriginState) {
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    final Set<ARGState> reachable = new HashSet<>();
    waitlist.push(pOriginState);
    reachable.add(pOriginState);
    while (!waitlist.isEmpty()) {
      ARGState s = waitlist.pop();
      List<ARGState> l =
          FluentIterable.from(s.getChildren())
              .filter(x -> !SlicingAbstractionsUtils.isAbstractionState(x))
              .filter(x -> !reachable.contains(x))
              .toList();
      waitlist.addAll(l);
      reachable.addAll(l);
    }
    return reachable;
  }

  public static boolean isAbstractionState(ARGState pState) {
    return getPredicateState(pState).isAbstractionState() || !pState.wasExpanded();
  }

  public enum AbstractionPosition {
    START,
    END;

    public static final ImmutableSet<AbstractionPosition> BOTH = Sets.immutableEnumSet(START, END);
    public static final ImmutableSet<AbstractionPosition> NONE = ImmutableSet.of();
    public static final ImmutableSet<AbstractionPosition> ONLY_START = Sets.immutableEnumSet(START);
    public static final ImmutableSet<AbstractionPosition> ONLY_END = Sets.immutableEnumSet(END);
  }

  /**
   * Create a path formula for a part of the ARG.
   *
   * @param start The (abstraction) state to start at
   * @param stop The (abstraction) state to end at (has to be reachable via non-abstraction states
   *     from start!)
   * @param pSSAMap The SSAMap to start with (needed e.g. for building sequences of PathFormulas,
   *     see {@link SlicingAbstractionsBlockFormulaStrategy}
   * @param pFmgr formula manager used for instantiation of formulas
   * @param pPfmgr {@link PathFormulaManager} for making PathFormulas from {@link CFAEdge}s
   * @param withInvariants whether to include the abstraction formulas of start and stop (with the
   *     right SSA indices)
   * @return generated PathFormula
   * @throws CPATransferException building the {@link PathFormula} from {@link CFAEdge}s failed
   * @throws InterruptedException building the {@link PathFormula} from {@link CFAEdge}s got
   *     interrupted
   */
  public static PathFormula buildPathFormula(
      ARGState start,
      ARGState stop,
      SSAMap pSSAMap,
      PointerTargetSet pPts,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      ImmutableSet<AbstractionPosition> withInvariants)
      throws CPATransferException, InterruptedException {
    List<ARGState> segmentList =
        SlicingAbstractionsUtils.calculateOutgoingSegments(start).get(stop);
    if (segmentList == null) {
      segmentList = ImmutableList.of();
    }
    return buildPathFormula(start, stop, segmentList, pSSAMap, pPts, pPfmgr, withInvariants);
  }

  /**
   * For better scaling, call this method instead of {@link
   * SlicingAbstractionsUtils#buildPathFormula(ARGState, ARGState, List, SSAMap, PointerTargetSet,
   * PathFormulaManager, ImmutableSet)} if you already have calculated the segmentList (states
   * between start and stop state).
   */
  public static PathFormula buildPathFormula(
      ARGState start,
      ARGState stop,
      List<ARGState> segmentList,
      SSAMap pSSAMap,
      PointerTargetSet pPts,
      PathFormulaManager pPfmgr,
      ImmutableSet<AbstractionPosition> withInvariants)
      throws CPATransferException, InterruptedException {

    final PathFormula pathFormula;
    PathFormula startFormula;
    final PathFormulaBuilder pfb;

    // start with either an empty PathFormula or the abstraction state of start
    // (depending on what the caller specified)
    if (withInvariants.contains(AbstractionPosition.START)) {
      startFormula = invariantPathFormulaFromState(start, pSSAMap, pPts, pPfmgr);
    } else {
      startFormula = pPfmgr.makeEmptyPathFormulaWithContext(pSSAMap, pPts);
    }

    // Add assumptions if any:
    AbstractStateWithAssumptions other =
        AbstractStates.extractStateByType(stop, AbstractStateWithAssumptions.class);
    if (other != null && !(other instanceof DCAState)) {
      if (stop.isTarget() && other instanceof OverflowState) {
        other = ((OverflowState) other).getParent();
      }
      for (CExpression assumption : Iterables.filter(other.getAssumptions(), CExpression.class)) {
        startFormula = pPfmgr.makeAnd(startFormula, assumption);
      }
    }

    // generate the PathFormula for the path between start and stop
    // using the relevant non-abstraction states
    pfb = buildFormulaBuilder(start, stop, segmentList, pPfmgr);
    PathFormula p = pfb.build(pPfmgr, startFormula);

    // add the abstraction formula of abstraction state if the caller wants this:
    if (withInvariants.contains(AbstractionPosition.END)) {
      BooleanFormula endInvariant =
          PredicateAbstractState.getPredicateState(stop).getAbstractionFormula().asFormula();
      pathFormula = pPfmgr.makeAnd(p, endInvariant);
    } else {
      pathFormula = p;
    }

    return pathFormula;
  }

  private static PathFormulaBuilder buildFormulaBuilder(
      ARGState start, ARGState stop, List<ARGState> segmentList, PathFormulaManager pPfmgr) {
    final Map<ARGState, PathFormulaBuilder> finishedBuilders = new TreeMap<>();
    List<ARGState> allList = new ArrayList<>(segmentList);
    allList.add(0, start);
    allList.add(stop);

    for (ARGState currentState : allList) {
      PathFormulaBuilder currentBuilder = null;
      for (ARGState parent : currentState.getParents()) {
        if (finishedBuilders.containsKey(parent)) {
          if (currentBuilder == null) {
            CFAEdge edge = parent.getEdgeToChild(currentState);
            if (edge != null) {
              currentBuilder = finishedBuilders.get(parent).makeAnd(edge);
            } else {
              // aggregateBasicBlocks is enabled!
              List<CFAEdge> edges = parent.getEdgesToChild(currentState);
              assert !edges.isEmpty();
              currentBuilder = finishedBuilders.get(parent);
              for (CFAEdge e : edges) {
                currentBuilder = currentBuilder.makeAnd(e);
              }
            }
            DCAState dcaState = AbstractStates.extractStateByType(currentState, DCAState.class);
            if (dcaState != null) {
              for (CExpression assumption :
                  FluentIterable.from(dcaState.getAssumptions()).filter(CExpression.class)) {
                currentBuilder = currentBuilder.makeAnd(assumption);
              }
            }
          } else {
            CFAEdge edge = parent.getEdgeToChild(currentState);
            if (edge != null) {
              currentBuilder = currentBuilder.makeOr(finishedBuilders.get(parent).makeAnd(edge));
            } else {
              // aggregateBasicBlocks is enabled!
              PathFormulaBuilder otherBuilder = finishedBuilders.get(parent);
              List<CFAEdge> edges = parent.getEdgesToChild(currentState);
              assert !edges.isEmpty();
              for (CFAEdge e : edges) {
                otherBuilder = otherBuilder.makeAnd(e);
              }
              currentBuilder = currentBuilder.makeOr(otherBuilder);
            }
          }
        }
      }
      if (currentBuilder == null) {
        currentBuilder = pPfmgr.createNewPathFormulaBuilder();
      }
      finishedBuilders.put(currentState, currentBuilder);
    }

    return finishedBuilders.get(stop);
  }

  private static PathFormula invariantPathFormulaFromState(
      ARGState state, SSAMap pSSAMap, PointerTargetSet pPts, PathFormulaManager pfmgr) {
    BooleanFormula initFormula = getPredicateState(state).getAbstractionFormula().asFormula();
    return pfmgr.makeAnd(pfmgr.makeEmptyPathFormulaWithContext(pSSAMap, pPts), initFormula);
  }

  /**
   * Retrieve a list of {@link PathFormula}s for a given list of {@link ARGState}s and a starting
   * point, which can then be used for e.g. building a {@link BlockFormulas}.
   *
   * @param pfmgr {@link PathFormulaManager} for making PathFormulas from {@link CFAEdge}s
   * @param pSolver solver object that provides the formula manager
   * @param pRoot The (abstraction) state to start at
   * @param pPath the path consisting of a list of (abstraction) states
   * @param includePartialInvariants whether to include the abstraction formulas of the first and
   *     last ARGState (with the right SSA indices)
   * @return generated list of PathFormulas
   * @throws CPATransferException building the {@link PathFormula} from {@link CFAEdge}s failed
   * @throws InterruptedException building the {@link PathFormula} from {@link CFAEdge}s got
   *     interrupted
   */
  public static List<PathFormula> getFormulasForPath(
      PathFormulaManager pfmgr,
      Solver pSolver,
      ARGState pRoot,
      List<ARGState> pPath,
      ImmutableSet<AbstractionPosition> includePartialInvariants)
      throws CPATransferException, InterruptedException {

    return getFormulasForPath(
        pfmgr,
        pSolver,
        pRoot,
        pPath,
        SSAMap.emptySSAMap().withDefault(1),
        PointerTargetSet.emptyPointerTargetSet(),
        includePartialInvariants);
  }

  /**
   * Retrieve a list of {@link PathFormula}s for a given list of {@link ARGState}s and a starting
   * point.
   *
   * <p>See also {@link SlicingAbstractionsUtils#getFormulasForPath(PathFormulaManager, Solver,
   * ARGState, List, ImmutableSet)}
   */
  public static List<PathFormula> getFormulasForPath(
      PathFormulaManager pfmgr,
      Solver pSolver,
      ARGState pRoot,
      List<ARGState> pPath,
      SSAMap pSSAMap,
      PointerTargetSet pPts,
      ImmutableSet<AbstractionPosition> includePartialInvariants)
      throws CPATransferException, InterruptedException {

    List<PathFormula> abstractionFormulas = new ArrayList<>();

    PathFormula currentPathFormula =
        buildPathFormula(
            pRoot,
            pPath.get(0),
            pSSAMap,
            pPts,
            pSolver.getFormulaManager(),
            pfmgr,
            includePartialInvariants);
    abstractionFormulas.add(currentPathFormula);

    for (int i = 0; i < pPath.size() - 1; i++) {
      PathFormula oldPathFormula = currentPathFormula;
      currentPathFormula =
          buildPathFormula(
              pPath.get(i),
              pPath.get(i + 1),
              oldPathFormula.getSsa(),
              oldPathFormula.getPointerTargetSet(),
              pSolver.getFormulaManager(),
              pfmgr,
              includePartialInvariants);
      abstractionFormulas.add(currentPathFormula);
    }

    return abstractionFormulas;
  }

  /**
   * Copy the effective edges of a (abstraction) state over to a new (abstraction) state. This means
   * that we make a copy of the non-abstraction states that lie between originalState and each
   * abstraction state that is connected to originalState via non-abstraction states.
   *
   * @param forkedState state at which the new edges should start/ end
   * @param originalState state at which the original edges start/ end
   * @param pReached the reached-set that will track the added states
   */
  public static void copyEdges(
      ARGState forkedState, ARGState originalState, ARGReachedSet pReached) {

    final Map<ARGState, PersistentList<ARGState>> outgoingSegmentMap =
        calculateOutgoingSegments(originalState);
    final Map<ARGState, PersistentList<ARGState>> incomingSegmentMap =
        calculateIncomingSegments(originalState);

    // copy the outgoing edges:
    for (Map.Entry<ARGState, PersistentList<ARGState>> entry : outgoingSegmentMap.entrySet()) {
      ARGState endState = entry.getKey();
      List<ARGState> intermediateStateList = entry.getValue();
      copyEdge(intermediateStateList, originalState, endState, forkedState, endState, pReached);
      // if we have a self-loop, we have to make a self-loop from forkedState->forkedState:
      if (endState.equals(originalState)) {
        copyEdge(
            intermediateStateList, originalState, endState, forkedState, forkedState, pReached);
      }
    }

    // copy the incoming edges:
    for (Map.Entry<ARGState, PersistentList<ARGState>> entry : incomingSegmentMap.entrySet()) {
      ARGState startState = entry.getKey();
      List<ARGState> intermediateStateList = entry.getValue();
      copyEdge(intermediateStateList, startState, originalState, startState, forkedState, pReached);
    }
  }

  private static void copyEdge(
      List<ARGState> pSegmentStates,
      ARGState oldStartState,
      ARGState oldEndState,
      ARGState newStartState,
      ARGState newEndState,
      ARGReachedSet pReached) {

    // we need to treat the case where we have no intermediate non-abstraction states differently:
    if (oldEndState.getParents().contains(oldStartState)) {
      if (oldStartState instanceof SLARGState) {
        EdgeSet newEdgeSet =
            new EdgeSet(((SLARGState) oldStartState).getEdgeSetToChild(oldEndState));
        ((SLARGState) newEndState).addParent((SLARGState) newStartState, newEdgeSet);
      } else {
        newEndState.addParent(newStartState);
      }
      if (newEndState.equals(newStartState)) {
        // self loop already exists, no need to copy something
      }
    }

    // now if we have intermediate non-abstraction states, we copy them appropriately
    // Note that we make use of the special ordering of the states in pSegmentStates:
    // pSegmentState has to be ordered such that each parent of a state in the list has a lower
    // list index as the state itself (or isn't in the list at all)
    List<ARGState> newSegmentStates = new ArrayList<>();
    for (ARGState existingState : pSegmentStates) {
      ARGState newState;
      if (!(existingState instanceof SLARGState)) {
        newState = new ARGState(existingState.getWrappedState(), null);
      } else {
        newState = new SLARGState((SLARGState) existingState);
      }
      newState.makeTwinOf(existingState);
      newSegmentStates.add(newState);
      for (ARGState parent : existingState.getParents()) {
        if (pSegmentStates.contains(parent)) {
          copyParent(
              parent,
              existingState,
              newSegmentStates.get(pSegmentStates.indexOf(parent)),
              newState);
        }
      }
      if (existingState.getParents().contains(oldStartState)) {
        copyParent(oldStartState, existingState, newStartState, newState);
      }
      if (existingState.getChildren().contains(oldEndState)) {
        copyParent(existingState, oldEndState, newState, newEndState);
      }
    }
    addForkedStatesToReachedSet(newSegmentStates, pSegmentStates, pReached);
  }

  private static void addForkedStatesToReachedSet(
      List<ARGState> newStates, List<ARGState> originalStates, ARGReachedSet pReached) {
    checkArgument(newStates.size() == originalStates.size());
    for (int i = 0; i < newStates.size(); i++) {
      pReached.addForkedState(newStates.get(i), originalStates.get(i));
    }
  }

  private static void copyParent(
      ARGState oldParent, ARGState oldState, ARGState newParent, ARGState newState) {
    if (!(newParent instanceof SLARGState)) {
      newState.addParent(newParent);
    } else {
      EdgeSet newEdgeSet = new EdgeSet(((SLARGState) oldParent).getEdgeSetToChild(oldState));
      ((SLARGState) newState).addParent((SLARGState) newParent, newEdgeSet);
    }
  }

  /**
   * Check that there is no path through the ARG anymore that has the same sequence of abstraction
   * states as the error path. This is a mathematically proven progress property that should hold
   * after splitting and slicing. This method makes no restrictions on the shape of the ARG, so one
   * abstraction state could have several abstraction state parents. For analysis where this is not
   * the case there are more efficient ways to check this.
   */
  public static boolean checkProgress(UnmodifiableReachedSet pReached, ARGPath pErrorPath) {
    Set<ARGState> rootStates = ARGUtils.getRootStates(pReached);
    assert rootStates.size() == 1;
    ARGState root = rootStates.iterator().next();
    final List<ARGState> abstractionStatesTrace =
        PredicateCPARefiner.filterAbstractionStates(pErrorPath);
    assert abstractionStatesTrace.get(0).getStateId() != 0;
    for (int i = -1; i < abstractionStatesTrace.size() - 1; i++) {
      ARGState first = (i == -1) ? root : abstractionStatesTrace.get(i);
      ARGState second = abstractionStatesTrace.get(i + 1);
      if (!SlicingAbstractionsUtils.calculateOutgoingSegments(first).containsKey(second)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Calculates all states on the error path to the given error state. This method is a replacement
   * of {@link ARGUtils#getAllStatesOnPathsTo(ARGState)} for cases where the abstraction states in
   * the ARG do not form a tree.
   *
   * @param errorState The state at which the error path ends. Should ideally contain counterexample
   *     information
   * @return A set of all states on the path to errorState
   */
  public static Set<ARGState> getStatesOnErrorPath(ARGState errorState) {

    final ARGPath path;
    if (errorState.getCounterexampleInformation().isPresent()) {
      CounterexampleInfo cexInfo = errorState.getCounterexampleInformation().orElseThrow();
      path = cexInfo.getTargetPath();
    } else {
      // fall back solution:
      path = ARGUtils.getOnePathTo(errorState);
    }

    final ImmutableList<ARGState> abstractionStatesOnErrorPath =
        from(path.asStatesList()).filter(x -> isAbstractionState(x)).toList();

    final Set<ARGState> statesOnErrorPath = new HashSet<>(abstractionStatesOnErrorPath);

    for (int i = 0; i < abstractionStatesOnErrorPath.size() - 1; i++) {
      ARGState start = abstractionStatesOnErrorPath.get(i);
      ARGState stop = abstractionStatesOnErrorPath.get(i + 1);
      statesOnErrorPath.addAll(SlicingAbstractionsUtils.calculateOutgoingSegments(start).get(stop));
    }

    return statesOnErrorPath;
  }

  private static Set<CFANode> getIncomingLocations(SLARGState pState) {
    ImmutableSet.Builder<CFANode> locations = ImmutableSet.builder();
    for (ARGState parent : pState.getParents()) {
      for (CFAEdge edge : ((SLARGState) parent).getEdgeSetToChild(pState)) {
        locations.add(edge.getSuccessor());
      }
    }
    return locations.build();
  }

  private static Set<CFANode> getOutgoingLocations(SLARGState pState) {
    ImmutableSet.Builder<CFANode> locations = ImmutableSet.builder();
    for (ARGState child : pState.getChildren()) {
      for (CFAEdge edge : pState.getEdgeSetToChild(child)) {
        locations.add(edge.getPredecessor());
      }
    }
    return locations.build();
  }

  public static void removeIncomingEdgesWithLocationMismatch(SLARGState state) {
    if (state.isTarget() || state.getParents().isEmpty()) {
      return;
    }
    Set<CFANode> locations = getOutgoingLocations(state);
    List<ARGState> toRemove = new ArrayList<>();
    for (ARGState parent : state.getParents()) {
      EdgeSet edgeSet = ((SLARGState) parent).getEdgeSetToChild(state);
      if (edgeSet != null) {
        for (Iterator<CFAEdge> it = edgeSet.iterator(); it.hasNext(); ) {
          CFAEdge edge = it.next();
          if (!locations.contains(edge.getSuccessor())) {
            it.remove();
          }
        }
        if (edgeSet.isEmpty()) {
          toRemove.add(parent);
        }
      }
    }
    for (ARGState parent : toRemove) {
      state.removeParent(parent);
    }
  }

  public static void removeOutgoingEdgesWithLocationMismatch(SLARGState state) {
    if (state.isTarget() || state.getParents().isEmpty()) {
      return;
    }
    Set<CFANode> locations = getIncomingLocations(state);
    List<ARGState> toRemove = new ArrayList<>();
    for (ARGState child : state.getChildren()) {
      EdgeSet edgeSet = state.getEdgeSetToChild(child);

      if (edgeSet != null) {
        for (Iterator<CFAEdge> it = edgeSet.iterator(); it.hasNext(); ) {
          CFAEdge edge = it.next();
          if (!locations.contains(edge.getPredecessor())) {
            it.remove();
          }
        }
        if (edgeSet.isEmpty()) {
          toRemove.add(child);
        }
      }
    }
    for (ARGState child : toRemove) {
      child.removeParent(state);
    }
  }

  private static boolean blk(ARGState pState) {

    // if it is the root state, return true:
    if (pState.getParents().isEmpty()) {
      return true;
    }
    // if it is a target state, return true:
    if (pState.isTarget()) {
      return true;
    }
    // if it is part of multiple incoming blocks, return true:
    if (calculateStartStates(pState).size() > 1) {
      return true;
    }
    // it is a loop head, return true:
    if (calculateOutgoingSegments(pState).containsKey(pState)) {
      return true;
    }
    if (pState instanceof SLARGState) {
      // if not all EdgeSets from parents to pState are singletons, return true:
      if (!pState.getParents().stream()
          .map(parent -> ((SLARGState) parent).getEdgeSetToChild(pState))
          .allMatch(x -> x.size() == 1)) {
        return true;
      }
      // if not all EdgeSets from pState to children are singletons, return true:
      if (!pState.getChildren().stream()
          .map(child -> ((SLARGState) pState).getEdgeSetToChild(child))
          .allMatch(x -> x.size() == 1)) {
        return true;
      }
    }
    if (calculateIncomingSegments(pState).entrySet().size() > 1
        || calculateOutgoingSegments(pState).entrySet().size() > 1) {
      return true;
    }
    return false;
  }

  static boolean performDynamicBlockEncoding(ARGReachedSet pArgReachedSet) {
    boolean changed = false;
    for (AbstractState state : new ArrayList<>(pArgReachedSet.asReachedSet().asCollection())) {
      ARGState currentState = (ARGState) state;
      PredicateAbstractState predState = PredicateAbstractState.getPredicateState(currentState);
      assert predState != null;
      if (predState.isAbstractionState() && !blk(currentState)) {
        changed = true;
        PredicateAbstractState replacement =
            PredicateAbstractState.mkNonAbstractionState(
                predState.getPathFormula(),
                predState.getAbstractionFormula(),
                predState.getAbstractionLocationsOnPath());
        ARGState newState = currentState.forkWithReplacements(Collections.singleton(replacement));
        currentState.replaceInARGWith(newState);
        pArgReachedSet.addForkedState(newState, (ARGState) state);
        if (newState instanceof SLARGState) {
          // check for incoming edges that do not have a suitable outgoing edge for their successor
          // location. E.g.: A-{1~>2}->B-{3~>4}->C
          // transfer from 1~>2 will be removed
          removeIncomingEdgesWithLocationMismatch((SLARGState) newState);

          // now do the same the other way around (check for outgoing edges that do not have a
          removeOutgoingEdgesWithLocationMismatch((SLARGState) newState);
        }
      } else if (predState.isAbstractionState() && !((ARGState) state).getParents().isEmpty()) {
        // here it is only sound to check for outgoing edges that do not have a suitable incoming
        // edge
        if (state instanceof SLARGState) {
          removeOutgoingEdgesWithLocationMismatch((SLARGState) state);
        }
      }
    }
    return changed;
  }
}
