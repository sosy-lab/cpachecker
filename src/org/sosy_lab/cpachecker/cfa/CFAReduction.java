/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import static org.sosy_lab.cpachecker.util.AbstractElements.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFA;

import com.google.common.collect.ImmutableSet;


/**
 * Perform a (very) simple cone-of-influence reduction on the given CFA.
 * That is, get rid of all the nodes/edges that are not reachable from the
 * error location(s) and assert(s).
 *
 * In fact, this should probably *not* be called ConeOfInfluenceCFAReduction,
 * since it is *much* more trivial (and less powerful) than that.
 */
@Options(prefix="cfa.pruning")
public class CFAReduction {

  private final Configuration config;
  private final LogManager logger;

  public CFAReduction(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    if (config.getProperty("specification") == null) {
      throw new InvalidConfigurationException("Option cfa.removeIrrelevantForErrorLocations is only valid if a specification is given!");
    }

    this.config = config;
    this.logger = logger;
  }


  public void removeIrrelevantForErrorLocations(final CFAFunctionDefinitionNode cfa) throws InterruptedException {
    Set<CFANode> allNodes = CFA.allNodes(cfa, true);

    Set<CFANode> errorNodes = getErrorNodesWithCPA(cfa, allNodes);

    if (errorNodes.isEmpty()) {
      // shortcut, all nodes are irrelevant

      // remove all outgoing edges of first node
      for (int i = cfa.getNumLeavingEdges() - 1; i >= 0; i--) {
        cfa.removeLeavingEdge(cfa.getLeavingEdge(i));
      }
      cfa.addLeavingSummaryEdge(null);
      return;
    }

    if (errorNodes.size() == allNodes.size()) {
      // shortcut, no node is irrelevant
      return;
    }

    // backwards search to determine all relevant nodes
    Set<CFANode> relevantNodes = new HashSet<CFANode>();
    for (CFANode n : errorNodes) {
      CFA.dfs(n, relevantNodes, true, true);
    }

    assert allNodes.containsAll(relevantNodes) : "Inconsistent CFA";

    logger.log(Level.INFO, "Detected", allNodes.size()-relevantNodes.size(), "irrelevant CFA nodes.");

    if (relevantNodes.size() == allNodes.size()) {
      // shortcut, no node is irrelevant
      return;
    }

    // now detach all the nodes not visited
    pruneIrrelevantNodes(allNodes, relevantNodes, errorNodes);
  }

  private Set<CFANode> getErrorNodesWithCPA(CFAFunctionDefinitionNode cfa, Set<CFANode> allNodes) throws InterruptedException {
    try {
      // create new configuration based on existing config but with default set of CPAs
      Configuration lConfig = Configuration.builder()
                                           .copyFrom(config)
                                           .setOption("output.disable", "true")
                                           .clearOption("cpa")
                                           .clearOption("cpas")
                                           .clearOption("CompositeCPA.cpas")
                                           .build();
      CPABuilder lBuilder = new CPABuilder(lConfig, logger);
      ConfigurableProgramAnalysis lCpas = lBuilder.buildCPAs();
      Algorithm lAlgorithm = new CPAAlgorithm(lCpas, logger);
      PartitionedReachedSet lReached = new PartitionedReachedSet(TraversalMethod.DFS);
      lReached.add(lCpas.getInitialElement(cfa), lCpas.getInitialPrecision(cfa));

      lAlgorithm.run(lReached);

      return ImmutableSet.copyOf(extractLocations(filterTargetElements(lReached)));

    } catch (CPAException e) {
      logger.log(Level.WARNING, "Error during CFA reduction, using full CFA");
      logger.logException(Level.ALL, e, "");
    } catch (InvalidConfigurationException e) {
      logger.log(Level.WARNING, "Error during CFA reduction, using full CFA");
      logger.logException(Level.ALL, e, "");
    }
    return allNodes;
  }


  private void pruneIrrelevantNodes(Set<CFANode> allNodes,
      Set<CFANode> relevantNodes, Set<CFANode> errorNodes) {
    for (CFANode n : allNodes) {
      if (!relevantNodes.contains(n)) {

        // check if node is successor of error node and remove incoming edges
        for (int edgeIndex = n.getNumEnteringEdges() - 1; edgeIndex >= 0; edgeIndex--) {
          CFAEdge removedEdge = n.getEnteringEdge(edgeIndex);
          CFANode prevNode = removedEdge.getPredecessor();

          if (!errorNodes.contains(prevNode)) {
            // do not remove the direct successors of error nodes

            prevNode.removeLeavingEdge(removedEdge);
            n.removeEnteringEdge(removedEdge);
          }
        }

        // remove all outgoing edges
        while (n.getNumLeavingEdges() > 0) {
          CFAEdge removedEdge = n.getLeavingEdge(0);
          CFANode succNode = removedEdge.getSuccessor();
          n.removeLeavingEdge(removedEdge);
          succNode.removeEnteringEdge(removedEdge);
        }
        n.addEnteringSummaryEdge(null);
        n.addLeavingSummaryEdge(null);
      }
    }
  }
}
