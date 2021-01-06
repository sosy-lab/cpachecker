// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
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

  private SystemDependenceGraph<CFAEdge, MemoryLocation> depGraph;

  private StatInt candidateSliceCount =
      new StatInt(StatKind.SUM, "Number of proposed slicing " + "procedures");
  private StatInt sliceCount = new StatInt(StatKind.SUM, "Number of slicing procedures");
  private StatTimer slicingTime = new StatTimer(StatKind.SUM, "Time needed for slicing");

  private final StatInt sliceEdgesNumber =
      new StatInt(StatKind.MAX, "Number of relevant slice edges");
  private final StatInt programEdgesNumber = new StatInt(StatKind.MAX, "Number of program edges");

  StaticSlicer(
      SlicingCriteriaExtractor pExtractor,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig,
      SystemDependenceGraph<CFAEdge, MemoryLocation> pDependenceGraph)
      throws InvalidConfigurationException {
    super(pExtractor, pLogger, pShutdownNotifier, pConfig);

    if (pDependenceGraph == null) {
      throw new InvalidConfigurationException("Dependence graph required, but missing");
    }

    depGraph = pDependenceGraph;
  }

  private static Set<CFAEdge> getAbortCallEdges(CFA pCfa) {

    Set<CFAEdge> abortCallEdges = new HashSet<>();

    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (edge instanceof CStatementEdge) {
          CStatement statement = ((CStatementEdge) edge).getStatement();
          if (statement instanceof CFunctionCallStatement) {
            CFunctionDeclaration declaration =
                ((CFunctionCallStatement) statement).getFunctionCallExpression().getDeclaration();
            if (declaration != null && declaration.getQualifiedName().equals("abort")) {
              abortCallEdges.add(edge);
            }
          }
        }
      }
    }

    return abortCallEdges;
  }

  @Override
  public Slice getSlice0(CFA pCfa, Collection<CFAEdge> pSlicingCriteria)
      throws InterruptedException {

    candidateSliceCount.setNextValue(pSlicingCriteria.size());
    int realSlices = 0;
    slicingTime.start();

    Set<CFAEdge> criteriaEdges = new HashSet<>();
    Set<CFAEdge> relevantEdges = new HashSet<>();

    criteriaEdges.addAll(pSlicingCriteria);

    // TODO: make this configurable
    if (!criteriaEdges.isEmpty()) {
      criteriaEdges.addAll(getAbortCallEdges(pCfa));
    }

    try {
      // Heuristic: Reverse to make states that are deeper in the path first - these
      // have a higher chance of including earlier states in their dependences
      ImmutableList<CFAEdge> sortedCriteriaEdges =
          ImmutableList.sortedCopyOf(
              Comparator.comparingInt(edge -> edge.getPredecessor().getReversePostorderId()),
              criteriaEdges);

      for (CFAEdge g : sortedCriteriaEdges) {
        if (relevantEdges.contains(g)) {
          // If the relevant edges contain g, then all dependences of g are also already included
          // and we can skip it (this is only true as long as no function call/return edge is a
          // criterion!)
          continue;
        } else {
          realSlices++;
        }

        depGraph.traverseOnce(
            SystemDependenceGraph.Direction.BACKWARDS,
            Set.of(depGraph.getNode(NodeType.STATEMENT, g, Optional.empty())),
            new SystemDependenceGraph.Visitor<CFAEdge, MemoryLocation>() {

              @Override
              public VisitResult visitNode(Node<CFAEdge, MemoryLocation> pNode) {

                relevantEdges.add(pNode.getStatement());

                return SystemDependenceGraph.VisitResult.CONTINUE;
              }

              @Override
              public VisitResult visitEdge(
                  EdgeType pType,
                  Node<CFAEdge, MemoryLocation> pPredecessor,
                  Node<CFAEdge, MemoryLocation> pSuccessor) {
                return SystemDependenceGraph.VisitResult.CONTINUE;
              }
            });
      }

      final Slice slice =
          new StaticSlicerSlice(
              pCfa, ImmutableSet.copyOf(criteriaEdges), ImmutableSet.copyOf(relevantEdges));
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

  private static final class StaticSlicerSlice implements Slice {

    private final CFA originalCfa;
    private final ImmutableCollection<CFAEdge> criteriaEdges;
    private final ImmutableSet<CFAEdge> relevantEdges;

    private StaticSlicerSlice(
        CFA pOriginalCfa,
        ImmutableCollection<CFAEdge> pCriteriaEdges,
        ImmutableSet<CFAEdge> pRelevantEdges) {
      originalCfa = pOriginalCfa;
      criteriaEdges = pCriteriaEdges;
      relevantEdges = pRelevantEdges;
    }

    @Override
    public CFA getOriginalCfa() {
      return originalCfa;
    }

    @Override
    public ImmutableCollection<CFAEdge> getUsedCriteria() {
      return criteriaEdges;
    }

    @Override
    public ImmutableSet<CFAEdge> getRelevantEdges() {
      return relevantEdges;
    }

    @Override
    public boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation) {
      return true;
    }
  }
}
