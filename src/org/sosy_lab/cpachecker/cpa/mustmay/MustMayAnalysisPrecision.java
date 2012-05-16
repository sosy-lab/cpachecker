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
package org.sosy_lab.cpachecker.cpa.mustmay;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class MustMayAnalysisPrecision implements Precision {
  Precision mMustPrecision;
  Precision mMayPrecision;

  public MustMayAnalysisPrecision(Precision pMustPrecision, Precision pMayPrecision) {
    assert(pMustPrecision != null);
    assert(pMayPrecision != null);

    mMustPrecision = pMustPrecision;
    mMayPrecision = pMayPrecision;
  }

  public Precision getMustPrecision() {
    return mMustPrecision;
  }

  public Precision getMayPrecision() {
    return mMayPrecision;
  }

  @Override
  public String toString() {
    return "<" + mMustPrecision.toString() + ", " + mMayPrecision.toString() + ">";
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
      MustMayAnalysisPrecision lPrecision = (MustMayAnalysisPrecision)pOther;

      Precision lMustPrecision = lPrecision.getMustPrecision();
      Precision lMayPrecision = lPrecision.getMayPrecision();

      return (mMustPrecision.equals(lMustPrecision) && mMayPrecision.equals(lMayPrecision));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mMustPrecision.hashCode() + mMayPrecision.hashCode();
  }
}
