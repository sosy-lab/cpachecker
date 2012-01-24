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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class FeatureVarsDomain implements AbstractDomain {

  private final NamedRegionManager rmgr;

  public FeatureVarsDomain(NamedRegionManager manager) {
    this.rmgr = manager;
  }

  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement) {
      // returns true if element1 < element2 on lattice
      // true if newElement represents less states (and a subset of the states of) reachedElement
    if (newElement instanceof FeatureVarsElement && reachedElement instanceof FeatureVarsElement){
      FeatureVarsElement fvn = (FeatureVarsElement)newElement;
      FeatureVarsElement fvr = (FeatureVarsElement)reachedElement;
      return rmgr.entails(fvn.getRegion(), fvr.getRegion());
    } else {
      throw new IllegalArgumentException("Called with non-FeatureVars-Elements");
    }
  }

  @Override
  public AbstractElement join(AbstractElement element1, AbstractElement element2) {
    FeatureVarsElement fv1 = (FeatureVarsElement)element1;
    FeatureVarsElement fv2 = (FeatureVarsElement)element2;

    Region result = rmgr.makeOr(fv1.getRegion(), fv2.getRegion());
    if (result.equals(fv2.getRegion())) {
      return fv2;
    } else if (result.equals(fv1.getRegion())) {
      return fv1;
    } else {
      return new FeatureVarsElement(result, rmgr);
    }
  }
}
