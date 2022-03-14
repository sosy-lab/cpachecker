// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks.builder;

import com.google.common.collect.ImmutableList;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Combine several heuristics. The order of heuristics determines the matching. Thus it is a good
 * idea to apply heuristics with a bigger block-size first.
 */
public class CompositePartitioning extends PartitioningHeuristic {

  private final ImmutableList<PartitioningHeuristic> partitionings;

  public CompositePartitioning(
      LogManager pLogger,
      CFA pCfa,
      Configuration pConfig,
      PartitioningHeuristic... pPartitionings) {
    super(pLogger, pCfa, pConfig);
    partitionings = ImmutableList.copyOf(pPartitionings);
  }

  @Override
  @Nullable
  protected Set<CFANode> getBlockForNode(CFANode pBlockHead) {
    for (PartitioningHeuristic partitioning : partitionings) {
      Set<CFANode> nodes = partitioning.getBlockForNode(pBlockHead);
      if (nodes != null) {
        return nodes;
      }
    }
    return null;
  }
}
