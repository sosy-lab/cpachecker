/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic.csif;


import static org.junit.Assert.*;

import org.junit.Test;


public class CorestarInterfaceTest {

  @SuppressWarnings("unchecked")
  private void compareEquality(String str) {
    CorestarInterface csif = CorestarInterface.getInstance();
    System.err.println(str);
    System.err.println(csif.parse(str, true).toString());
    System.err.println(csif.parse(str).getInternalRepr());
    assertEquals("unequal", str, csif.parse(str).toString());
  }

  @Test
  public void testEntailsAndEquality() {
    CorestarInterface csif = CorestarInterface.getInstance();
    assertTrue(csif.entails("lspe(nil, nil)", "lspe(nil, nil)"));
    compareEquality("(field(y, f, j) * field(a, b, c))");
    compareEquality("((x != nil() * (x != nil() || field(x, f, y))) * Emp)");
    compareEquality("len(cons(_x, empty())) = builtin_plus(numeric_const(\"1\"), numeric_const(\"0\"))");
    compareEquality("!pure(x, y)");
    assertEquals("Garbage()", csif.abstract_("Garbage() * Garbage()").get(0));
    // System.out.println("AST: " + printTree((GraphNode<?>) value, new ToStringFormatter<GraphNode>(null)) + '\n');
  }

  @Test
  public void testParseable() {
    CorestarInterface csif = CorestarInterface.getInstance();
    csif.parse("abc(\"8\") = \"8\"\n * \"1\" = \"2\"", true);
  }
}
