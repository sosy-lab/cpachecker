// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

public class WeightedNode {

  private final int nodeNumber;
  private final int weight;

  public int getNodeNumber() {
    return nodeNumber;
  }

  public int getWeight() {
    return weight;
  }

  public WeightedNode(int pNode, int pWeight) {
    nodeNumber = pNode;
    weight = pWeight;
  }

  /** Node represented by [node(W: weight)] */
  @Override
  public String toString() {
    return nodeNumber + "(W:" + weight + ")";
  }
}
