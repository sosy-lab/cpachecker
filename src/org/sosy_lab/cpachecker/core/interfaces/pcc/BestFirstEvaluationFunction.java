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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Set;

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

/**
 * Interface providing a method to compute a nodes priority.
 * With this priority best-first-algorithm can determine  which node to be explored next.
 */
public interface BestFirstEvaluationFunction {

  /**
  * Compute priority for node on wait-list to be expanded next, depending on actual situation and chosen evaluation function
  * @param partition The partition predecessor was added to
  * @param priority Priority of predecessor
  * @param node Node which is considered
  * @param wGraph The graph algorithm is working on
  * @return Priority to expand successor as next node
  */
  int computePriority(Set<Integer> partition, int priority, WeightedNode node,
      WeightedGraph wGraph);
}
