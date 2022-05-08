// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import java.util.List;

/**
 * DomInput stores the predecessors for every node as well as the number of nodes in the whole
 * graph.
 */
final class DomInput {

  static final int DELIMITER = -2;

  // the data array contains the predecessors (their IDs) of every node
  // the predecessors of a node are separated by a DELIMITER from other predecessors
  // the first group of predecessors are for the node with ID == 0,
  // the second group for node with ID == 1, ..., the last group for node with ID == nodeCount - 1
  // the array must contain exactly one DELIMITER per node and its last element must be DELIMITER
  //
  // p_X_Y: predecessor Y of node X
  // format example: [p_0_a, p_0_b, DELIMITER, p_1_c, DELIMITER, DELIMITER, p_3_d, ...]
  // - node 0 has 2 predecessors
  // - node 1 has 1 predecessor
  // - node 2 has 0 predecessors
  // - node 3 has (at least) 1 predecessor
  private final int[] data;

  private final int nodeCount;

  private DomInput(int[] pData, int pNodeCount) {
    data = pData;
    nodeCount = pNodeCount;
  }

  static DomInput create(List<List<Integer>> pData, int pPredCount) {

    int[] data = new int[pPredCount + pData.size()];

    int index = 0;
    for (List<Integer> preds : pData) {
      for (int pred : preds) {
        data[index++] = pred;
      }
      data[index++] = DELIMITER;
    }

    return new DomInput(data, pData.size());
  }

  int getValue(int index) {
    return data[index];
  }

  /** Number of nodes in the whole graph. */
  int getNodeCount() {
    return nodeCount;
  }
}
