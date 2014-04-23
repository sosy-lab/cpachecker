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
package org.sosy_lab.cpachecker.cpa.smgfork;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGRegion;

public class SMGEdgePointsToTest {

  @Test
  public void testSMGEdgePointsTo() {
    Integer val = Integer.valueOf(6);
    SMGObject obj = new SMGRegion(8, "object");
    SMGEdgePointsTo edge = new SMGEdgePointsTo(val, obj, 0);

    Assert.assertEquals(val.intValue(), edge.getValue());
    Assert.assertEquals(obj, edge.getObject());
    Assert.assertEquals(0, edge.getOffset());
  }

  @Test
  public void testIsConsistentWith() {
    Integer val1 = Integer.valueOf(1);
    Integer val2 = Integer.valueOf(2);
    SMGObject obj = new SMGRegion(8, "object");
    SMGObject obj2 = new SMGRegion(8, "object2");

    SMGEdgePointsTo edge1 = new SMGEdgePointsTo(val1, obj, 0);
    SMGEdgePointsTo edge2 = new SMGEdgePointsTo(val2, obj, 0);
    SMGEdgePointsTo edge3 = new SMGEdgePointsTo(val1, obj, 4);
    SMGEdgePointsTo edge4 = new SMGEdgePointsTo(val1, obj2, 0);

    // An edge is consistent with itself
    Assert.assertTrue(edge1.isConsistentWith(edge1));

    // Different vals pointing to same place: violates "injective"
    Assert.assertFalse(edge1.isConsistentWith(edge2));

    // Same val pointing to different offsets
    Assert.assertFalse(edge1.isConsistentWith(edge3));

    // Same val pointing to different objects
    Assert.assertFalse(edge1.isConsistentWith(edge4));
  }
}
