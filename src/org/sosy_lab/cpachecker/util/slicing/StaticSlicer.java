// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Static program slicer using a given system dependence graph.
 *
 * <p>Implementation detail: this slicing method is based on "Interprocedural Slicing Using
 * Dependence Graphs" (Horwitz et al.).
 *
 * @see SlicerFactory
 */
public class StaticSlicer extends AbstractSlicer implements StatisticsProvider {

  private final CSystemDependenceGraph sdg;

  private final StatCounter sliceCount = new StatCounter("Number of slicing procedures");
  private final StatTimer slicingTime = new StatTimer(StatKind.SUM, "Time needed for slicing");

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

  private Function<CFAEdge, Iterable<CSystemDependenceGraph.Node>>
      createCfaEdgeToSdgNodesFunction() {

    Multimap<CFAEdge, CSystemDependenceGraph.Node> nodesPerCfaNode = ArrayListMultimap.create();

    for (CSystemDependenceGraph.Node node : sdg.getNodes()) {
      Optional<CFAEdge> optCfaEdge = node.getStatement();
      if (optCfaEdge.isPresent()) {
        nodesPerCfaNode.put(optCfaEdge.orElseThrow(), node);
      }
    }

    return cfaEdge -> nodesPerCfaNode.get(cfaEdge);
  }

