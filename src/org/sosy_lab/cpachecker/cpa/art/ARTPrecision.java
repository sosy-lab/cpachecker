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
package org.sosy_lab.cpachecker.cpa.art;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;

public class ARTPrecision implements WrapperPrecision  {

  /** mapping from CFA nodes of other threads to location classes */
  private final RGLocationMapping locMapping;

  private final CompositePrecision compositePrec;

  public ARTPrecision(RGLocationMapping lm, CompositePrecision wrappedPrec) {
    assert lm != null;
    assert wrappedPrec != null;
    this.locMapping = lm;
    this.compositePrec = wrappedPrec;
  }

  public RGLocationMapping getLocationMapping() {
    return locMapping;
  }

  public CompositePrecision getWrappedPrecision() {
    return compositePrec;
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    // TODO Auto-generated method stub
    return compositePrec.retrieveWrappedPrecision(pType);
  }

  @Override
  public Precision replaceWrappedPrecision(Precision pNewPrecision,
      Class<? extends Precision> pReplaceType) {

    if (pReplaceType.equals(ARTPrecision.class)){
      return pNewPrecision;
    }

    Precision newCP = compositePrec.replaceWrappedPrecision(pNewPrecision, pReplaceType);
    return new ARTPrecision(locMapping, (CompositePrecision) newCP);
  }

  public String toString(){
    StringBuilder bldr = new StringBuilder();
    bldr.append(locMapping.toString());
    bldr.append(", "+compositePrec.toString());
    return bldr.toString();
  }

}
