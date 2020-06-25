/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

class LoopEntry {

  private final CFANode entryPoint;

  private final Loop loop;

  public LoopEntry(CFANode pEntryPoint, Loop pLoop) {
    Preconditions.checkArgument(pLoop.getLoopHeads().contains(pEntryPoint));
    entryPoint = Objects.requireNonNull(pEntryPoint);
    loop = Objects.requireNonNull(pLoop);
  }

  public CFANode getEntryPoint() {
    return entryPoint;
  }

  public Loop getLoop() {
    return loop;
  }

  @Override
  public String toString() {
    return "Loop starting at " + entryPoint;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof LoopEntry) {
      LoopEntry other = (LoopEntry) pObj;
      return entryPoint.equals(other.entryPoint)
          && loop.equals(other.loop);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entryPoint, loop);
  }

}
