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

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;


public class RefinementPairStub implements ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>{

  @Override
  public void update(Class<? extends RefinementInterface> pCallerClass, Class<? extends RefinementInterface> pDstClass,
      Object pData) {}

  @Override
  public void start(Class<? extends RefinementInterface> pCallerClass) {}

  @Override
  public void finish(Class<? extends RefinementInterface> pCallerClass) throws CPAException, InterruptedException {}

  @Override
  public void printStatistics(StatisticsWriter pOut) {}

  @Override
  public RefinementResult performBlockRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput) throws CPAException, InterruptedException {

    return RefinementResult.createTrue(pInput.getFirst(), pInput.getSecond());
  }

}
