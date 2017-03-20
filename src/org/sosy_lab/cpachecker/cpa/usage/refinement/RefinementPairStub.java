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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.refinement.ConfigurableRefinementBlock;
import org.sosy_lab.cpachecker.cpa.usage.refinement.ExtendedARGPath;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementInterface;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementResult;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;


public class RefinementPairStub implements ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>{

  private UnsafeDetector detector = null;
  @Override
  public void update(Class<? extends RefinementInterface> pCallerClass, Class<? extends RefinementInterface> pDstClass,
      Object pData) {
  }

  @Override
  public void start(Class<? extends RefinementInterface> pCallerClass) {
  }

  @Override
  public void finish(Class<? extends RefinementInterface> pCallerClass) throws CPAException, InterruptedException {
  }

  @Override
  public void printStatistics(PrintStream pOut) {

  }

  @Override
  public RefinementResult performRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput) throws CPAException, InterruptedException {

    if (detector == null) {
      detector = AbstractStates.extractStateByType(pInput.getFirst().getUsageInfo().getKeyState(),
          UsageState.class).getContainer().getUnsafeDetector();

    }
    Set<UsageInfo> tmpSet = new HashSet<>();
    tmpSet.add(pInput.getFirst().getUsageInfo());
    tmpSet.add(pInput.getSecond().getUsageInfo());
    boolean b = detector.isUnsafe(tmpSet);
    if (b) {
      return RefinementResult.createTrue(pInput.getFirst(), pInput.getSecond());
    } else {
      return RefinementResult.createFalse();
    }
  }

}
