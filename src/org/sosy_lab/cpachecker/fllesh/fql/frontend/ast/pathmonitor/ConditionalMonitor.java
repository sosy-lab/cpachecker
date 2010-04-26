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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;

public class ConditionalMonitor implements PathMonitor {

  private Predicates mPreconditions;
  private PathMonitor mSubmonitor;
  private Predicates mPostconditions;

  public ConditionalMonitor(Predicates pPreconditions, PathMonitor pSubmonitor, Predicates pPostconditions) {
    assert(pPreconditions != null);
    assert(pSubmonitor != null);
    assert(pPostconditions != null);

    mPreconditions = pPreconditions;
    mSubmonitor = pSubmonitor;
    mPostconditions = pPostconditions;
  }

  public Predicates getPreconditions() {
    return mPreconditions;
  }

  public Predicates getPostconditions() {
    return mPostconditions;
  }

  public PathMonitor getSubmonitor() {
    return mSubmonitor;
  }

  @Override
  public String toString() {
    return mPreconditions.toString() + " " + mSubmonitor.toString() + " " + mPostconditions.toString();
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
      ConditionalMonitor lMonitor = (ConditionalMonitor)pOther;

      return lMonitor.mPreconditions.equals(mPreconditions)
              && lMonitor.mSubmonitor.equals(mSubmonitor)
              && lMonitor.mPostconditions.equals(mPostconditions);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 20393 + mPreconditions.hashCode() + mSubmonitor.hashCode() + mPostconditions.hashCode();
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

}
