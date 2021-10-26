// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blockgraph.builder;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.blockgraph.BlockGraph;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

@SuppressWarnings("UnstableApiUsage")
public class BlockGraphBuilder {
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("UnstableApiUsage")
  private final ImmutableValueGraph.Builder<BlockBuilder, CFANode> graphBuilder
      = ValueGraphBuilder.directed().allowsSelfLoops(true).immutable();

  private FlatBlockBuilder entryBlock;

  private BlockGraphBuilder(final ShutdownNotifier pShutdownNotifier) {
    shutdownNotifier = pShutdownNotifier;
  }

  public static BlockGraphBuilder create(final ShutdownNotifier pShutdownNotifier) {
    return new BlockGraphBuilder(pShutdownNotifier);
  }

  /**
   * Construct the flat block graph starting at entry.
   * If a block contains a loop, the loop is not broken apart.
   * This method is called recursively for the same block.
   */
  private void exploreDepth(final CFANode initial, final BlockOperator blk) {
    Map<CFANode, BlockBuilder> completeBlocks = new HashMap<>();
    Map<CFANode, BlockBuilder> pendingBlocks = new HashMap<>();

    entryBlock = new FlatBlockBuilder(initial, new ArrayDeque<>(), shutdownNotifier);
    pendingBlocks.put(initial, entryBlock);

    while(!pendingBlocks.isEmpty()) {
      CFANode start = pendingBlocks.keySet().iterator().next();
      BlockBuilder block = pendingBlocks.get(start);
      pendingBlocks.remove(start);

      Set<BlockBuilder> exits = block.explore(blk);

      completeBlocks.put(start, block);
      graphBuilder.addNode(block);

      for (final BlockBuilder exit : exits) {
        BlockBuilder target = exit;
        CFANode entry = exit.getStart();

        if(completeBlocks.containsKey(entry)) {
          target = completeBlocks.get(entry);
        }
        else if(pendingBlocks.containsKey(entry)) {
          target = pendingBlocks.get(entry);
        }
        else {
          pendingBlocks.put(entry, exit);
        }

        if(target != entryBlock) {
          graphBuilder.putEdgeValue(block, target, entry);
        }
      }
    }
  }

  private BlockGraph compile() {
    ImmutableValueGraph<BlockBuilder, CFANode> graph = graphBuilder.build();
    Set<Block> blocks = new HashSet<>();

    for(BlockBuilder builder : graph.nodes()) {
      Block block = builder.getBlock();
      blocks.add(block);

      for(BlockBuilder successor : graph.successors(builder)) {
        Block target = successor.getBlock();
        CFANode exit = graph.edgeValue(builder, successor).orElseThrow();

        block.addExit(exit, target);
      }

      for(BlockBuilder predecessor : graph.predecessors(builder)) {
        Block source = predecessor.getBlock();
        block.addPredecessor(source);
      }
    }

    for(Block block : blocks) {
      block.complete();
    }

    return new BlockGraph(entryBlock.getBlock(), blocks);
  }

  public BlockGraph build(final CFANode start, final BlockOperator blk) {
    exploreDepth(start, blk);
    return compile();
  }
}
