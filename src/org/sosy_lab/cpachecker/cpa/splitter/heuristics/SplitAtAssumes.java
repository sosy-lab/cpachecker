/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.splitter.heuristics;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

public class SplitAtAssumes implements SplitHeuristic {

  public SplitAtAssumes() {}

  @Override
  public boolean removeSplitIndices(final CFAEdge pCfaEdge) {
    return false;
  }

  @Override
  public Collection<Integer> getIndicesToRemove(final CFAEdge pCfaEdge) {
    return Collections.emptyList();
  }

  @Override
  public boolean divideSplitIndices(CFAEdge pCfaEdge) {
    return pCfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge;
  }

  @Override
  public int divideIntoHowManyParts(CFAEdge pCfaEdge) {
    return 2;
  }
}
