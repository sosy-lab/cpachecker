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
package org.sosy_lab.cpachecker.fshell.fql2.ast;

import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPatternVisitor;

public class Predicate implements Atom {

  private org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate mPredicate;

  public Predicate(org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate pPredicate) {
    assert(pPredicate != null);

    mPredicate = pPredicate;
  }

  public org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate getPredicate() {
    return mPredicate;
  }

  @Override
  public String toString() {
    return "{ " + mPredicate.toString() + " }";
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
      Predicate lOther = (Predicate)pOther;

      return mPredicate.equals(lOther.mPredicate);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 23423 + mPredicate.hashCode();
  }

  @Override
  public <T> T accept(CoverageSpecificationVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <T> T accept(PathPatternVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
