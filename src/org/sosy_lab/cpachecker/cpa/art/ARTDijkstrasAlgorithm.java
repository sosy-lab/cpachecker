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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;


class DNode implements Comparable<DNode> {

  private final ARTElement artElem;
  private DNode shortestPredecessor;
  private CFAEdge shortestEdge;
  private int   distance;

  public DNode(ARTElement artElem){
    assert artElem != null;
    this.artElem = artElem;
    this.shortestPredecessor = null;
    this.shortestEdge = null;
    this.distance  = Integer.MAX_VALUE;
  }

  public ARTElement getArtElem() {
    return artElem;
  }

  public DNode getShortestPredecessor() {
    return shortestPredecessor;
  }

  public int getDistance() {
    return distance;
  }

  public void setShortestPredecessor(DNode pShortestPredecessor) {
    assert pShortestPredecessor != null;
    shortestPredecessor = pShortestPredecessor;
  }

  public void setDistance(int pDistance) {
    distance = pDistance;
  }

  public CFAEdge getShortestEdge() {
    return shortestEdge;
  }

  public void setShortestEdge(CFAEdge pShortestEdge) {
    shortestEdge = pShortestEdge;
  }

  @Override
  public int compareTo(DNode dobj) {
    assert dobj != null;

    if (distance > dobj.getDistance()){
      return 1;
    }
    else if (distance == dobj.getDistance()){
      return 0;
    }
    else {
      return -1;
    }

  }

}

public class ARTDijkstrasAlgorithm {

  /**
   * Algorithm returns the shortest path from source to the element that triggers terminator.
   * @param source
   * @param terminator
   * @return
   */
  public static Path shortestPath(ARTElement source, ARTDijkstrasTermination terminator){

    DNode node = DijkstrasAlgorithm(source, terminator);
    Path path = new Path();

    while(node != null && node.getShortestPredecessor() != null){
      path.add(Pair.of(node.getArtElem(), node.getShortestEdge()));
      node = node.getShortestPredecessor();
    }

    return path;
  }

  /**
   * Algorithm computes shortest distances until the terminator gives true or all nodes have been processes.
   * Returns a set of discovered nodes and the node that caused the termination, if any.
   * @param source
   * @param terminator
   * @return
   */
  private static DNode DijkstrasAlgorithm(ARTElement source, ARTDijkstrasTermination terminator){
    /*
     * Standard Dijkstra's algorithm. Undiscovered nodes have implicitly maximum distance and
     * no predecessor. When discovered, their are add to nodeMap.
     */

    PriorityQueue<DNode> queue = new PriorityQueue<DNode>();
    Map<ARTElement, DNode> nodeMap = new HashMap<ARTElement, DNode>();
    DNode target = null;

    DNode sn = new DNode(source);
    sn.setDistance(0);
    queue.add(sn);
    nodeMap.put(source, sn);

    while(!queue.isEmpty()){
      DNode node = queue.poll();

      if (terminator.isTarget(node.getArtElem())){
        target = node;
        break;
      }

      for (ARTElement child : node.getArtElem().getChildren()){
        DNode cn = nodeMap.get(child);
        if (cn == null){
          cn = new DNode(child);
          nodeMap.put(child, cn);
          assert !queue.contains(cn);
          queue.add(cn);
        }

        assert queue.contains(cn);

        int distance = node.getDistance() + 1;
        if (distance < cn.getDistance()){
          cn.setDistance(distance);
          cn.setShortestPredecessor(node);
          CFAEdge edge = node.getArtElem().getEdgeToChild(child);
          cn.setShortestEdge(edge);
        }
      }
    }

    return target;
  }


}
