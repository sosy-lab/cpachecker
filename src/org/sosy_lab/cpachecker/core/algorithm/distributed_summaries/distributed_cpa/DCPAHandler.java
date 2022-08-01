// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
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

  public void registerDCPA(
      ConfigurableProgramAnalysis pCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    if (pCPA instanceof PredicateCPA) {
      registerDCPA((PredicateCPA) pCPA, pBlockNode, pDirection);
      return;
    }
    if (pCPA instanceof CallstackCPA) {
      registerDCPA((CallstackCPA) pCPA, pBlockNode, pDirection);
      return;
    }
    if (pCPA instanceof FunctionPointerCPA) {
      registerDCPA((FunctionPointerCPA) pCPA, pBlockNode);
    }
  }

  private void registerDCPA(
      PredicateCPA pPredicateCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    analyses.put(
        pPredicateCPA.getClass(),
        new DistributedPredicateCPA(pPredicateCPA, pBlockNode, pDirection, options));
  }

  private void registerDCPA(
      CallstackCPA pCallstackCPA, BlockNode pBlockNode, AnalysisDirection pDirection) {
    analyses.put(
        pCallstackCPA.getClass(),
        new DistributedCallstackCPA(pCallstackCPA, pBlockNode, pDirection));
  }

  private void registerDCPA(FunctionPointerCPA pFunctionPointerCPA, BlockNode pBlockNode) {
    analyses.put(
        pFunctionPointerCPA.getClass(),
        new DistributedFunctionPointerCPA(pFunctionPointerCPA, pBlockNode));
  }

  public Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      getRegisteredAnalyses() {
    return analyses;
  }
}
