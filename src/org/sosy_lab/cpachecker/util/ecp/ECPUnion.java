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
package org.sosy_lab.cpachecker.util.ecp;

import java.util.Iterator;
import java.util.LinkedList;

public class ECPUnion implements ElementaryCoveragePattern , Iterable<ElementaryCoveragePattern> {

  private LinkedList<ElementaryCoveragePattern> mSubpatterns;

  public ECPUnion(ElementaryCoveragePattern pFirstSubpattern, ElementaryCoveragePattern pSecondSubpattern) {
    mSubpatterns = new LinkedList<ElementaryCoveragePattern>();

    if (pFirstSubpattern instanceof ECPUnion) {
      ECPUnion lFirstSubpattern = (ECPUnion)pFirstSubpattern;

      mSubpatterns.addAll(lFirstSubpattern.mSubpatterns);
    }
    else {
      mSubpatterns.add(pFirstSubpattern);
    }

    if (pSecondSubpattern instanceof ECPUnion) {
      ECPUnion lSecondSubpattern = (ECPUnion)pSecondSubpattern;

      mSubpatterns.addAll(lSecondSubpattern.mSubpatterns);
    }
    else {
      mSubpatterns.add(pSecondSubpattern);
    }
  }

  @Override
  public Iterator<ElementaryCoveragePattern> iterator() {
    return mSubpatterns.iterator();
  }

  public int size() {
    return mSubpatterns.size();
  }

  public boolean isEmpty() {
    return mSubpatterns.isEmpty();
  }

  public ElementaryCoveragePattern get(int lIndex) {
    return mSubpatterns.get(lIndex);
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
      ECPUnion lOther = (ECPUnion)pOther;

      return mSubpatterns.equals(lOther.mSubpatterns);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mSubpatterns.hashCode();
  }

  @Override
  public String toString() {
    StringBuffer lResult = new StringBuffer();

    boolean isFirst = true;

    for (ElementaryCoveragePattern lSubpattern : mSubpatterns) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lResult.append(" + ");
      }

      lResult.append(lSubpattern.toString());
    }

    return lResult.toString();
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
