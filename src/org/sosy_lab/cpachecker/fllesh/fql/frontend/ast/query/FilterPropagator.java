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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.FQLNode;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Coverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Edges;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Paths;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Sequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.States;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Alternative;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.FilterMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.PathMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.UpperBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.CIdentifier;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.NaturalNumber;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Compose;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Filter;

public class FilterPropagator implements ASTVisitor<FQLNode> {

  private Filter mFilter;

  public FilterPropagator(Filter pFilter) {
    assert(pFilter != null);

    mFilter = pFilter;
  }

  private Compose apply(Filter pFilter) {
    assert(pFilter != null);

    return new Compose(pFilter, mFilter);
  }

  @Override
  public FQLNode visit(Predicate pPredicate) {
    assert(false);

    return null;
  }

  @Override
  public FQLNode visit(CIdentifier pCIdentifier) {
    assert(false);

    return null;
  }

  @Override
  public FQLNode visit(NaturalNumber pNaturalNumber) {
    assert(false);

    return null;
  }

  @Override
  public FQLNode visit(Predicates pPredicates) {
    assert(false);

    return null;
  }

  @Override
  public FQLNode visit(States pStates) {
    return new States(apply(pStates.getFilter()), pStates.getPredicates());
  }

  @Override
  public FQLNode visit(Edges pEdges) {
    return new Edges(apply(pEdges.getFilter()), pEdges.getPredicates());
  }

  @Override
  public FQLNode visit(Paths pPaths) {
    return new Paths(apply(pPaths.getFilter()), pPaths.getBound(), pPaths.getPredicates());
  }

  @Override
  public FQLNode visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.SetMinus pSetMinus) {
    FQLNode lNode1 = pSetMinus.getLeftCoverage().accept(this);
    FQLNode lNode2 = pSetMinus.getRightCoverage().accept(this);

    assert(lNode1 instanceof Coverage);
    assert(lNode2 instanceof Coverage);

    return new org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.SetMinus((Coverage)lNode1, (Coverage)lNode2);
  }

  @Override
  public FQLNode visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Union pUnion) {
    FQLNode lNode1 = pUnion.getLeftCoverage().accept(this);
    FQLNode lNode2 = pUnion.getRightCoverage().accept(this);

    assert(lNode1 instanceof Coverage);
    assert(lNode2 instanceof Coverage);

    return new org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Union((Coverage)lNode1, (Coverage)lNode2);
  }

  @Override
  public FQLNode visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Intersection pIntersection) {
    FQLNode lNode1 = pIntersection.getLeftCoverage().accept(this);
    FQLNode lNode2 = pIntersection.getRightCoverage().accept(this);

    assert(lNode1 instanceof Coverage);
    assert(lNode2 instanceof Coverage);

    return new org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Intersection((Coverage)lNode1, (Coverage)lNode2);
  }

  @Override
  public FQLNode visit(ConditionalCoverage pConditionalCoverage) {
    FQLNode lNode = pConditionalCoverage.getCoverage().accept(this);

    assert(lNode instanceof Coverage);

    return new ConditionalCoverage(pConditionalCoverage.getPreconditions(), (Coverage)lNode, pConditionalCoverage.getPostconditions());
  }

  @Override
  public FQLNode visit(Sequence pSequence) {
    // we do not change initial and final path monitor

    Pair<PathMonitor, Coverage> lPair = pSequence.get(0);

    PathMonitor lInitialMonitor = lPair.getFirst();
    Coverage lCoverage = lPair.getSecond();

    FQLNode lNode = lCoverage.accept(this);

    assert(lNode instanceof Coverage);

    Sequence lSequence = new Sequence(lInitialMonitor, (Coverage)lNode, pSequence.getFinalMonitor());

    for (int i = 1; i < pSequence.size(); i++) {
      Pair<PathMonitor, Coverage> lPair2 = pSequence.get(i);

      FQLNode lNode1 = lPair2.getFirst().accept(this);
      FQLNode lNode2 = lPair2.getSecond().accept(this);

      assert(lNode1 instanceof PathMonitor);
      assert(lNode2 instanceof Coverage);

      lSequence.extend((PathMonitor)lNode1, (Coverage)lNode2);
    }

    return lSequence;
  }

  @Override
  public FQLNode visit(ConditionalMonitor pConditionalMonitor) {
    FQLNode lNode = pConditionalMonitor.getSubmonitor().accept(this);

    assert(lNode instanceof PathMonitor);

    return new ConditionalMonitor(pConditionalMonitor.getPreconditions(), (PathMonitor)lNode, pConditionalMonitor.getPostconditions());
  }

  @Override
  public FQLNode visit(Alternative pAlternative) {
    FQLNode lNode1 = pAlternative.getLeftSubmonitor().accept(this);
    FQLNode lNode2 = pAlternative.getRightSubmonitor().accept(this);

    assert(lNode1 instanceof PathMonitor);
    assert(lNode2 instanceof PathMonitor);

    return new Alternative((PathMonitor)lNode1, (PathMonitor)lNode2);
  }

  @Override
  public FQLNode visit(Concatenation pConcatenation) {
    FQLNode lNode1 = pConcatenation.getLeftSubmonitor().accept(this);
    FQLNode lNode2 = pConcatenation.getRightSubmonitor().accept(this);

    assert(lNode1 instanceof PathMonitor);
    assert(lNode2 instanceof PathMonitor);

    return new Concatenation((PathMonitor)lNode1, (PathMonitor)lNode2);
  }

  @Override
  public FQLNode visit(UpperBound pUpperBound) {
    FQLNode lNode = pUpperBound.getSubmonitor().accept(this);

    assert(lNode instanceof PathMonitor);

    return new UpperBound((PathMonitor)lNode, pUpperBound.getBound());
  }

  @Override
  public FQLNode visit(LowerBound pLowerBound) {
    FQLNode lNode = pLowerBound.getSubmonitor().accept(this);

    assert(lNode instanceof PathMonitor);

    return new LowerBound((PathMonitor)lNode, pLowerBound.getBound());
  }

  @Override
  public FQLNode visit(Query pQuery) {
    FQLNode lNode = pQuery.getCoverage().accept(this);

    assert(lNode instanceof Coverage);

    return new Query((Coverage)lNode, pQuery.getPassingMonitor());
  }

  @Override
  public FQLNode visit(FilterMonitor pFilterMonitor) {
    return new FilterMonitor(apply(pFilterMonitor.getFilter()));
  }

}
