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

public class ImportDecomposition implements DssBlockDecomposition {

  private final Map<String, ImportedBlock> blocks;

  public ImportDecomposition(Path pImportFile) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    blocks = objectMapper.readValue(pImportFile.toFile(), new TypeReference<>() {});
    for (ImportedBlock value : blocks.values()) {
      if (value.edges().stream().anyMatch(e -> e.size() != 2)) {
        throw new IllegalArgumentException(
            "Imported block has edges with more than two nodes (" + value.edges() + ")");
      }
    }
  }

  // All node IDs are shifted such that they start from 0
  private int shiftedNodeNumber(int originalNodeNumber, int shift) {
    return originalNodeNumber - shift;
  }

  private String edgeToString(CFAEdge edge, int minCfaNodeNumber) {
    return shiftedNodeNumber(edge.getPredecessor().getNodeNumber(), minCfaNodeNumber)
        + " "
        + shiftedNodeNumber(edge.getSuccessor().getNodeNumber(), minCfaNodeNumber);
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    ImmutableSet.Builder<BlockNodeWithoutGraphInformation> nodes =
        ImmutableSet.builderWithExpectedSize(blocks.size());
    int minCfaNodeNumber =
        cfa.nodes().stream().mapToInt(CFANode::getNodeNumber).min().orElseThrow();
    Map<Integer, CFANode> nodeIdMap =
        Maps.uniqueIndex(cfa.nodes(), n -> shiftedNodeNumber(n.getNodeNumber(), minCfaNodeNumber));
    Map<String, CFAEdge> edgeIdsMap =
        Maps.uniqueIndex(cfa.edges(), e -> edgeToString(e, minCfaNodeNumber));

    for (Entry<String, ImportedBlock> importedBlock : blocks.entrySet()) {
      FluentIterable<List<Integer>> edges = FluentIterable.from(importedBlock.getValue().edges());
      ImmutableSet<CFANode> cfaNodes =
          edges.transformAndConcat(e -> e).transform(nodeIdMap::get).toSet();
      ImmutableSet<CFAEdge> cfaEdges =
          edges.transform(e -> edgeIdsMap.get(e.getFirst() + " " + e.get(1))).toSet();
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
