/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
*/
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.GraphUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * Helper class with collection of ARG related utility methods.
 */
/**
 *
 */
public class ARGUtils {

  private ARGUtils() { }

  /**
   * Get all elements on all paths from the ARG root to a given element.
   *
   * @param pLastElement The last element in the paths.
   * @return A set of elements, all of which have pLastElement as their (transitive) child.
   */
  public static Set<ARGState> getAllStatesOnPathsTo(ARGState pLastElement) {

    Set<ARGState> result = new HashSet<>();
    Deque<ARGState> waitList = new ArrayDeque<>();

    result.add(pLastElement);
    waitList.add(pLastElement);

    while (!waitList.isEmpty()) {
      ARGState currentElement = waitList.poll();
      for (ARGState parent : currentElement.getParents()) {
        if (result.add(parent)) {
          waitList.push(parent);
        }
      }
    }

    return result;
  }

  /**
   * Get all abstract states without parents.
   */
  public static Set<ARGState> getRootStates(UnmodifiableReachedSet pReached) {

    Set<ARGState> result = new HashSet<>();

    for (AbstractState e : pReached) {
      ARGState state = AbstractStates.extractStateByType(e, ARGState.class);
      if (state.getParents().isEmpty()) {
        result.add(state);
      }
    }

    return result;
  }

  /**
   * Create a path in the ARG from root to the given element.
   * If there are several such paths, one is chosen randomly.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static ARGPath getOnePathTo(ARGState pLastElement) {
    List<ARGState> states = new ArrayList<>(); // reversed order
    Set<ARGState> seenElements = new HashSet<>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pLastElement;
    states.add(currentARGState);
    seenElements.add(currentARGState);

    while (!currentARGState.getParents().isEmpty()) {
      Iterator<ARGState> parents = currentARGState.getParents().iterator();

      ARGState parentElement = parents.next();
      while (!seenElements.add(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      states.add(parentElement);

      currentARGState = parentElement;
    }
    return new ARGPath(Lists.reverse(states));
  }

  public static Optional<ARGPath> getOnePathTo(
      final ARGState pEndState, final Collection<ARGPath> pOtherPathThan) {

    List<ARGState> states = new ArrayList<>(); // reversed order
    Set<ARGState> seenElements = new HashSet<>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pEndState;
    CFANode currentLocation = AbstractStates.extractLocation(pEndState);
    states.add(currentARGState);
    seenElements.add(currentARGState);

    Collection<PathPosition> tracePrefixesToAvoid =
        Collections2.transform(
            pOtherPathThan,
            pArg0 -> {
              PathPosition result = pArg0.reversePathIterator().getPosition();
              CFANode expectedPostfixLoc = AbstractStates.extractLocation(pEndState);
              Verify.verify(result.getLocation().equals(expectedPostfixLoc));
              return result;
            });

    // Get all traces from pTryCoverOtherStatesThan that start at the same location
    tracePrefixesToAvoid = getTracePrefixesBeforePostfix(tracePrefixesToAvoid, currentLocation);

    boolean lastTransitionIsDifferent = false;
    while (!currentARGState.getParents().isEmpty()) {
      List<ARGState> potentialParents = Lists.newArrayList();
      potentialParents.addAll(currentARGState.getParents());
      if (!tracePrefixesToAvoid.isEmpty()) {
        potentialParents.addAll(currentARGState.getCoveredByThis());
      }
      Iterator<ARGState> parents = potentialParents.iterator();

      boolean uniqueParentFound = false;
      ARGState parentElement = parents.next();

      do {
        while (!seenElements.add(parentElement) && parents.hasNext()) {
          // while seenElements already contained parentElement, try next parent
          parentElement = parents.next();
        }

        // goal: choosen a path that has not yet been taken
        uniqueParentFound = true;
        final CFANode parentLocation = extractLocation(parentElement);
        for (PathPosition t: tracePrefixesToAvoid) {
          if (t.getLocation().equals(parentLocation)) {
            uniqueParentFound = false;
            lastTransitionIsDifferent = false;
            break;
          }
        }

        lastTransitionIsDifferent = tracePrefixesToAvoid.isEmpty();
      } while (!uniqueParentFound && parents.hasNext());

      states.add(parentElement);

      currentARGState = parentElement;
      currentLocation = AbstractStates.extractLocation(currentARGState);
      tracePrefixesToAvoid = getTracePrefixesBeforePostfix(tracePrefixesToAvoid, currentLocation);
    }

    if (!lastTransitionIsDifferent) {
      return Optional.empty();
    }

    return Optional.of(new ARGPath(Lists.reverse(states)));
  }

  public static Collection<PathPosition> getTracePrefixesBeforePostfix(
      final Collection<PathPosition> pTracePosition,
      final CFANode pPostfixLocation) {

    Preconditions.checkNotNull(pTracePosition);
    Preconditions.checkNotNull(pPostfixLocation);

    Builder<PathPosition> result = ImmutableList.builder();

    for (PathPosition p: pTracePosition) {

      if (pPostfixLocation.equals(p.getLocation())) {
        PathIterator it = p.reverseIterator();

        if (!it.hasNext()) {
          continue;
        }

        it.advance();
        result.add(it.getPosition());
      }
    }

    return result.build();
  }

  /**
   * Get one random path from the ARG root to an ARG leaf.
   * @param root The root state of an ARG (may not have any parents)
   */
  public static ARGPath getRandomPath(final ARGState root) {
    checkArgument(root.getParents().isEmpty());

    List<ARGState> states = new ArrayList<>();
    ARGState currentElement = root;
    while (currentElement.getChildren().size() > 0) {
      states.add(currentElement);
      currentElement = currentElement.getChildren().iterator().next();
    }
    states.add(currentElement);
    return new ARGPath(states);
  }

