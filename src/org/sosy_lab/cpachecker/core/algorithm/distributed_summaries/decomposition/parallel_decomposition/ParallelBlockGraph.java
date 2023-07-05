// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

/*
 * Like BlockGraph but the root has all blocks as successors
 */
public class ParallelBlockGraph extends BlockGraph {

  public ParallelBlockGraph(BlockNode pRoot, ImmutableSet<BlockNode> pNodes) {
    super(pRoot, pNodes);
  }

  public static ParallelBlockGraph fromBlockNodesWithoutGraphInformation(
      CFA pCFA, Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startNodes = ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> endNodes = ArrayListMultimap.create();
    for (BlockNodeWithoutGraphInformation blockNode : pNodes) {
      startNodes.put(blockNode.getFirst(), blockNode);
      endNodes.put(blockNode.getLast(), blockNode);
    }
    BlockNode root =
        new BlockNode(
            BlockGraph.ROOT_ID,
            pCFA.getMainFunction(),
            pCFA.getMainFunction(),
            ImmutableSet.of(pCFA.getMainFunction()),
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            FluentIterable.from(pNodes)
                .transform(BlockNodeWithoutGraphInformation::getId)
                .filter(id -> !id.equals(ROOT_ID))
                .toSet());

    Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation> loopPredecessors =
        findLoopPredecessors(root, pNodes);

    startNodes.put(root.getFirst(), root);
    endNodes.put(root.getLast(), root);
    ImmutableSet<BlockNode> blockNodes =
        transformedImmutableSetCopy(
            pNodes,
            b ->
                new BlockNode(
                    b.getId(),
                    b.getFirst(),
                    b.getLast(),
                    b.getNodes(),
                    b.getEdges(),
                    ImmutableSet.of(ROOT_ID),
                    Sets.intersection(
                            transformedImmutableSetCopy(
                                endNodes.get(b.getFirst()),
                                BlockNodeWithoutGraphInformation::getId),
                            transformedImmutableSetCopy(
                                loopPredecessors.get(b), BlockNodeWithoutGraphInformation::getId))
                        .immutableCopy(),
                    transformedImmutableSetCopy(
                        startNodes.get(b.getLast()), BlockNodeWithoutGraphInformation::getId)));
    return new ParallelBlockGraph(root, blockNodes);
  }
}
