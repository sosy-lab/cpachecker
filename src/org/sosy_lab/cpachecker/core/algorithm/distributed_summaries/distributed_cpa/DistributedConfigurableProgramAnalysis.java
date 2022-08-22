// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.util.CPAs;

public interface DistributedConfigurableProgramAnalysis extends ConfigurableProgramAnalysis {

  /**
   * Operator that knows how to serialize the abstract states from {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Serialize operator for a distributed CPA.
   */
  SerializeOperator getSerializeOperator();

  /**
   * Operator that knows how to combine several abstract states from {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Combine operator for a distributed CPA.
   */
  CombineOperator getCombineOperator();

  /**
   * Operator that knows how to deserialize a message to abstract states of type {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Deserialize operator for a distributed CPA.
   */
  DeserializeOperator getDeserializeOperator();

  /**
   * Operator that decides whether to proceed with an analysis based on the given message.
   *
   * @return Proceed operator for a distributed CPA.
   */
  ProceedOperator getProceedOperator();

  /**
   * The abstract state this distributed analysis works n.
   *
   * @return Parent class of all abstract states that this distributed CPA can handle.
   */
  Class<? extends AbstractState> getAbstractStateClass();

  /**
   * Check whether this distributed CPA can work with {@code pClass}.
   *
   * @param pClass Decide whether this DCPA can work with this class.
   * @return Returns whether this DCPA accepts {@code pClass}
   */
  default boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return getAbstractStateClass().isAssignableFrom(pClass);
  }

  /**
   * Synchronize the knowledge of the forward analysis with the knowledge of the backward analysis
   * for later infeasibility checks.
   *
   * @param pAnalysis Synchronize the knowledge of {@code pAnalysis} with this proceed operator
   */
  void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis)
      throws InterruptedException;

  static DistributedConfigurableProgramAnalysis distribute(
      ConfigurableProgramAnalysis pCPA,
      BlockNode pBlock,
      AnalysisDirection pDirection,
      BlockSummaryAnalysisOptions pOptions) {
    DCPAHandler builder = new DCPAHandler(pOptions);
    CompositeCPA compositeCPA = CPAs.retrieveCPA(pCPA, CompositeCPA.class);
    if (compositeCPA == null) {
      builder.registerDCPA(pCPA, pBlock, pDirection);
      return Iterables.getOnlyElement(builder.getRegisteredAnalyses().values());
    }
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      builder.registerDCPA(wrappedCPA, pBlock, pDirection);
    }
    return new DistributedCompositeCPA(
        compositeCPA, pBlock, pDirection, builder.getRegisteredAnalyses());
  }
}
