/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgePointsToTest {

  @Test
  public void testSMGEdgePointsTo() {
    SMGValue val = SMGKnownExpValue.valueOf(6);
    SMGObject obj = new SMGRegion(64, "object");
    SMGEdgePointsTo edge = new SMGEdgePointsTo(val, obj, 0);

    assertThat(edge.getValue()).isEqualTo(val);
    assertThat(edge.getObject()).isEqualTo(obj);
    assertThat(edge.getOffset()).isEqualTo(0);
  }

  @Test
  public void testIsConsistentWith() {
    SMGValue val1 = SMGKnownExpValue.valueOf(1);
    SMGValue val2 = SMGKnownExpValue.valueOf(2);
    SMGObject obj = new SMGRegion(64, "object");
    SMGObject obj2 = new SMGRegion(64, "object2");

    SMGEdgePointsTo edge1 = new SMGEdgePointsTo(val1, obj, 0);
    SMGEdgePointsTo edge2 = new SMGEdgePointsTo(val2, obj, 0);
    SMGEdgePointsTo edge3 = new SMGEdgePointsTo(val1, obj, 32);
    SMGEdgePointsTo edge4 = new SMGEdgePointsTo(val1, obj2, 0);

    // An edge is consistent with itself
    assertThat(edge1.isConsistentWith(edge1)).isTrue();

    // Different vals pointing to same place: violates "injective"
    assertThat(edge1.isConsistentWith(edge2)).isFalse();

    // Same val pointing to different offsets
    assertThat(edge1.isConsistentWith(edge3)).isFalse();

    // Same val pointing to different objects
    assertThat(edge1.isConsistentWith(edge4)).isFalse();
  }
}
