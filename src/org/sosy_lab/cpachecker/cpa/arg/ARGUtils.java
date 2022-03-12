// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.Traverser;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.path.PathPosition;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.GraphUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/** Helper class with collection of ARG related utility methods. */
/** */
public class ARGUtils {

  private ARGUtils() {}

  /**
   * Get all elements on all paths from the ARG root to a given element.
   *
   * @param pLastElement The last element in the paths.
   * @return A set of elements, all of which have pLastElement as their (transitive) child.
   */
  public static ImmutableSet<ARGState> getAllStatesOnPathsTo(ARGState pLastElement) {
    return ImmutableSet.copyOf(
        Traverser.forGraph(ARGState::getParents).depthFirstPreOrder(pLastElement));
  }

  /** Get all abstract states without parents. */
  public static ImmutableSet<ARGState> getRootStates(UnmodifiableReachedSet pReached) {

    ImmutableSet.Builder<ARGState> result = ImmutableSet.builder();

    for (AbstractState e : pReached) {
      ARGState state = AbstractStates.extractStateByType(e, ARGState.class);
      if (state != null && state.getParents().isEmpty()) {
        result.add(state);
      }
    }

    return result.build();
  }

  /**
   * Explores the paths through the ARG starting at the given root state and considering only the
   * given relevant states, and checks if there is any branching that can not be mapped uniquely to
   * a branching in the CFA.
   *
   * @param pRootState the root state to start exploration from.
   * @param pRelevantStates the states to consider for exploration.
   * @return {@code true} if there is ambiguous branching, {@code false} otherwise.
   */
  public static boolean hasAmbiguousBranching(ARGState pRootState, Set<ARGState> pRelevantStates) {
    Objects.requireNonNull(pRootState);
    if (!pRelevantStates.contains(pRootState)) {
      return false;
    }
    Set<ARGState> visited = new HashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.push(pRootState);
    visited.add(pRootState);
    while (!waitlist.isEmpty()) {
      ARGState current = waitlist.pop();
      List<ARGState> children =
          from(current.getChildren()).filter(Predicates.in(pRelevantStates)).toList();
      if (children.size() > 2) {
        return true;
      } else if (children.size() == 2) {
        ARGState firstChild = children.get(0);
        ARGState secondChild = children.get(1);
        List<CFAEdge> edgesToFirstChild = current.getEdgesToChild(firstChild);
        if (edgesToFirstChild.size() > 1) {
          return true;
        }
        CFAEdge edgeToFirstChild = edgesToFirstChild.iterator().next();
        if (!(edgeToFirstChild instanceof AssumeEdge)) {
          return true;
        }
        List<CFAEdge> edgesToSecondChild = current.getEdgesToChild(secondChild);
        if (edgesToSecondChild.size() > 1) {
          return true;
        }
        CFAEdge edgeToSecondChild = edgesToSecondChild.iterator().next();
        if (!(edgeToSecondChild instanceof AssumeEdge)) {
          return true;
        }
        if (!CFAUtils.getComplimentaryAssumeEdge((AssumeEdge) edgeToFirstChild)
            .equals(edgeToSecondChild)) {
          return true;
        }
      }
      for (ARGState child : children) {
        if (visited.add(child)) {
          waitlist.push(child);
        }
      }
    }
    return false;
  }