  private static final Predicate<CFANode> IS_RELEVANT_LOCATION =
      pInput ->
          pInput.isLoopStart()
              || pInput instanceof FunctionEntryNode
              || pInput instanceof FunctionExitNode;

  private static final Predicate<Iterable<CFANode>> CONTAINS_RELEVANT_LOCATION =
      nodes -> Iterables.any(nodes, IS_RELEVANT_LOCATION);

  private static final Predicate<AbstractState> AT_RELEVANT_LOCATION =
      Predicates.compose(CONTAINS_RELEVANT_LOCATION, AbstractStates::extractLocations);

  @SuppressWarnings("unchecked")
  static final Predicate<ARGState> RELEVANT_STATE =
      Predicates.or(
          AbstractStates.IS_TARGET_STATE,
          AT_RELEVANT_LOCATION,
          pInput -> !pInput.wasExpanded(),
          ARGState::shouldBeHighlighted);

  /**
   * Project the ARG to a subset of "relevant" states.
   * The result is a SetMultimap containing the successor relationships between all relevant states.
   * A pair of states (a, b) is in the SetMultimap,
   * if there is a path through the ARG from a to b which does not pass through
   * any other relevant state.
   *
   * To get the predecessor relationship, you can use {@link Multimaps#invertFrom(com.google.common.collect.Multimap, com.google.common.collect.Multimap)}.
   *
   * @param root The start of the subgraph of the ARG to project (always considered relevant).
   * @param isRelevant The predicate determining which states are in the resulting relationship.
   */
  public static SetMultimap<ARGState, ARGState> projectARG(final ARGState root,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      Predicate<? super ARGState> isRelevant) {

    return GraphUtils.projectARG(root, successorFunction, isRelevant);
  }

