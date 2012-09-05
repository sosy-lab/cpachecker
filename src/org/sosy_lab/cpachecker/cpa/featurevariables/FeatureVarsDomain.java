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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class FeatureVarsDomain implements AbstractDomain {

  private final NamedRegionManager rmgr;

  public FeatureVarsDomain(NamedRegionManager manager) {
    this.rmgr = manager;
  }

  @Override
  public boolean isLessOrEqual(AbstractState newElement, AbstractState reachedState) {
      // returns true if element1 < element2 on lattice
      // true if newElement represents less states (and a subset of the states of) reachedState
    if (newElement instanceof FeatureVarsState && reachedState instanceof FeatureVarsState){
      FeatureVarsState fvn = (FeatureVarsState)newElement;
      FeatureVarsState fvr = (FeatureVarsState)reachedState;
      return rmgr.entails(fvn.getRegion(), fvr.getRegion());
    } else {
      throw new IllegalArgumentException("Called with non-FeatureVars-Elements");
    }
  }

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
    FeatureVarsState fv1 = (FeatureVarsState)element1;
    FeatureVarsState fv2 = (FeatureVarsState)element2;

    Region result = rmgr.makeOr(fv1.getRegion(), fv2.getRegion());
    if (result.equals(fv2.getRegion())) {
      return fv2;
    } else if (result.equals(fv1.getRegion())) {
      return fv1;
    } else {
      return new FeatureVarsState(result, rmgr);
    }
  }
}
