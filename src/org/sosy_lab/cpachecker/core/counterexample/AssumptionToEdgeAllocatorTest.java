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
package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import java.util.HashMap;
import java.util.Map;

public class AssumptionToEdgeAllocatorTest {

  private MachineModel machineModel;
  private LogManager logger;
  private CFA cfa;

  private ConcreteState empty;
  private ConcreteState symbolic;
  private ConcreteState full;
  private static final String MEMORYNAME = "Test_Heap";

  private MemoryName memoryName = (pExp, pAddress) -> MEMORYNAME;

  @Before
  public void setUp() throws Exception {
    cfa =
        TestDataTools.makeCFA(
            "typedef struct dataNode {",
            "  int h;",
            "  int h2[3];",
            "  int *h3;",
            "} data;", // sizeof 20
            "",
            "typedef struct node {",
            "  int h;",
            "  struct node *n;",
            "  data d;",
            "} List;", // sizeof 28
            "",
            "typedef List *ListP;",
            "",
            "int x;",
            "",
            "int main() {",
            "  int a;",
            "  int* b;",
            "  int** c;",
            "  int  d[6];",
            "  int  e[2][3];",
            "  List list;",
            "  a = a;",
            "  *b = *b;",
            "  **c = **c;",
            "  d[1] = d[1];",
            "  *(d + 1) = *(d + 1);",
            "  e[1][2] = e[1][2];",
            "  list.h = list.h;",
            "  list.n = list.n;",
            "  list.d.h = list.d.h;",
            "  list.n->d.h2[0] = list.n->d.h2[0];",
            "  list.n->d.h3 = list.n->d.h3;",
            "}");

    machineModel = cfa.getMachineModel();
    logger = LogManager.createTestLogManager();
    empty = createEmptyState();
    full = createFullState();
    symbolic = createSymbolicState();
  }

  private ConcreteState createFullState() {

    Map<LeftHandSide, Object> variables = new HashMap<>();
    variables.put(makeVariable("x",""), 0);

    Map<LeftHandSide, Address> variableAddressMap = new HashMap<>();
    variableAddressMap.put(makeVariable("a", "main"), makeConcreteAddress(20));
    variableAddressMap.put(makeVariable("b", "main"), makeConcreteAddress(24));
    variableAddressMap.put(makeVariable("c", "main"), makeConcreteAddress(28));
    variableAddressMap.put(makeVariable("d", "main"), makeConcreteAddress(32));
    variableAddressMap.put(makeVariable("e", "main"), makeConcreteAddress(56));
    variableAddressMap.put(makeVariable("list", "main"), makeConcreteAddress(80));

    Map<String, Memory> allocatedMemory = new HashMap<>();
    Map<Address, Object> values = new HashMap<>();
    values.put(makeConcreteAddress(20), 1); // a = 1
    values.put(makeConcreteAddress(24), 20); // *b = 1
    values.put(makeConcreteAddress(28), 24); // **c = 1
    values.put(makeConcreteAddress(32), 21); // d[0] = 21
    values.put(makeConcreteAddress(36), 22); // d[1] = 22
    values.put(makeConcreteAddress(40), 23); // d[2] = 23
    values.put(makeConcreteAddress(44), 24); // d[3] = 24
    values.put(makeConcreteAddress(48), 25); // d[4] = 25
    values.put(makeConcreteAddress(52), 26); // d[5] = 26
    values.put(makeConcreteAddress(56), 21); // e[0][0] = 21
    values.put(makeConcreteAddress(60), 22); // e[0][1] = 22
    values.put(makeConcreteAddress(64), 23); // e[0][2] = 23
    values.put(makeConcreteAddress(68), 24); // e[1][0] = 24
    values.put(makeConcreteAddress(72), 25); // e[1][1] = 25
    values.put(makeConcreteAddress(76), 26); // e[1][2] = 26
    values.put(makeConcreteAddress(80), 31); // list.h = 31
    values.put(makeConcreteAddress(84), 108); // list.n->h = 41
    values.put(makeConcreteAddress(88), 32);  // list.d.h = 32
    values.put(makeConcreteAddress(92), 33);  // list.d.h2[0] = 33
    values.put(makeConcreteAddress(96), 34);  // list.d.h2[1] = 34
    values.put(makeConcreteAddress(100), 35); // list.d.h2[2] = 35
    values.put(makeConcreteAddress(104), 32); //list.d.h3 = 21
    values.put(makeConcreteAddress(108), 41); // list.n.h = 41
    values.put(makeConcreteAddress(112), 80); // list.n->n.h = 31
    values.put(makeConcreteAddress(116), 42); // list.n->d.h = 42
    values.put(makeConcreteAddress(120), 43); // list.n->d.h2[0] = 43
    values.put(makeConcreteAddress(124), 44); // list.n->d.h2[1] = 44
    values.put(makeConcreteAddress(128), 45); // list.n->d.h2[2] = 45
    values.put(makeConcreteAddress(132), 36); // list.n->d.h3 = 22

    Memory memory = new Memory(MEMORYNAME, values);
    allocatedMemory.put(MEMORYNAME, memory);

    return new ConcreteState(variables, allocatedMemory, variableAddressMap, memoryName);
  }

