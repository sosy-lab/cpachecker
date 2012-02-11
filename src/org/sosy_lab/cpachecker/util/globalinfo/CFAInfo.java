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
package org.sosy_lab.cpachecker.util.globalinfo;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


public class CFAInfo {
  private final CFA cfa;
  private final Map<Integer, CFANode> nodeNumberToNode;

  CFAInfo(CFA cfa) {
    this.cfa = cfa;

    HashMap<Integer, CFANode> nodeNumberToNode = new HashMap<Integer, CFANode>();
    for(CFANode node : cfa.getAllNodes()) {
      nodeNumberToNode.put(node.getNodeNumber(), node);
    }
    this.nodeNumberToNode = nodeNumberToNode;
  }

  public CFANode getNodeByNodeNumber(int nodeNumber) {
    return nodeNumberToNode.get(nodeNumber);
  }
}
