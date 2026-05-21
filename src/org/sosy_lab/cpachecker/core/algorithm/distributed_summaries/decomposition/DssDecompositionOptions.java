// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

@Options(prefix = "distributedSummaries.decomposition")
public class DssDecompositionOptions {

  public enum DecompositionType {
    LINEAR_DECOMPOSITION,
    MERGE_DECOMPOSITION,
    BRIDGE_DECOMPOSITION,
    NO_DECOMPOSITION
  }

  @Option(
      description =
          "Allows to set the algorithm for decomposing the CFA. LINEAR_DECOMPOSITION creates blocks"
              + " from each merge/branching point to the next merge/branching point."
              + " MERGE_DECOMPOSITION merges blocks obtained by LINEAR_DECOMPOSITION. The final"
              + " number of blocks should converge to the number of functions in the program."
              + " NO_DECOMPOSITION creates one block around the CFA.",
      secure = true)
  private DecompositionType decompositionType = DecompositionType.MERGE_DECOMPOSITION;

  @Option(
      description =
          "The number of blocks is dependent by the number of functions in the program."
              + "A tolerance of 1 means, that we subtract 1 of the total number of functions.",
      secure = true)
  private boolean allowSingleBlockDecompositionWhenMerging = false;

  @Option(
      description =
          "Abstraction nodes are added to each block after they are created. "
              + "They are needed to strengthen the preconditions of blocks. "
              + "Missing blocks make the analysis slower but not impossible.",
      secure = true)
  private boolean allowMissingAbstractionNodes = true;

  @FileOption(Type.OUTPUT_FILE)
  @Option(description = "Where to store the block graph in JSON format", secure = true)
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  @Option(description = "Whether to stop after exporting the blockgraph", secure = true)
  private boolean generateBlockGraphOnly = false;

  @Option(description = "Import an existing decomposition from a file", secure = true)
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path importDecomposition = null;

  private final BlockOperator blockOperator;

  public DssDecompositionOptions(Configuration pConfiguration, CFA pCFA)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    blockOperator = new BlockOperator();
    pConfiguration.inject(blockOperator);
    try {
      blockOperator.setCFA(pCFA);
    } catch (CPAException e) {
      // if blockOperator.setCFA throws a CPAexception, this is because of an invalid configuration
      throw new InvalidConfigurationException("Initialization of block operator failed", e);
    }
  }

  public DssBlockDecomposition getConfiguredDecomposition() throws IOException {
    if (importDecomposition != null) {
      return new ImportDecomposition(importDecomposition);
    }
    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);
    return switch (decompositionType) {
      case LINEAR_DECOMPOSITION -> new LinearBlockNodeDecomposition(isBlockEnd);
      case MERGE_DECOMPOSITION ->
          new MergeBlockNodesDecomposition(
              new LinearBlockNodeDecomposition(isBlockEnd),
              2,
              Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
              allowSingleBlockDecompositionWhenMerging);
      case BRIDGE_DECOMPOSITION ->
          new VerticalMergeDecomposition(
              new BridgeDecomposition(), 1, Comparator.comparingInt(b -> b.getEdges().size()));
      case NO_DECOMPOSITION -> new SingleBlockDecomposition();
    };
  }

  public Path getBlockCFAFile() {
    return blockCFAFile;
  }

  public DecompositionType getDecompositionType() {
    return decompositionType;
  }

  public boolean allowMissingAbstractionNodes() {
    return allowMissingAbstractionNodes;
  }

  public boolean generateBlockGraphOnly() {
    return generateBlockGraphOnly;
  }
}