  /**
   * Find a path in the ARG. The necessary information to find the path is a
   * boolean value for each branching situation that indicates which of the two
   * AssumeEdges should be taken.
   *
   * @param root The root element of the ARG (where to start the path)
   * @param arg All elements in the ARG or a subset thereof (elements outside this set will be ignored).
   * @param branchingInformation A map from ARG state ids to boolean values indicating the outgoing direction.
   * @return A path through the ARG from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root, Set<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {
    return getPathFromBranchingInformation(root, arg, branchingInformation, true);
  }

  /**
   * Find a path in the ARG. The necessary information to find the path is a boolean value for each
   * branching situation that indicates which of the two AssumeEdges should be taken.
   *
   * @param root The root element of the ARG (where to start the path)
   * @param arg All elements in the ARG or a subset thereof (elements outside this set will be
   *     ignored).
   * @param branchingInformation A map from ARG state ids to boolean values indicating the outgoing
   *     direction.
   * @param mustEndInTarget If {@code true}, the path must end in a target state to be considered
   *     consistent.
   * @return A path through the ARG unambiguously described by the branching information.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG
   *     is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root,
      Set<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation,
      boolean mustEndInTarget)
      throws IllegalArgumentException {

    checkArgument(arg.contains(root));

    ARGPathBuilder builder = ARGPath.builder();
    ARGState currentElement = root;
    while (!currentElement.isTarget()) {
      Set<ARGState> children = Sets.newHashSet(currentElement.getChildren());
      Set<ARGState> childrenInArg = Sets.intersection(children, arg).immutableCopy();

      ARGState child;
      CFAEdge edge;
      switch (childrenInArg.size()) {

        case 0:
          if (mustEndInTarget) {
            throw new IllegalArgumentException(
                "ARG target path terminates without reaching target state!");
          }
          return builder.build(currentElement);

      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(childrenInArg);
        edge = currentElement.getEdgeToChild(child);
        break;

      case 2: // branch
        // first, find out the edges and the children
        CFAEdge trueEdge = null;
        CFAEdge falseEdge = null;
        ARGState trueChild = null;
        ARGState falseChild = null;

        CFANode loc = AbstractStates.extractLocation(currentElement);
        if (!leavingEdges(loc).allMatch(Predicates.instanceOf(AssumeEdge.class))) {
          throw new IllegalArgumentException("ARG branches where there is no AssumeEdge!");
        }

        for (ARGState currentChild : childrenInArg) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            trueEdge = currentEdge;
            trueChild = currentChild;
          } else {
            falseEdge = currentEdge;
            falseChild = currentChild;
          }
        }
        if (trueEdge == null || falseEdge == null) {
          throw new IllegalArgumentException("ARG branches with non-complementary AssumeEdges!");
        }
        assert trueChild != null;
        assert falseChild != null;

        // search first idx where we have a predicate for the current branching
        Boolean predValue = branchingInformation.get(currentElement.getStateId());
        if (predValue == null) {
          throw new IllegalArgumentException("ARG branches without direction information!");
        }

        // now select the right edge
        if (predValue) {
          edge = trueEdge;
          child = trueChild;
        } else {
          edge = falseEdge;
          child = falseChild;
        }
        break;

      default:
        throw new IllegalArgumentException("ARG splits with more than two branches!");
      }

      if (!arg.contains(child)) {
        throw new IllegalArgumentException("ARG and direction information from solver disagree!");
      }

      builder.add(currentElement, edge);
      currentElement = child;
    }

    return builder.build(currentElement);
  }

  /**
   * Find a path in the ARG. The necessary information to find the path is a
   * boolean value for each branching situation that indicates which of the two
   * AssumeEdges should be taken.
   * This method checks that the path ends in a certain element.
   *
   * @param root The root element of the ARG (where to start the path)
   * @param target The target state (where to end the path, needs to be a target state)
   * @param arg All elements in the ARG or a subset thereof (elements outside this set will be ignored).
   * @param branchingInformation A map from ARG state ids to boolean values indicating the outgoing direction.
   * @return A path through the ARG from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root, ARGState target, Set<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(arg.contains(target));
    checkArgument(target.isTarget());

    ARGPath result = getPathFromBranchingInformation(root, arg, branchingInformation);

    if (result.getLastState() != target) {
      throw new IllegalArgumentException("ARG target path reached the wrong target state!");
    }

    return result;
  }

  /**
   * This method gets all children from an ARGState,
   * but replaces all covered states by their respective covering state.
   * It can be seen as giving a view of the ARG where the covered states are
   * transparently replaced by their covering state.
   *
   * The returned collection is unmodifiable and a live view of the children of
   * the given state.
   *
   * @param s an ARGState
   * @return The children with covered states transparently replaced.
   */
  public static Collection<ARGState> getUncoveredChildrenView(final ARGState s) {
    return new AbstractCollection<ARGState>() {

      @Override
      public Iterator<ARGState> iterator() {

        return new UnmodifiableIterator<ARGState>() {
          private final Iterator<ARGState> children = s.getChildren().iterator();

          @Override
          public boolean hasNext() {
            return children.hasNext();
          }

          @Override
          public ARGState next() {
            ARGState child = children.next();
            if (child.isCovered()) {
              return checkNotNull(child.getCoveringState());
            }
            return child;
          }
        };
      }

      @Override
      public int size() {
        return s.getChildren().size();
      }
    };
  }

