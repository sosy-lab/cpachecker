/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import static org.mockito.Mockito.mock;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


public class SMGTest {
  private SMG smg;
  SMGObject obj1 = new SMGObject(8, "object-1");
  SMGObject obj2 = new SMGObject(8, "object-2");

  Integer val1 = Integer.valueOf(1);
  Integer val2 = Integer.valueOf(2);

  SMGEdgePointsTo pt1to1 = new SMGEdgePointsTo(val1, obj1, 0);
  SMGEdgeHasValue hv2has2at0 = new SMGEdgeHasValue(mock(CType.class), 0, obj2, val2);
  SMGEdgeHasValue hv2has1at4 = new SMGEdgeHasValue(mock(CType.class), 4, obj2, val1);

  @Before
  public void setUp(){
    smg = new SMG();

    smg.addObject(obj1);
    smg.addObject(obj2);

    smg.addValue(val1.intValue());
    smg.addValue(val2.intValue());

    smg.addPointsToEdge(pt1to1);

    smg.addHasValueEdge(hv2has2at0);
    smg.addHasValueEdge(hv2has1at4);
  }

  @Test
  public void testGetObjects() {
    HashSet<SMGObject> set = new HashSet<>();
    set.add(obj1);
    set.add(obj2);

    Assert.assertTrue(smg.getObjects().containsAll(set));
    Assert.assertTrue(set.containsAll(smg.getObjects()));
  }

  @Test
  public void testGetObjectPointedBy(){
    Assert.assertEquals(obj1, smg.getObjectPointedBy(val1));
    Assert.assertNull(smg.getObjectPointedBy(val2));
  }

  @Test
  public void getGetValuesForObject(){
    Assert.assertEquals(smg.getValuesForObject(obj1).size(), 0);
    Assert.assertEquals(smg.getValuesForObject(obj2).size(), 2);

    Assert.assertEquals(smg.getValuesForObject(obj2, 0).size(), 1);
    Assert.assertTrue(smg.getValuesForObject(obj2, 0).contains(hv2has2at0));
    Assert.assertEquals(smg.getValuesForObject(obj2, 3).size(), 0);
    Assert.assertEquals(smg.getValuesForObject(obj2, 4).size(), 1);
    Assert.assertTrue(smg.getValuesForObject(obj2, 4).contains(hv2has1at4));

    //TODO: Filter by types
  }

}