  private Address makeConcreteAddress(int pValue) {
    return Address.valueOf(pValue);
  }

  private Address makeSymbolicAddress(int pValue) {
    return Address.valueOf(Integer.toString(pValue));
  }

  private LeftHandSide makeVariable(String pName, String pFunction) {

    if (pFunction.equals("")) {
      return new IDExpression(pName);
    } else {
      return new IDExpression(pName, pFunction);
    }
  }

  private ConcreteState createSymbolicState() {
    Map<LeftHandSide, Object> variables = new HashMap<>();
    variables.put(makeVariable("x",""), 0);

    Map<LeftHandSide, Address> variableAddressMap = new HashMap<>();
    variableAddressMap.put(makeVariable("a", "main"), makeSymbolicAddress(20));
    variableAddressMap.put(makeVariable("b", "main"), makeSymbolicAddress(24));
    variableAddressMap.put(makeVariable("c", "main"), makeSymbolicAddress(28));
    variableAddressMap.put(makeVariable("d", "main"), makeSymbolicAddress(32));
    variableAddressMap.put(makeVariable("e", "main"), makeSymbolicAddress(56));
    variableAddressMap.put(makeVariable("list", "main"), makeSymbolicAddress(80));

    Map<String, Memory> allocatedMemory = new HashMap<>();
    Map<Address, Object> values = new HashMap<>();
    values.put(makeSymbolicAddress(20), 1); // a = 1
    values.put(makeSymbolicAddress(24), "20"); // *b = 1
    values.put(makeSymbolicAddress(28), "24"); // **c = 1
    values.put(makeSymbolicAddress(32), 21); // d[0] = 21
    values.put(makeSymbolicAddress(36), 22); // d[1] = 22
    values.put(makeSymbolicAddress(40), 23); // d[2] = 23
    values.put(makeSymbolicAddress(44), 24); // d[3] = 24
    values.put(makeSymbolicAddress(48), 25); // d[4] = 25
    values.put(makeSymbolicAddress(52), 26); // d[5] = 26
    values.put(makeSymbolicAddress(56), 21); // e[0][0] = 21
    values.put(makeSymbolicAddress(60), 22); // e[0][1] = 22
    values.put(makeSymbolicAddress(64), 23); // e[0][2] = 23
    values.put(makeSymbolicAddress(68), 24); // e[1][0] = 24
    values.put(makeSymbolicAddress(72), 25); // e[1][1] = 25
    values.put(makeSymbolicAddress(76), 26); // e[1][2] = 26
    values.put(makeSymbolicAddress(80), 31); // list.h = 31
    values.put(makeSymbolicAddress(84), "108"); // list.n->h = 41
    values.put(makeSymbolicAddress(88), 32);  // list.d.h = 32
    values.put(makeSymbolicAddress(92), 33);  // list.d.h2[0] = 33
    values.put(makeSymbolicAddress(96), 34);  // list.d.h2[1] = 34
    values.put(makeSymbolicAddress(100), 35); // list.d.h2[2] = 35
    values.put(makeSymbolicAddress(104), "32"); //list.d->h3 = 21
    values.put(makeSymbolicAddress(108), 41); // list.n.h = 41
    values.put(makeSymbolicAddress(112), "80"); // list.n->n.h = 31
    values.put(makeSymbolicAddress(116), 42); // list.n->d.h = 42
    values.put(makeSymbolicAddress(120), 43); // list.n->d.h2[0] = 43
    values.put(makeSymbolicAddress(124), 44); // list.n->d.h2[1] = 44
    values.put(makeSymbolicAddress(128), 45); // list.n->d.h2[2] = 45
    values.put(makeSymbolicAddress(132), "36"); // list.n->d.h3 = 22

    Memory memory = new Memory(MEMORYNAME, values);
    allocatedMemory.put(MEMORYNAME, memory);

    return new ConcreteState(variables, allocatedMemory, variableAddressMap, memoryName);
  }

