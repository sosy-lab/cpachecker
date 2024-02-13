// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.ImportedBlock;

public class ImportDecomposition implements BlockSummaryCFADecomposer {

  private final Map<String, ImportedBlock> blocks;

  public ImportDecomposition(Path pImportFile) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    blocks =
        objectMapper.readValue(
            pImportFile.toFile(), new TypeReference<Map<String, ImportedBlock>>() {});
    for (ImportedBlock value : blocks.values()) {
      if (value.edges().stream().anyMatch(e -> e.size() != 2)) {
        throw new IllegalArgumentException(
            "Imported block has edges with more than two nodes (" + value.edges() + ")");
      }
    }
  }

  private String edgeToString(CFAEdge edge) {
    return edge.getPredecessor().getNodeNumber() + " " + edge.getSuccessor().getNodeNumber();
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    ImmutableSet.Builder<BlockNodeWithoutGraphInformation> nodes =
        ImmutableSet.builderWithExpectedSize(blocks.size());
    Map<Integer, CFANode> nodeIdMap = Maps.uniqueIndex(cfa.nodes(), CFANode::getNodeNumber);
    Map<String, CFAEdge> edgeIdsMap = Maps.uniqueIndex(cfa.edges(), this::edgeToString);
    for (Entry<String, ImportedBlock> importedBlock : blocks.entrySet()) {
      FluentIterable<List<Integer>> edges = FluentIterable.from(importedBlock.getValue().edges());
      ImmutableSet<CFANode> cfaNodes =
          edges.transformAndConcat(e -> e).transform(nodeIdMap::get).toSet();
      ImmutableSet<CFAEdge> cfaEdges =
          edges.transform(e -> edgeIdsMap.get(e.get(0) + " " + e.get(1))).toSet();
      nodes.add(
          new BlockNodeWithoutGraphInformation(
              importedBlock.getKey(),
              Objects.requireNonNull(nodeIdMap.get(importedBlock.getValue().startNode())),
              Objects.requireNonNull(nodeIdMap.get(importedBlock.getValue().endNode())),
              cfaNodes,
              cfaEdges));
    }
    return BlockGraph.fromImportedNodes(nodes.build(), blocks, nodeIdMap);
  }
}
