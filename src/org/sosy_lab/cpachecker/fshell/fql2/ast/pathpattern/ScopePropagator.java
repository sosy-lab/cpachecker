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
package org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Compose;
import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Filter;

public class ScopePropagator implements PathPatternVisitor<PathPattern> {

  Filter mFilter;

  public ScopePropagator(Filter pFilter) {
    mFilter = pFilter;
  }

  public Filter getFilter() {
    return mFilter;
  }

  @Override
  public Concatenation visit(Concatenation pConcatenation) {
    PathPattern lFirstSubpattern = pConcatenation.getFirstSubpattern();
    PathPattern lSecondSubpattern = pConcatenation.getSecondSubpattern();

    PathPattern lNewFirstSubpattern = lFirstSubpattern.accept(this);
    PathPattern lNewSecondSubpattern = lSecondSubpattern.accept(this);

    if (lFirstSubpattern.equals(lNewFirstSubpattern) && lSecondSubpattern.equals(lNewSecondSubpattern)) {
      return pConcatenation;
    }
    else {
      return new Concatenation(lNewFirstSubpattern, lNewSecondSubpattern);
    }
  }

  @Override
  public Repetition visit(Repetition pRepetition) {
    PathPattern lSubpattern = pRepetition.getSubpattern();

    PathPattern lNewSubpattern = lSubpattern.accept(this);

    if (lSubpattern.equals(lNewSubpattern)) {
      return pRepetition;
    }
    else {
      return new Repetition(lNewSubpattern);
    }
  }

  @Override
  public Union visit(Union pUnion) {
    PathPattern lFirstSubpattern = pUnion.getFirstSubpattern();
    PathPattern lSecondSubpattern = pUnion.getSecondSubpattern();

    PathPattern lNewFirstSubpattern = lFirstSubpattern.accept(this);
    PathPattern lNewSecondSubpattern = lSecondSubpattern.accept(this);

    if (lFirstSubpattern.equals(lNewFirstSubpattern) && lSecondSubpattern.equals(lNewSecondSubpattern)) {
      return pUnion;
    }
    else {
      return new Union(lNewFirstSubpattern, lNewSecondSubpattern);
    }
  }

  @Override
  public Edges visit(Edges pEdges) {
    Filter lFilter = pEdges.getFilter();

    return new Edges(new Compose(lFilter, getFilter()));
  }

  @Override
  public Nodes visit(Nodes pNodes) {
    Filter lFilter = pNodes.getFilter();

    return new Nodes(new Compose(lFilter, getFilter()));
  }

  @Override
  public Paths visit(Paths pPaths) {
    Filter lFilter = pPaths.getFilter();
    int lBound = pPaths.getBound();

    return new Paths(new Compose(lFilter, getFilter()), lBound);
  }

  @Override
  public Predicate visit(Predicate pPredicate) {
    return pPredicate;
  }

}
