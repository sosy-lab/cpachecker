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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.HashMap;

import org.sosy_lab.cpachecker.cpa.location.LocationState;

class OctWideningControl {

  HashMap<Integer, LoopNode> loopNodeList = new HashMap<>();

  static class LoopNode {
    @SuppressWarnings("unused")
    private int nodeId;
    private int iterationCount = 0;
    private boolean isWideningUsed = false;

    public LoopNode(int id) {
      nodeId = id;
    }

    public boolean exceedThreshold() {
      return iterationCount > OctConstants.wideningThreshold;
    }

    public boolean isWideningUsed() {
      if (isWideningUsed) {
        return true;
      } else {
        iterationCount++;
        if (exceedThreshold()) {
          isWideningUsed = true;
        }
      }
      return isWideningUsed;
    }

    public void switchToWideningUsed() {
      isWideningUsed = true;
    }
  }

  public boolean isWideningUsed(LocationState le) {
    Integer nodeId = le.getLocationNode().getNodeNumber();
    LoopNode ln;
    if (loopNodeList.containsKey(nodeId)) {
      ln = loopNodeList.get(nodeId);
      return ln.isWideningUsed();
    } else {
      ln = new LoopNode(nodeId);
      loopNodeList.put(nodeId, ln);
      return   ln.isWideningUsed();
    }
  }
}
