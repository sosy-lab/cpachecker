/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabsCPA;

import symbpredabstraction.interfaces.Predicate;
import cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import cpa.common.interfaces.Precision;

public class SymbPredAbsPrecision implements Precision {

  private final SetMultimap<CFANode, Predicate> predicateMap;
  
  public SymbPredAbsPrecision(ImmutableSetMultimap<CFANode, Predicate> predicateMap) {
    assert predicateMap != null;
    this.predicateMap = predicateMap;
  }
  
  public SymbPredAbsPrecision() {
    predicateMap = ImmutableSetMultimap.of();
  }
  
  public SetMultimap<CFANode, Predicate> getPredicateMap() {
    return predicateMap;
  }
  
  @Override
  public int hashCode() {
    return predicateMap.hashCode();
  }
  
  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof SymbPredAbsPrecision)) {
      return false;
    } else {
      return predicateMap.equals(((SymbPredAbsPrecision)pObj).predicateMap);
    }
  }
}