  /**
   * Check consistency of ARG, and consistency between ARG and reached set.
   *
   * Checks we do here currently:
   * - child-parent relationship of ARG states
   * - states in ARG are also in reached set and vice versa (as far as possible to check)
   * - no destroyed states present
   *
   * This method is potentially expensive,
   * and should be called only from an assert statement.
   * @return <code>true</code>
   * @throws AssertionError If any consistency check is violated.
   */
  public static boolean checkARG(ReachedSet pReached) {
    // Not all states in ARG might be reachable from a single root state
    // in case of multiple initial states and disjoint ARGs.

    for (ARGState e : from(pReached).transform(toState(ARGState.class))) {
      assert e != null : "Reached set contains abstract state without ARGState.";
      assert !e.isDestroyed() : "Reached set contains destroyed ARGState, which should have been removed.";

      for (ARGState parent : e.getParents()) {
        assert parent.getChildren().contains(e) : "Reference from parent to child is missing in ARG";
        assert pReached.contains(parent) : "Referenced parent is missing in reached";
      }

      for (ARGState child : e.getChildren()) {
        assert child.getParents().contains(e) : "Reference from child to parent is missing in ARG";

        // Usually, all children should be in reached set, with two exceptions.
        // 1) Covered states need not be in the reached set (this depends on cpa.arg.keepCoveredStatesInReached),
        // but if they are not in the reached set, they may not have children.
        // 2) If the state is the sibling of the target state, it might have not
        // been added to the reached set if CPAAlgorithm stopped before.
        // But in this case its parent is in the waitlist.

        if (!pReached.contains(child)) {
          assert (child.isCovered() && child.getChildren().isEmpty()) // 1)
              || pReached.getWaitlist().containsAll(child.getParents()) // 2)
              : "Referenced child is missing in reached set.";
        }
      }
    }

    return true;
  }

  /**
   * Produce an automaton in the format for the AutomatonCPA from a given connected list of paths.
   * The automaton matches exactly the edges along the path. If there is a target state, it is
   * signaled as an error state in the automaton.
   *
   * @param sb Where to write the automaton to
   * @param pPaths The states along the path
   * @param pCounterExample Given to try to write exact variable assignment values into the
   *     automaton, may be null
   */
  public static void producePathAutomaton(
      Appendable sb,
      List<ARGPath> pPaths,
      String name,
      @Nullable CounterexampleInfo pCounterExample)
      throws IOException {

    ARGState rootState = pPaths.iterator().next().getFirstState();

    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = ImmutableMultimap.of();

    if (pCounterExample != null && pCounterExample.isPreciseCounterExample()) {
      valueMap = pCounterExample.getExactVariableValues();
    }

    int index = 0;

    Function<ARGState, String> getLocationName =
        s -> Joiner.on("_OR_").join(AbstractStates.extractLocations(s));
    Function<Integer, Function<ARGState, String>> getStateNameFunction =
        i -> (s -> "S" + i + "at" + getLocationName.apply(s));

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    String stateName = getStateNameFunction.apply(index).apply(rootState);
    sb.append("INITIAL STATE " + stateName + ";\n\n");

    for (ARGPath path : pPaths) {
      PathIterator pathIterator = path.fullPathIterator();
      while (pathIterator.advanceIfPossible()) {
        stateName =
            getStateNameFunction.apply(index).apply(pathIterator.getPreviousAbstractState());
        ++index;
        sb.append("STATE USEFIRST " + stateName + " :\n");
        ARGState child = pathIterator.getAbstractState();
        CFAEdge edge = pathIterator.getIncomingEdge();

        handleMatchCase(sb, edge);

        if (child.isTarget()) {
          sb.append("ERROR");
        } else {
          addAssumption(valueMap, pathIterator.getPreviousAbstractState(), edge, sb);
          stateName = getStateNameFunction.apply(index).apply(child);
          sb.append("GOTO " + stateName);
        }
        sb.append(";\n");
        sb.append("    TRUE -> STOP;\n\n");
      }
    }
    sb.append("END AUTOMATON\n");
  }

