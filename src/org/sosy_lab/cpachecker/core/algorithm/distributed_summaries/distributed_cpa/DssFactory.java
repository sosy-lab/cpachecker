// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DistributedLocationCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DssFactory {

  /**
   * What the analyses of all blocks need to know about the whole program.
   *
   * @param types the types of all variables and functions
   * @param memoryShare the share of the CFA edges with a non-trivial encoding that access memory
   *     through the pointer-target symbols of the pointer-aliasing encoding, from 0 to 1
   */
  record ProgramFacts(ImmutableMap<String, Type> types, double memoryShare) {}

  static final class TypeAndLocationCache {

    private static final Map<CFA, ProgramFacts> cachedProgramFacts = new LinkedHashMap<>();
    private static final Map<CFA, ImmutableBiMap<Integer, CFANode>> integerToNodeMap =
        new LinkedHashMap<>();

    /**
     * The prefix of the pointer-target symbols of the pointer-aliasing encoding, see {@code
     * TypeHandlerWithPointerAliasing#isPointerAccessSymbol}. No C identifier can start with it.
     */
    private static final String POINTER_TARGET_PREFIX = "*";

    private TypeAndLocationCache() {}

    static synchronized ProgramFacts getOrCreateProgramFacts(
        CFA pCFA,
        Configuration pConfiguration,
        LogManager pLogManager,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPATransferException, InterruptedException {
      ProgramFacts cached = cachedProgramFacts.get(pCFA);
      if (cached == null) {
        // computeIfAbsent cannot be used here because computeProgramFacts throws checked exceptions
        // that the functional interface cannot propagate.
        cached = computeProgramFacts(pCFA, pConfiguration, pLogManager, pShutdownNotifier);
        pLogManager.logf(
            Level.INFO,
            "%.1f%% of the encoded CFA edges access modelled memory.",
            100 * cached.memoryShare());
        cachedProgramFacts.put(pCFA, cached);
      }
      return cached;
    }

    /**
     * Get a mapping from variable and function names to their types, and the share of the edges
     * that access memory through pointer-target symbols.
     *
     * @param pCfa CFA to get the mapping for
     * @param pConfiguration configuration to create the solver for the path formula manager
     * @param pLogManager log manager to create the solver for the path formula manager
     * @param pShutdownNotifier shutdown notifier to create the solver for the path formula manager
     * @return the types of all variables and functions, and the share of memory accesses
     * @throws InvalidConfigurationException if the configuration is invalid for the solver
     * @throws CPATransferException if the path formula manager cannot create a path formula for the
     *     given CFA
     * @throws InterruptedException if the thread is interrupted while creating the path formula
     */
    private static ProgramFacts computeProgramFacts(
        CFA pCfa,
        Configuration pConfiguration,
        LogManager pLogManager,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPATransferException, InterruptedException {
      try (Solver solver = Solver.create(pConfiguration, pLogManager, pShutdownNotifier)) {
        PathFormulaManagerImpl pfm =
            new PathFormulaManagerImpl(
                solver.getFormulaManager(),
                pConfiguration,
                pLogManager,
                pShutdownNotifier,
                pCfa,
                AnalysisDirection.FORWARD);
        FormulaManagerView fmgr = solver.getFormulaManager();
        PathFormula pathFormula = pfm.makeEmptyPathFormula();
        int encodedEdges = 0;
        int memoryEdges = 0;
        for (CFAEdge edge : pCfa.edges()) {
          try {
            PathFormula next = pfm.makeAnd(pathFormula, edge);
            // The formula of the previous edge was dropped, so this is the formula of this edge.
            BooleanFormula edgeFormula = next.getFormula();
            if (!fmgr.getBooleanFormulaManager().isTrue(edgeFormula)) {
              encodedEdges++;
              if (fmgr.extractFunctionNames(edgeFormula).stream()
                  .anyMatch(name -> name.startsWith(POINTER_TARGET_PREFIX))) {
                memoryEdges++;
              }
            }
            // Only the types in the SSA map are needed. Keeping the conjunction of all edges of
            // the program would let the formula grow with the program for nothing.
            pathFormula =
                pfm.makeEmptyPathFormulaWithContext(next.getSsa(), next.getPointerTargetSet());
          } catch (UnrecognizedCodeException e) {
            // this code might never be executed, so we continue.
          }
        }
        return new ProgramFacts(
            ImmutableMap.copyOf(
                Maps.toMap(pathFormula.getSsa().allVariables(), pathFormula.getSsa()::getType)),
            encodedEdges == 0 ? 0 : (double) memoryEdges / encodedEdges);
      }
    }

    static synchronized BiMap<Integer, CFANode> getOrCreateLocationMapping(CFA pCFA) {
      if (!integerToNodeMap.containsKey(pCFA)) {
        ImmutableMap<Integer, CFANode> nodeMap =
            ImmutableMap.copyOf(CFAUtils.getMappingFromNodeIDsToCFANodes(pCFA));

        int minCfaNodeNumber = nodeMap.keySet().stream().min(Integer::compareTo).orElseThrow();

        // All node IDs are shifted such that they start from 0
        BiMap<Integer, CFANode> cfaNodeIdMap = HashBiMap.create();

        for (Map.Entry<Integer, CFANode> entry : nodeMap.entrySet()) {
          int index = entry.getKey() - minCfaNodeNumber;
          cfaNodeIdMap.put(index, entry.getValue());
        }
        integerToNodeMap.put(pCFA, ImmutableBiMap.copyOf(cfaNodeIdMap));
      }
      return integerToNodeMap.get(pCFA);
    }
  }

  private DssFactory() {}

  /**
   * Register corresponding DCPA to a CPA
   *
   * @param pCPA underlying CPA
   * @param pBlockNode block node for which the new DCPA is responsible
   * @return DCPA for pCPA
   */
  public static DistributedConfigurableProgramAnalysis distribute(
      ConfigurableProgramAnalysis pCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    return switch (pCPA) {
      case PredicateCPA predicateCPA ->
          distribute(
              predicateCPA,
              pBlockNode,
              pCFA,
              pConfiguration,
              pOptions,
              pLogManager,
              pShutdownNotifier,
              TypeAndLocationCache.getOrCreateLocationMapping(pCFA),
              TypeAndLocationCache.getOrCreateProgramFacts(
                  pCFA, pConfiguration, pLogManager, pShutdownNotifier));
      case DssCallstackCPA callstackCPA ->
          distribute(
              callstackCPA,
              pBlockNode,
              pCFA,
              pOptions.callStackStateRequiresStateReset(),
              TypeAndLocationCache.getOrCreateLocationMapping(pCFA));
      case CallstackCPA ignored ->
          throw new IllegalArgumentException(
              "Distributed summary synthesis requires DssCallstackCPA instead of CallstackCPA");
      case FunctionPointerCPA functionPointerCPA -> distribute(functionPointerCPA, pBlockNode);
      case BlockCPA blockCPA -> distribute(blockCPA, pBlockNode, pOptions);
      case ARGCPA argCPA ->
          distribute(
              argCPA,
              pBlockNode,
              pCFA,
              pConfiguration,
              pOptions,
              pMessageFactory,
              pLogManager,
              pShutdownNotifier);
      case CompositeCPA compositeCPA ->
          distribute(
              compositeCPA,
              pBlockNode,
              pCFA,
              pConfiguration,
              pOptions,
              pMessageFactory,
              pLogManager,
              pShutdownNotifier);
      case DssLocationCPA locationCPA ->
          new DistributedLocationCPA(
              locationCPA,
              locationCPA.getStateProvider(),
              pBlockNode,
              TypeAndLocationCache.getOrCreateLocationMapping(pCFA));
      case LocationCPA locationCPA ->
          distribute(
              locationCPA, pBlockNode, TypeAndLocationCache.getOrCreateLocationMapping(pCFA));
      case ControlAutomatonCPA ignored -> null;
      default ->
          throw new IllegalArgumentException(
              "Unsupported CPA type for distribution: " + pCPA.getClass().getCanonicalName());
    };
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      BlockCPA pBlockCPA, BlockNode pBlockNode, DssAnalysisOptions pOptions) {
    return new DistributedBlockCPA(pBlockCPA, pBlockNode, pOptions);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      LocationCPA pLocationCPA, BlockNode pNode, BiMap<Integer, CFANode> pNodeMap) {
    return new DistributedLocationCPA(
        pLocationCPA, pLocationCPA.getStateFactory(), pNode, pNodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      PredicateCPA pPredicateCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      BiMap<Integer, CFANode> pCfaNodeIdMap,
      ProgramFacts pProgramFacts)
      throws InvalidConfigurationException {
    return new DistributedPredicateCPA(
        pPredicateCPA,
        pBlockNode,
        pCFA,
        pConfiguration,
        pOptions,
        pLogManager,
        pShutdownNotifier,
        pCfaNodeIdMap,
        pProgramFacts.types(),
        pProgramFacts.memoryShare());
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      DssCallstackCPA pCallstackCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      boolean pRequiresStateReset,
      BiMap<Integer, CFANode> pIdToNodeMap) {
    return new DistributedCallstackCPA(
        pCallstackCPA, pBlockNode, pCFA, pRequiresStateReset, pIdToNodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      FunctionPointerCPA pFunctionPointerCPA, BlockNode pNode) {
    return new DistributedFunctionPointerCPA(pFunctionPointerCPA, pNode);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      CompositeCPA pCompositeCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    ImmutableMap.Builder<
            Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
        builder = ImmutableMap.builder();
    for (ConfigurableProgramAnalysis wrappedCPA : pCompositeCPA.getWrappedCPAs()) {
      DistributedConfigurableProgramAnalysis dcpa =
          distribute(
              wrappedCPA,
              pBlockNode,
              pCFA,
              pConfiguration,
              pOptions,
              pMessageFactory,
              pLogManager,
              pShutdownNotifier);
      if (dcpa == null) {
        continue;
      }
      builder.put(wrappedCPA.getClass(), dcpa);
    }
    return new DistributedCompositeCPA(
        pLogManager, pCompositeCPA, pBlockNode, builder.buildOrThrow());
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      ARGCPA pARGCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    return new DistributedARGCPA(
        pARGCPA,
        distribute(
            Iterables.getOnlyElement(pARGCPA.getWrappedCPAs()),
            pBlockNode,
            pCFA,
            pConfiguration,
            pOptions,
            pMessageFactory,
            pLogManager,
            pShutdownNotifier));
  }
}