  @Override
  public Slice getSlice0(CFA pCfa, Collection<CFAEdge> pSlicingCriteria)
      throws InterruptedException {

    slicingTime.start();

    Set<CFAEdge> criteriaEdges = new LinkedHashSet<>(pSlicingCriteria);

    Set<CSystemDependenceGraph.Node> startNodes = new LinkedHashSet<>();
    Function<CFAEdge, Iterable<CSystemDependenceGraph.Node>> cfaEdgeToSdgNodes =
        createCfaEdgeToSdgNodesFunction();

    for (CFAEdge criteriaEdge : criteriaEdges) {
      Iterables.addAll(startNodes, cfaEdgeToSdgNodes.apply(criteriaEdge));
    }

    Phase1Visitor phase1Visitor = new Phase1Visitor();
    sdg.traverse(startNodes, sdg.createVisitOnceVisitor(phase1Visitor));
    Set<CFAEdge> relevantEdges = new LinkedHashSet<>(phase1Visitor.getRelevantEdges());

    startNodes.clear();
    // the second phase depends on the results of the first phase
    if (partiallyRelevantEdges) {
      startNodes.addAll(phase1Visitor.getVisitedSdgNodes());
    } else {
      for (CFAEdge criteriaEdge : relevantEdges) {
        Iterables.addAll(startNodes, cfaEdgeToSdgNodes.apply(criteriaEdge));
      }
    }

    Phase2Visitor phase2Visitor = new Phase2Visitor(relevantEdges);
    sdg.traverse(startNodes, sdg.createVisitOnceVisitor(phase2Visitor));
    relevantEdges.addAll(phase2Visitor.getRelevantEdges());

    Set<CSystemDependenceGraph.Node> relevantSdgNodes =
        Sets.union(phase1Visitor.getVisitedSdgNodes(), phase2Visitor.getVisitedSdgNodes());
    final Slice slice =
        new SdgProgramSlice(
            pCfa,
            sdg,
            cfaEdgeToSdgNodes,
            ImmutableSet.copyOf(relevantSdgNodes),
            ImmutableSet.copyOf(criteriaEdges),
            ImmutableSet.copyOf(relevantEdges));

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

  private static final class SdgProgramSlice extends AbstractSlice {

    private final CSystemDependenceGraph sdg;
    private final Function<CFAEdge, Iterable<CSystemDependenceGraph.Node>> cfaEdgeToSdgNodes;
    private final ImmutableSet<CSystemDependenceGraph.Node> relevantSdgNodes;

    private final ImmutableSet<ActualNode> relevantActualNodes;

    private SdgProgramSlice(
        CFA pOriginalCfa,
        CSystemDependenceGraph pSdg,
        Function<CFAEdge, Iterable<CSystemDependenceGraph.Node>> pCfaEdgeToSdgNodes,
        ImmutableSet<CSystemDependenceGraph.Node> pRelevantSdgNodes,
        ImmutableCollection<CFAEdge> pCriteriaEdges,
        ImmutableSet<CFAEdge> pRelevantEdges) {
      super(
          pOriginalCfa,
          pCriteriaEdges,
          pRelevantEdges,
          AbstractSlice.computeRelevantDeclarations(
              pRelevantEdges, createRelevantDeclarationFilter(pRelevantSdgNodes)));

      sdg = pSdg;
      cfaEdgeToSdgNodes = pCfaEdgeToSdgNodes;
      relevantSdgNodes = pRelevantSdgNodes;

      relevantActualNodes =
          pRelevantSdgNodes.stream()
              .filter(SdgProgramSlice::isActualNode)
              .map(ActualNode::new)
              .collect(ImmutableSet.toImmutableSet());
    }

    private static boolean isFormalNode(CSystemDependenceGraph.Node pNode) {
      return pNode.getType() == SystemDependenceGraph.NodeType.FORMAL_IN
          || pNode.getType() == SystemDependenceGraph.NodeType.FORMAL_OUT;
    }

    private static boolean isActualNode(CSystemDependenceGraph.Node pNode) {
      return pNode.getType() == SystemDependenceGraph.NodeType.ACTUAL_IN
          || pNode.getType() == SystemDependenceGraph.NodeType.ACTUAL_OUT;
    }

    private static Predicate<ASimpleDeclaration> createRelevantDeclarationFilter(
        ImmutableSet<CSystemDependenceGraph.Node> pRelevantSdgNodes) {

      ImmutableSet<MemoryLocation> relevantFormalVariables =
          pRelevantSdgNodes.stream()
              .filter(SdgProgramSlice::isFormalNode)
              .map(node -> node.getVariable())
              .flatMap(Optional::stream)
              .collect(ImmutableSet.toImmutableSet());

      return declaration -> {
        if (declaration instanceof CParameterDeclaration
            || declaration instanceof CVariableDeclaration) {
          return relevantFormalVariables.contains(MemoryLocation.forDeclaration(declaration));
        } else {
          return true;
        }
      };
    }

    private boolean isInitializerRelevant(CFAEdge pEdge) {

      var declarationEdgeSdgVisitor =
          new CSystemDependenceGraph.ForwardsVisitor() {

            private boolean relevantDef = false;

            private boolean isDefRelevant() {
              return relevantDef;
            }

            @Override
            public SystemDependenceGraph.VisitResult visitNode(CSystemDependenceGraph.Node pNode) {
              return relevantSdgNodes.contains(pNode)
                  ? SystemDependenceGraph.VisitResult.CONTINUE
                  : SystemDependenceGraph.VisitResult.SKIP;
            }

            @Override
            public SystemDependenceGraph.VisitResult visitEdge(
                SystemDependenceGraph.EdgeType pType,
                CSystemDependenceGraph.Node pPredecessor,
                CSystemDependenceGraph.Node pSuccessor) {

              if (relevantSdgNodes.contains(pSuccessor)
                  && pType == SystemDependenceGraph.EdgeType.FLOW_DEPENDENCY) {
                relevantDef = true;
              }

              return SystemDependenceGraph.VisitResult.SKIP;
            }
          };

      sdg.traverse(
          ImmutableSet.copyOf(cfaEdgeToSdgNodes.apply(pEdge)),
          sdg.createVisitOnceVisitor(declarationEdgeSdgVisitor));

      return declarationEdgeSdgVisitor.isDefRelevant();
    }

    @Override
    public boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation) {

      checkNotNull(pEdge, "pEdge must not be null");
      checkNotNull(pMemoryLocation, "pEdge must not be null");
      checkArgument(
          getRelevantEdges().contains(pEdge), "pEdge is not relevant to this program slice");

      if (pEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          return isInitializerRelevant(pEdge);
        }
      } else if (pEdge instanceof CFunctionCallEdge
          || pEdge instanceof CFunctionReturnEdge
          || pEdge instanceof CFunctionSummaryEdge) {
        return relevantActualNodes.contains(new ActualNode(pEdge, pMemoryLocation));
      }

      return true;
    }

