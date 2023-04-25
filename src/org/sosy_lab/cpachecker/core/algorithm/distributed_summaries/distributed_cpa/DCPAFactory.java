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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class DCPAFactory {

  /**
   * Register corresponding DCPA to a CPA
   *
   * @param pCPA underlying CPA
   * @param pBlockNode block node for which the new DCPA is responsible
   * @param pDirection analysis direction
   * @return DCPA for pCPA
   */
  public static DistributedConfigurableProgramAnalysis distribute(
      ConfigurableProgramAnalysis pCPA,
      BlockNode pBlockNode,
      AnalysisDirection pDirection,
      CFA pCFA) {
    ImmutableMap<Integer, CFANode> integerToNodeMap =
        ImmutableMap.copyOf(CFAUtils.getMappingFromNodeIDsToCFANodes(pCFA));
    if (pCPA instanceof PredicateCPA predicateCPA) {
      return distribute(predicateCPA, pBlockNode, pDirection, pCFA);
    }
    if (pCPA instanceof CallstackCPA callstackCPA) {
      return distribute(callstackCPA, pCFA, integerToNodeMap);
    }
    if (pCPA instanceof FunctionPointerCPA functionPointerCPA) {
      return distribute(functionPointerCPA, integerToNodeMap);
    }
    if (pCPA instanceof BlockCPA blockCPA) {
      return distribute(blockCPA, pBlockNode, integerToNodeMap, pDirection);
    }
    if (pCPA instanceof BlockCPABackward backwardBlockCPA) {
      return distribute(backwardBlockCPA, pBlockNode, integerToNodeMap, pDirection);
    }
    if (pCPA instanceof ARGCPA argCPA) {
      return distribute(argCPA, pBlockNode, pDirection, pCFA);
    }
    if (pCPA instanceof CompositeCPA compositeCPA) {
      return distribute(compositeCPA, pBlockNode, pDirection, pCFA, integerToNodeMap);
    }
    return null;
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      BlockCPA pBlockCPA,
      BlockNode pBlockNode,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap,
      AnalysisDirection pDirection) {
    return new DistributedBlockCPA(pBlockCPA, pBlockNode, pIntegerCFANodeMap, pDirection);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      BlockCPABackward pBlockCPA,
      BlockNode pBlockNode,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap,
      AnalysisDirection pDirection) {
    return new DistributedBlockCPA(pBlockCPA, pBlockNode, pIntegerCFANodeMap, pDirection);
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      PredicateCPA pPredicateCPA, BlockNode pBlockNode, AnalysisDirection pDirection, CFA pCFA) {
    return new DistributedPredicateCPA(pPredicateCPA, pBlockNode, pCFA, pDirection);
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
      AnalysisDirection pDirection,
      CFA pCFA,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap) {
    ImmutableMap.Builder<
            Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
        builder = ImmutableMap.builder();
    for (ConfigurableProgramAnalysis wrappedCPA : pCompositeCPA.getWrappedCPAs()) {
      DistributedConfigurableProgramAnalysis dcpa =
          distribute(wrappedCPA, pBlockNode, pDirection, pCFA);
      if (dcpa == null) {
        continue;
      }
      builder.put(wrappedCPA.getClass(), dcpa);
    }
    return new DistributedCompositeCPA(
        pCompositeCPA, pBlockNode, pIntegerCFANodeMap, pDirection, builder.buildOrThrow());
  }

  private static DistributedConfigurableProgramAnalysis distribute(
      ARGCPA pARGCPA, BlockNode pBlockNode, AnalysisDirection pDirection, CFA pCFA) {
    return new DistributedARGCPA(
        pARGCPA,
        distribute(
            Iterables.getOnlyElement(pARGCPA.getWrappedCPAs()), pBlockNode, pDirection, pCFA));
  }
}
