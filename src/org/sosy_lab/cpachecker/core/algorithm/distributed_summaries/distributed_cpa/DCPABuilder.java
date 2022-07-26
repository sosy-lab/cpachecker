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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DCPABuilder {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;
  private final UpdatedTypeMap typeMap;
  private final AnalysisOptions options;

  public DCPABuilder(UpdatedTypeMap pTypeMap, AnalysisOptions pOptions) {
    analyses = new HashMap<>();
    typeMap = pTypeMap;
    options = pOptions;
  }

  public void addCPA(
      ConfigurableProgramAnalysis pCPA, BlockNode pBlockNode, AnalysisDirection pDirection)
      throws CPAException {
    if (pCPA instanceof PredicateCPA) {
      addCPA((PredicateCPA) pCPA, pBlockNode, pDirection);
      return;
    }
    if (pCPA instanceof CallstackCPA) {
      addCPA((CallstackCPA) pCPA, pBlockNode, pDirection);
      return;
    }
    if (pCPA instanceof FunctionPointerCPA) {
      addCPA((FunctionPointerCPA) pCPA, pBlockNode);
    }
  }

  private void addCPA(
      PredicateCPA pPredicateCPA, BlockNode pBlockNode, AnalysisDirection pDirection)
      throws CPAException {
    analyses.put(
        pPredicateCPA.getClass(),
        new DistributedPredicateCPA(pPredicateCPA, pBlockNode, typeMap, pDirection, options));
  }

  private void addCPA(
      CallstackCPA pCallstackCPA, BlockNode pBlockNode, AnalysisDirection pDirection)
      throws CPAException {
    analyses.put(
        pCallstackCPA.getClass(),
        new DistributedCallstackCPA(pCallstackCPA, pBlockNode, pDirection));
  }

  private void addCPA(FunctionPointerCPA pFunctionPointerCPA, BlockNode pBlockNode)
      throws CPAException {
    analyses.put(
        pFunctionPointerCPA.getClass(),
        new DistributedFunctionPointerCPA(pFunctionPointerCPA, pBlockNode));
  }

  public Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      getAnalyses() {
    return analyses;
  }
}
