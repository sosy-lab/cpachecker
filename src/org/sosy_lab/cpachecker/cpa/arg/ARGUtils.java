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

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.concrete_counterexample.CFAEdgeWithAssignments;
import org.sosy_lab.cpachecker.core.concrete_counterexample.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.GraphUtils;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;

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
   * Create a path in the ARG from root to the given element.
   * If there are several such paths, one is chosen randomly.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static ARGPath getOnePathTo(ARGState pLastElement) {
    ARGPath path = new ARGPath();
    Set<ARGState> seenElements = new HashSet<>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pLastElement;
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFANode loc = extractLocation(currentARGState);
    CFAEdge lastEdge = leavingEdges(loc).first().orNull();
    path.addFirst(Pair.of(currentARGState, lastEdge));
    seenElements.add(currentARGState);

    while (!currentARGState.getParents().isEmpty()) {
      Iterator<ARGState> parents = currentARGState.getParents().iterator();

      ARGState parentElement = parents.next();
      while (!seenElements.add(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      CFAEdge edge = parentElement.getEdgeToChild(currentARGState);
      path.addFirst(Pair.of(parentElement, edge));

      currentARGState = parentElement;
    }
    return path;
  }


  public static final Function<ARGState, Collection<ARGState>> CHILDREN_OF_STATE = new Function<ARGState, Collection<ARGState>>() {
        @Override
        public Collection<ARGState> apply(ARGState pInput) {
          return pInput.getChildren();
        }
      };

  public static final Function<ARGState, Collection<ARGState>> PARENTS_OF_STATE = new Function<ARGState, Collection<ARGState>>() {
        @Override
        public Collection<ARGState> apply(ARGState pInput) {
          return pInput.getParents();
        }
      };

  public static final Predicate<AbstractState> AT_RELEVANT_LOCATION = Predicates.compose(
      new Predicate<CFANode>() {
        @Override
        public boolean apply(CFANode pInput) {
          return pInput.isLoopStart()
              || pInput instanceof FunctionEntryNode
              || pInput instanceof FunctionExitNode;
        }
      },
      AbstractStates.EXTRACT_LOCATION);

  static final Predicate<AbstractState> IMPORTANT_FOR_ANALYSIS = Predicates.compose(
      notNullAnd(PredicateAbstractState.FILTER_ABSTRACTION_STATES),
      AbstractStates.toState(PredicateAbstractState.class));

  private static <T> Predicate<T> notNullAnd(final Predicate<T> p) {
    return new Predicate<T>() {
        @Override
        public boolean apply(T pInput) {
          if (pInput == null) {
            return false;
          }
          return p.apply(pInput);
        }
      };
  }

  @SuppressWarnings("unchecked")
  public static final Predicate<ARGState> RELEVANT_STATE = Predicates.or(
      AbstractStates.IS_TARGET_STATE,
      AT_RELEVANT_LOCATION,
      new Predicate<ARGState>() {
          @Override
          public boolean apply(ARGState pInput) {
            return !pInput.wasExpanded();
          }
        },
      IMPORTANT_FOR_ANALYSIS
      );

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
   * Writes the ARG with the root state pRootState to pSb as a graphviz dot file
   *
   */
  public static void writeARGAsDot(Appendable pSb, ARGState pRootState) throws IOException {
    ARGToDotWriter.write(pSb, pRootState,
        ARGUtils.CHILDREN_OF_STATE,
        Predicates.alwaysTrue(),
        Predicates.alwaysFalse());
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

    checkArgument(arg.contains(root));

    ARGPath result = new ARGPath();
    ARGState currentElement = root;
    while (!currentElement.isTarget()) {
      Collection<ARGState> children = currentElement.getChildren();

      ARGState child;
      CFAEdge edge;
      switch (children.size()) {

      case 0:
        throw new IllegalArgumentException("ARG target path terminates without reaching target state!");

      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(children);
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
          Set<ARGState> candidates = Sets.intersection(Sets.newHashSet(children), arg).immutableCopy();
          if (candidates.size() != 1) {
            throw new IllegalArgumentException("ARG branches where there is no AssumeEdge!");
          }
          child = Iterables.getOnlyElement(candidates);
          edge = currentElement.getEdgeToChild(child);
          break;
        }

        for (ARGState currentChild : children) {
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
        Set<ARGState> candidates = Sets.intersection(Sets.newHashSet(children), arg).immutableCopy();
        if (candidates.size() != 1) {
          throw new IllegalArgumentException("ARG splits with more than two branches!");
        }
        child = Iterables.getOnlyElement(candidates);
        edge = currentElement.getEdgeToChild(child);
        break;
      }

      if (!arg.contains(child)) {
        throw new IllegalArgumentException("ARG and direction information from solver disagree!");
      }

      result.add(Pair.of(currentElement, edge));
      currentElement = child;
    }


    // need to add another pair with target state and one (arbitrary) outgoing edge
    CFANode loc = extractLocation(currentElement);
    CFAEdge lastEdge = leavingEdges(loc).first().orNull();
    result.add(Pair.of(currentElement, lastEdge));

    return result;
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

    if (result.getLast().getFirst() != target) {
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
  public static final Collection<ARGState> getUncoveredChildrenView(final ARGState s) {
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

  public static boolean checkARG(ReachedSet pReached) {

      Deque<AbstractState> workList = new ArrayDeque<>();
      Set<ARGState> arg = new HashSet<>();

      workList.add(pReached.getFirstState());
      while (!workList.isEmpty()) {
        ARGState currentElement = (ARGState)workList.removeFirst();
        assert !currentElement.isDestroyed();

        for (ARGState parent : currentElement.getParents()) {
          assert parent.getChildren().contains(currentElement) : "Reference from parent to child is missing in ARG";
        }
        for (ARGState child : currentElement.getChildren()) {
          assert child.getParents().contains(currentElement) : "Reference from child to parent is missing in ARG";
        }

        // check if (e \in ARG) => (e \in Reached || e.isCovered())
        if (currentElement.isCovered()) {
          // Assertion removed because now covered states are allowed to be in the reached set.
          // But they don't need to be!
  //        assert !pReached.contains(currentElement) : "Reached set contains covered element";

        } else {
          // There is a special case here:
          // If the element is the sibling of the target state, it might have not
          // been added to the reached set if CPAAlgorithm stopped before.
          // But in this case its parent is in the waitlist.

          assert pReached.contains(currentElement)
              || pReached.getWaitlist().containsAll(currentElement.getParents())
              : "Element in ARG but not in reached set";
        }

        if (arg.add(currentElement)) {
          workList.addAll(currentElement.getChildren());
        }
      }

      // check if (e \in Reached) => (e \in ARG)
      assert arg.containsAll(pReached.asCollection()) : "Element in reached set but not in ARG";

      return true;
    }


  public static void produceTestGenPathAutomaton(Appendable sb, String name, CounterexampleTraceInfo pCounterExampleTrace)
      throws IOException {

    Model model = pCounterExampleTrace.getModel();
    CFAPathWithAssignments assignmentCFAPath = model.getAssignedTermsPerEdge();

    int stateCounter = 1;

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    sb.append("INITIAL STATE STATE" + stateCounter + ";\n\n");

    for (Iterator<CFAEdgeWithAssignments> it = assignmentCFAPath.iterator(); it.hasNext();) {
      CFAEdgeWithAssignments edge = it.next();

      sb.append("STATE USEFIRST STATE" + stateCounter + " :\n");

      sb.append("    MATCH \"");
      escape(edge.getCFAEdge().getRawStatement(), sb);
      sb.append("\" -> ");

      if (it.hasNext()) {
        String code = edge.getAsCode();
        String assumption = code == null ? "" : "ASSUME {" + code + "}";
        sb.append(assumption + "GOTO STATE" + ++stateCounter);
      } else {
        sb.append("GOTO EndLoop");
      }

      sb.append(";\n");
      sb.append("    TRUE -> STOP;\n\n");

    }

    //sb.append("    TRUE -> STOP;\n\n");
    sb.append("STATE USEFIRST EndLoop" + " :\n");
    sb.append("    MATCH EXIT -> BREAK;\n");
    sb.append("    TRUE -> GOTO EndLoop;\n\n");

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
   * @throws IOException
   */
  public static void produceTestGenPathAutomaton(Appendable sb, ARGState pRootState,
      Set<ARGState> pPathStates, String name, CounterexampleInfo pCounterExample, boolean generateAssumes) throws IOException {

    Map<ARGState, CFAEdgeWithAssignments> valueMap = null;

    if (pCounterExample != null) {
      Model model = pCounterExample.getTargetPathModel();
      if (model != null) {
        CFAPathWithAssignments cfaPath = model.getAssignedTermsPerEdge();
        if (cfaPath != null) {
          ARGPath targetPath = pCounterExample.getTargetPath();
          valueMap = model.getExactVariableValues(targetPath);
        }
      }
    }

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    sb.append("INITIAL STATE ARG" + pRootState.getStateId() + ";\n\n");

    int multiEdgeCount = 0; // see below
    Pair<ARGState,CFAEdge> lastElement = pCounterExample.getTargetPath().getLast();
    for (ARGState s : pPathStates) {

      CFANode loc = AbstractStates.extractLocation(s);
      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");
      for (ARGState child : s.getChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        if (pPathStates.contains(child)) {
          CFANode childLoc = AbstractStates.extractLocation(child);
          CFAEdge edge = loc.getEdgeTo(childLoc);
          if (edge instanceof MultiEdge) {
            // The successor state might have several incoming MultiEdges.
            // In this case the state names like ARG<successor>_0 would occur
            // several times.
            // So we add this counter to the state names to make them unique.
            multiEdgeCount++;

            // Write out a long linear chain of pseudo-states
            // because the AutomatonCPA also iterates through the MultiEdge.
            List<CFAEdge> edges = ((MultiEdge)edge).getEdges();

            // first, write edge entering the list
            int i = 0;
            sb.append("    MATCH \"");
            escape(edges.get(i).getRawStatement(), sb);
            sb.append("\" -> ");
            sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
            sb.append(";\n");

            // inner part (without first and last edge)
            for (; i < edges.size()-1; i++) {
              sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
              sb.append("    MATCH \"");
              escape(edges.get(i).getRawStatement(), sb);
              sb.append("\" -> ");
              sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
              sb.append(";\n");
            }

            // last edge connecting it with the real successor
            edge = edges.get(i);
            sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
            // remainder is written by code below
          }

          sb.append("    MATCH \"");
          escape(edge.getRawStatement(), sb);
          sb.append("\" -> ");

          if (child.isTarget()) {
            sb.append("ERROR");
          } else {
            String assumption ="";
            if(generateAssumes)
            {
              assumption = getAssumption(valueMap, s);
            }
            sb.append(assumption + "GOTO ARG" + child.getStateId());
          }
          sb.append(";\n");
        }
      }
      if(!s.equals(lastElement.getFirst())) {
        sb.append("    TRUE -> STOP;\n\n");
      }
    }


    if(lastElement.getSecond() != null)
    {
      sb.append("    MATCH \"");
      escape(lastElement.getSecond().getRawStatement(), sb);
      sb.append("\" -> ");
      sb.append("GOTO EndLoop");
      sb.append(";\n");
      sb.append("    TRUE -> STOP;\n\n");
//        lastElement.getSecond().getRawStatement()
      sb.append("STATE USEFIRST EndLoop" + " :\n");
      sb.append("    MATCH EXIT -> BREAK;\n");
      sb.append("    TRUE -> GOTO EndLoop;\n\n");

    }
    else{
      sb.append("    TRUE -> STOP;\n\n");
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
   * @throws IOException
   */
  public static void producePathAutomaton(Appendable sb, ARGState pRootState,
      Set<ARGState> pPathStates, String name, CounterexampleInfo pCounterExample) throws IOException {

    Map<ARGState, CFAEdgeWithAssignments> valueMap = null;

    if (pCounterExample != null) {
      Model model = pCounterExample.getTargetPathModel();
      if (model != null) {
        CFAPathWithAssignments cfaPath = model.getAssignedTermsPerEdge();
        if (cfaPath != null) {
          ARGPath targetPath = pCounterExample.getTargetPath();
          valueMap = model.getExactVariableValues(targetPath);
        }
      }
    }

    sb.append("CONTROL AUTOMATON " + name + "\n\n");
    sb.append("INITIAL STATE ARG" + pRootState.getStateId() + ";\n\n");

    int multiEdgeCount = 0; // see below

    for (ARGState s : pPathStates) {

      CFANode loc = AbstractStates.extractLocation(s);
      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");

      for (ARGState child : s.getChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        if (pPathStates.contains(child)) {
          CFANode childLoc = AbstractStates.extractLocation(child);
          CFAEdge edge = loc.getEdgeTo(childLoc);
          if (edge instanceof MultiEdge) {
            // The successor state might have several incoming MultiEdges.
            // In this case the state names like ARG<successor>_0 would occur
            // several times.
            // So we add this counter to the state names to make them unique.
            multiEdgeCount++;

            // Write out a long linear chain of pseudo-states
            // because the AutomatonCPA also iterates through the MultiEdge.
            List<CFAEdge> edges = ((MultiEdge)edge).getEdges();

            // first, write edge entering the list
            int i = 0;
            sb.append("    MATCH \"");
            escape(edges.get(i).getRawStatement(), sb);
            sb.append("\" -> ");
            sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
            sb.append(";\n");

            // inner part (without first and last edge)
            for (; i < edges.size()-1; i++) {
              sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
              sb.append("    MATCH \"");
              escape(edges.get(i).getRawStatement(), sb);
              sb.append("\" -> ");
              sb.append("GOTO ARG" + child.getStateId() + "_" + (i+1) + "_" + multiEdgeCount);
              sb.append(";\n");
            }

            // last edge connecting it with the real successor
            edge = edges.get(i);
            sb.append("STATE USEFIRST ARG" + child.getStateId() + "_" + i + "_" + multiEdgeCount + " :\n");
            // remainder is written by code below
          }

          sb.append("    MATCH \"");
          escape(edge.getRawStatement(), sb);
          sb.append("\" -> ");

          if (child.isTarget()) {
            sb.append("ERROR");
          } else {
            String assumption = getAssumption(valueMap, s);
            sb.append(assumption + "GOTO ARG" + child.getStateId());
          }
          sb.append(";\n");
        }
      }
      sb.append("    TRUE -> STOP;\n\n");
    }
    sb.append("END AUTOMATON\n");
  }

  private static String getAssumption(Map<ARGState, CFAEdgeWithAssignments> pValueMap, ARGState pState) {

    String assumption = "";

    if (pValueMap != null && pValueMap.containsKey(pState)) {

      CFAEdgeWithAssignments cfaEdgeWithAssignments = pValueMap.get(pState);

      String code = cfaEdgeWithAssignments.getAsCode();

      if(code != null) {
        assumption = "ASSUME {" + code + "}";
      }
    }

    return assumption;
  }

  private static void escape(String s, Appendable appendTo) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '\n':
        appendTo.append("\\n");
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
}
