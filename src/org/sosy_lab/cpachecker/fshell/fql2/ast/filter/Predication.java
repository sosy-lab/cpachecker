/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.fql2.ast.filter;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;

public class Predication implements Filter {

  private Filter mFilter;
  private Predicate mPredicate;

  public Predication(Filter pFilter, Predicate pPredicate) {
    mFilter = pFilter;
    mPredicate = pPredicate;
  }

  public Filter getFilter() {
    return mFilter;
  }

  public Predicate getPredicate() {
    return mPredicate;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() != getClass()) {
      return false;
    }

    Predication lOther = (Predication)pOther;

    return mFilter.equals(lOther.mFilter) && mPredicate.equals(lOther.mPredicate);
  }

  @Override
  public int hashCode() {
    return mFilter.hashCode() + mPredicate.hashCode() + 243;
  }

  @Override
  public String toString() {
    return "PRED(" + mFilter.toString() + ", " + mPredicate.toString() + ")";
  }

  @Override
  public <T> T accept(FilterVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
