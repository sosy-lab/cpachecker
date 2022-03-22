// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

public class WeightedEdge {

  private final WeightedNode startNode;
  private final WeightedNode endNode;
  private int weight;

  public WeightedNode getStartNode() {
    return startNode;
  }

  public WeightedNode getEndNode() {
    return endNode;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int pWeight) {
    weight = pWeight;
  }

  public void addWeight(int pWeight) {
    weight += pWeight;
  }

  public WeightedEdge(WeightedNode pStartNode, WeightedNode pEndNode, int pWeight) {
    startNode = pStartNode;
    endNode = pEndNode;
    weight = pWeight;
  }

  /** Edge represented by "start--weight-->end" */
  @Override
  public String toString() {
    return startNode + "--" + weight + "-->" + endNode;
  }
}
