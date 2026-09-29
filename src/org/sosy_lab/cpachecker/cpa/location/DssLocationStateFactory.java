// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSortedSet;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Allocates location states only for nodes visited by a DSS block analysis. */
@Options(prefix = "cpa.location")
public final class DssLocationStateFactory implements CachedLocationStateProvider {

  private final ImmutableSortedSet<CFANode> nodes;
  private final Map<CFANode, LocationState> states = new HashMap<>();

  @Option(
      secure = true,
      description =
          "With this option enabled, function calls that occur"
              + " in the CFA are followed. By disabling this option one can traverse a function"
              + " without following function calls (in this case FunctionSummaryEdges are used)")
  private boolean followFunctionCalls = true;

  public DssLocationStateFactory(CFA pCfa, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    nodes = ImmutableSortedSet.copyOf(pCfa.nodes());
  }

  @Override
  public LocationState getState(CFANode pNode) {
    int nodeNumber = checkNotNull(pNode).getNodeNumber();
    if (nodeNumber >= 0 && nodeNumber <= nodes.getLast().getNodeNumber()) {
      return states.computeIfAbsent(
          pNode,
          node ->
              new LocationState(
                  checkNotNull(
                      nodes.contains(node) ? node : null,
                      "LocationState for CFANode %s in function %s requested,"
                          + " but this node is not part of the current CFA.",
                      node,
                      node.getFunctionName()),
                  followFunctionCalls));
    }
    // Match LocationStateFactory's handling of nodes added after factory construction.
    return new LocationState(pNode, followFunctionCalls);
  }
}
