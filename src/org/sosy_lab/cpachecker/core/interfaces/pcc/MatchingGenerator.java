// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Map;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

/**
 * Interface for computing matchings. Matchings are given implicitly by a map from node number to a
 * new node. I.e. if 2 nodes are matched, they point to the same super node.
 */
public interface MatchingGenerator {

  /**
   * Compute a matching on a weighted graph. I.e. >>each<< node has to be mapped onto another node
   * number. And a node cannot be matched twice
   *
   * @param wGraph the graph, on which a matching is computed
   * @return the computed matching
   */
  Map<Integer, Integer> computeMatching(WeightedGraph wGraph);
}
