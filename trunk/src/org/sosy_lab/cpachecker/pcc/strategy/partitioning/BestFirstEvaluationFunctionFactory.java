/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
    BestFirstEvaluationFunctions(String pDescription){
      description=pDescription;
    }
    @Override
    public String toString(){
      return description;
    }
  }

  public static  BestFirstEvaluationFunction createEvaluationFunction(BestFirstEvaluationFunctions function) {
    switch (function) {
      case BREADTH_FIRST:
      return new BestFirstEvaluationFunction(){
        @Override
        public int computePriority(Set<Integer> partition, int priority, WeightedNode node,
            WeightedGraph wGraph) {
          return priority + 1; //expand next level nodes, when this level complete
        }
      };

      case DEPTH_FIRST:
        return new BestFirstEvaluationFunction(){
          @Override
          public int computePriority(Set<Integer> partition, int priority, WeightedNode node,
              WeightedGraph wGraph) {
            return priority - 1; //expand next level nodes, as next step (assumption: PriorityQueue preserves order of inserting)
          }
        };

      default:
        return new BestFirstEvaluationFunction(){
          @Override
          public int computePriority(Set<Integer> partition, int priority, WeightedNode node,
              WeightedGraph wGraph) {
            /*
             * if node not in partition it has cost of its weight for the actual partition ==> node-weight is gain
             * all of its successors which are not in the partition right now ==>  cost
             */
            Set<Integer> successors = wGraph.getIntSuccessors(node); //successors of this node
            successors.removeAll(partition); // successors, that are not in given partition
            int gain = node.getWeight();
            return WeightedGraph.computeWeight(successors, wGraph) - gain; //chance +/- since least priority is chosen first

          }
        };

    }
  }
}
