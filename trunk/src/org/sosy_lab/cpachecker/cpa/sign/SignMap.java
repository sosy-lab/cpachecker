/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import java.util.Set;

import com.google.common.collect.ImmutableMap;

public class SignMap {

  private ImmutableMap<String, SIGN> possibleSigns;

  public SignMap(ImmutableMap<String, SIGN> pPossibleSigns) {
      possibleSigns = pPossibleSigns;
  }

  public SIGN getSignForVariable(String pVarIdent) {
    if(possibleSigns.containsKey(pVarIdent)) {
      return possibleSigns.get(pVarIdent);
    }
    return SIGN.ALL;
  }

  public Set<String> keySet() {
    return possibleSigns.keySet();
  }

  public boolean containsKey(Object pKey) {
    return possibleSigns.containsKey(pKey);
  }

  public SignMap mergeWith(SignMap pSignMap) {
      ImmutableMap.Builder<String, SIGN> mapBuilder = ImmutableMap.builder();
      for(String key : possibleSigns.keySet()) {
          if(pSignMap.containsKey(key)) {
              // Use minimal sign if both maps contain the same key
              mapBuilder.put(key, SIGN.min(possibleSigns.get(key), pSignMap.possibleSigns.get(key)));
          } else {
              mapBuilder.put(key, possibleSigns.get(key));
          }
      }
      for(String key : pSignMap.keySet()) {
          if(!possibleSigns.containsKey(key)) {
              mapBuilder.put(key, pSignMap.possibleSigns.get(key));
          }
      }
      return new SignMap(mapBuilder.build());
  }

  @Override
  public int hashCode() {
    return possibleSigns.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if(!(pObj instanceof SignMap)) {
      return false;
    }
    return ((SignMap)pObj).possibleSigns.equals(this.possibleSigns);
  }

}