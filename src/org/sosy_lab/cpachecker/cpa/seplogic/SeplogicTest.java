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
package org.sosy_lab.cpachecker.cpa.seplogic;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Empty;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Equality;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeparatingConjunction;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.StringArgument;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class SeplogicTest {

  @Test
  public void testEntails() throws CPAException {
    SeplogicElement TOP = new SeplogicElement(new SeparatingConjunction(new Equality(new StringArgument("1"), new StringArgument("1")), new Empty()));
    AbstractDomain dom = new SeplogicDomain();
    SeplogicElement e1 = new SeplogicElement(new Equality(new StringArgument("1"), new StringArgument("1")));
    SeplogicElement e2 = new SeplogicElement(new Equality(new StringArgument("1"), new StringArgument("0")));
    assertTrue(!e1.entails(e2));
    assertTrue(e2.entails(e1));

    assertFalse(dom.isLessOrEqual(TOP, new SeplogicElement(new Equality(new StringArgument("1"), new StringArgument("0")))));
    assertTrue(dom.isLessOrEqual(new SeplogicElement(new Equality(new StringArgument("1"), new StringArgument("0"))), TOP));
  }

}
