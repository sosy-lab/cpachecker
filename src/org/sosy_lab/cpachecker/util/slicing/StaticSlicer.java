/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.TraversalDirection;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Static program slicer based on a given dependence graph.
 *
 * <p>For a given slicing criterion CFA edge g and a dependence graph, the slice consists of all CFA
 * edges reachable in the dependence graph through backwards-traversal from g.
 *
 * @see SlicerFactory
 */
@Options(prefix = "slicing")
public class StaticSlicer extends AbstractSlicer implements StatisticsProvider {

  @Option(secure = true, name = "preserveTargetPaths",
      description = "Whether to create slices that are behaviorally equivalent not only to "
          + "the target location, but also on the paths to that target location.")
  private boolean preserveTargetPaths = false;

  private DependenceGraph depGraph;

  private StatInt candidateSliceCount =
      new StatInt(StatKind.SUM, "Number of proposed slicing " + "procedures");
  private StatInt sliceCount = new StatInt(StatKind.SUM, "Number of slicing procedures");
  private StatTimer slicingTime = new StatTimer(StatKind.SUM, "Time needed for slicing");

  StaticSlicer(
      SlicingCriteriaExtractor pExtractor,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pExtractor, pLogger, pShutdownNotifier, pConfig);

    pConfig.inject(this);

    depGraph =
        pCfa.getDependenceGraph()
            .orElseThrow(
                () -> new InvalidConfigurationException("Dependence graph required, but missing"));

  }

  @Override
  public Slice getSlice0(CFA pCfa, Collection<CFAEdge> pSlicingCriteria)
      throws InterruptedException {
    candidateSliceCount.setNextValue(pSlicingCriteria.size());
    int realSlices = 0;
    slicingTime.start();
    Set<CFAEdge> relevantEdges = new HashSet<>();
    try {
      // Heuristic: Reverse to make states that are deeper in the path first - these
      // have a higher chance of including earlier states in their dependences
      ImmutableList<CFAEdge> criteriaEdges =
          ImmutableList.sortedCopyOf(
              Comparator.comparingInt(edge -> edge.getPredecessor().getReversePostorderId()),
              pSlicingCriteria);

      for (CFAEdge g : criteriaEdges) {
        if (relevantEdges.contains(g)) {
          // If the relevant edges contain g, then all dependences of g are also already included
          // and we can skip it (this is only true as long as no function call/return edge is a
          // criterion!)
          continue;
        } else {
          realSlices++;
        }
        relevantEdges.addAll(depGraph.getReachable(g, TraversalDirection.BACKWARD));
      }

      if (preserveTargetPaths) {
        // we do this only after we computed the slices for all slicing criteria,
        // because this would otherwise disturb our optimization above that stops
        // if a criterion is already part of the criteria edges (because we don't
        // add any dependences for assumptions on cfa paths)
        for (CFAEdge g : criteriaEdges) {
          EdgeCollectingCFAVisitor visitor = new EdgeCollectingCFAVisitor();
          CFATraversal.dfs().backwards().traverseOnce(g.getPredecessor(), visitor);
          Set<CFAEdge> assumptions =
              visitor
                  .getVisitedEdges()
                  .stream()
                  .filter(x -> x.getEdgeType().equals(CFAEdgeType.AssumeEdge))
                  .collect(Collectors.toSet());
          relevantEdges.addAll(assumptions);
        }
      }

      final Slice slice = new Slice(pCfa, relevantEdges, pSlicingCriteria);
      slicingTime.stop();
      return slice;

    } finally {
      sliceCount.setNextValue(realSlices);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {

            StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
            writer.put(candidateSliceCount).put(sliceCount).put(slicingTime);
          }

          @Override
          public String getName() {
            return StaticSlicer.class.getSimpleName();
          }
        });
  }
}
