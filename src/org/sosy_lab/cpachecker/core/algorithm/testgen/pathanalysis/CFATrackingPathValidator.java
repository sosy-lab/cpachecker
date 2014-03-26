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
package org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis;

import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Lists;


public class CFATrackingPathValidator extends AbstractPathValidator{

  private final PathChecker pathChecker;
  private final List<CFANode> handledDecisions;

  public CFATrackingPathValidator(PathChecker pPathChecker, StartupConfig pConfig) {
    super(pConfig);
    pathChecker = pPathChecker;
    handledDecisions = Lists.newLinkedList();
  }

  @Override
  public CounterexampleTraceInfo validatePath(List<CFAEdge> pPath) throws CPATransferException,
      InterruptedException {
    return pathChecker.checkPath(pPath);
  }

  @Override
  public boolean isVisitedBranching(ARGPath pNewARGPath, Pair<ARGState, CFAEdge> pCurrentElement, CFANode pNode,
      CFAEdge pOtherEdge) {
    return handledDecisions.contains(pNode);
  }

  @Override
  public void handleValidPath(PredicatePathAnalysisResult result) {
    handledDecisions.add(result.getDecidingNode());
  }

}