    @Override
    public boolean isRelevantUse(CFAEdge pEdge, MemoryLocation pMemoryLocation) {

      checkNotNull(pEdge, "pEdge must not be null");
      checkNotNull(pMemoryLocation, "pEdge must not be null");
      checkArgument(
          getRelevantEdges().contains(pEdge), "pEdge is not relevant to this program slice");

      if (pEdge instanceof CFunctionCallEdge
          || pEdge instanceof CFunctionReturnEdge
          || pEdge instanceof CFunctionSummaryEdge) {
        return relevantActualNodes.contains(new ActualNode(pEdge, pMemoryLocation));
      }

      return true;
    }

    private static final class ActualNode {

      private final CFAEdge edge;
      private final MemoryLocation variable;

      private ActualNode(CFAEdge pEdge, MemoryLocation pVariable) {
        edge = pEdge;
        variable = pVariable;
      }

      private ActualNode(CSystemDependenceGraph.Node pNode) {
        this(pNode.getStatement().orElseThrow(), pNode.getVariable().orElseThrow());
      }

      @Override
      public int hashCode() {
        return Objects.hash(edge, variable);
      }

      @Override
      public boolean equals(Object pObject) {

        if (this == pObject) {
          return true;
        }

        if (!(pObject instanceof ActualNode)) {
          return false;
        }

        ActualNode other = (ActualNode) pObject;

        return Objects.equals(edge, other.edge) && Objects.equals(variable, other.variable);
      }
    }
  }

  /**
   * SDG visitor for the first phase of interprocedural slicing.
   *
   * <p>During the first phase, the SDG is traversed backwards, starting from the criteria edges,
   * while only calling procedures are visited (don't "descend" into called procedures). For a more
   * comprehensive description, see "Interprocedural Slicing Using Dependence Graphs" (Horwitz et
   * al.).
   */
  private static final class Phase1Visitor implements CSystemDependenceGraph.BackwardsVisitor {

    private final Set<CFAEdge> relevantEdges;
    private final Set<CSystemDependenceGraph.Node> visitedSdgNodes;

    private Phase1Visitor() {
      relevantEdges = new LinkedHashSet<>();
      visitedSdgNodes = new LinkedHashSet<>();
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
   * SDG visitor for the second phase of interprocedural slicing.
   *
   * <p>During the second phase, the SDG is traversed backwards, starting from the SDG nodes visited
   * during the first phase, while only called procedures are visited (don't "ascend" into calling
   * procedures). For a more comprehensive description, see "Interprocedural Slicing Using
   * Dependence Graphs" (Horwitz et al.).
   */
  private static final class Phase2Visitor implements CSystemDependenceGraph.BackwardsVisitor {

    private final Set<CFAEdge> relevantEdges;
    private final Set<CSystemDependenceGraph.Node> visitedSdgNodes;

    private Phase2Visitor(Set<CFAEdge> pRelevantEdges) {
      relevantEdges = new LinkedHashSet<>(pRelevantEdges);
      visitedSdgNodes = new LinkedHashSet<>();
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

      // don't "ascend" into calling procedures
      if (pSuccessor.getType() == SystemDependenceGraph.NodeType.FORMAL_IN
          || pType == SystemDependenceGraph.EdgeType.CALL_EDGE) {
        return SystemDependenceGraph.VisitResult.SKIP;
      }

      return SystemDependenceGraph.VisitResult.CONTINUE;
    }
  }
}
