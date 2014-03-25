/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Abstract base class for static refinement approaches.
 */
@Options(prefix="staticRefiner")
abstract public class StaticRefiner {

  @Option(description="collect at most this number of assumes along a path, backwards from each target (= error) location")
  private int maxBackscanPathAssumes = 1;

  protected final LogManager logger;

  public StaticRefiner(
      Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {

    this.logger = pLogger;

    pConfig.inject(this, StaticRefiner.class);
  }

  protected Set<CIdExpression> getVariablesOfAssume(AssumeEdge pAssume) throws CPATransferException {
    if (pAssume.getExpression() instanceof CExpression) {
      CExpression ce = (CExpression) pAssume.getExpression();
      CIdExpressionCollectorVisitor referencedVariablesVisitor = new CIdExpressionCollectorVisitor();
      ce.accept(referencedVariablesVisitor);
      return referencedVariablesVisitor.getReferencedIdExpressions();
    } else {
      throw new RuntimeException("Only C programming language supported!");
    }
  }

  /**
   * This method finds in a backwards search, starting from the target locations in the
   * CFA, the list of n assume edges preceeding each target node, where n equals the
   * maxBackscanPathAssumes option.
   *
   * @return the mapping from target nodes to the corresponding preceeding assume edges
   */
  protected ListMultimap<CFANode, AssumeEdge> getTargetLocationAssumes(Collection<CFANode> targetNodes) {
    ListMultimap<CFANode, AssumeEdge> result  = ArrayListMultimap.create();
    if (targetNodes.isEmpty()) {
      return result;
    }

    // backwards search to determine all relevant edges
    for (CFANode targetNode : targetNodes) {
      Deque<Pair<CFANode, Integer>> queue = new ArrayDeque<>();
      queue.add(Pair.of(targetNode, 0));
      Set<CFANode> explored = new HashSet<>();

      while (!queue.isEmpty()) {
        // Take the next node that should be explored from the queue
        Pair<CFANode, Integer> v = queue.pop();

        // Each node that enters node v
        for (CFAEdge e: CFAUtils.enteringEdges(v.getFirst())) {
          CFANode u = e.getPredecessor();

          boolean isAssumeEdge = (e instanceof AssumeEdge);
          int depthIncrease = isAssumeEdge ? 1 : 0;

          if (isAssumeEdge) {
            AssumeEdge assume = (AssumeEdge) e;
            if (v.getSecond() < maxBackscanPathAssumes) {
              result.put(targetNode, assume);
            } else {
              continue;
            }
          }

          if (!explored.contains(u)) {
            queue.add(Pair.of(u, v.getSecond() + depthIncrease));
          }
        }

        explored.add(v.getFirst());
      }
    }

    return result;
  }
}
