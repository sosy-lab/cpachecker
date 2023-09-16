// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DistributedFunctionPointerCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;

  private final FunctionPointerCPA functionPointerCPA;

  public DistributedFunctionPointerCPA(
      FunctionPointerCPA pParentCPA, ImmutableMap<Integer, CFANode> pIntegerCFANodeMap) {
    functionPointerCPA = pParentCPA;
    serialize = new SerializeFunctionPointerStateOperator();
    deserialize = new DeserializeFunctionPointerStateOperator(pParentCPA, pIntegerCFANodeMap);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return ProceedOperator.always();
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return FunctionPointerState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return functionPointerCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    return true;
  }

  @Override
  public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws InterruptedException, CPATransferException, VerificationConditionException {
    AbstractState error =
        Objects.requireNonNull(
            pPreviousCondition == null
                ? FunctionPointerState.createEmptyState()
                : AbstractStates.extractStateByType(pPreviousCondition, getAbstractStateClass()));
    for (CFAEdge cfaEdge : Lists.reverse(pARGPath.getFullPath())) {
      Collection<? extends AbstractState> abstractSuccessorsForEdge =
          functionPointerCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(
                  error,
                  getInitialPrecision(
                      cfaEdge.getSuccessor(), StateSpacePartition.getDefaultPartition()),
                  cfaEdge);
      if (abstractSuccessorsForEdge.isEmpty()) {
        throw new VerificationConditionException("FunctionPointerCPA does not allow transfer");
      }
      error = Iterables.getOnlyElement(abstractSuccessorsForEdge);
    }
    return error;
  }
}