  private ConcreteState createEmptyState() {


    Map<LeftHandSide, Object> pVariables = ImmutableMap.of();
    Map<String, Memory> pAllocatedMemory = ImmutableMap.of();
    Map<LeftHandSide, Address> pVariableAddressMap = ImmutableMap.of();
    return new ConcreteState(pVariables, pAllocatedMemory, pVariableAddressMap, memoryName);
  }

  @Test
  public void testAllocateAssignmentsToEdge() throws InvalidConfigurationException {

    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        testWithEdge(edge);
      }
    }
  }

  private void testWithEdge(CFAEdge pEdge) throws InvalidConfigurationException {

    Configuration testConfig = Configuration
        .builder()
        .copyFrom(Configuration.defaultConfiguration())
        .setOption("counterexample.export.assumptions.includeConstantsForPointers", "true")
        .build();

    AssumptionToEdgeAllocator allocator = new AssumptionToEdgeAllocator(testConfig, logger, machineModel);

    CFAEdgeWithAssumptions assignmentEdgeFull = allocator.allocateAssumptionsToEdge(pEdge, full);
    CFAEdgeWithAssumptions assignmentEdgeSymbolic = allocator.allocateAssumptionsToEdge(pEdge, symbolic);
    CFAEdgeWithAssumptions assignmentEdgeEmpty = allocator.allocateAssumptionsToEdge(pEdge, empty);
    Truth.assertThat(assignmentEdgeEmpty.getExpStmts()).isEmpty();

    switch (pEdge.getRawStatement()) {
    case "int x;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("x == (0);");
      Truth.assertThat(assignmentEdgeSymbolic.getAsCode()).contains("x == (0);");
      break;
    case "int a;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("a == (1);");
      Truth.assertThat(assignmentEdgeSymbolic.getAsCode()).contains("a == (1);");
      break;
    case "int* b;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(b)) == (1);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("b == (20LL);");
      Truth.assertThat(assignmentEdgeSymbolic.getAsCode()).contains("(*(b)) == (1);");
      break;
    case "int** c;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(*(c))) == (1);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(c)) == (20LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("c == (24LL);");
      Truth.assertThat(assignmentEdgeSymbolic.getAsCode()).contains("(*(*(c))) == (1);");
      break;
    case "int  d[6];":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(&d) == (32LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[0]) == (21);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[1]) == (22);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[2]) == (23);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[3]) == (24);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[4]) == (25);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[5]) == (26);");
      break;
    case "int  e[2][3];":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(&e) == (56LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[0][0]) == (21);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[0][1]) == (22);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[0][2]) == (23);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[1][0]) == (24);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[1][1]) == (25);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[1][2]) == (26);");
      break;
    case "List list;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(signed long long int)(&list)) == (80LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.h) == (31);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n) == (108LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->h) == (41);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->n) == (80LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->n->d.h) == (32);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->n->d.h2)[0]) == (33);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->n->d.h2)[1]) == (34);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->n->d.h2)[2]) == (35);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->n->d.h3) == (32LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(list.n->n->d.h3)) == (21);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->d.h) == (42);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->d.h2)[0]) == (43);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->d.h2)[1]) == (44);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->d.h2)[2]) == (45);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->d.h3) == (36LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(list.n->d.h3)) == (22);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(list.n->d.h3)) == (22);");
      break;
    case "a = a;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("a == (1);");
      break;
    case "*b = *b;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(b)) == (1);");
      break;
    case "**c = **c;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(*(c))) == (1);");
      break;
    case "d[1] = d[1];":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(d[1]) == (22);");
      break;
    case "*(d + 1) = *(d + 1);":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(d + (1))) == (22);");
      break;
    case "e[1][2] = e[1][2];":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(e[1][2]) == (26);");
      break;
    case "list.h = list.h;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.h) == (31);");
      break;
    case "list.n = list.n;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n) == (108LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->h) == (41);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->n->h) == (31);");
      break;
    case "list.d.h = list.d.h;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.d.h) == (32);");
      break;
    case "list.n->d.h2[0] = list.n->d.h2[0];":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("((list.n->d.h2)[0]) == (43);");
      break;
    case "list.n->d.h3 = list.n->d.h3;":
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(list.n->d.h3) == (36LL);");
      Truth.assertThat(assignmentEdgeFull.getAsCode()).contains("(*(list.n->d.h3)) == (22);");
      break;
    default:
      Truth.assertThat(pEdge.getRawStatement()).doesNotContain(" = ");
    }
  }
}