  /**
   * Produce an automaton in the format for the AutomatonCPA from
   * a given path. The automaton matches exactly the edges along the path.
   * If there is a target state, it is signaled as an error state in the automaton.
   * @param sb Where to write the automaton to
   * @param pRootState The root of the ARG
   * @param pPathStates The states along the path
   * @param pCounterExample Given to try to write exact variable assignment values
   * into the automaton, may be null
   */
  public static void producePathAutomaton(Appendable sb, ARGState pRootState,
      Set<ARGState> pPathStates, String name, @Nullable CounterexampleInfo pCounterExample) throws IOException {

    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = ImmutableMultimap.of();

    if (pCounterExample != null && pCounterExample.isPreciseCounterExample()) {
      valueMap = pCounterExample.getExactVariableValues();
    }

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    sb.append("INITIAL STATE ARG" + pRootState.getStateId() + ";\n\n");

    int multiEdgeCount = 0; // see below

    for (ARGState s : Ordering.natural().immutableSortedCopy(pPathStates)) {

      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");

      for (ARGState child : s.getChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        if (pPathStates.contains(child)) {
          List<CFAEdge> allEdges = s.getEdgesToChild(child);
          CFAEdge edge;

          if (allEdges.size() == 1) {
            edge = Iterables.getOnlyElement(allEdges);

            // this is a dynamic multi edge
          } else {
            // The successor state might have several incoming MultiEdges.
            // In this case the state names like ARG<successor>_0 would occur
            // several times.
            // So we add this counter to the state names to make them unique.
            multiEdgeCount++;

            // first, write edge entering the list
            int i = 0;
            sb.append("    MATCH \"");
            escape(allEdges.get(i).getRawStatement(), sb);
            sb.append("\" -> ");
            sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
            sb.append(";\n");

            // inner part (without first and last edge)
            for (; i < allEdges.size() - 1; i++) {
              sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
              sb.append("    MATCH \"");
              escape(allEdges.get(i).getRawStatement(), sb);
              sb.append("\" -> ");
              sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
              sb.append(";\n");
            }

            // last edge connecting it with the real successor
            edge = allEdges.get(i);
            sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
          }

          handleMatchCase(sb, edge);

          if (child.isTarget()) {
            sb.append("ERROR");
          } else {
            addAssumption(valueMap, s, edge, sb);
            sb.append("GOTO ARG" + child.getStateId());
          }
          sb.append(";\n");
        }
      }
      sb.append("    TRUE -> STOP;\n\n");
    }
    sb.append("END AUTOMATON\n");
  }

