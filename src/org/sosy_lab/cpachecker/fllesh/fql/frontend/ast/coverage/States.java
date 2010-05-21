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

public class States implements Coverage {

  private Filter mFilter;
  private Predicates mPredicates;

  public States(Filter pFilter) {
    this(pFilter, new Predicates());
  }

  public States(Filter pFilter, Predicates pPredicates) {
    assert(pFilter != null);
    assert(pPredicates != null);

    mFilter = pFilter;
    mPredicates = pPredicates;
  }

  public Filter getFilter() {
    return mFilter;
  }

  public Predicates getPredicates() {
    return mPredicates;
  }

  @Override
  public String toString() {
    return "STATES(" + mFilter.toString() + ", " + mPredicates.toString() + ")";
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
      States lOther = (States)pOther;

      return (lOther.mFilter.equals(mFilter) && lOther.mPredicates.equals(mPredicates));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 34532 + mFilter.hashCode() + mPredicates.hashCode();
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

}
