// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks.builder;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;

/** <code>PartitioningHeuristic</code> that creates a block for each function-body. */
@Options(prefix = "cpa.bam.blockHeuristic.functionPartitioning")
public class FunctionPartitioning extends PartitioningHeuristic {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION =
      CFATraversal.dfs().ignoreFunctionCalls();

  @Option(
      secure = true,
      description =
          "only consider function with a minimum number of CFA nodes. "
              + "This approach is similar to 'inlining' small functions, when using BAM.")
  private int minFunctionSize = 0;

  @Option(
      secure = true,
      description =
          "only consider function with a minimum number of calls. "
              + "This approach is similar to 'inlining' functions used only a few times. "
              + "Info: If a function is called several times in a loop, we only count 'one' call.")
  private int minFunctionCalls = 0;

  @Option(
      secure = true,
      description =
          "only consider functions with a matching name, i.e., select only some functions"
              + " directly.")
  private ImmutableSet<String> matchFunctions = null;

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

      // consider only build blocks for matching functions
      if (matchFunctions != null && !matchFunctions.contains(pBlockHead.getFunctionName())) {
        return null;
      }

      // heuristics based on function metrics
      if (nodes.size() >= minFunctionSize && pBlockHead.getNumEnteringEdges() >= minFunctionCalls) {
        return nodes;
      }
    }
    return null;
  }
}
