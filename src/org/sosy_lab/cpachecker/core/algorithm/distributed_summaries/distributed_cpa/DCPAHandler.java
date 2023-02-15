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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
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

  public DCPAHandler() {
    analyses = new HashMap<>();
  }

  /**
   * Register corresponding DCPA to a CPA
   *
   * @param pCPA underlying CPA
   * @param pBlockNode block node for which the new DCPA is responsible
   * @param pDirection analysis direction
   * @return previous DCPA of same type if already registered
   */
  @CanIgnoreReturnValue
  public Optional<DistributedConfigurableProgramAnalysis> registerDCPA(
      ConfigurableProgramAnalysis pCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    checkArgument(
        !(pCPA instanceof CompositeCPA), "Cannot register DCPA for type " + "%s", pCPA.getClass());
    if (pCPA instanceof PredicateCPA) {
      return Optional.ofNullable(registerDCPA((PredicateCPA) pCPA, pBlockNode, pDirection));
    }
    if (pCPA instanceof CallstackCPA) {
      return Optional.ofNullable(registerDCPA((CallstackCPA) pCPA, pBlockNode));
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
      BlockCPA pBlockCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    return analyses.put(
        pBlockCPA.getClass(), new DistributedBlockCPA(pBlockCPA, pBlockNode, pDirection));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      BlockCPABackward pBlockCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    return analyses.put(
        pBlockCPA.getClass(), new DistributedBlockCPA(pBlockCPA, pBlockNode, pDirection));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      PredicateCPA pPredicateCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    return analyses.put(
        pPredicateCPA.getClass(),
        new DistributedPredicateCPA(pPredicateCPA, pBlockNode, pDirection));
  }

  private DistributedConfigurableProgramAnalysis registerDCPA(
      CallstackCPA pCallstackCPA, BlockNode pBlockNode) {
    return analyses.put(
        pCallstackCPA.getClass(), new DistributedCallstackCPA(pCallstackCPA, pBlockNode));
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
