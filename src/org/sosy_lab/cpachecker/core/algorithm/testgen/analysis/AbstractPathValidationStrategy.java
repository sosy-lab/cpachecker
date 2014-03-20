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
package org.sosy_lab.cpachecker.core.algorithm.testgen.analysis;

import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;


public abstract class AbstractPathValidationStrategy implements PathValidationStrategy {

  @Override
  public CounterexampleTraceInfo checkPathCandidate(Pair<ARGState, CFAEdge> pCurrentElement, List<CFAEdge> pNewPath) throws CPATransferException, InterruptedException {
    return checkPath(pNewPath);
  }

  @Override
  public void handleSpuriousPath(List<CFAEdge> pNewPath) {
  }

  @Override
  public void handleSinglePathElement(Pair<ARGState, CFAEdge> pCurrentElement) {
//    handleElement(pCurrentElement.getSecond());
  }

  @Override
  public void handleNewCheck(ARGPath pExecutedPath) {
  }

  @Override
  public void handleNext(long pNodeCounter) {
  }

}
