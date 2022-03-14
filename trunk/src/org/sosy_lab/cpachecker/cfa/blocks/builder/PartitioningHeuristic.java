// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Defines an interface for heuristics for the partition of a program's CFA into blocks.
 *
 * <p>Subclasses need to have exactly one public constructor or a static method named "create" which
 * may take a {@link LogManager} and a {@link CFA}, and throw at most a {@link CPAException}.
 */
public abstract class PartitioningHeuristic {

  public interface Factory {
    PartitioningHeuristic create(LogManager logger, CFA cfa, Configuration pConfig)
        throws CPAException, InvalidConfigurationException;
  }

  protected final CFA cfa;
  protected final LogManager logger;

  /**
   * Create instance.
   *
   * @param pConfig unused, but can be used and injected in subclasses.
   */
  protected PartitioningHeuristic(LogManager pLogger, CFA pCfa, Configuration pConfig) {
    cfa = pCfa;
    logger = pLogger;
  }

  /**
   * Creates a <code>BlockPartitioning</code> using the represented heuristic.
   *
   * @return BlockPartitioning
   * @see org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning
   */
  public final BlockPartitioning buildPartitioning(BlockPartitioningBuilder builder) {

    // traverse CFG
    final Set<CFANode> seen = new HashSet<>();
    final Deque<CFANode> stack = new ArrayDeque<>();

    final CFANode mainFunction = cfa.getMainFunction();
    seen.add(mainFunction);
    stack.push(mainFunction);

    while (!stack.isEmpty()) {
      final CFANode node = stack.pop();

      final Set<CFANode> subtree = getBlockForNode(node);
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

    return builder.build(cfa);
  }

  /**
   * Compute the nodes of a block, such that the entry-node and all possible exit-nodes should be
   * part of the block. For efficiency a block should not contain the nodes of inner function calls,
   * because we will add them automatically later.
   *
   * <p>(TODO This case never happened before, but who knows... : If a block contains a partial body
   * of a called function, we expect that either the function entry or the function exit is not part
   * of the block.)
   *
   * @param pBlockHead CFANode that should be cached.
   * @return set of nodes that represent a {@link Block}, or NULL, if no block should be build for
   *     this node. In most cases, we will return NULL.
   */
  @Nullable
  protected abstract Set<CFANode> getBlockForNode(CFANode pBlockHead);
}
