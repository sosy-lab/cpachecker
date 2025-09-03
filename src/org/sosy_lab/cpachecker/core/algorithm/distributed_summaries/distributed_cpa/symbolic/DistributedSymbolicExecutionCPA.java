// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;

public class DistributedSymbolicExecutionCPA implements
                                             ForwardingDistributedConfigurableProgramAnalysis


{
  private final ConstraintsCPA constraintsCPA;
  private final BlockNode blockNode;
  //private final DeserializeSymbolicStateOperator deserialize;
  //private final SerializeSymbolicStateOperator serialize;
  //private final SymbolicViolationConditionOperator violationConditionOperator;


  CFA pCFA;


  public DistributedSymbolicExecutionCPA(
      ConstraintsCPA pConstraintsCPA,
      BlockNode pBlockNode)
/*      DeserializeSymbolicStateOperator pDeserialize,
      SerializeSymbolicStateOperator pSerialize,
      SymbolicViolationConditionOperator pViolationConditionOperator)*/ {
    constraintsCPA = pConstraintsCPA;
    blockNode = pBlockNode;
    //deserialize = pDeserialize;
    //serialize = pSerialize;
    //violationConditionOperator = pViolationConditionOperator;
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return null;//serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return null;//deserialize;
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return null;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return null;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return null;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return null;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return null;//violationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return null;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return null;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return ConstraintsState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return constraintsCPA;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    ConstraintsState constraintsState = (ConstraintsState) pAbstractState;
    // What if we have "assume x == x"? 
    // create new "true" state, ask is it is subsumed?
    return constraintsState.getDefiniteAssignment().isEmpty() &&
        constraintsState.getLastAddedConstraint().isEmpty();
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    return null;
  }
}
