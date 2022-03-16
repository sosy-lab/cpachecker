// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public class AssignmentToPathAllocatorTest {

  private AssignmentToPathAllocator allocator;

  @Before
  public void setUp() throws InvalidConfigurationException {
    allocator =
        new AssignmentToPathAllocator(
            Configuration.defaultConfiguration(),
            ShutdownNotifier.createDummy(),
            LogManager.createTestLogManager(),
            MachineModel.LINUX32);
  }

  @Test
  public void testFindFirstOccurrenceOfVariable() {

    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    List<SSAMap> ssaMaps = new ArrayList<>();

    ssaMaps.add(SSAMap.emptySSAMap());

    ssaMapBuilder.setIndex("x", CNumericTypes.INT, 4);
    ssaMaps.add(ssaMapBuilder.build());

    ssaMapBuilder.setIndex("y", CNumericTypes.INT, 5);
    ssaMapBuilder.setIndex("z", CNumericTypes.INT, 6);
    SSAMap ssaMap = ssaMapBuilder.build();
    ssaMaps.add(ssaMap);

    ssaMapBuilder.deleteVariable("z");
    ssaMaps.add(ssaMapBuilder.build());

    ValueAssignment varX =
        new ValueAssignment(
            mock(Formula.class),
            mock(Formula.class),
            mock(BooleanFormula.class),
            FormulaManagerView.instantiateVariableName("x", ssaMap),
            1,
            ImmutableList.of());
    ValueAssignment varY =
        new ValueAssignment(
            mock(Formula.class),
            mock(Formula.class),
            mock(BooleanFormula.class),
            FormulaManagerView.instantiateVariableName("y", ssaMap),
            1,
            ImmutableList.of());
    ValueAssignment varZ =
        new ValueAssignment(
            mock(Formula.class),
            mock(Formula.class),
            mock(BooleanFormula.class),
            FormulaManagerView.instantiateVariableName("z", ssaMap),
            1,
            ImmutableList.of());

    assertThat(allocator.findFirstOccurrenceOf(varX, ssaMaps)).isEqualTo(1);
    assertThat(allocator.findFirstOccurrenceOf(varY, ssaMaps)).isEqualTo(2);
    assertThat(allocator.findFirstOccurrenceOf(varZ, ssaMaps)).isEqualTo(2);
  }
}
