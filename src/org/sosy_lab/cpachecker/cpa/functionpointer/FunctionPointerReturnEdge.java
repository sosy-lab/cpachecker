/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;

/**
 * Marker class for edges that were created due to a function pointer call.
 */
class FunctionPointerReturnEdge extends FunctionReturnEdge {

  private final FunctionPointerCallEdge callEdge;

  public FunctionPointerReturnEdge(int pLineNumber,
      CFAFunctionExitNode pPredecessor, CFANode pSuccessor,
      FunctionPointerCallEdge pCallEdge, CallToReturnEdge pSummaryEdge) {
    super(pLineNumber, pPredecessor, pSuccessor, pSummaryEdge);

    callEdge = pCallEdge;
  }

  public FunctionPointerCallEdge getCallEdge() {
    return callEdge;
  }
}
