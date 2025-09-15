// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Map;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow.DistributedDataFlowAnalysisCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.DistributedValueAnalysisCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DssFactory {

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
      throws InvalidConfigurationException {
    ImmutableMap<Integer, CFANode> integerToNodeMap =
        ImmutableMap.copyOf(CFAUtils.getMappingFromNodeIDsToCFANodes(pCFA));
    Map<MemoryLocation, CType> variableTypes = CFAUtils.extractVariableTypes(pCFA);
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
              integerToNodeMap,
              variableTypes);
      case CallstackCPA callstackCPA -> distribute(callstackCPA, pCFA, integerToNodeMap);
      case FunctionPointerCPA functionPointerCPA ->
          distribute(functionPointerCPA, integerToNodeMap);
      case BlockCPA blockCPA -> distribute(blockCPA, pBlockNode, integerToNodeMap);
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
              pShutdownNotifier,
              integerToNodeMap);
      case ValueAnalysisCPA valueAnalysisCPA ->
          distribute(
              valueAnalysisCPA,
              pBlockNode,
              pCFA,
              pConfiguration,
              pLogManager,
              pShutdownNotifier,
              variableTypes);
      case InvariantsCPA invariantsCPA ->
          distribute(
              invariantsCPA,
              pBlockNode,
              pCFA,
              pConfiguration,
              pLogManager,
              pShutdownNotifier,
              variableTypes);
      case null /*TODO check if null is necessary*/, default ->
          /* TODO: implement support for LocationCPA and LocationBackwardCPA
          as soon as targetCFANode is not required anymore */
          null; // creates CPA for every thread without communication
    };
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      ValueAnalysisCPA pValueAnalysisCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      Map<MemoryLocation, CType> pVaribleTypes)
      throws InvalidConfigurationException {
    return new DistributedValueAnalysisCPA(
        pValueAnalysisCPA,
        pBlockNode,
        pCFA,
        pConfiguration,
        pLogManager,
        pShutdownNotifier,
        pVaribleTypes);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      InvariantsCPA pInvariantsCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      Map<MemoryLocation, CType> pVaribleTypes)
      throws InvalidConfigurationException {
    return new DistributedDataFlowAnalysisCPA(
        pInvariantsCPA,
        pBlockNode,
        pCFA,
        pConfiguration,
        pLogManager,
        pShutdownNotifier,
        pVaribleTypes);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      BlockCPA pBlockCPA, BlockNode pBlockNode, ImmutableMap<Integer, CFANode> pIntegerCFANodeMap) {
    return new DistributedBlockCPA(pBlockCPA, pBlockNode, pIntegerCFANodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      PredicateCPA pPredicateCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      Map<Integer, CFANode> pIntegerCFANodeMap,
      Map<MemoryLocation, CType> pVariableTypes)
      throws InvalidConfigurationException {
    return new DistributedPredicateCPA(
        pPredicateCPA,
        pBlockNode,
        pCFA,
        pConfiguration,
        pOptions,
        pLogManager,
        pShutdownNotifier,
        pIntegerCFANodeMap,
        pVariableTypes);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      CallstackCPA pCallstackCPA, CFA pCFA, Map<Integer, CFANode> pIdToNodeMap) {
    return new DistributedCallstackCPA(pCallstackCPA, pCFA, pIdToNodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      FunctionPointerCPA pFunctionPointerCPA, ImmutableMap<Integer, CFANode> pIntegerCFANodeMap) {
    return new DistributedFunctionPointerCPA(pFunctionPointerCPA, pIntegerCFANodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      CompositeCPA pCompositeCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap)
      throws InvalidConfigurationException {
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
        pCompositeCPA, pBlockNode, pIntegerCFANodeMap, builder.buildOrThrow());
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
      throws InvalidConfigurationException {
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
