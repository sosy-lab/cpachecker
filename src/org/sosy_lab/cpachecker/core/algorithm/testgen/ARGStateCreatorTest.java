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
package org.sosy_lab.cpachecker.core.algorithm.testgen;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.testgen.dummygen.ARGStateDummyCreator;
import org.sosy_lab.cpachecker.core.algorithm.testgen.dummygen.TestgenTestHelper;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.Iterables;


public class ARGStateCreatorTest {

  UnmodifiableReachedSet reachedSet;
  CFA cfa;
  private ARGStateDummyCreator argStateDummyCreator;

  @Before
  public void setUp() throws Exception {
    Pair<UnmodifiableReachedSet, Triple<CFA,Configuration,LogManager>> p = TestgenTestHelper.createReachedSetFromFile("test/programs/simple/minimalIf.c");
    reachedSet = p.getFirst();

    cfa = p.getSecond().getFirst();
//    argStateDummyCreator = new ARGStateDummyCreator(cfa, p.getSecond().getSecond(), p.getSecond().getThird());
//    Statistics.printStatistics(out, Result.SAFE, reachedSet);
  }


  @Test
  public void test() {
    ARGState firstState = (ARGState) reachedSet.getFirstState();
    for (AbstractState state : reachedSet.asCollection()) {
      ARGState arg = (ARGState) state;

      if(arg.getChildren().size() > 1){
        ARGState followingState = Iterables.get(arg.getChildren(), 0);
        try {
          ARGState newState = argStateDummyCreator.computeOtherSuccessor(arg, followingState);
          Collection<ARGState> parents = newState.getParents();
        } catch (CPATransferException | InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    Assert.assertNotNull(reachedSet);
  }

}
