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

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;

public class PredicatesEdge extends DefaultAutomatonEdge {

  private Predicates mPredicates;

  public PredicatesEdge(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation, Predicates pPredicates) {
    super(pSource, pTarget, pTransitionRelation);

    assert(pPredicates != null);
    //assert(!pPredicates.isEmpty());

    mPredicates = pPredicates;
  }

  public Predicates getPredicate() {
    return mPredicates;
  }

  @Override
  public String toString() {
    return getSource().toString() + "-<Predicates>->" + getTarget().toString();
  }

  @Override
  public <T> T accept(AutomatonEdgeVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

  @Override
  public AutomatonEdge duplicate(Integer pSource, Integer pTarget,
      DirectedGraph<Integer, AutomatonEdge> pTransitionRelation) {
    return new PredicatesEdge(pSource, pTarget, pTransitionRelation, mPredicates);
  }

}
