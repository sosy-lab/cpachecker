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
package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

class LocationCPAFactory extends AbstractCPAFactory {

  private final boolean backwards;

  private CFA cfa;

  public LocationCPAFactory(boolean pBackwards) {
    backwards = pBackwards;
  }

  @Override
  public <T> LocationCPAFactory set(T pObject, Class<T> pClass) {
    if (CFA.class.isAssignableFrom(pClass)) {
      cfa = (CFA)pObject;
    } else {
      super.set(pObject, pClass);
    }
    return this;
  }

  @Override
  public ConfigurableProgramAnalysis createInstance() {
    checkNotNull(cfa, "CFA instance needed to create LocationCPA");

    if (backwards) {
      return new LocationCPABackwards(cfa);
    } else {
      return new LocationCPA(cfa);
    }
  }
}