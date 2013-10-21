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
package org.sosy_lab.cpachecker.cpa.cpalien.SMGJoin;

import org.junit.Assert;
import org.junit.Test;

public class SMGJoinTest {

  private void joinUpdateUnit(SMGJoinStatus firstOperand, SMGJoinStatus forLe, SMGJoinStatus forRe) {
    Assert.assertEquals(firstOperand, SMGUpdateJoinStatus.updateStatus(firstOperand, SMGJoinStatus.EQUAL));
    Assert.assertEquals(forLe, SMGUpdateJoinStatus.updateStatus(firstOperand, SMGJoinStatus.LEFT_ENTAIL));
    Assert.assertEquals(forRe, SMGUpdateJoinStatus.updateStatus(firstOperand, SMGJoinStatus.RIGHT_ENTAIL));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGUpdateJoinStatus.updateStatus(firstOperand, SMGJoinStatus.INCOMPARABLE));
  }

  @Test
  public void joinUpdateTest() {
    joinUpdateUnit(SMGJoinStatus.EQUAL, SMGJoinStatus.LEFT_ENTAIL,
        SMGJoinStatus.RIGHT_ENTAIL);
    joinUpdateUnit(SMGJoinStatus.LEFT_ENTAIL, SMGJoinStatus.LEFT_ENTAIL,
        SMGJoinStatus.INCOMPARABLE);
    joinUpdateUnit(SMGJoinStatus.RIGHT_ENTAIL, SMGJoinStatus.INCOMPARABLE,
        SMGJoinStatus.RIGHT_ENTAIL);
    joinUpdateUnit(SMGJoinStatus.INCOMPARABLE, SMGJoinStatus.INCOMPARABLE,
        SMGJoinStatus.INCOMPARABLE);
  }
}
