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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow.DistributedDataFlowAnalysisCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.DistributedValueAnalysisCPA;
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

public class DCPAFactory {

  private DCPAFactory() {}

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
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    ImmutableMap<Integer, CFANode> integerToNodeMap =
        ImmutableMap.copyOf(CFAUtils.getMappingFromNodeIDsToCFANodes(pCFA));
    if (pCPA instanceof PredicateCPA predicateCPA) {
      return distribute(
          predicateCPA,
          pBlockNode,
          pCFA,
          pConfiguration,
          pLogManager,
          pShutdownNotifier,
          integerToNodeMap);
    }

    if (pCPA instanceof InvariantsCPA invariantsCPA) {
      return distribute(invariantsCPA, pBlockNode, pCFA);
    }
    if (pCPA instanceof CallstackCPA callstackCPA) {
      return distribute(callstackCPA, pCFA, integerToNodeMap);
    }
    if (pCPA instanceof FunctionPointerCPA functionPointerCPA) {
      return distribute(functionPointerCPA, integerToNodeMap);
    }
    if (pCPA instanceof BlockCPA blockCPA) {
      return distribute(blockCPA, pBlockNode, integerToNodeMap);
    }
    if (pCPA instanceof ARGCPA argCPA) {
      return distribute(argCPA, pBlockNode, pCFA, pConfiguration, pLogManager, pShutdownNotifier);
    }
    if (pCPA instanceof CompositeCPA compositeCPA) {
      return distribute(
          compositeCPA,
          pBlockNode,
          pCFA,
          pConfiguration,
          pLogManager,
          pShutdownNotifier,
          integerToNodeMap);
    }
    if (pCPA instanceof ValueAnalysisCPA valueCPA) {
      return distribute(valueCPA);
    }

    /* TODO: implement support for LocationCPA and LocationBackwardCPA
    as soon as targetCFANode is not required anymore */
    // creates CPA for every thread without communication
    return null;
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      ValueAnalysisCPA pValueAnalysisCPA) {
    return new DistributedValueAnalysisCPA(pValueAnalysisCPA);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      InvariantsCPA pInvariantsCPA, BlockNode pBlockNode, CFA pCFA) {
    return new DistributedDataFlowAnalysisCPA(pInvariantsCPA, pBlockNode, pCFA);
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
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      Map<Integer, CFANode> pIntegerCFANodeMap)
      throws InvalidConfigurationException {
    return new DistributedPredicateCPA(
        pPredicateCPA,
        pBlockNode,
        pCFA,
        pConfiguration,
        pLogManager,
        pShutdownNotifier,
        pIntegerCFANodeMap);
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
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap)
      throws InvalidConfigurationException {
    ImmutableMap.Builder<
            Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
        builder = ImmutableMap.builder();
    for (ConfigurableProgramAnalysis wrappedCPA : pCompositeCPA.getWrappedCPAs()) {
      DistributedConfigurableProgramAnalysis dcpa =
          distribute(wrappedCPA, pBlockNode, pCFA, pConfiguration, pLogManager, pShutdownNotifier);
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
            pLogManager,
            pShutdownNotifier));
  }
}
