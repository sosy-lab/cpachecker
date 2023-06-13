// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BestFirstEvaluationFunction;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

public class BestFirstEvaluationFunctionFactory {

  private BestFirstEvaluationFunctionFactory() {}

  public enum BestFirstEvaluationFunctions {
    BREADTH_FIRST("breadth-first"),
    DEPTH_FIRST("depth-first"),
    BEST_IMPROVEMENT_FIRST("best-improvement-first");

    private final String description;

    BestFirstEvaluationFunctions(String pDescription) {
      description = pDescription;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  public static BestFirstEvaluationFunction createEvaluationFunction(
      BestFirstEvaluationFunctions function) {
    switch (function) {
      case BREADTH_FIRST:
        return new BestFirstEvaluationFunction() {
          @Override
          public int computePriority(
              Set<Integer> partition, int priority, WeightedNode node, WeightedGraph wGraph) {
            return priority + 1; // expand next level nodes, when this level complete
          }
        };

      case DEPTH_FIRST:
        return new BestFirstEvaluationFunction() {
          @Override
          public int computePriority(
              Set<Integer> partition, int priority, WeightedNode node, WeightedGraph wGraph) {
            // expand next level nodes, as next step (assumption: PriorityQueue preserves order of
            // inserting)
            return priority - 1;
          }
        };

      default:
        return new BestFirstEvaluationFunction() {
          @Override
          public int computePriority(
              Set<Integer> partition, int priority, WeightedNode node, WeightedGraph wGraph) {
            /*
             * if node not in partition it has cost of its weight for the actual partition ==> node-weight is gain
             * all of its successors which are not in the partition right now ==>  cost
             */
            Set<Integer> successors = wGraph.getIntSuccessors(node); // successors of this node
            successors.removeAll(partition); // successors, that are not in given partition
            int gain = node.getWeight();
            // chance +/- since least priority is chosen first
            return WeightedGraph.computeWeight(successors, wGraph) - gain;
          }
        };
    }
  }
}