  /**
   * Produce an automaton in the format for the AutomatonCPA from
   * a given path. The automaton matches the edges along the path until a
   * state is at location which is also included in a loop. Then this loop
   * is recreated. Outgoing edges of this loop are then handled once again
   * as they occur in the path. So for all outgoing edges of a loop which
   * do not occur in the given path we create a sink (TRUE) and for the outgoing
   * edge which is on the path we continue with unrolling the ARGPath from this
   * point.
   * If there is a target state, it is signaled as an error state in the automaton.
   *
   * This method does not work if the path has recursive elements.
   *
   * @param sb Where to write the automaton to
   * @param pRootState The root of the ARG
   * @param pPathStates The states along the path
   * @param name the name the automaton should have
   * @param loopsToUproll the loops which should be recreated in the automaton
   */
  public static void producePathAutomatonWithLoops(Appendable sb, ARGState pRootState,
      Set<ARGState> pPathStates, String name, Set<Loop> loopsToUproll) throws IOException {

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    sb.append("INITIAL STATE ARG" + pRootState.getStateId() + ";\n\n");

    int multiEdgeCount = 0; // see below

    ARGState inLoopState = null;
    ARGState outLoopState = null;
    ARGState inFunctionState = null;
    ARGState outFunctionState = null;
    Map<ARGState, ARGState> inToOutLoopMap = new HashMap<>();
    Map<ARGState, ARGState> inToOutFunctionsMap = new HashMap<>();

    CFANode inLoopNode = null;
    CFANode inFunctionNode = null;

    ImmutableList<ARGState> sortedStates = Ordering.natural().immutableSortedCopy(pPathStates);

    LinkedList<String> sortedFunctionOccurrence = new LinkedList<>();
    for (ARGState s : sortedStates) {
      CFANode node = extractLocation(s);
      if (!sortedFunctionOccurrence.isEmpty()
          && sortedFunctionOccurrence.getLast().equals(node.getFunctionName())) {
        continue;
      } else {
        sortedFunctionOccurrence.add(node.getFunctionName());
      }
    }

    for (ARGState s : sortedStates) {
      CFANode loc = AbstractStates.extractLocation(s);

      boolean loopFound = false;
      for (Loop loop : loopsToUproll) {
        if (loop.getLoopNodes().contains(loc)) {
          loopFound = true;
          break;
        }
      }

      if (loopFound && inLoopState == null) {
        inLoopState = s;
        inLoopNode = extractLocation(inLoopState);
        outLoopState = null;
        continue;

        // function call inside a loop we want to uproll
      } else if (!loopFound
                 && inLoopNode != null
                 && !inLoopNode.getFunctionName().equals(extractLocation(s).getFunctionName())) {
        continue;

        // function call in the path we want to uproll
      } else if (!loopFound
          && inLoopNode == null
          && loc.getLeavingSummaryEdge() != null
          && inFunctionState == null
          // we have found the function where the error is, as we do not support
          // direct and indirect recursion we know that we can skip all other function
          // calls right now
          && sortedFunctionOccurrence.getFirst().equals(sortedFunctionOccurrence.getLast())
          && sortedFunctionOccurrence.size() > 1) {
        inFunctionState = s;
        inFunctionNode = extractLocation(inFunctionState);
        outFunctionState = null;

      } else if (inFunctionNode != null
          // as long as we are in the other function we can just continue, this
          // is handled later on
          && !inFunctionNode.getFunctionName().equals(extractLocation(s).getFunctionName())) {
        continue;

      } else if (!loopFound) {
        if (inLoopState != null && outLoopState == null) {
          outLoopState = s;
          inToOutLoopMap.put(inLoopState, outLoopState);
          inLoopNode = null;
          inLoopState = null;
        }
        if (inFunctionState != null && outFunctionState == null) {
          outFunctionState = s;
          inToOutFunctionsMap.put(inFunctionState, outFunctionState);
          inFunctionNode = null;
          inFunctionState = null;
        }
        if (!sortedFunctionOccurrence.getFirst().equals(extractLocation(s).getFunctionName())) {
          sortedFunctionOccurrence.removeFirst();
        }

        sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");

        // no loop found up to now, we can create the states without
        // any special constraints
        if (!loopFound) {
          for (ARGState child : s.getChildren()) {
            if (child.isCovered()) {
              child = child.getCoveringState();
              assert !child.isCovered();
            }

            if (pPathStates.contains(child)) {
              List<CFAEdge> allEdges = s.getEdgesToChild(child);
              CFAEdge edge;

              if (allEdges.size() == 1) {
                edge = Iterables.getOnlyElement(allEdges);

                // this is a dynamic multi edge
              } else {
                // The successor state might have several incoming MultiEdges.
                // In this case the state names like ARG<successor>_0 would occur
                // several times.
                // So we add this counter to the state names to make them unique.
                multiEdgeCount++;

                // first, write edge entering the list
                int i = 0;
                sb.append("    MATCH \"");
                escape(allEdges.get(i).getRawStatement(), sb);
                sb.append("\" -> ");
                sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
                sb.append(";\n");

                // inner part (without first and last edge)
                for (; i < allEdges.size() - 1; i++) {
                  sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
                  sb.append("    MATCH \"");
                  escape(allEdges.get(i).getRawStatement(), sb);
                  sb.append("\" -> ");
                  sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
                  sb.append(";\n");
                }

                // last edge connecting it with the real successor
                edge = allEdges.get(i);
                sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
              }

              handleMatchCase(sb, edge);

              if (child.isTarget()) {
                sb.append("ERROR");
              } else {
                sb.append("GOTO ARG" + child.getStateId());
              }
              sb.append(";\n");
            }
          }
        }
        sb.append("    TRUE -> STOP;\n\n");
      }
    }

    // now handle loop
    for (Entry<ARGState, ARGState> entry : inToOutLoopMap.entrySet()) {
      ARGState intoLoopState = entry.getKey();
      ARGState outOfLoopState = entry.getValue();
      handleLoop(sb, loopsToUproll, intoLoopState, outOfLoopState);
    }

    for (Entry<ARGState, ARGState> entry : inToOutFunctionsMap.entrySet()) {
      ARGState intoFunctionState = entry.getKey();
      ARGState outOfFunctionState = entry.getValue();
      handleFunctionCall(sb, intoFunctionState, outOfFunctionState);
    }

    // last loop encountered has no outgoing edge
    if (inLoopState != null) {
      handleLoop(sb, loopsToUproll, inLoopState, null);
    }

    sb.append("END AUTOMATON\n");
  }

  private static void handleFunctionCall(Appendable sb, ARGState callState, ARGState returnState)
      throws IOException {
    FunctionSummaryEdge sumEdge = extractLocation(callState).getLeavingSummaryEdge();
    CFANode sumEdgePredecessor = sumEdge.getPredecessor();

    sb.append("STATE USEFIRST ARG")
        .append(Integer.toString(callState.getStateId()))
        .append(" :\n")
        .append("    TRUE -> ");
    handleGotoNode(sb, sumEdgePredecessor, true);

    handleUseFirstNode(sb, sumEdgePredecessor, true);

    sb.append("    ( CHECK(location, \"functionname==")
        .append(sumEdgePredecessor.getFunctionName())
        .append("\")) -> ");
    handleGotoArg(sb, returnState); // edge from function back to path

    sb.append("    TRUE -> ");
    handleGotoNode(sb, sumEdgePredecessor, true);
    sb.append("\n");
  }

