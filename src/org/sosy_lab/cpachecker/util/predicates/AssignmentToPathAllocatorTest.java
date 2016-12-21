/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

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
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class AssignmentToPathAllocatorTest {

  private AssignmentToPathAllocator allocator;

  @Before
  public void setUp() throws InvalidConfigurationException {
    this.allocator =
        new AssignmentToPathAllocator(
            Configuration.defaultConfiguration(),
            ShutdownNotifier.createDummy(),
            LogManager.createTestLogManager(),
            MachineModel.LINUX32);
  }

  @Test
  public void testFindFirstOccurrenceOfVariable() {
    ValueAssignment varX = new ValueAssignment(mock(Formula.class), "x@4", 1, ImmutableList.of());
    ValueAssignment varY = new ValueAssignment(mock(Formula.class), "y@5", 1, ImmutableList.of());
    ValueAssignment varZ = new ValueAssignment(mock(Formula.class), "z@6", 1, ImmutableList.of());

    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    List<SSAMap> ssaMaps = Lists.newArrayList();

    ssaMaps.add(SSAMap.emptySSAMap());

    ssaMapBuilder.setIndex("x", CNumericTypes.INT, 4);
    ssaMaps.add(ssaMapBuilder.build());

    ssaMapBuilder.setIndex("y", CNumericTypes.INT, 5);
    ssaMapBuilder.setIndex("z", CNumericTypes.INT, 6);
    ssaMaps.add(ssaMapBuilder.build());

    ssaMapBuilder.deleteVariable("z");
    ssaMaps.add(ssaMapBuilder.build());

    assertEquals(1, allocator.findFirstOccurrenceOfVariable(varX, ssaMaps));
    assertEquals(2, allocator.findFirstOccurrenceOfVariable(varY, ssaMaps));
    assertEquals(2, allocator.findFirstOccurrenceOfVariable(varZ, ssaMaps));
  }

}
