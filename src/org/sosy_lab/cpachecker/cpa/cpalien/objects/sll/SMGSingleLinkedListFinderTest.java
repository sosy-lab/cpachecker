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
package org.sosy_lab.cpachecker.cpa.cpalien.objects.sll;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.cpalien.CLangSMG;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGRegion;

import com.google.common.collect.Iterables;


public class SMGSingleLinkedListFinderTest {
  @Test
  public void simpleListTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 5, 16, 8);

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder();
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(1, candidates.size());
    SMGAbstractionCandidate candidate = Iterables.getOnlyElement(candidates);
    Assert.assertTrue(candidate instanceof SMGSingleLinkedListCandidate);
    SMGSingleLinkedListCandidate sllCandidate = (SMGSingleLinkedListCandidate)candidate;
    Assert.assertEquals(5, sllCandidate.getLength());
    Assert.assertEquals(8, sllCandidate.getOffset());
    SMGRegion expectedStart = (SMGRegion) smg.getPointer(root.getValue()).getObject();
    Assert.assertSame(expectedStart, sllCandidate.getStart());
  }

  @Test
  public void nullifiedPointerInferenceTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    TestHelpers.createGlobalList(smg, 2, 16, 8);

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder();
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(1, candidates.size());
  }
}
