// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import com.google.common.collect.FluentIterable;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.PredicateOperatorUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

// Overrides getFormulaApproximation for infer specific cases
public class InferBlockState extends BlockState
    implements AbstractQueryableState, Partitionable, Targetable, FormulaReportingState {

  private final BlockNode blockNode;
  private Optional<AbstractState> errorCondition;

  public InferBlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      AnalysisDirection pDirection,
      BlockStateType pType,
      Optional<AbstractState> pErrorCondition) {
    super(pNode, pTargetNode, pDirection, pType, pErrorCondition);
    blockNode = pTargetNode;
    errorCondition = pErrorCondition;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    if (errorCondition.isPresent()
        && blockNode.getFirst().getEnteringSummaryEdge() != null
        && !isStartNodeOfBlock()) {
      FluentIterable<BooleanFormula> approximations =
          AbstractStates.asIterable(errorCondition.orElseThrow())
              .filter(PredicateAbstractState.class)
              .transform(
                  s ->
                      PredicateOperatorUtil.uninstantiate(
                              s.getAbstractionFormula().getBlockFormula(), manager)
                          .booleanFormula());
      return manager.getBooleanFormulaManager().and(approximations.toList());
    }
    return manager.getBooleanFormulaManager().makeTrue();
  }
}
