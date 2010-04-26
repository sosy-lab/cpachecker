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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate;

import java.util.Iterator;
import java.util.LinkedList;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.FQLNode;

public class Predicates implements FQLNode, Iterable<Predicate> {
  private LinkedList<Predicate> mPredicates = new LinkedList<Predicate>();

  public void add(Predicate pPredicate) {
    assert(pPredicate != null);

    mPredicates.add(pPredicate);
  }

  public void add(Predicates pPredicates) {
    assert(pPredicates != null);

    mPredicates.addAll(pPredicates.mPredicates);
  }

  public boolean isEmpty() {
    return mPredicates.isEmpty();
  }

  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    lBuffer.append("{");

    boolean lFirst = true;

    for (Predicate lPredicate : mPredicates) {
      lBuffer.append(" ");
      lBuffer.append(lPredicate.toString());

      if (lFirst) {
        lFirst = false;
      }
      else {
        lBuffer.append(",");
      }
    }

    lBuffer.append(" }");

    return lBuffer.toString();
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
      Predicates lOther = (Predicates)pOther;

      return mPredicates.equals(lOther.mPredicates);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 24383 + mPredicates.hashCode();
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

  @Override
  public Iterator<Predicate> iterator() {
    return mPredicates.iterator();
  }

}
