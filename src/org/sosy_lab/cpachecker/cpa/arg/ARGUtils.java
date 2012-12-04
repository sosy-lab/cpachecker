/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.Iterables;

/**
 * Helper class with collection of ARG related utility methods.
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

    Set<ARGState> result = new HashSet<ARGState>();
    Deque<ARGState> waitList = new ArrayDeque<ARGState>();

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
  public static Path getOnePathTo(ARGState pLastElement) {
    Path path = new Path();
    Set<ARGState> seenElements = new HashSet<ARGState>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pLastElement;
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFANode loc = extractLocation(currentARGState);
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
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

  /**
   * Get the set of all elements covered by any of the given elements,
   * i.e., the union of calling {@link ARGState#getCoveredByThis()} on all
   * elements.
   *
   * However, elements in the given set are never in the returned set.
   * If you pass in a subtree, this will return exactly the set of covering
   * edges which enter the subtree.
   */
  public static Set<ARGState> getCoveredBy(Set<ARGState> elements) {
    Set<ARGState> result = new HashSet<ARGState>();
    for (ARGState element : elements) {
      result.addAll(element.getCoveredByThis());
    }

    result.removeAll(elements);
    return result;
  }

  private static String determineColor(ARGState currentElement)
  {
    String color;

    if (currentElement.isCovered()) {
      color = "green";

    } else if (currentElement.isTarget()) {
      color = "red";

    } else {
      PredicateAbstractState abselem = AbstractStates.extractStateByType(currentElement, PredicateAbstractState.class);
      if (abselem != null && abselem.isAbstractionState()) {
        color = "cornflowerblue";
      } else {
        color = null;
      }
    }

    return color;
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   * @param rootState the root element of the ARG
   * @param displayedElements An optional set of elements. If given, all other elements are ignored. If null, all elements are dumped.
   * @param highlightedEdges Set of edges to highlight in the graph.
   * @return the ARG as DOT graph
   */
  public static String convertARTToDot(final ARGState rootState,
      final Set<ARGState> displayedElements,
      final Set<Pair<ARGState, ARGState>> highlightedEdges) {
    Deque<ARGState> worklist = new LinkedList<ARGState>();
    Set<Integer> nodesList = new HashSet<Integer>();
    Set<ARGState> processed = new HashSet<ARGState>();
    StringBuilder sb = new StringBuilder();
    StringBuilder edges = new StringBuilder();

    sb.append("digraph ARG {\n");
    // default style for nodes
    sb.append("node [style=\"filled\" shape=\"box\" color=\"white\"]\n");

    worklist.add(rootState);

    while (worklist.size() != 0){
      ARGState currentElement = worklist.removeLast();
      if (processed.contains(currentElement)){
        continue;
      }
      if (displayedElements != null && !displayedElements.contains(currentElement)) {
        continue;
      }

      processed.add(currentElement);

      if (!nodesList.contains(currentElement.getStateId())){

        String label = determineLabel(currentElement);

        sb.append(currentElement.getStateId());
        sb.append(" [");
        String color = determineColor(currentElement);
        if (color != null) {
          sb.append("fillcolor=\"" + color + "\" ");
        }
        sb.append("label=\"" + label +"\" ");
        sb.append("id=\"" + currentElement.getStateId() + "\"");
        sb.append("]");
        sb.append("\n");

        nodesList.add(currentElement.getStateId());
      }

      for (ARGState covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getStateId());
        edges.append(" -> ");
        edges.append(currentElement.getStateId());
        edges.append(" [style=\"dashed\" label=\"covered by\"]\n");
      }

      for (ARGState child : currentElement.getChildren()) {
        edges.append(currentElement.getStateId());
        edges.append(" -> ");
        edges.append(child.getStateId());
        edges.append(" [");

        boolean colored = highlightedEdges.contains(Pair.of(currentElement, child));
        CFAEdge edge = currentElement.getEdgeToChild(child);
        if (colored) {
          edges.append("color=\"red\"");
        }

        if (edge != null) {
          if (colored) {
            edges.append(" ");
          }
          edges.append("label=\"");
          edges.append("Line ");
          edges.append(edge.getLineNumber());
          edges.append(": ");
          edges.append(edge.getDescription().replaceAll("\n", " ").replace('"', '\''));
          edges.append("\"");
          edges.append(" id=\"");
          edges.append(currentElement.getStateId());
          edges.append(" -> ");
          edges.append(child.getStateId());
          edges.append("\"");
        }

        edges.append("]\n");
        if (!worklist.contains(child)){
          worklist.add(child);
        }
      }
    }
    sb.append(edges);
    sb.append("}\n");
    return sb.toString();
  }

  private static String determineLabel(ARGState currentElement) {
    StringBuilder builder = new StringBuilder();

    builder.append(currentElement.getStateId());

    CFANode loc = AbstractStates.extractLocation(currentElement);
    if (loc != null) {
      builder.append(" @ ");
      builder.append(loc.toString());
    }

    for (AutomatonState state : asIterable(currentElement).filter(AutomatonState.class)) {
      if (!state.getInternalStateName().equals("Init")) {
        builder.append("\\n");
        builder.append(state.getCPAName().replaceFirst("AutomatonAnalysis_", ""));
        builder.append(": ");
        builder.append(state.getInternalStateName());
      }
    }

    PredicateAbstractState abstraction = AbstractStates.extractStateByType(currentElement, PredicateAbstractState.class);
    if (abstraction != null && abstraction.isAbstractionState()) {
      builder.append("\\n");
      builder.append(abstraction.getAbstractionFormula());
    }

    ExplicitState explicit = AbstractStates.extractStateByType(currentElement, ExplicitState.class);
    if (explicit != null) {
      builder.append("\\n");
      builder.append(explicit.toCompactString());
    }

    RTTState rtt = AbstractStates.extractStateByType(currentElement, RTTState.class);
    if (explicit != null) {
      builder.append("\\n");
      builder.append(rtt.toCompactString());
    }

    return builder.toString();
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
  public static Path getPathFromBranchingInformation(
      ARGState root, Collection<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(arg.contains(root));

    Path result = new Path();
    ARGState currentElement = root;
    while (!currentElement.isTarget()) {
      Set<ARGState> children = currentElement.getChildren();

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

        for (ARGState currentChild : children) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (!(currentEdge instanceof CAssumeEdge)) {
            throw new IllegalArgumentException("ARG branches where there is no CAssumeEdge!");
          }

          if (((CAssumeEdge)currentEdge).getTruthAssumption()) {
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

      result.add(Pair.of(currentElement, edge));
      currentElement = child;
    }


    // need to add another pair with target state and one (arbitrary) outgoing edge
    CFANode loc = extractLocation(currentElement);
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
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
  public static Path getPathFromBranchingInformation(
      ARGState root, ARGState target, Collection<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(arg.contains(target));
    checkArgument(target.isTarget());

    Path result = getPathFromBranchingInformation(root, arg, branchingInformation);

    if (result.getLast().getFirst() != target) {
      throw new IllegalArgumentException("ARG target path reached the wrong target state!");
    }

    return result;
  }
}
