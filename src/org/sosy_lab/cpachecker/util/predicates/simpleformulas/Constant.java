/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.simpleformulas;

public class Constant implements Term {

  private int mValue;

  public Constant(int pValue) {
    mValue = pValue;
  }

  public int getValue() {
    return mValue;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!(pOther.getClass().equals(getClass()))) {
      return false;
    }

    Constant lConstant = (Constant)pOther;

    return (mValue == lConstant.mValue);
  }

  @Override
  public int hashCode() {
    return mValue;
  }

  @Override
  public String toString() {
    return "" + mValue;
  }

  @Override
  public <T> T accept(TermVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
