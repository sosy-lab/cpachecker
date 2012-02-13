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
package org.sosy_lab.pcc.common;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public class WithCorrespondingCFAEdgeARTEdge extends ARTEdge{


  private CFAEdge cfaEdge;


  public WithCorrespondingCFAEdgeARTEdge(int pTargetARTId, CFAEdge pCFAEdge)
      throws IllegalArgumentException {
    super(pTargetARTId);
    if (pCFAEdge == null) { throw new IllegalArgumentException(
        "Cannot be a valid ART edge."); }
    cfaEdge = pCFAEdge;

  }

  public CFAEdge getCorrespondingCFAEdge() {
    return cfaEdge;
  }

  public boolean equals(WithCorrespondingCFAEdgeARTEdge pEdge){
    if(pEdge == null){ return false;}
    if(getTarget() == pEdge.getTarget() && cfaEdge.equals(pEdge.cfaEdge)){
      return true;
    }
    return false;
  }
}
