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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DistributedLocationCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints.DistributedConstraintsCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.DistributedValueAnalysisCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CFAUtils;

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
    if (pCPA instanceof PredicateCPA predicateCPA) {
      return distribute(
          predicateCPA,
          pBlockNode,
          pCFA,
          pConfiguration,
          pOptions,
          pLogManager,
          pShutdownNotifier,
          integerToNodeMap);
    }
    if (pCPA instanceof CallstackCPA callstackCPA) {
      return distribute(callstackCPA, pBlockNode, pCFA, integerToNodeMap);
    }
    if (pCPA instanceof FunctionPointerCPA functionPointerCPA) {
      return distribute(functionPointerCPA, pBlockNode);
    }
    if (pCPA instanceof BlockCPA blockCPA) {
      return distribute(blockCPA, pBlockNode);
    }
    if (pCPA instanceof ARGCPA argCPA) {
      return distribute(
          argCPA,
          pBlockNode,
          pCFA,
          pConfiguration,
          pOptions,
          pMessageFactory,
          pLogManager,
          pShutdownNotifier);
    }
    if (pCPA instanceof CompositeCPA compositeCPA) {
      return distribute(
          compositeCPA,
          pBlockNode,
          pCFA,
          pConfiguration,
          pOptions,
          pMessageFactory,
          pLogManager,
          pShutdownNotifier);
    }
    if (pCPA instanceof LocationCPA locationCPA) {
      return distribute(locationCPA, pBlockNode, integerToNodeMap);
    }

    if (pCPA instanceof ConstraintsCPA constraintsCPA) {
      return distribute(constraintsCPA, pBlockNode);
    }

    if (pCPA instanceof ValueAnalysisCPA valueAnalysisCPA) {
      return distribute(valueAnalysisCPA, pCFA, pConfiguration, pBlockNode,
          pLogManager, pShutdownNotifier);
    }

    return null;
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      BlockCPA pBlockCPA, BlockNode pBlockNode) {
    return new DistributedBlockCPA(pBlockCPA, pBlockNode);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      LocationCPA pLocationCPA, BlockNode pNode, Map<Integer, CFANode> pNodeMap) {
    return new DistributedLocationCPA(pLocationCPA, pNode, pNodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      ConstraintsCPA pConstraintsCPA, BlockNode pBlockNode) {
    return new DistributedConstraintsCPA(pConstraintsCPA, pBlockNode);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      ValueAnalysisCPA pValueCPA, CFA pCFA, Configuration pConfiguration, BlockNode pBlockNode,
      LogManager pLogManager, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new DistributedValueAnalysisCPA(pValueCPA, pCFA, pConfiguration, pBlockNode, pLogManager,
        pShutdownNotifier);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      PredicateCPA pPredicateCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap)
      throws InvalidConfigurationException {
    return new DistributedPredicateCPA(
        pPredicateCPA,
        pBlockNode,
        pCFA,
        pConfiguration,
        pOptions,
        pLogManager,
        pShutdownNotifier,
        pIntegerCFANodeMap);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      CallstackCPA pCallstackCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Map<Integer, CFANode> pIdToNodeMap) {
    return new DistributedCallstackCPA(pCallstackCPA, pBlockNode, pCFA, pIdToNodeMap);
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
    return new DistributedCompositeCPA(pCompositeCPA, pBlockNode, builder.buildOrThrow());
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
