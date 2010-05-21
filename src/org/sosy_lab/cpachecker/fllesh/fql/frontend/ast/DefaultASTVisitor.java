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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Edges;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Paths;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Sequence;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.States;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Alternative;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.FilterMonitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.UpperBound;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.CIdentifier;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.NaturalNumber;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query.Query;

public class DefaultASTVisitor<T> implements ASTVisitor<T> {

  @Override
  public T visit(Predicate pPredicate) {
    throw new UnsupportedOperationException("The method visit(Predicate pPredicate) is not supported!");
  }

  @Override
  public T visit(CIdentifier pCIdentifier) {
    throw new UnsupportedOperationException("The method visit(CIdentifier pCIdentifier) is not supported!");
  }

  @Override
  public T visit(NaturalNumber pNaturalNumber) {
    throw new UnsupportedOperationException("The method visit(NaturalNumber pNaturalNumber) is not supported!");
  }

  @Override
  public T visit(Predicates pPredicates) {
    throw new UnsupportedOperationException("The method visit(Predicates pPredicates) is not supported!");
  }

  @Override
  public T visit(States pStates) {
    throw new UnsupportedOperationException("The method visit(States pStates) is not supported!");
  }

  @Override
  public T visit(Edges pEdges) {
    throw new UnsupportedOperationException("The method visit(Edges pEdges) is not supported!");
  }

  @Override
  public T visit(Paths pPaths) {
    throw new UnsupportedOperationException("The method visit(Paths pPaths) is not supported!");
  }

  @Override
  public T visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.SetMinus pSetMinus) {
    throw new UnsupportedOperationException("The method visit(fql.frontend.ast.coverage.SetMinus pSetMinus) is not supported!");
  }

  @Override
  public T visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Union pUnion) {
    throw new UnsupportedOperationException("The method visit(fql.frontend.ast.coverage.Union pUnion) is not supported!");
  }

  @Override
  public T visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Intersection pIntersection) {
    throw new UnsupportedOperationException("The method visit(fql.frontend.ast.coverage.Intersection pIntersection) is not supported!");
  }

  @Override
  public T visit(ConditionalCoverage pConditionalCoverage) {
    throw new UnsupportedOperationException("The method visit(ConditionalCoverage pConditionalCoverage) is not supported!");
  }

  @Override
  public T visit(Sequence pSequence) {
    throw new UnsupportedOperationException("The method visit(Sequence pSequence) is not supported!");
  }

  @Override
  public T visit(ConditionalMonitor pConditionalMonitor) {
    throw new UnsupportedOperationException("The method visit(ConditionalMonitor pConditionalMonitor) is not supported!");
  }

  @Override
  public T visit(Alternative pAlternative) {
    throw new UnsupportedOperationException("The method visit(Alternative pAlternative) is not supported!");
  }

  @Override
  public T visit(Concatenation pConcatenation) {
    throw new UnsupportedOperationException("The method visit(Concatenation pConcatenation) is not supported!");
  }

  @Override
  public T visit(UpperBound pUpperBound) {
    throw new UnsupportedOperationException("The method visit(UpperBound pUpperBound) is not supported!");
  }

  @Override
  public T visit(LowerBound pLowerBound) {
    throw new UnsupportedOperationException("The method visit(LowerBound pLowerBound) is not supported!");
  }

  @Override
  public T visit(Query pQuery) {
    throw new UnsupportedOperationException("The method visit(Query pQuery) is not supported!");
  }

  @Override
  public T visit(FilterMonitor pFilterMonitor) {
    throw new UnsupportedOperationException("The method visit(FilterMonitor pFilterMonitor) is not supported!");
  }

}
