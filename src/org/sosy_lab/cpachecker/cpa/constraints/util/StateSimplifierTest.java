// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Unit tests for {@link org.sosy_lab.cpachecker.cpa.constraints.util.StateSimplifier}.
 *
 * <p>There exist four symbolic identifiers that are divided into two groups by their dependencies:
 * Constraints exist so that the first identifier is dependent on the second one and so that the
 * third is dependent on the fourth. No dependencies exist between these two groups.
 */
public class StateSimplifierTest {

  private final MachineModel machineModel = MachineModel.LINUX32;

  private final StateSimplifier simplifier;

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  private final Type defaultNumericType = CNumericTypes.INT;

  private final SymbolicExpression number =
      factory.asConstant(new NumericValue(5), defaultNumericType);

  private final MemoryLocation memLoc1 = MemoryLocation.forIdentifier("id1");
  private final SymbolicExpression group1Id1 =
      factory.asConstant(factory.newIdentifier(memLoc1), defaultNumericType);
  private final SymbolicExpression group1Id2 =
      factory.asConstant(factory.newIdentifier(memLoc1), defaultNumericType);
  private final SymbolicExpression group2Id1 =
      factory.asConstant(factory.newIdentifier(memLoc1), defaultNumericType);
  private final SymbolicExpression group2Id2 =
      factory.asConstant(factory.newIdentifier(memLoc1), defaultNumericType);

  private final Constraint group1Constraint1 =
      (Constraint)
          factory.greaterThanOrEqual(group1Id1, group1Id2, defaultNumericType, defaultNumericType);
  private final Constraint group1Constraint2 =
      (Constraint)
          factory.lessThanOrEqual(group1Id2, group1Id1, defaultNumericType, defaultNumericType);

  private final Constraint group2Constraint1 =
      (Constraint) factory.lessThan(group2Id1, number, defaultNumericType, defaultNumericType);
  private final Constraint group2Constraint2 =
      (Constraint) factory.lessThan(group2Id2, group2Id1, defaultNumericType, defaultNumericType);

  private final MemoryLocation group1MemLoc1 = MemoryLocation.forIdentifier("a");
  private final MemoryLocation group1MemLoc2 = MemoryLocation.forIdentifier("b");
  private final MemoryLocation group2MemLoc1 = MemoryLocation.forIdentifier("c");
  private final MemoryLocation group2MemLoc2 = MemoryLocation.forIdentifier("d");

  public StateSimplifierTest() throws InvalidConfigurationException {
    Configuration config =
        Configuration.builder().setOption("cpa.constraints.removeTrivial", "true").build();
    simplifier = new StateSimplifier(config, new ConstraintsStatistics());
  }

  @Test
  public void testRemoveOutdatedConstraints_allConstraintsOutdated() {
    final ValueAnalysisState initialValueState = new ValueAnalysisState(machineModel);

    ConstraintsState constraintsState = getSampleConstraints();

    simplifier.removeOutdatedConstraints(constraintsState, initialValueState);

    assertThat(constraintsState).isEmpty();
  }

  @Test
  public void testRemoveOutdatedConstraints_group1ConstraintOutdated() {
    final ValueAnalysisState valueState = getCompleteValueState();

    valueState.forget(group1MemLoc1);
    valueState.forget(group2MemLoc1);

    ConstraintsState constraintsState = getSampleConstraints();

    simplifier.removeOutdatedConstraints(constraintsState, valueState);

    assertThat(group2ConstraintsExist(constraintsState)).isTrue();
  }

  private boolean group2ConstraintsExist(ConstraintsState pNewState) {
    return pNewState.contains(group2Constraint1) && pNewState.contains(group2Constraint2);
  }

  @Test
  public void testRemoveOutdatedConstraints_allButOneConstraintsOutdated() {
    final ValueAnalysisState valueState = getCompleteValueState();

    valueState.forget(group1MemLoc1);
    valueState.forget(group1MemLoc2);
    valueState.forget(group2MemLoc2);
    ConstraintsState constraintsState = getSampleConstraints();

    simplifier.removeOutdatedConstraints(constraintsState, valueState);

    assertThat(constraintsState).hasSize(1);
    assertThat(constraintsState).contains(group2Constraint1);
  }

  private ConstraintsState getSampleConstraints() {
    ConstraintsState state = new ConstraintsState();

    state.add(group1Constraint1);
    state.add(group1Constraint2);
    state.add(group2Constraint1);
    state.add(group2Constraint2);

    return state;
  }

  private ValueAnalysisState getCompleteValueState() {
    ValueAnalysisState state = new ValueAnalysisState(machineModel);

    state.assignConstant(group1MemLoc1, group1Id1, defaultNumericType);
    state.assignConstant(group1MemLoc2, group1Id2, defaultNumericType);
    state.assignConstant(group2MemLoc1, group2Id1, defaultNumericType);
    state.assignConstant(group2MemLoc2, group2Id2, defaultNumericType);

    return state;
  }
}