  private static void handleLoop(Appendable sb, Set<Loop> loopsToUproll, ARGState intoLoopState,
      ARGState outOfLoopState) throws IOException {

    Set<CFANode> handledNodes = new HashSet<>();
    Deque<CFANode> nodesToHandle = new ArrayDeque<>();
    CFANode loopHead = AbstractStates.extractLocation(intoLoopState);
    nodesToHandle.offer(loopHead);
    boolean isFirstLoopIteration = true;
    while(!nodesToHandle.isEmpty()) {
      CFANode curNode = nodesToHandle.poll();
      if (!handledNodes.add(curNode)) {
        continue;
      }

      if (isFirstLoopIteration) {
        sb.append("STATE USEFIRST ARG")
          .append(Integer.toString(intoLoopState.getStateId()))
          .append(" :\n");
        isFirstLoopIteration = false;
      } else {
        handleUseFirstNode(sb, curNode, false);
      }

      for(CFAEdge edge : leavingEdges(curNode)) {
        CFANode edgeSuccessor = edge.getSuccessor();

        // skip function calls
        if (edge instanceof FunctionCallEdge) {
          FunctionSummaryEdge sumEdge = ((FunctionCallEdge) edge).getSummaryEdge();
          CFANode sumEdgeSuccessor = sumEdge.getSuccessor();

          // only continue if we do not meet the loophead again
          if (sumEdgeSuccessor != loopHead) {
            nodesToHandle.offer(sumEdgeSuccessor);
          }

          sb.append("    TRUE -> ");
          handleGotoNode(sb, curNode, true);

          handleUseFirstNode(sb, curNode, true);

          sb.append("    ( CHECK(location, \"functionname==")
            .append(sumEdge.getPredecessor().getFunctionName())
            .append("\")) -> ");

          handlePossibleOutOfLoopSuccessor(sb, intoLoopState, loopHead, sumEdgeSuccessor);

          sb.append("    TRUE -> ");
          handleGotoNode(sb, curNode, true);

        // all other edges can be handled together
        } else {
          boolean stillInLoop = false;
          for (Loop loop : loopsToUproll) {
            if (loop.getLoopNodes().contains(edgeSuccessor)) {
              stillInLoop = true;
              break;
            }
          }

          handleMatchCase(sb, edge);

          // we are still in the loop, so we do not need to handle special cases
          if (stillInLoop && edgeSuccessor != loopHead) {
            handleGotoNode(sb, edgeSuccessor, false);

            nodesToHandle.offer(edgeSuccessor);

            // we are in the loop but reaching the head again
          } else if (stillInLoop) {
            handleGotoArg(sb, intoLoopState);

          // out of loop edge, check if it is the same edge as in the ARGPath
          // if not we need a sink with STOP
          } else if (outOfLoopState == null
                     || !AbstractStates.extractLocation(outOfLoopState).equals(edgeSuccessor)) {
            sb.append("STOP;\n");

          // here we go out of the loop back to the arg path
          } else {
            handleGotoArg(sb, outOfLoopState);
          }
        }
      }
      sb.append("    TRUE -> STOP;\n\n");
    }
  }

  private static void handleMatchCase(Appendable sb, CFAEdge edge) throws IOException {
    sb.append("    MATCH \"");
    escape(edge.getRawStatement(), sb);
    sb.append("\" -> ");
  }

  private static void handleUseFirstNode(Appendable sb, CFANode node, boolean isFunctionSink) throws IOException {
    sb.append("STATE USEFIRST NODE")
      .append(Integer.toString(node.getNodeNumber()));

    if (isFunctionSink) {
      sb.append("_FUNCTIONSINK");
    }

    sb.append(" :\n");
  }

  private static void handleGotoArg(Appendable sb, ARGState state) throws IOException {
    sb.append("GOTO ARG")
      .append(Integer.toString(state.getStateId()))
      .append(";\n");
  }

  private static void handleGotoNode(Appendable sb, CFANode node, boolean isFunctionSink) throws IOException {
    sb.append("GOTO NODE")
      .append(Integer.toString(node.getNodeNumber()));

    if (isFunctionSink) {
      sb.append("_FUNCTIONSINK");
    }

    sb.append(";\n");
  }

