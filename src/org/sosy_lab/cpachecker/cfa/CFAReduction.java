/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;


/**
 * Perform a (very) simple cone-of-influence reduction on the given CFA.
 * That is, get rid of all the nodes/edges that are not reachable from the
 * potential error states (according to the specification).
 *
 * In fact, this should probably *not* be called ConeOfInfluenceCFAReduction,
 * since it is *much* more trivial (and less powerful) than that.
 */
@Options(prefix="cfa.pruning")
public class CFAReduction {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public CFAReduction(Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);

    if (config.getProperty("specification") == null) {
      throw new InvalidConfigurationException("Option cfa.removeIrrelevantForSpecification is only valid if a specification is given!");
    }

    this.config = config;
    this.logger = logger;
    this.shutdownNotifier = pShutdownNotifier;
  }


  public void removeIrrelevantForSpecification(final MutableCFA cfa) throws InterruptedException {
    Collection<CFANode> errorNodes = getErrorNodesWithCPA(cfa);

    if (errorNodes.isEmpty()) {
      // shortcut, all nodes are irrelevant
      cfa.clear();
      return;
    }

    Collection<CFANode> allNodes = cfa.getAllNodes();

    if (errorNodes.size() == allNodes.size()) {
      // shortcut, no node is irrelevant
      return;
    }

    CFATraversal.NodeCollectingCFAVisitor cfaVisitor = new CFATraversal.NodeCollectingCFAVisitor();
    CFATraversal traversal = CFATraversal.dfs().backwards();
    // backwards search to determine all relevant nodes
    for (CFANode n : errorNodes) {
      traversal.traverse(n, cfaVisitor);
    }
    Set<CFANode> relevantNodes = cfaVisitor.getVisitedNodes();

    assert allNodes.containsAll(relevantNodes) : "Inconsistent CFA";

    int numIrrelevantNodes = allNodes.size() - relevantNodes.size();

    logger.log(Level.INFO, "Detected", numIrrelevantNodes, "irrelevant CFA nodes.");

    if (numIrrelevantNodes == 0) {
      // shortcut, no node is irrelevant
      return;
    }

    Predicate<CFANode> irrelevantNode = Predicates.not(Predicates.in(relevantNodes));
    Collection<CFANode> removedNodes = ImmutableList.copyOf(Collections2.filter(allNodes, irrelevantNode));

    // now detach all the nodes not visited
    pruneIrrelevantNodes(cfa, removedNodes, errorNodes);
  }

  private Collection<CFANode> getErrorNodesWithCPA(MutableCFA cfa) throws InterruptedException {
    try {
      ReachedSetFactory lReachedSetFactory = new ReachedSetFactory(Configuration.defaultConfiguration(), logger);

      // create new configuration based on existing config but with default set of CPAs
      Configuration lConfig = Configuration.builder()
                                           .copyFrom(config)
                                           .setOption("output.disable", "true")
                                           .clearOption("cpa")
                                           .clearOption("cpas")
                                           .clearOption("CompositeCPA.cpas")
                                           .build();

      CPABuilder lBuilder = new CPABuilder(lConfig, logger, shutdownNotifier, lReachedSetFactory);
      ConfigurableProgramAnalysis lCpas = lBuilder.buildCPAs(cfa);
      Algorithm lAlgorithm = CPAAlgorithm.create(lCpas, logger, lConfig, shutdownNotifier);
      ReachedSet lReached = lReachedSetFactory.create();
      lReached.add(lCpas.getInitialState(cfa.getMainFunction()), lCpas.getInitialPrecision(cfa.getMainFunction()));

      lAlgorithm.run(lReached);

      CPAs.closeCpaIfPossible(lCpas, logger);
      CPAs.closeIfPossible(lAlgorithm, logger);

      return from(lReached)
               .filter(IS_TARGET_STATE)
               .transform(EXTRACT_LOCATION)
               .toSet();

    } catch (CPAException e) {
      logger.logUserException(Level.WARNING, e, "Error during CFA reduction, using full CFA");
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.WARNING, e, "Invalid configuration used for CFA reduction, using full CFA");
    }
    return cfa.getAllNodes();
  }


  private void pruneIrrelevantNodes(MutableCFA cfa, Collection<CFANode> irrelevantNodes,
      Collection<CFANode> errorNodes) {

    for (CFANode n : irrelevantNodes) {
      cfa.removeNode(n);

      // check if node is successor of error node and remove incoming edges
      for (int edgeIndex = n.getNumEnteringEdges() - 1; edgeIndex >= 0; edgeIndex--) {
        CFAEdge removedEdge = n.getEnteringEdge(edgeIndex);
        CFANode prevNode = removedEdge.getPredecessor();

        if (!errorNodes.contains(prevNode)) {
          // do not remove the direct successors of error nodes

          CFACreationUtils.removeEdgeFromNodes(removedEdge);
        }
      }

      // remove all outgoing edges
      while (n.getNumLeavingEdges() > 0) {
        CFACreationUtils.removeEdgeFromNodes(n.getLeavingEdge(0));
      }

      // remove all summary edges
      if (n.getEnteringSummaryEdge() != null) {
        CFACreationUtils.removeSummaryEdgeFromNodes(n.getEnteringSummaryEdge());
      }
      if (n.getLeavingSummaryEdge() != null) {
        CFACreationUtils.removeSummaryEdgeFromNodes(n.getLeavingSummaryEdge());
      }
    }
  }
}
