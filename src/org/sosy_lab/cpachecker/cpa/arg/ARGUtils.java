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
import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.collect.Iterables;

/**
 * Helper class with collection of ART related utility methods.
 */
public class ARGUtils {

  private ARGUtils() { }

  /**
   * Get all elements on all paths from the ART root to a given element.
   *
   * @param pLastElement The last element in the paths.
   * @return A set of elements, all of which have pLastElement as their (transitive) child.
   */
  public static Set<ARGElement> getAllElementsOnPathsTo(ARGElement pLastElement) {

    Set<ARGElement> result = new HashSet<ARGElement>();
    Deque<ARGElement> waitList = new ArrayDeque<ARGElement>();

    result.add(pLastElement);
    waitList.add(pLastElement);

    while (!waitList.isEmpty()) {
      ARGElement currentElement = waitList.poll();
      for (ARGElement parent : currentElement.getParents()) {
        if (result.add(parent)) {
          waitList.push(parent);
        }
      }
    }

    return result;
  }

  /**
   * Create a path in the ART from root to the given element.
   * If there are several such paths, one is chosen randomly.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static Path getOnePathTo(ARGElement pLastElement) {
    Path path = new Path();
    Set<ARGElement> seenElements = new HashSet<ARGElement>();

    // each element of the path consists of the abstract element and the outgoing
    // edge to its successor

    ARGElement currentARGElement = pLastElement;
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFANode loc = extractLocation(currentARGElement);
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
    path.addFirst(Pair.of(currentARGElement, lastEdge));
    seenElements.add(currentARGElement);

    while (!currentARGElement.getParents().isEmpty()) {
      Iterator<ARGElement> parents = currentARGElement.getParents().iterator();

      ARGElement parentElement = parents.next();
      while (!seenElements.add(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      CFAEdge edge = parentElement.getEdgeToChild(currentARGElement);
      path.addFirst(Pair.of(parentElement, edge));

      currentARGElement = parentElement;
    }
    return path;
  }

  /**
   * Get the set of all elements covered by any of the given elements,
   * i.e., the union of calling {@link ARGElement#getCoveredByThis()} on all
   * elements.
   *
   * However, elements in the given set are never in the returned set.
   * If you pass in a subtree, this will return exactly the set of covering
   * edges which enter the subtree.
   */
  public static Set<ARGElement> getCoveredBy(Set<ARGElement> elements) {
    Set<ARGElement> result = new HashSet<ARGElement>();
    for (ARGElement element : elements) {
      result.addAll(element.getCoveredByThis());
    }

    result.removeAll(elements);
    return result;
  }

  private static String determineColor(ARGElement currentElement)
  {
    String color;

    if (currentElement.isCovered()) {
      color = "green";

    } else if (currentElement.isTarget()) {
      color = "red";

    } else {
      PredicateAbstractElement abselem = AbstractElements.extractElementByType(currentElement, PredicateAbstractElement.class);
      if (abselem != null && abselem.isAbstractionElement()) {
        color = "cornflowerblue";
      } else {
        color = null;
      }
    }

    return color;
  }

