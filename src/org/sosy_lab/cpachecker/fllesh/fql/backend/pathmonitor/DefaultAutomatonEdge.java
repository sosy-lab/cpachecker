/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor;

import org.jgrapht.DirectedGraph;

public abstract class DefaultAutomatonEdge implements AutomatonEdge {

  private Integer mSource;
  private Integer mTarget;

  public DefaultAutomatonEdge(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation) {
    assert(pSource != null);
    assert(pTarget != null);
    assert(pTransitionRelation != null);

    mSource = pSource;
    mTarget = pTarget;

    pTransitionRelation.addVertex(mSource);
    pTransitionRelation.addVertex(mTarget);
    pTransitionRelation.addEdge(mSource, mTarget, this);
  }

  public Integer getSource() {
    return mSource;
  }

  public Integer getTarget() {
    return mTarget;
  }

}
