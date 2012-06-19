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

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionEntryNode;

/**
 * Marker class for edges that were created due to a function pointer call.
 */
class FunctionPointerCallEdge extends CFunctionCallEdge {

  public FunctionPointerCallEdge(String pRawStatement,
      int pLineNumber, CFANode pPredecessor, CFunctionEntryNode pSuccessor,
      CFunctionCall pFunctionCall, CFunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pLineNumber, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);
  }

}
