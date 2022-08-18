// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

public class DCPAHandler {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;
  private final BlockSummaryAnalysisOptions options;

  public DCPAHandler(BlockSummaryAnalysisOptions pOptions) {
    analyses = new HashMap<>();
    options = pOptions;
  }

  @CanIgnoreReturnValue
  public Optional<DistributedConfigurableProgramAnalysis> registerDCPA(
      ConfigurableProgramAnalysis pCPA, BlockNode pBlockNode, AnalysisDirection pDirection)
      throws InvalidConfigurationException {
    checkArgument(
        !(pCPA instanceof CompositeCPA), "Cannot register DCPA for type " + "%s", pCPA.getClass());
    if (pCPA instanceof PredicateCPA) {
      return Optional.ofNullable(registerDCPA((PredicateCPA) pCPA, pBlockNode, pDirection));
    }
    if (pCPA instanceof CallstackCPA) {
      return Optional.ofNullable(registerDCPA((CallstackCPA) pCPA, pBlockNode, pDirection));
    }
    if (pCPA instanceof FunctionPointerCPA) {
      return Optional.ofNullable(registerDCPA((FunctionPointerCPA) pCPA, pBlockNode));
    }
    if (pCPA instanceof BlockCPA) {
      return Optional.ofNullable(registerDCPA((BlockCPA) pCPA, pBlockNode, pDirection));
    }
    if (pCPA instanceof BlockCPABackward) {
      return Optional.ofNullable(registerDCPA((BlockCPABackward) pCPA, pBlockNode, pDirection));
    }
    return Optional.empty();
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      BlockCPA pBlockCPA, BlockNode pBlockNode, AnalysisDirection pDirection)
      throws InvalidConfigurationException {
    return analyses.put(
        pBlockCPA.getClass(),
        new DistributedBlockCPA(pBlockCPA, pBlockNode, pDirection, () -> analyses.values()));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      BlockCPABackward pBlockCPA, BlockNode pBlockNode, AnalysisDirection pDirection)
      throws InvalidConfigurationException {
    return analyses.put(
        pBlockCPA.getClass(),
        new DistributedBlockCPA(pBlockCPA, pBlockNode, pDirection, () -> analyses.values()));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      PredicateCPA pPredicateCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    return analyses.put(
        pPredicateCPA.getClass(),
        new DistributedPredicateCPA(pPredicateCPA, pBlockNode, pDirection, options));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      CallstackCPA pCallstackCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    return analyses.put(
        pCallstackCPA.getClass(),
        new DistributedCallstackCPA(pCallstackCPA, pBlockNode, pDirection));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      FunctionPointerCPA pFunctionPointerCPA, BlockNode pBlockNode) {
    return analyses.put(
        pFunctionPointerCPA.getClass(),
        new DistributedFunctionPointerCPA(pFunctionPointerCPA, pBlockNode));
  }

  public Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      getRegisteredAnalyses() {
    return analyses;
  }
}
