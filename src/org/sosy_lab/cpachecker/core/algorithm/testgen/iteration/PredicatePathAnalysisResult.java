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
package org.sosy_lab.cpachecker.core.algorithm.testgen.iteration;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

/**
 * represents the result of a path modification.
 * Consists of a solver result, as well as the deciding node (the last node that both paths have in common)
 * and the 'wrong' node (the successor of the deciding node that is no longer in the path)
 * The stored {@link ARGState}s should be used with care, because the states might not represent the model.
 * (the list of cfa edges matches the model, but the {@link ARGState}s can differ)
 */
public class PredicatePathAnalysisResult {

  public static final PredicatePathAnalysisResult INVALID = new PredicatePathAnalysisResult(null, null, null,null);

  public PredicatePathAnalysisResult(CounterexampleTraceInfo pTrace, ARGState pDecidingState,
      ARGState pWrongState, ARGPath pArgPath) {
    super();
    trace = pTrace;
    decidingState = pDecidingState;
    wrongState = pWrongState;
    this.argPath = pArgPath;

  }

  private CounterexampleTraceInfo trace;
  private ARGState decidingState;
  private ARGState wrongState;
  private ARGPath argPath;

  public CounterexampleTraceInfo getTrace() {
    checkValid();
    return trace;
  }

  public ARGState getDecidingState() {
    checkValid();
    return decidingState;
  }

  public ARGState getWrongState() {
    checkValid();
    return wrongState;
  }

  public CFANode getDecidingNode() {
    checkValid();
    return AbstractStates.extractLocation(decidingState);
  }

  public @Nullable CFANode getWrongNode() {
    ARGState wState = getWrongState();
    return AbstractStates.extractLocation(wState);
  }
  public CFAEdge getSelectedLastEdge() {
    checkValid();
    CFAEdge edge = argPath.getLast().getSecond();
    if(edge == null && argPath.size()>1) {
      edge = argPath.get(argPath.size()-2).getSecond();
    }
    return edge;
  }

  public ARGPath getPath() {
    checkValid();
    return argPath;
  }

  /**
   *
   * @return
   */
  public boolean isValid() {
    return !isEmpty();
  }

  /**
   * checks if the result contains any data. Especially returns true for {@link PredicatePathAnalysisResult#INVALID}
   * @return
   */
  public boolean isEmpty() {
    return trace == null;
  }

  private void checkValid() {
    if (!isValid()) { throw new IllegalStateException(
        "this is not a valid result. It is not allowed to access data of an invalid result");
    }
  }
}

