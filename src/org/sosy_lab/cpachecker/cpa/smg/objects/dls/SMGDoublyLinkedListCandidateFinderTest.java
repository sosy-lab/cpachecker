/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import com.google.common.collect.ImmutableList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Set;


public class SMGDoublyLinkedListCandidateFinderTest {

  private final CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  private final CFunctionDeclaration functionDeclaration3 = new CFunctionDeclaration(FileLocation.DUMMY, functionType, "main", ImmutableList.<CParameterDeclaration>of());
  private CSimpleType intType = new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);
  private CType pointerType = new CPointerType(false, false, intType);

  private CLangSMG smg1;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    smg1 = new CLangSMG(MachineModel.LINUX32);
  }

  @Test
  public void simpleFindAbstractionCandidateTest() throws SMGInconsistentException {

    smg1.addStackFrame(functionDeclaration3);

    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();

    SMGRegion l1 = new SMGRegion(96, "l1");
    SMGRegion l2 = new SMGRegion(96, "l2");
    SMGRegion l3 = new SMGRegion(96, "l3");
    SMGRegion l4 = new SMGRegion(96, "l4");
    SMGRegion l5 = new SMGRegion(96, "l5");
    SMGRegion head = new SMGRegion(64, "head");

    SMGEdgeHasValue headfn = new SMGEdgeHasValue(pointerType, 0, head, 6);
    SMGEdgeHasValue l1fn = new SMGEdgeHasValue(pointerType, 0, l1, 7);
    SMGEdgeHasValue l2fn = new SMGEdgeHasValue(pointerType, 0, l2, 8);
    SMGEdgeHasValue l3fn = new SMGEdgeHasValue(pointerType, 0, l3, 9);
    SMGEdgeHasValue l4fn = new SMGEdgeHasValue(pointerType, 0, l4, 10);
    SMGEdgeHasValue l5fn = new SMGEdgeHasValue(pointerType, 0, l5, 5);

    SMGEdgeHasValue l1fp = new SMGEdgeHasValue(pointerType, 32, l1, 5);
    SMGEdgeHasValue l2fp = new SMGEdgeHasValue(pointerType, 32, l2, 6);
    SMGEdgeHasValue l3fp = new SMGEdgeHasValue(pointerType, 32, l3, 7);
    SMGEdgeHasValue l4fp = new SMGEdgeHasValue(pointerType, 32, l4, 8);
    SMGEdgeHasValue l5fp = new SMGEdgeHasValue(pointerType, 32, l5, 9);
    SMGEdgeHasValue headfp = new SMGEdgeHasValue(pointerType, 32, head, 10);

    SMGEdgePointsTo lht = new SMGEdgePointsTo(5, head, 0);
    SMGEdgePointsTo l1t = new SMGEdgePointsTo(6, l1, 0);
    SMGEdgePointsTo l2t = new SMGEdgePointsTo(7, l2, 0);
    SMGEdgePointsTo l3t = new SMGEdgePointsTo(8, l3, 0);
    SMGEdgePointsTo l4t = new SMGEdgePointsTo(9, l4, 0);
    SMGEdgePointsTo l5t = new SMGEdgePointsTo(10, l5, 0);


    smg1.addGlobalObject(head);
    smg1.addHeapObject(l1);
    smg1.addHeapObject(l2);
    smg1.addHeapObject(l3);
    smg1.addHeapObject(l4);
    smg1.addHeapObject(l5);

    smg1.addValue(5);
    smg1.addValue(6);
    smg1.addValue(7);
    smg1.addValue(8);
    smg1.addValue(9);
    smg1.addValue(10);

    smg1.addHasValueEdge(headfn);
    smg1.addHasValueEdge(l1fn);
    smg1.addHasValueEdge(l2fn);
    smg1.addHasValueEdge(l3fn);
    smg1.addHasValueEdge(l4fn);
    smg1.addHasValueEdge(l5fn);

    smg1.addHasValueEdge(headfp);
    smg1.addHasValueEdge(l1fp);
    smg1.addHasValueEdge(l2fp);
    smg1.addHasValueEdge(l3fp);
    smg1.addHasValueEdge(l4fp);
    smg1.addHasValueEdge(l5fp);

    smg1.addPointsToEdge(l1t);
    smg1.addPointsToEdge(l2t);
    smg1.addPointsToEdge(l3t);
    smg1.addPointsToEdge(l4t);
    smg1.addPointsToEdge(l5t);
    smg1.addPointsToEdge(lht);

    smg1.setValidity(l1, true);
    smg1.setValidity(l2, true);
    smg1.setValidity(l3, true);
    smg1.setValidity(l4, true);
    smg1.setValidity(l5, true);
    smg1.setValidity(head, true);

    SMGDoublyLinkedListCandidateFinder f = new SMGDoublyLinkedListCandidateFinder();

     Set<SMGAbstractionCandidate> s = f.traverse(smg1, null);

     Assert.assertTrue(s.size() > 0);
  }
}
