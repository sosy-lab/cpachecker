// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Static program slicer based on a given system dependence graph.
 *
 * <p>For a given slicing criterion CFA edge g, the slice consists of all CFA edges that influences
 * the values of variables used by g and whether g get executed.
 *
 * <p>Implementation detail: this slicing method is based on "Interprocedural Slicing Using
 * Dependence Graphs" (Horwitz et al.).
 *
 * @see SlicerFactory
 */
public class StaticSlicer extends AbstractSlicer implements StatisticsProvider {

  private CSystemDependenceGraph sdg;

  private StatCounter sliceCount = new StatCounter("Number of slicing procedures");
  private StatTimer slicingTime = new StatTimer(StatKind.SUM, "Time needed for slicing");

  private final StatInt sliceEdgesNumber =
      new StatInt(StatKind.MAX, "Number of relevant slice edges");
  private final StatInt programEdgesNumber = new StatInt(StatKind.MAX, "Number of program edges");

  private final boolean partiallyRelevantEdges;

  StaticSlicer(
      SlicingCriteriaExtractor pExtractor,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig,
      CSystemDependenceGraph pSdg,
      boolean pPartiallyRelevantEdges)
      throws InvalidConfigurationException {
    super(pExtractor, pLogger, pShutdownNotifier, pConfig);

    if (pSdg == null) {
      throw new InvalidConfigurationException("Dependence graph required, but missing");
    }

    sdg = pSdg;
    partiallyRelevantEdges = pPartiallyRelevantEdges;
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

  private Multimap<CFAEdge, CSystemDependenceGraph.Node> getNodesPerCfaEdge() {

    Multimap<CFAEdge, CSystemDependenceGraph.Node> nodesPerCfaNode = ArrayListMultimap.create();

    for (CSystemDependenceGraph.Node node : sdg.getNodes()) {
      Optional<CFAEdge> optCfaEdge = node.getStatement();
      if (optCfaEdge.isPresent()) {
        nodesPerCfaNode.put(optCfaEdge.orElseThrow(), node);
      }
    }

    return nodesPerCfaNode;
  }

  @Override
  public Slice getSlice0(CFA pCfa, Collection<CFAEdge> pSlicingCriteria)
      throws InterruptedException {

    slicingTime.start();

    Set<CFAEdge> criteriaEdges = new HashSet<>(pSlicingCriteria);

    // TODO: make this configurable
    if (!criteriaEdges.isEmpty()) {
      criteriaEdges.addAll(getAbortCallEdges(pCfa));
    }

    Set<CSystemDependenceGraph.Node> startNodes = new HashSet<>();
    Multimap<CFAEdge, CSystemDependenceGraph.Node> nodesPerCfaEdge = getNodesPerCfaEdge();

    for (CFAEdge criteriaEdge : criteriaEdges) {
      startNodes.addAll(nodesPerCfaEdge.get(criteriaEdge));
    }

    Phase1Visitor phase1Visitor = new Phase1Visitor();
    sdg.traverse(startNodes, sdg.createVisitOnceVisitor(phase1Visitor));
    Set<CFAEdge> relevantEdges = new HashSet<>(phase1Visitor.getRelevantEdges());

    startNodes.clear();
    // phase 2 start with the result from phase 1
    if (partiallyRelevantEdges) {
      startNodes.addAll(phase1Visitor.getVisitedSdgNodes());
    } else {
      for (CFAEdge criteriaEdge : relevantEdges) {
        startNodes.addAll(nodesPerCfaEdge.get(criteriaEdge));
      }
    }

    Phase2Visitor phase2Visitor = new Phase2Visitor(relevantEdges);
    sdg.traverse(startNodes, sdg.createVisitOnceVisitor(phase2Visitor));
    relevantEdges.addAll(phase2Visitor.getRelevantEdges());

    final Slice slice =
        new StaticSlicerSlice(
            pCfa, ImmutableSet.copyOf(criteriaEdges), ImmutableSet.copyOf(relevantEdges));

    slicingTime.stop();
    sliceCount.inc();

    sliceEdgesNumber.setNextValue(relevantEdges.size());
    if (programEdgesNumber.getValueCount() == 0) {
      programEdgesNumber.setNextValue(countProgramEdges(pCfa));
    }

      return slice;
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
            writer.put(sliceCount).put(slicingTime);

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

  /**
   * Represents a SDG visitor for slicing phase 1.
   *
   * <p>{@code CritP}: all procedures that contain a criteria edges
   *
   * <p>{@code CallP}: all procedures that directly or transitively call a procedure in {@code
   * CritP}
   *
   * <p>Phase 1 identifies SDG nodes that can reach any criteria edge and are either from {@code p,
   * p in CritP}, or from {@code p', p' in CritP}. For a more comprehensive description, see
   * "Interprocedural Slicing Using Dependence Graphs" (Horwitz et al.).
   */
  private static final class Phase1Visitor implements CSystemDependenceGraph.BackwardsVisitor {

    private final Set<CFAEdge> relevantEdges;
    private final Set<CSystemDependenceGraph.Node> visitedSdgNodes;

    private Phase1Visitor() {
      relevantEdges = new HashSet<>();
      visitedSdgNodes = new HashSet<>();
    }

    private Set<CFAEdge> getRelevantEdges() {
      return relevantEdges;
    }

    private Set<CSystemDependenceGraph.Node> getVisitedSdgNodes() {
      return visitedSdgNodes;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitNode(CSystemDependenceGraph.Node pNode) {

      visitedSdgNodes.add(pNode);
      pNode.getStatement().ifPresent(relevantEdges::add);

      return SystemDependenceGraph.VisitResult.CONTINUE;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitEdge(
        SystemDependenceGraph.EdgeType pType,
        CSystemDependenceGraph.Node pPredecessor,
        CSystemDependenceGraph.Node pSuccessor) {

      // don't "descend" into called procedures
      if (pPredecessor.getType() == SystemDependenceGraph.NodeType.FORMAL_OUT) {
        return SystemDependenceGraph.VisitResult.SKIP;
      }

      return SystemDependenceGraph.VisitResult.CONTINUE;
    }
  }

  /**
   * Represents a SDG visitor for slicing phase 2.
   *
   * <p>{@code CritP}: all procedures that contain a criteria edges
   *
   * <p>{@code CallP}: all procedures that directly or transitively call a procedure in {@code
   * CritP}
   *
   * <p>Phase 2 identifies SDG nodes that can reach any criteria edge and are from procedures
   * (transitively) called inside {@code p, p in CritP}, or from procedures called inside {@code p',
   * p' in CallP}. For a more comprehensive description, see "Interprocedural Slicing Using
   * Dependence Graphs" (Horwitz et al.).
   */
  private static final class Phase2Visitor implements CSystemDependenceGraph.BackwardsVisitor {

    private final Set<CFAEdge> relevantEdges;

    private Phase2Visitor(Set<CFAEdge> pRelevantEdges) {
      relevantEdges = new HashSet<>(pRelevantEdges);
    }

    private Set<CFAEdge> getRelevantEdges() {
      return relevantEdges;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitNode(CSystemDependenceGraph.Node pNode) {

      pNode.getStatement().ifPresent(relevantEdges::add);

      return SystemDependenceGraph.VisitResult.CONTINUE;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitEdge(
        SystemDependenceGraph.EdgeType pType,
        CSystemDependenceGraph.Node pPredecessor,
        CSystemDependenceGraph.Node pSuccessor) {

      // don't "ascend" into calling procedures
      if (pSuccessor.getType() == SystemDependenceGraph.NodeType.FORMAL_IN
          || pType == EdgeType.CALL_EDGE) {
        return SystemDependenceGraph.VisitResult.SKIP;
      }

      return SystemDependenceGraph.VisitResult.CONTINUE;
    }
  }
}
