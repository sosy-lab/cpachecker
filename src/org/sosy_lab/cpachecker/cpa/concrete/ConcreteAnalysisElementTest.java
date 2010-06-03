/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.concrete;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.HashMap;

public class ConcreteAnalysisElementTest {

  @Test
  public void testHashCode() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("x", 10);
    lElement.assignConstant("y", 55);

    ConcreteAnalysisElement lElement2 = new ConcreteAnalysisElement(lElement);

    assertEquals(lElement.hashCode(), lElement2.hashCode());

    assertFalse(ConcreteAnalysisTopElement.getInstance().hashCode() == ConcreteAnalysisBottomElement.getInstance().hashCode());

    int lElement2HashCode = lElement2.hashCode();

    lElement2.assignConstant("z", 999);

    // CAUTION: hash code changes!
    assertTrue(lElement2HashCode != lElement2.hashCode());
  }

  @Test
  public void testConcreteAnalysisElement() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    assertFalse(lElement.contains("x"));

    lElement.assignConstant("x", 10);

    assertTrue(lElement.contains("x"));

    ConcreteAnalysisElement lElement2 = new ConcreteAnalysisElement(lElement);

    assertTrue(lElement.equals(lElement2));

    HashMap<String, Long> lMap = new HashMap<String, Long>();

    lMap.put("x", Long.valueOf(10));

    ConcreteAnalysisElement lElement3 = new ConcreteAnalysisElement(lMap);

    assertTrue(lElement.equals(lElement3));

    //lMap.put("y", new Long(11));

    //lMap.put("x", new Long(15));

    assertTrue(lElement.equals(lElement3));

    assertFalse(lElement.equals(null));

    assertTrue(lElement.equals(lElement));
  }

  /*@Test
  public void testConcreteAnalysisElementMapOfStringLong() {
    fail("Not yet implemented");
  }

  @Test
  public void testConcreteAnalysisElementConcreteAnalysisElement() {
    fail("Not yet implemented");
  }*/

  @Test
  public void testAssignConstant() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("x", 10);

    assertEquals(lElement.getValueFor("x"), 10);

    // this operation was added to achieve basic block coverage in
    // assignConstant
    lElement.assignConstant("x", 10);

    assertEquals(lElement.getValueFor("x"), 10);
  }

  /*@Test
  public void testGetValueFor() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("x", 10);

    assert(lElement.getValueFor("x") == 10);
  }*/

  @Test
  public void testContains() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("x", 10);

    assertTrue(lElement.contains("x"));

    lElement.assignConstant("y", 55);

    assertTrue(lElement.contains("y"));

    assertFalse(lElement.contains("z"));

    ConcreteAnalysisElement lElement2 = new ConcreteAnalysisElement(lElement);

    assertTrue(lElement2.contains("x"));

    assertTrue(lElement2.contains("y"));

    assertFalse(lElement2.contains("z"));
  }

  @Test
  public void testClone() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("x", 10);
    lElement.assignConstant("y", 55);

    ConcreteAnalysisElement lElement2 = new ConcreteAnalysisElement(lElement);

    assertTrue(lElement.equals(lElement2));
  }

  @Test
  public void testEqualsObject() {
    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("z", -100);
    lElement.assignConstant("w", -800980);

    ConcreteAnalysisElement lElement2 = new ConcreteAnalysisElement(lElement);

    assertTrue(lElement.equals(lElement2));

    ConcreteAnalysisElement lElement3 = new ConcreteAnalysisElement();

    assertFalse(lElement3.equals(lElement2));

    assertFalse(lElement.equals(ConcreteAnalysisTopElement.getInstance()));

    assertFalse(lElement.equals(ConcreteAnalysisBottomElement.getInstance()));

    assertFalse(lElement.equals(null));

    assertTrue(lElement.equals(lElement));

    assertFalse(ConcreteAnalysisTopElement.getInstance().equals(ConcreteAnalysisBottomElement.getInstance()));

    assertFalse(ConcreteAnalysisBottomElement.getInstance().equals(ConcreteAnalysisTopElement.getInstance()));

    lElement2.assignConstant("z", 10);

    assertFalse(lElement.equals(lElement2));

    ConcreteAnalysisElement lElement4 = new ConcreteAnalysisElement();

    lElement4.assignConstant("u", 0);

    lElement4.assignConstant("z", 10);

    assertFalse(lElement.equals(lElement4));
  }

  @Test
  public void testToString() {
    assertFalse(ConcreteAnalysisTopElement.getInstance().toString().equals(ConcreteAnalysisBottomElement.getInstance().toString()));

    ConcreteAnalysisElement lElement = new ConcreteAnalysisElement();

    lElement.assignConstant("z", -100);
    lElement.assignConstant("w", -800980);

    assertEquals(lElement.toString(), "[ <w = -800980>  <z = -100> ] size->  2");
  }

  @Test
  public void testIsError() {
    assertFalse(ConcreteAnalysisBottomElement.getInstance().isError());
  }

}
