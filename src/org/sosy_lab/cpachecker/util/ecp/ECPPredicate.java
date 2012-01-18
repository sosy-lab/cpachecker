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
package org.sosy_lab.cpachecker.util.ecp;

import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate;

public class ECPPredicate implements ECPGuard {

  private Predicate mPredicate;

  public ECPPredicate(Predicate pPredicate) {
    mPredicate = pPredicate;
  }

  /** copy constructor */
  public ECPPredicate(ECPPredicate pPredicate) {
    this(pPredicate.mPredicate);
  }

  @Override
  public int hashCode() {
    return mPredicate.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass().equals(getClass())) {
      ECPPredicate lOther = (ECPPredicate)pOther;

      return mPredicate.equals(lOther.mPredicate);
    }

    return false;
  }

  @Override
  public String toString() {
    return mPredicate.toString();
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  public Predicate getPredicate() {
    return mPredicate;
  }

}
