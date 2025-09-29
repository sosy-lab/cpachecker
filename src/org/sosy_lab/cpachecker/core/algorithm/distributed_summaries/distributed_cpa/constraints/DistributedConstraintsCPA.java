// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DistributedConstraintsCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final ConstraintsCPA constraintsCPA;
  private final DeserializeConstraintsStateOperator deserializeOperator;
  private final SerializeConstraintsStateOperator serializeOperator;
  private final ConstraintsViolationConditionOperator violationConditionOperator;
  private final SerializeConstraintsPrecisionOperator serializePrecisionOperator;
  private final DeserializeConstraintsPrecisionOperator deserializePrecisionOperator;
  private final ProceedConstraintsStateOperator proceedOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;
  private final ConstraintsStateCoverageOperator coverageOperator;
  private final BlockNode blockNode;

  public DistributedConstraintsCPA(ConstraintsCPA pConstraintsCPA, BlockNode pBlockNode) {
    constraintsCPA = pConstraintsCPA;
    serializeOperator = new SerializeConstraintsStateOperator();
    deserializeOperator = new DeserializeConstraintsStateOperator();
    violationConditionOperator = new ConstraintsViolationConditionOperator();
    serializePrecisionOperator = new SerializeConstraintsPrecisionOperator();
    deserializePrecisionOperator = new DeserializeConstraintsPrecisionOperator();
    proceedOperator = new ProceedConstraintsStateOperator();
    combinePrecisionOperator = new CombineConstraintsPrecisionOperator();
    coverageOperator =
        new ConstraintsStateCoverageOperator(constraintsCPA, pBlockNode.getInitialLocation());
    blockNode = pBlockNode;
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeOperator;
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return combinePrecisionOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return violationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return coverageOperator;
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
    BooleanFormulaManagerView bfm =
        constraintsCPA.getSolver().getFormulaManager().getBooleanFormulaManager();
    try {
      BooleanFormula stateAsFormula1 =
          bfm.and(
              constraintsCPA
                  .getSolver()
                  .getFullFormula(
                      constraintsState, blockNode.getInitialLocation().getFunctionName()));
      return bfm.isTrue(stateAsFormula1);
    } catch (UnrecognizedCodeException | InterruptedException pE) {
      throw new AssertionError("Failed creating formula from constraints", pE);
    }
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    return pAbstractState;
  }
}
