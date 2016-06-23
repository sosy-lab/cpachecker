/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.TrinaryEqualable.Equality;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchLabelExact;

import com.google.common.truth.Truth;


public class AutomatonBoolExprTest {

  @Test
  public void test_MatchLabelExact_EqualityTo() {
    MatchLabelExact l1_1 = new AutomatonBoolExpr.MatchLabelExact("L1");
    MatchLabelExact l1_2 = new AutomatonBoolExpr.MatchLabelExact("L1");
    MatchLabelExact l2_1 = new AutomatonBoolExpr.MatchLabelExact("L2");

    Truth.assertThat(l1_1.equalityTo(l1_2)).isEqualTo(Equality.EQUAL);
    Truth.assertThat(l1_1.equalityTo(l2_1)).isEqualTo(Equality.UNEQUAL);
  }

}
