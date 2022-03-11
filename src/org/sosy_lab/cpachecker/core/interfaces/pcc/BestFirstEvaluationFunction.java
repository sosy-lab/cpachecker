// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Set;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

/**
 * Interface providing a method to compute a nodes priority. With this priority best-first-algorithm
 * can determine which node to be explored next.
 */
public interface BestFirstEvaluationFunction {

  /**
   * Compute priority for node on wait-list to be expanded next, depending on actual situation and
   * chosen evaluation function
   *
   * @param partition The partition predecessor was added to
   * @param priority Priority of predecessor
   * @param node Node which is considered
   * @param wGraph The graph algorithm is working on
   * @return Priority to expand successor as next node
   */
  int computePriority(
      Set<Integer> partition, int priority, WeightedNode node, WeightedGraph wGraph);
}
