// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeDeserialization;

public class ImportDecomposition implements DssBlockDecomposition {

  private final Path importFile;

  public ImportDecomposition(Path pImportFile) {
    importFile = pImportFile;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(BlockNode.class, new BlockNodeDeserialization(cfa));
    mapper.registerModule(module);
    try {
      BlockNode[] nodesArray =
          mapper.readerForArrayOf(BlockNode.class).readValue(importFile.toFile());
      ImmutableSet<BlockNode> nodes = ImmutableSet.copyOf(nodesArray);
      ImmutableSet<@NonNull BlockNode> roots =
          FluentIterable.from(nodes).filter(BlockNode::isRoot).toSet();
      ImmutableSet<@NonNull BlockNode> remainingNodes =
          FluentIterable.from(nodes).filter(b -> !b.isRoot()).toSet();
      return BlockGraph.fromBlockNodes(Iterables.getOnlyElement(roots), remainingNodes);
    } catch (IOException pE) {
      throw new AssertionError("Could not read from file", pE);
    }
  }
}
