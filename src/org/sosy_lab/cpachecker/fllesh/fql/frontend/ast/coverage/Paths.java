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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Filter;

public class Paths implements Coverage {

  private Filter mFilter;
  private Predicates mPredicates;
  private int mBound;

  public Paths(Filter pFilter, int pBound) {
    this(pFilter, pBound, new Predicates());
  }

  public Paths(Filter pFilter, int pBound, Predicates pPredicates) {
    assert(pFilter != null);
    assert(pPredicates != null);
    assert(pBound > 0);

    mFilter = pFilter;
    mBound = pBound;
    mPredicates = pPredicates;
  }

  public int getBound() {
    return mBound;
  }

  public Filter getFilter() {
    return mFilter;
  }

  public Predicates getPredicates() {
    return mPredicates;
  }

  @Override
  public String toString() {
    return "PATHS(" + mFilter.toString() + ", " + mBound + ", " + mPredicates.toString() + ")";
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      Paths lOther = (Paths)pOther;

      return (lOther.mFilter.equals(mFilter) && lOther.mPredicates.equals(mPredicates));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 32233 + mFilter.hashCode() + mPredicates.hashCode() + mBound;
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

}
