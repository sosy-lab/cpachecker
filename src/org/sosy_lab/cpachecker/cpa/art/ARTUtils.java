/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper class with collection of ART related utility methods.
 */
public class ARTUtils {

  private ARTUtils() { }

  /**
   * Get all elements on all paths from the ART root to a given element.
   *
   * @param pLastElement The last element in the paths.
   * @return A set of elements, all of which have pLastElement as their (transitive) child.
   */
  public static Set<ARTElement> getAllElementsOnPathsTo(ARTElement pLastElement) {

    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> waitList = new ArrayDeque<ARTElement>();

    result.add(pLastElement);
    waitList.add(pLastElement);

    while (!waitList.isEmpty()) {
      ARTElement currentElement = waitList.poll();
      for (ARTElement parent : currentElement.getParents()) {
        if (result.add(parent)) {
          waitList.push(parent);
        }
      }
    }

    return result;
  }

  /**
   * Get all elements on all paths between the source and target elemenents.
   * @param source
   * @param target
   * @return
   */

  public static Set<ARTElement> getAllElementsBetween(ARTElement source, ARTElement target){
    Collection<Path> paths = getAllPathsBetween(source, target);

    Set<ARTElement> elems = new HashSet<ARTElement>();

    for (Path pi : paths){
      elems.addAll(pi.asARTList());
    }

    // add the missing target
    elems.add(target);

    return elems;
  }

  /**
   * Get all paths in ART from the source to the target.
   * @param source
   * @param target
   * @return
   */
  public static Collection<Path> getAllPathsBetween(ARTElement source, ARTElement target) {
    Multimap<ARTElement, Path> pathMap = HashMultimap.create();
    Deque<ARTElement> waitList = new ArrayDeque<ARTElement>();
    Set<ARTElement> explored = new HashSet<ARTElement>();

    Set<Path> toAdd = new HashSet<Path>();

    waitList.add(target);
    explored.add(target);
    pathMap.put(target, new Path());
    while(!waitList.isEmpty()){
      ARTElement currentElement = waitList.poll();
      if (currentElement == source) {
        continue;
      }

      for (ARTElement parent : currentElement.getParents()){
        if (currentElement == parent){
          continue;
        }


        waitList.add(parent);
        // extend the path for every parent
        CFAEdge edge = parent.getEdgeToChild(currentElement);
        for (Path currentPath : pathMap.get(currentElement)) {

          Path newPath = (Path) currentPath.clone();
          newPath.add(new Pair<ARTElement,CFAEdge>(parent,edge));
          pathMap.put(parent, newPath);

        }
      }
    }
    return pathMap.get(source);
  }


  /**
   * Create a path from the ART root to the first rely-guarantee
   * abstraction element after the target element.
   *
   * @param target
   * @return
   */
  public static Path getPathToRGAbstraction(ARTElement target){
    // get the path up to the target
    Path pathTo = getOnePathTo(target);
    // find the first abstraction element after the target.

    ARTDijkstrasTermination trm = new ARTDijkstrasTermination() {
      @Override
      public boolean isTarget(ARTElement e) {
        RGAbstractElement rgElem = AbstractElements.extractElementByType(e, RGAbstractElement.class);
        return rgElem instanceof RGAbstractElement.AbstractionElement;
      }
    };


    // TODO correctness check
    RGAbstractElement rgElem = AbstractElements.extractElementByType(target, RGAbstractElement.class);
    if (!(rgElem instanceof RGAbstractElement.AbstractionElement)){
      System.out.println();
    }

    Path pathFrom = ARTDijkstrasAlgorithm.shortestPath(target, trm);
    assert !(rgElem instanceof RGAbstractElement.AbstractionElement) || pathFrom.isEmpty();

    pathTo.addAll(pathFrom);

    return pathTo;
  }







  /**
   * Create a path in the ART from root to the given element.
   * If there are several such paths, one is chosen randomly.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static Path getOnePathTo(ARTElement pLastElement) {
    Path path = new Path();
    Set<ARTElement> seenElements = new HashSet<ARTElement>();

    // each element of the path consists of the abstract element and the outgoing
    // edge to its successor

    ARTElement currentARTElement = pLastElement;
    // TODO under Rely Guarantee method the assertion below does not have to hold
    //assert pLastElement.isTarget();
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFANode loc = currentARTElement.retrieveLocationElement().getLocationNode();
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
    path.addFirst(Pair.of(currentARTElement, lastEdge));
    seenElements.add(currentARTElement);

    while (!currentARTElement.getParents().isEmpty()) {
      Iterator<ARTElement> parents = currentARTElement.getParents().iterator();


      ARTElement parentElement = parents.next();
      // skip over self loops
      while(parentElement == currentARTElement){
        parentElement = parents.next();
      }

      while (!seenElements.add(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        ARTElement newParentElement = parents.next();
        // avoid choosing a self-loop
        if (newParentElement == currentARTElement){
          if (parents.hasNext()){
            parentElement = parents.next();
          }
        } else {
          parentElement = newParentElement;
        }
      }

      CFAEdge edge = parentElement.getEdgeToChild(currentARTElement);
      path.addFirst(Pair.of(parentElement, edge));

      currentARTElement = parentElement;
    }
    return path;
  }




}
