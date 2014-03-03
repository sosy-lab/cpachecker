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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.SMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGConsistencyVerifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.DummyAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;


public class SMGSingleLinkedListTest {
  static private final  LogManager logger = mock(LogManager.class);
  static private MachineModel mm = MachineModel.LINUX64;

  private SMGRegion prepareSingleLinkedList(SMGState pState) throws SMGInconsistentException {
    SMGRegion pointer = new SMGRegion(mm.getSizeofPtr(), "pointer");
    Integer listAddress = pState.getAddress(pState.getNullObject(), 0);
    pState.addGlobalObject(pointer);
    for (int i = 0; i < 15; i++) {
      SMGEdgePointsTo pt = pState.addNewHeapAllocation(mm.getSizeofPtr(), "node");
      pState.writeValue(pt.getObject(), 0, AnonymousTypes.dummyPointer, SMGKnownSymValue.valueOf(listAddress));
      listAddress = pt.getValue();
      pState.writeValue(pointer, 0, AnonymousTypes.dummyPointer,SMGKnownSymValue.valueOf(listAddress));
    }
    pState.attemptAbstraction();

    Integer sllAddress = pState.readValue(pointer, 0, AnonymousTypes.dummyPointer);
    SMGEdgePointsTo pt = pState.getPointerFromValue(sllAddress);
    SMGObject object = pt.getObject();

    Assert.assertTrue(object instanceof SMGSingleLinkedList);
    SMGSingleLinkedList sll = (SMGSingleLinkedList) object;
    Assert.assertEquals(15, sll.getLength());
    Assert.assertEquals(0, sll.getOffset());

    return pointer;
  }

  @Test
  public void freeTest() throws SMGInconsistentException, InvalidQueryException {
    SMGState state = new SMGState(logger, mm);
    SMGRegion variable = prepareSingleLinkedList(state);

    Integer address = state.readValue(variable, 0, AnonymousTypes.dummyPointer);
    SMGEdgePointsTo pt = state.getPointerFromValue(address);
    SMGSingleLinkedList sll = (SMGSingleLinkedList)(pt.getObject());
    int originalLength = sll.getLength();

    state.free(pt.getValue(), pt.getOffset(), sll);

    Integer addressAfterFree = state.getAddress(sll, 0);
    Assert.assertNotNull(addressAfterFree);
    Assert.assertEquals(originalLength - 1, sll.getLength());
    state.pruneUnreachable();

    Integer addressAfterPrune = state.getAddress(sll, 0);
    Assert.assertNull(addressAfterPrune);
    Assert.assertTrue(state.checkProperty("has-leaks"));
  }

  @Test
  public void consistencyInvalidSLLTest() {
    SMG smg = new SMG(mm);
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(prototype, 2, 4);
    smg.addObject(sll, false);

    Assert.assertFalse(SMGConsistencyVerifier.verifySMG(logger, smg));
  }

  @Test
  public void basicsTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(prototype, 2, 4);

    Assert.assertTrue(sll.isAbstract());
    Assert.assertEquals(4, sll.getLength());
    Assert.assertEquals(16, sll.getSize());
    Assert.assertEquals(2, sll.getOffset());
  }

  @Test
  public void matchGenericShapeTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 0, 4);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 0, 7);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(prototype, 8, 4);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    Assert.assertFalse(sll1.matchGenericShape(dummy));
    Assert.assertTrue(sll1.matchGenericShape(sll2));
    Assert.assertTrue(sll2.matchGenericShape(sll3));
    Assert.assertTrue(sll1.matchGenericShape(sll3));
  }

  @Test
  public void matchSpecificShapeTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 0, 4);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 0, 7);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(prototype, 8, 4);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    Assert.assertFalse(sll1.matchSpecificShape(dummy));
    Assert.assertTrue(sll1.matchSpecificShape(sll2));
    Assert.assertFalse(sll2.matchSpecificShape(sll3));
    Assert.assertFalse(sll1.matchSpecificShape(sll3));
  }

  @Test(expected=IllegalArgumentException.class)
  public void isMoreGenericDiffSizeTest() {
    SMGRegion prototype1 = new SMGRegion(16, "prototype_1");
    SMGRegion prototype2 = new SMGRegion(32, "prototype_2");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype1, 8, 8);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype2, 8, 8);
    sll1.isMoreGeneral(sll2);
  }

  @Test
  public void isMoreGenericConcreteTest() {
    SMGRegion prototype1 = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype1, 8, 8);
    Assert.assertTrue(sll1.isMoreGeneral(prototype1));
  }

  @Test(expected=IllegalArgumentException.class)
  public void isMoreGenericNonMatchTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 0, 8);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 8, 10);
    sll1.isMoreGeneral(sll2);
  }

  @Test
  public void isMoreGenericMatchTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 8, 4);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 8, 12);

    Assert.assertTrue(sll1.isMoreGeneral(sll2));
    Assert.assertFalse(sll2.isMoreGeneral(sll1));
  }
}
