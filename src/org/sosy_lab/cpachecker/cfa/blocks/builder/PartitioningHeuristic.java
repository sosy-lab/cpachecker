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
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;


/**
 * Defines an interface for heuristics for the partition of a program's CFA into blocks.
 *
 * Subclasses need to have exactly one public constructor or a static method named "create"
 * which may take a {@link LogManager} and a {@link CFA}, and throw at most a {@link CPAException}.
 */
public abstract class PartitioningHeuristic {

  public interface Factory {
    PartitioningHeuristic create(LogManager logger, CFA cfa, Configuration pConfig)
        throws CPAException, InvalidConfigurationException;
  }

  protected final CFA cfa;
  protected final LogManager logger;

  /**
   * @param pConfig configuration can be used and injected in subclasses.
   */
  public PartitioningHeuristic(LogManager pLogger, CFA pCfa, Configuration pConfig) {
    cfa = pCfa;
    logger = pLogger;
  }

  /**
   * Creates a <code>BlockPartitioning</code> using the represented heuristic.
   * @param mainFunction CFANode at which the main-function is defined
   * @return BlockPartitioning
   * @see org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning
   */
  public final BlockPartitioning buildPartitioning(CFANode mainFunction, BlockPartitioningBuilder builder) {

    //traverse CFG
    Set<CFANode> seen = new HashSet<>();
    Deque<CFANode> stack = new ArrayDeque<>();

    seen.add(mainFunction);
    stack.push(mainFunction);

    while (!stack.isEmpty()) {
      CFANode node = stack.pop();

      Set<CFANode> subtree = getBlockForNode(node);
      if (subtree != null) {
        builder.addBlock(subtree, node);
      }

      for (CFANode nextNode : CFAUtils.successorsOf(node)) {
        if (!seen.contains(nextNode)) {
          stack.push(nextNode);
          seen.add(nextNode);
        }
      }
    }

    return builder.build(mainFunction);
  }

  /**
   * @param pBlockHead CFANode that should be cached.
   * @return set of nodes that represent a {@link Block},
   *         or NULL, if no block should be build for this node.
   *         In most cases, we will return NULL.
   */
  @Nullable
  protected abstract Set<CFANode> getBlockForNode(CFANode pBlockHead);
}