  /**
   * Create a path in the ARG from root to the given element. If there are several such paths, one
   * is chosen arbitrarily.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static ARGPath getOnePathTo(ARGState pLastElement) {
    return getOnePathFromTo((x) -> x.getParents().isEmpty(), pLastElement);
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
            otherPath -> {
              PathPosition result = otherPath.reversePathIterator().getPosition();
              CFANode expectedPostfixLoc = AbstractStates.extractLocation(pEndState);
              Verify.verify(result.getLocation().equals(expectedPostfixLoc));
              return result;
            });

    // Get all traces from pTryCoverOtherStatesThan that start at the same location
    tracePrefixesToAvoid = getTracePrefixesBeforePostfix(tracePrefixesToAvoid, currentLocation);

    boolean lastTransitionIsDifferent = false;
    while (!currentARGState.getParents().isEmpty()) {
      List<ARGState> potentialParents = new ArrayList<>(currentARGState.getParents());

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
        for (PathPosition t : tracePrefixesToAvoid) {
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

  public static ARGPath getOnePathFromTo(final Predicate<ARGState> pIsStart, final ARGState pEnd) {
    List<ARGState> states = new ArrayList<>(); // reversed order
    Set<ARGState> seenElements = new HashSet<>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pEnd;
    states.add(currentARGState);
    seenElements.add(currentARGState);
    Deque<ARGState> backTrackPoints = new ArrayDeque<>();
    Deque<List<ARGState>> backTrackOptions = new ArrayDeque<>();

    while (!pIsStart.apply(currentARGState)) {
      Iterator<ARGState> parents = currentARGState.getParents().iterator();

      ARGState parentElement = parents.next();
      while (seenElements.contains(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      if (seenElements.contains(parentElement)) {
        // Backtrack
        checkArgument(
            !backTrackPoints.isEmpty(), "No ARG path from the target state to a root state.");
        ARGState backTrackPoint = backTrackPoints.pop();
        ListIterator<ARGState> stateIterator = states.listIterator(states.size());
        while (stateIterator.hasPrevious() && !stateIterator.previous().equals(backTrackPoint)) {
          stateIterator.remove();
        }
        List<ARGState> options = backTrackOptions.pop();
        for (ARGState parent : backTrackPoint.getParents()) {
          if (!options.contains(parent)) {
            seenElements.add(parent);
          }
        }
        currentARGState = backTrackPoint;
      } else {
        // Record backtracking options
        if (parents.hasNext()) {
          List<ARGState> options = new ArrayList<>(1);
          while (parents.hasNext()) {
            ARGState parent = parents.next();
            if (!seenElements.contains(parent)) {
              options.add(parent);
            }
          }
          if (!options.isEmpty()) {
            backTrackPoints.push(currentARGState);
            backTrackOptions.push(options);
          }
        }

        seenElements.add(parentElement);
        states.add(parentElement);

        currentARGState = parentElement;
      }
    }
    return new ARGPath(Lists.reverse(states));
  }

  /**
   * Create the shortest path in the ARG from root to the given element. If there are several such
   * paths, one is chosen arbitrarily. This method is suited for analysis where {@link
   * ARGUtils#getOnePathTo(ARGState)} is not fast enough due to the structure of the ARG.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static ARGPath getShortestPathTo(final ARGState pLastElement) {
    Map<ARGState, ARGState> searchTree = new HashMap<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    searchTree.put(pLastElement, null);
    waitlist.add(pLastElement);
    ARGState firstElement = null;
    while (!waitlist.isEmpty()) {
      ARGState currentState = waitlist.pop();
      for (ARGState parent : currentState.getParents()) {
        if (parent.getParents().isEmpty()) {
          firstElement = parent;
          searchTree.put(parent, currentState);
          break;
        }
        if (!searchTree.containsKey(parent)) {
          waitlist.add(parent);
          searchTree.put(parent, currentState);
        }
      }
      if (firstElement != null) {
        break;
      }
    }
    assert firstElement != null : "ARG seems to have no initial state (state without parents)!";
    ImmutableList.Builder<ARGState> path = ImmutableList.builder();
    while (firstElement != null) {
      path.add(firstElement);
      firstElement = searchTree.get(firstElement);
    }
    return new ARGPath(path.build());
  }

  public static Collection<PathPosition> getTracePrefixesBeforePostfix(
      final Collection<PathPosition> pTracePosition, final CFANode pPostfixLocation) {

    Preconditions.checkNotNull(pTracePosition);
    Preconditions.checkNotNull(pPostfixLocation);

    ImmutableList.Builder<PathPosition> result = ImmutableList.builder();

    for (PathPosition p : pTracePosition) {

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
   *
   * @param root The root state of an ARG (may not have any parents)
   */
  public static ARGPath getRandomPath(final ARGState root) {
    checkArgument(root.getParents().isEmpty());

    List<ARGState> states = new ArrayList<>();
    ARGState currentElement = root;
    while (!currentElement.getChildren().isEmpty()) {
      states.add(currentElement);
      currentElement = currentElement.getChildren().iterator().next();
    }
    states.add(currentElement);
    return new ARGPath(states);
  }

