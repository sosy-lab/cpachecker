/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;

import java.util.Set;


/**
 * <code>PartitioningHeuristic</code> that creates a block for each function-body.
 */
@Options(prefix = "cpa.bam.blockHeuristic.functionPartitioning")
public class FunctionPartitioning extends PartitioningHeuristic {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION = CFATraversal.dfs().ignoreFunctionCalls();

  @Option(
    secure = true,
    description =
        "only consider function with a minimum number of CFA nodes. "
            + "This approach is similar to 'inlining' small functions, when using BAM."
  )
  private int minFunctionSize = 0;

  @Option(
    secure = true,
    description =
        "only consider function with a minimum number of calls. "
            + "This approach is similar to 'inlining' functions used only a few times. "
            + "Info: If a function is called several times in a loop, we only count 'one' call."
  )
  private int minFunctionCalls = 0;

  /** Do not change signature! Constructor will be created with Reflections. */
  public FunctionPartitioning(LogManager pLogger, CFA pCfa, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pLogger, pCfa, pConfig);
    pConfig.inject(this);
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pBlockHead) {
    if (pBlockHead instanceof FunctionEntryNode) {
      Set<CFANode> nodes = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(pBlockHead);

      // main function
      if (pBlockHead.getNumEnteringEdges() == 0) {
        return nodes;
      }

      // heuristics based on function metrics
      if (nodes.size() >= minFunctionSize && pBlockHead.getNumEnteringEdges() >= minFunctionCalls) {
        return nodes;
      }
    }
    return null;
  }
}
