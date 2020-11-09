// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
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
public class StaticSlicer extends AbstractSlicer implements StatisticsProvider {

  private DependenceGraph depGraph;

  private StatInt candidateSliceCount =
      new StatInt(StatKind.SUM, "Number of proposed slicing " + "procedures");
  private StatInt sliceCount = new StatInt(StatKind.SUM, "Number of slicing procedures");
  private StatTimer slicingTime = new StatTimer(StatKind.SUM, "Time needed for slicing");

  private final StatInt sliceEdgesNumber =
      new StatInt(StatKind.SUM, "Number of relevant slice edges");
  private final StatInt programEdgesNumber = new StatInt(StatKind.SUM, "Number of program edges");

  StaticSlicer(
      SlicingCriteriaExtractor pExtractor,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pExtractor, pLogger, pShutdownNotifier, pConfig);

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

      final Slice slice = new Slice(pCfa, relevantEdges, pSlicingCriteria);
      slicingTime.stop();

      sliceEdgesNumber.setNextValue(relevantEdges.size());
      if (programEdgesNumber.getValueCount() == 0) {
        programEdgesNumber.setNextValue(countProgramEdges(pCfa));
      }

      return slice;

    } finally {
      sliceCount.setNextValue(realSlices);
    }
  }

  private int countProgramEdges(CFA pCfa) {

    int programEdgeCounter = 0;
    for (CFANode node : pCfa.getAllNodes()) {
      programEdgeCounter += CFAUtils.allLeavingEdges(node).size();
    }

    return programEdgeCounter;
  }

  private double getSliceProgramRatio() {

    double sliceEdges = sliceEdgesNumber.getMaxValue();
    double programEdges = programEdgesNumber.getMaxValue();

    return programEdges > 0.0 ? sliceEdges / programEdges : 1.0;
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

            writer.put(sliceEdgesNumber).put(programEdgesNumber);
            writer.put(
                "Largest slice / program ratio",
                String.format(Locale.US, "%.3f", getSliceProgramRatio()));
          }

          @Override
          public String getName() {
            return StaticSlicer.class.getSimpleName();
          }
        });
  }
}