  private static boolean isRelevantLocation(CFANode pInput) {
    return pInput.isLoopStart()
        || pInput instanceof FunctionEntryNode
        || pInput instanceof FunctionExitNode;
  }

  public static boolean isRelevantState(ARGState state) {
    return AbstractStates.isTargetState(state)
        || Iterables.any(AbstractStates.extractLocations(state), ARGUtils::isRelevantLocation)
        || !state.wasExpanded()
        || state.shouldBeHighlighted();
  }

  /**
   * Project the ARG to a subset of "relevant" states. The result is a SetMultimap containing the
   * successor relationships between all relevant states. A pair of states (a, b) is in the
   * SetMultimap, if there is a path through the ARG from a to b which does not pass through any
   * other relevant state.
   *
   * <p>To get the predecessor relationship, you can use {@link
   * Multimaps#invertFrom(com.google.common.collect.Multimap, com.google.common.collect.Multimap)}.
   *
   * @param root The start of the subgraph of the ARG to project (always considered relevant).
   * @param isRelevant The predicate determining which states are in the resulting relationship.
   */
  public static SetMultimap<ARGState, ARGState> projectARG(
      final ARGState root,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      Predicate<? super ARGState> isRelevant) {

    return GraphUtils.projectARG(root, successorFunction, isRelevant);
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
   * @return A path through the ARG from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG
   *     is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root, Set<? extends AbstractState> arg, Map<Integer, Boolean> branchingInformation)
      throws IllegalArgumentException {
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
      final ImmutableSet<ARGState> childrenInArg =
          from(currentElement.getChildren()).filter(arg::contains).toSet();

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

          Iterable<CFANode> locs = AbstractStates.extractLocations(currentElement);
          if (Iterables.any(
              locs, loc -> !leavingEdges(loc).allMatch(Predicates.instanceOf(AssumeEdge.class)))) {
            throw new IllegalArgumentException("ARG branches where there is no AssumeEdge!");
          }

          for (ARGState currentChild : childrenInArg) {
            CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
            if (((AssumeEdge) currentEdge).getTruthAssumption()) {
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

      checkArgument(arg.contains(child), "ARG and direction information from solver disagree!");

      builder.add(currentElement, edge);
      currentElement = child;
    }

    return builder.build(currentElement);
  }

  /**
   * Find a path in the ARG. The necessary information to find the path is a boolean value for each
   * branching situation that indicates which of the two AssumeEdges should be taken. This method
   * checks that the path ends in a certain element.
   *
   * @param root The root element of the ARG (where to start the path)
   * @param target The target state (where to end the path, needs to be a target state)
   * @param arg All elements in the ARG or a subset thereof (elements outside this set will be
   *     ignored).
   * @param branchingInformation A map from ARG state ids to boolean values indicating the outgoing
   *     direction.
   * @return A path through the ARG from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG
   *     is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root,
      ARGState target,
      Set<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation)
      throws IllegalArgumentException {

    checkArgument(arg.contains(target));
    checkArgument(target.isTarget());

    ARGPath result = getPathFromBranchingInformation(root, arg, branchingInformation);

    checkArgument(
        result.getLastState().equals(target), "ARG target path reached the wrong target state!");

    return result;
  }

  /**
   * This method gets all children from an ARGState, but replaces all covered states by their
   * respective covering state. It can be seen as giving a view of the ARG where the covered states
   * are transparently replaced by their covering state.
   *
   * <p>The returned collection is unmodifiable and a live view of the children of the given state.
   *
   * @param s an ARGState
   * @return The children with covered states transparently replaced.
   */
  public static Collection<ARGState> getUncoveredChildrenView(final ARGState s) {
    return new AbstractCollection<>() {

      @Override
      public Iterator<ARGState> iterator() {

        return new UnmodifiableIterator<>() {
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
   * <p>Checks we do here currently: - child-parent relationship of ARG states - states in ARG are
   * also in reached set and vice versa (as far as possible to check) - no destroyed states present
   *
   * <p>This method is potentially expensive, and should be called only from an assert statement.
   *
   * @return <code>true</code>
   * @throws AssertionError If any consistency check is violated.
   */
  public static boolean checkARG(ReachedSet pReached) {
    // Not all states in ARG might be reachable from a single root state
    // in case of multiple initial states and disjoint ARGs.

    for (ARGState e : from(pReached).transform(toState(ARGState.class))) {
      assert e != null : "Reached set contains abstract state without ARGState.";
      assert !e.isDestroyed()
          : "Reached set contains destroyed ARGState, which should have been removed.";

      for (ARGState parent : e.getParents()) {
        assert parent.getChildren().contains(e)
            : "Reference from parent to child is missing in ARG";
        assert pReached.contains(parent) : "Referenced parent is missing in reached";
      }

      for (ARGState child : e.getChildren()) {
        assert child.getParents().contains(e) : "Reference from child to parent is missing in ARG";

        // Usually, all children should be in reached set, with two exceptions.
        // 1) Covered states need not be in the reached set (this depends on
        // cpa.arg.keepCoveredStatesInReached),
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

    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = ImmutableListMultimap.of();

    if (pCounterExample != null && pCounterExample.isPreciseCounterExample()) {
      valueMap = pCounterExample.getExactVariableValues();
    }

    int index = 0;

    Function<ARGState, String> getLocationName =
        s -> Joiner.on("_OR_").join(AbstractStates.extractLocations(s));
    Function<Integer, Function<ARGState, String>> getStateNameFunction =
        i -> s -> "S" + i + "at" + getLocationName.apply(s);

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
   * Produce an automaton in the format for the AutomatonCPA from a given path. The automaton
   * matches exactly the edges along the path. If there is a target state, it is signaled as an
   * error state in the automaton.
   *
   * @param sb Where to write the automaton to
   * @param pRootState The root of the ARG
   * @param pPathStates The states along the path
   * @param pCounterExample Given to try to write exact variable assignment values into the
   *     automaton, may be null
   */
  public static void producePathAutomaton(
      Appendable sb,
      ARGState pRootState,
      Set<ARGState> pPathStates,
      String name,
      @Nullable CounterexampleInfo pCounterExample)
      throws IOException {

    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = ImmutableListMultimap.of();

    if (pCounterExample != null && pCounterExample.isPreciseCounterExample()) {
      valueMap = pCounterExample.getExactVariableValues();
    }

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    sb.append("INITIAL STATE ARG" + pRootState.getStateId() + ";\n\n");

    int multiEdgeCount = 0; // see below

    for (ARGState s : ImmutableList.sortedCopyOf(pPathStates)) {

      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");

      for (ARGState child : s.getChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        if (pPathStates.contains(child)) {
          List<CFAEdge> allEdges = s.getEdgesToChild(child);
          CFAEdge edge;

          if (allEdges.isEmpty()) {
            // this is a missing edge, e.g., caused by SSCCPA
            edge = new DummyCFAEdge(extractLocation(s), extractLocation(child));

          } else if (allEdges.size() == 1) {
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
            sb.append("GOTO ARG" + child.getStateId() + "_" + (i + 1) + "_" + multiEdgeCount);
            sb.append(";\n");

            // inner part (without first and last edge)
            for (; i < allEdges.size() - 1; i++) {
              sb.append(
                  "STATE USEFIRST ARG"
                      + child.getStateId()
                      + "_"
                      + i
                      + "_"
                      + multiEdgeCount
                      + " :\n");
              sb.append("    MATCH \"");
              escape(allEdges.get(i).getRawStatement(), sb);
              sb.append("\" -> ");
              sb.append("GOTO ARG" + child.getStateId() + "_" + (i + 1) + "_" + multiEdgeCount);
              sb.append(";\n");
            }

            // last edge connecting it with the real successor
            edge = allEdges.get(i);
            sb.append(
                "STATE USEFIRST ARG"
                    + child.getStateId()
                    + "_"
                    + i
                    + "_"
                    + multiEdgeCount
                    + " :\n");
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
   * Produce an automaton in the format for the AutomatonCPA from a given path. The automaton
   * matches the edges along the path until a state is at location which is also included in a loop.
   * Then this loop is recreated. Outgoing edges of this loop are then handled once again as they
   * occur in the path. So for all outgoing edges of a loop which do not occur in the given path we
   * create a sink (TRUE) and for the outgoing edge which is on the path we continue with unrolling
   * the ARGPath from this point. If there is a target state, it is signaled as an error state in
   * the automaton.
   *
   * <p>This method does not work if the path has recursive elements.
   *
   * @param sb Where to write the automaton to
   * @param pRootState The root of the ARG
   * @param pPathStates The states along the path
   * @param name the name the automaton should have
   * @param loopsToUproll the loops which should be recreated in the automaton
   */
  public static void producePathAutomatonWithLoops(
      Appendable sb,
      ARGState pRootState,
      Set<ARGState> pPathStates,
      String name,
      Set<Loop> loopsToUproll)
      throws IOException {

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

    ImmutableList<ARGState> sortedStates = ImmutableList.sortedCopyOf(pPathStates);

    Deque<String> sortedFunctionOccurrence = new ArrayDeque<>();
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
                sb.append("GOTO ARG" + child.getStateId() + "_" + (i + 1) + "_" + multiEdgeCount);
                sb.append(";\n");

                // inner part (without first and last edge)
                for (; i < allEdges.size() - 1; i++) {
                  sb.append(
                      "STATE USEFIRST ARG"
                          + child.getStateId()
                          + "_"
                          + i
                          + "_"
                          + multiEdgeCount
                          + " :\n");
                  sb.append("    MATCH \"");
                  escape(allEdges.get(i).getRawStatement(), sb);
                  sb.append("\" -> ");
                  sb.append("GOTO ARG" + child.getStateId() + "_" + (i + 1) + "_" + multiEdgeCount);
                  sb.append(";\n");
                }

                // last edge connecting it with the real successor
                edge = allEdges.get(i);
                sb.append(
                    "STATE USEFIRST ARG"
                        + child.getStateId()
                        + "_"
                        + i
                        + "_"
                        + multiEdgeCount
                        + " :\n");
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

  private static void handleLoop(
      Appendable sb, Set<Loop> loopsToUproll, ARGState intoLoopState, ARGState outOfLoopState)
      throws IOException {

    Set<CFANode> handledNodes = new HashSet<>();
    Deque<CFANode> nodesToHandle = new ArrayDeque<>();
    CFANode loopHead = AbstractStates.extractLocation(intoLoopState);
    nodesToHandle.offer(loopHead);
    boolean isFirstLoopIteration = true;
    while (!nodesToHandle.isEmpty()) {
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

      for (CFAEdge edge : leavingEdges(curNode)) {
        CFANode edgeSuccessor = edge.getSuccessor();

        // skip function calls
        if (edge instanceof FunctionCallEdge) {
          FunctionSummaryEdge sumEdge = ((FunctionCallEdge) edge).getSummaryEdge();
          CFANode sumEdgeSuccessor = sumEdge.getSuccessor();

          // only continue if we do not meet the loophead again
          if (!sumEdgeSuccessor.equals(loopHead)) {
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
          if (stillInLoop && !edgeSuccessor.equals(loopHead)) {
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

  private static void handleUseFirstNode(Appendable sb, CFANode node, boolean isFunctionSink)
      throws IOException {
    sb.append("STATE USEFIRST NODE").append(Integer.toString(node.getNodeNumber()));

    if (isFunctionSink) {
      sb.append("_FUNCTIONSINK");
    }

    sb.append(" :\n");
  }

  private static void handleGotoArg(Appendable sb, ARGState state) throws IOException {
    sb.append("GOTO ARG").append(Integer.toString(state.getStateId())).append(";\n");
  }

  private static void handleGotoNode(Appendable sb, CFANode node, boolean isFunctionSink)
      throws IOException {
    sb.append("GOTO NODE").append(Integer.toString(node.getNodeNumber()));

    if (isFunctionSink) {
      sb.append("_FUNCTIONSINK");
    }

    sb.append(";\n");
  }

  private static void handlePossibleOutOfLoopSuccessor(
      Appendable sb, ARGState intoLoopState, CFANode loopHead, CFANode successor)
      throws IOException {

    // depending on successor add the transition for going out of the loop
    if (successor.equals(loopHead)) {
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
      // this might be a partial path in BAM, from an intermediate TargetState to root of its
      // ReachedSet.
      // TODO this check does not avoid dummy-paths in BAM, that might exist in main-reachedSet.
      return Optional.empty();
    }

    CFAPathWithAdditionalInfo additionalInfo = CFAPathWithAdditionalInfo.of(path, pCPA);

    // We should not claim that the counterexample is precise unless we have one unique path
    Set<ARGState> states = path.getStateSet();
    if (states.stream().allMatch(s -> states.containsAll(s.getParents()))) {
      CFAPathWithAssumptions assignments =
          CFAPathWithAssumptions.of(path, pCPA, pAssumptionToEdgeAllocator);
      if (!assignments.isEmpty()) {
        return Optional.of(CounterexampleInfo.feasiblePrecise(path, assignments, additionalInfo));
      }
    }
    return Optional.of(CounterexampleInfo.feasibleImprecise(path, additionalInfo));
  }

  public static FluentIterable<ARGState> getNonCoveredStatesInSubgraph(ARGState pRoot) {
    return pRoot.getSubgraph().filter(s -> !s.isCovered());
  }

  /** Returns all possible paths from the given state to the root of the ARG. */
  public static Set<ARGPath> getAllPaths(final ReachedSet pReachedSet, final ARGState pStart) {
    ARGState root = AbstractStates.extractStateByType(pReachedSet.getFirstState(), ARGState.class);
    List<ARGState> states = new ArrayList<>();
    ImmutableSet.Builder<ARGPath> results = ImmutableSet.builder();
    List<List<ARGState>> paths = new ArrayList<>();

    states.add(pStart);
    paths.add(states);

    // This is assuming from each node there is a way to go to the start
    // Loop until all paths reached the root
    while (!paths.isEmpty()) {
      // Expand currently considered path
      List<ARGState> curPath = paths.remove(paths.size() - 1);
      Preconditions.checkNotNull(curPath);
      // If there is no more to expand - add this path and continue
      if (curPath.get(curPath.size() - 1) == root) {
        results.add(new ARGPath(Lists.reverse(curPath)));

        continue;
      }

      // Add all parents of currently first state on the current path
      for (ARGState parentElement : curPath.get(curPath.size() - 1).getParents()) {
        ImmutableList.Builder<ARGState> tmp =
            ImmutableList.builderWithExpectedSize(curPath.size() + 1);
        tmp.addAll(curPath);

        tmp.add(parentElement);
        paths.add(tmp.build());
      }
    }
    return results.build();
  }
}
