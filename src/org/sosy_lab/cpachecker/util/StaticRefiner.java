// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Abstract base class for static refinement approaches. */
@Options(prefix = "staticRefiner")
public abstract class StaticRefiner {

  @Option(
      secure = true,
      description =
          "collect at most this number of assumes along a path, backwards from each target (="
              + " error) location")
  private int maxBackscanPathAssumes = 1;

  protected final LogManager logger;

  protected StaticRefiner(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    logger = pLogger;

    pConfig.inject(this, StaticRefiner.class);
  }

  /**
   * This method finds in a backwards search, starting from the target locations in the CFA, the
   * list of n assume edges preceeding each target node, where n equals the maxBackscanPathAssumes
   * option.
   *
   * @return the mapping from target nodes to the corresponding preceeding assume edges
   */
  protected ListMultimap<CFANode, AssumeEdge> getTargetLocationAssumes(
      Collection<CFANode> targetNodes) {
    ListMultimap<CFANode, AssumeEdge> result = ArrayListMultimap.create();
    if (targetNodes.isEmpty()) {
      return result;
    }

    // backwards search to determine all relevant edges
    for (CFANode targetNode : targetNodes) {
      Deque<Pair<CFANode, Integer>> queue = new ArrayDeque<>();
      queue.add(Pair.of(targetNode, 0));
      Set<CFANode> explored = new HashSet<>();

      while (!queue.isEmpty()) {
        // Take the next node that should be explored from the queue
        Pair<CFANode, Integer> v = queue.pop();

        // Each node that enters node v
        for (CFAEdge e : CFAUtils.enteringEdges(v.getFirst())) {
          CFANode u = e.getPredecessor();

          boolean isAssumeEdge = (e instanceof AssumeEdge);
          int depthIncrease = isAssumeEdge ? 1 : 0;

          if (isAssumeEdge) {
            AssumeEdge assume = (AssumeEdge) e;
            if (v.getSecond() < maxBackscanPathAssumes) {
              result.put(targetNode, assume);
            } else {
              continue;
            }
          }

          if (!explored.contains(u)) {
            queue.add(Pair.of(u, v.getSecond() + depthIncrease));
          }
        }

        explored.add(v.getFirst());
      }
    }

    return result;
  }
}
