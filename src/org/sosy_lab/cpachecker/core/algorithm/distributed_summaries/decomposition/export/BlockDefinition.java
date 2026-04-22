// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockDefinition {

  private final DssMetadata metadata;
  private final List<BlockMetadata> blocks;

  public BlockDefinition(DssMetadata pMetadata, List<BlockMetadata> pBlocks) {
    metadata = pMetadata;
    blocks = pBlocks;
  }

  public DssMetadata getMetadata() {
    return metadata;
  }

  public List<BlockMetadata> getBlocks() {
    return blocks;
  }
}