  private static void handlePossibleOutOfLoopSuccessor(Appendable sb, ARGState intoLoopState,
                                                       CFANode loopHead, CFANode successor) throws IOException {

    // depending on successor add the transition for going out of the loop
    if (successor == loopHead) {
      handleGotoArg(sb, intoLoopState);
    } else {
      handleGotoNode(sb, successor, false);
    }
  }

  private static void addAssumption(
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      ARGState pState,
      CFAEdge pEdge,
      Appendable sb)
      throws IOException {

    Iterable<CFAEdgeWithAssumptions> assumptions = pValueMap.get(pState);
    assumptions = Iterables.filter(assumptions, a -> a.getCFAEdge().equals(pEdge));
    if (Iterables.isEmpty(assumptions)) {
      return;
    }
    addAssumption(Iterables.getOnlyElement(assumptions), sb);
  }

  private static void addAssumption(CFAEdgeWithAssumptions pCFAEdgeWithAssignments, Appendable sb)
      throws IOException {

    if (pCFAEdgeWithAssignments != null) {
      String code = pCFAEdgeWithAssignments.getAsCode();

      if (!code.isEmpty()) {
        sb.append("ASSUME {" + code + "} ");
      }
    }
  }

  private static void escape(String s, Appendable appendTo) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '\n':
        appendTo.append("\\n");
        break;
      case '\r':
        appendTo.append("\\r");
        break;
      case '\"':
        appendTo.append("\\\"");
        break;
      case '\\':
        appendTo.append("\\\\");
        break;
      default:
        appendTo.append(c);
        break;
      }
    }
  }

  /**
   * Attempts to get the counterexample registered with the given target state, or, if no
   * counterexample has been registered with the target state, attempts to create a counterexample.
   *
   * <p>Currently, there are multiple issues with this function:
   *
   * <ol>
   *   <li>This function may fail to create a counterexample due to a conceptual issue with BAM.
   *       Therefore, the return value of this function is wrapped in an {@link Optional}. When this
   *       issue is fixed, the prefix "try" should be removed from the function name and the return
   *       type should be changed to {@link CounterexampleInfo}.
   *   <li>If no counterexample is registered for the state yet, this function uses a heuristic for
   *       determining whether or not the counterexample should be marked as imprecise. Currently,
   *       this heuristic will simply always mark a counterexample as feasible if and only if the
   *       analysis used consists of either a ValueAnalysisCPA or a SMGCPA.
   * </ol>
   *
   * @param pTargetState the target state to get the counterexample for.
   * @param pCPA the analysis that was used to explore the state space.
   * @param pAssumptionToEdgeAllocator if no counterexample was registered for the state yet, this
   *     component will be used to mapping assumptions over variables extracted from the states on
   *     the target path to the edges of the target path of the counterexample.
   * @return the counterexample registered with the given target state, or, if no counterexample has
   *     been registered with the target state, a counterexample created heuristically, or {@link
   *     Optional#empty()}.
   */
  public static Optional<CounterexampleInfo> tryGetOrCreateCounterexampleInformation(
      ARGState pTargetState,
      ConfigurableProgramAnalysis pCPA,
      AssumptionToEdgeAllocator pAssumptionToEdgeAllocator) {
    Optional<CounterexampleInfo> cex = pTargetState.getCounterexampleInformation();
    Objects.requireNonNull(pCPA);
    Objects.requireNonNull(pAssumptionToEdgeAllocator);
    if (cex.isPresent()) {
      return cex;
    }
    ARGPath path = ARGUtils.getOnePathTo(pTargetState);
    if (path.getFullPath().isEmpty()) {
      // path is invalid,
      // this might be a partial path in BAM, from an intermediate TargetState to root of its ReachedSet.
      // TODO this check does not avoid dummy-paths in BAM, that might exist in main-reachedSet.
      return Optional.empty();
    }

    CFAPathWithAssumptions assignments =
        CFAPathWithAssumptions.of(path, pCPA, pAssumptionToEdgeAllocator);
    // we use the imprecise version of the CounterexampleInfo, due to the possible
    // merges which are done in the used CPAs, but if we can compute a path with assignments,
    // it is probably precise
    if (!assignments.isEmpty()) {
      return Optional.of(CounterexampleInfo.feasiblePrecise(path, assignments));
    }
    return Optional.of(CounterexampleInfo.feasibleImprecise(path));
  }
}