  /**
   * Create String with ART in the DOT format of Graphviz.
   * @param rootElement the root element of the ART
   * @param displayedElements An optional set of elements. If given, all other elements are ignored. If null, all elements are dumped.
   * @param highlightedEdges Set of edges to highlight in the graph.
   * @return the ART as DOT graph
   */
  public static String convertARTToDot(final ARGElement rootElement,
      final Set<ARGElement> displayedElements,
      final Set<Pair<ARGElement, ARGElement>> highlightedEdges) {
    Deque<ARGElement> worklist = new LinkedList<ARGElement>();
    Set<Integer> nodesList = new HashSet<Integer>();
    Set<ARGElement> processed = new HashSet<ARGElement>();
    StringBuilder sb = new StringBuilder();
    StringBuilder edges = new StringBuilder();

    sb.append("digraph ART {\n");
    // default style for nodes
    sb.append("node [style=\"filled\" shape=\"box\" color=\"white\"]\n");

    worklist.add(rootElement);

    while(worklist.size() != 0){
      ARGElement currentElement = worklist.removeLast();
      if(processed.contains(currentElement)){
        continue;
      }
      if (displayedElements != null && !displayedElements.contains(currentElement)) {
        continue;
      }

      processed.add(currentElement);

      if(!nodesList.contains(currentElement.getElementId())){

        String label = determineLabel(currentElement);

        sb.append(currentElement.getElementId());
        sb.append(" [");
        String color = determineColor(currentElement);
        if (color != null) {
          sb.append("fillcolor=\"" + color + "\" ");
        }
        sb.append("label=\"" + label +"\" ");
        sb.append("id=\"" + currentElement.getElementId() + "\"");
        sb.append("]");
        sb.append("\n");

        nodesList.add(currentElement.getElementId());
      }

      for (ARGElement covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getElementId());
        edges.append(" -> ");
        edges.append(currentElement.getElementId());
        edges.append(" [style=\"dashed\" label=\"covered by\"]\n");
      }

      for (ARGElement child : currentElement.getChildren()) {
        edges.append(currentElement.getElementId());
        edges.append(" -> ");
        edges.append(child.getElementId());
        edges.append(" [");

        boolean colored = highlightedEdges.contains(Pair.of(currentElement, child));
        CFAEdge edge = currentElement.getEdgeToChild(child);
        if(colored) {
          edges.append("color=\"red\"");
        }

        if(edge != null) {
          if(colored) {
            edges.append(" ");
          }
          edges.append("label=\"");
          edges.append("Line ");
          edges.append(edge.getLineNumber());
          edges.append(": ");
          edges.append(edge.getDescription().replaceAll("\n", " ").replace('"', '\''));
          edges.append("\"");
          edges.append(" id=\"");
          edges.append(currentElement.getElementId());
          edges.append(" -> ");
          edges.append(child.getElementId());
          edges.append("\"");
        }

        edges.append("]\n");
        if(!worklist.contains(child)){
          worklist.add(child);
        }
      }
    }
    sb.append(edges);
    sb.append("}\n");
    return sb.toString();
  }

  private static String determineLabel(ARGElement currentElement) {
    StringBuilder builder = new StringBuilder();

    builder.append(currentElement.getElementId());

    CFANode loc = AbstractElements.extractLocation(currentElement);
    if(loc != null) {
      builder.append(" @ ");
      builder.append(loc.toString());
    }

    Iterable<AutomatonState> states = AbstractElements.extractAllElementsOfType(currentElement, AutomatonState.class);
    for (AutomatonState state : states) {
      if (!state.getInternalStateName().equals("Init")) {
        builder.append("\\n");
        builder.append(state.getCPAName().replaceFirst("AutomatonAnalysis_", ""));
        builder.append(": ");
        builder.append(state.getInternalStateName());
      }
    }

    PredicateAbstractElement abstraction = AbstractElements.extractElementByType(currentElement, PredicateAbstractElement.class);
    if(abstraction != null && abstraction.isAbstractionElement()) {
      builder.append("\\n");
      builder.append(abstraction.getAbstractionFormula());
    }

    ExplicitElement explicit = AbstractElements.extractElementByType(currentElement, ExplicitElement.class);
    if(explicit != null) {
      builder.append("\\n");
      builder.append(explicit.toCompactString());
    }

    return builder.toString();
  }

  /**
   * Find a path in the ART. The necessary information to find the path is a
   * boolean value for each branching situation that indicates which of the two
   * AssumeEdges should be taken.
   *
   * @param root The root element of the ART (where to start the path)
   * @param art All elements in the ART or a subset thereof (elements outside this set will be ignored).
   * @param branchingInformation A map from ART element ids to boolean values indicating the outgoing direction.
   * @return A path through the ART from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ART or the ART is inconsistent.
   */
  public static Path getPathFromBranchingInformation(
      ARGElement root, Collection<? extends AbstractElement> art,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(art.contains(root));

    Path result = new Path();
    ARGElement currentElement = root;
    while (!currentElement.isTarget()) {
      Set<ARGElement> children = currentElement.getChildren();

      ARGElement child;
      CFAEdge edge;
      switch (children.size()) {

      case 0:
        throw new IllegalArgumentException("ART target path terminates without reaching target element!");

      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(children);
        edge = currentElement.getEdgeToChild(child);
        break;

      case 2: // branch
        // first, find out the edges and the children
        CFAEdge trueEdge = null;
        CFAEdge falseEdge = null;
        ARGElement trueChild = null;
        ARGElement falseChild = null;

        for (ARGElement currentChild : children) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (!(currentEdge instanceof AssumeEdge)) {
            throw new IllegalArgumentException("ART branches where there is no AssumeEdge!");
          }

          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            trueEdge = currentEdge;
            trueChild = currentChild;
          } else {
            falseEdge = currentEdge;
            falseChild = currentChild;
          }
        }
        if (trueEdge == null || falseEdge == null) {
          throw new IllegalArgumentException("ART branches with non-complementary AssumeEdges!");
        }
        assert trueChild != null;
        assert falseChild != null;

        // search first idx where we have a predicate for the current branching
        Boolean predValue = branchingInformation.get(currentElement.getElementId());
        if (predValue == null) {
          throw new IllegalArgumentException("ART branches without direction information!");
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
        throw new IllegalArgumentException("ART splits with more than two branches!");
      }

      if (!art.contains(child)) {
        throw new IllegalArgumentException("ART and direction information from solver disagree!");
      }

      result.add(Pair.of(currentElement, edge));
      currentElement = child;
    }


    // need to add another pair with target element and one (arbitrary) outgoing edge
    CFANode loc = extractLocation(currentElement);
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
    result.add(Pair.of(currentElement, lastEdge));

    return result;
  }

  /**
   * Find a path in the ART. The necessary information to find the path is a
   * boolean value for each branching situation that indicates which of the two
   * AssumeEdges should be taken.
   * This method checks that the path ends in a certain element.
   *
   * @param root The root element of the ART (where to start the path)
   * @param target The target element (where to end the path, needs to be a target element)
   * @param art All elements in the ART or a subset thereof (elements outside this set will be ignored).
   * @param branchingInformation A map from ART element ids to boolean values indicating the outgoing direction.
   * @return A path through the ART from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ART or the ART is inconsistent.
   */
  public static Path getPathFromBranchingInformation(
      ARGElement root, ARGElement target, Collection<? extends AbstractElement> art,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(art.contains(target));
    checkArgument(target.isTarget());

    Path result = getPathFromBranchingInformation(root, art, branchingInformation);

    if (result.getLast().getFirst() != target) {
      throw new IllegalArgumentException("ART target path reached the wrong target element!");
    }

    return result;
  }
}
