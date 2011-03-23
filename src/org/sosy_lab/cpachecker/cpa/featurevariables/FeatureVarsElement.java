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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class FeatureVarsElement implements AbstractElement, Cloneable {

  static BiMap<String, Region> regionMap = HashBiMap.create();
  static RegionManager manager = BDDRegionManager.getInstance();
  
  final Region currentState;
  
  public FeatureVarsElement( Region currentState ) {
    this.currentState = currentState;
  }
  
  public Region getRegion() {
    return currentState;
  }
  
  public Region getVariableRegion(String pVarName) {
    Region ret = regionMap.get(pVarName);
    if (ret == null) {
      ret = manager.createPredicate();
      regionMap.put(pVarName, ret);      
    }
    return ret;
  }
  
  

}
