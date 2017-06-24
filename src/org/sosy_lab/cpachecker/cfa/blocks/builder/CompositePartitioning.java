/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.ImmutableList;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Combine several heuristics. The order of heuristics determines the matching.
 * Thus it is a good idea to apply heuristics with a bigger block-size first. */
public class CompositePartitioning extends PartitioningHeuristic {

  private final ImmutableList<PartitioningHeuristic> partitionings;

  public CompositePartitioning(LogManager pLogger, CFA pCfa, Configuration pConfig,
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
