/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.bnbmemorymodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

import javax.annotation.Nonnull;

public class BnBMapMerger {

  /**
   * This method is used to merge specific types of maps
   ** @param first of two maps to be merged, contains merge result, must not be null
   * @param second map
   * @return map with the elements of both input maps
   */
  public void mergeMaps(
          @Nonnull Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> first,
          Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> second) {

    assert(first != null);

    if (second == null || second.isEmpty()){
      return;
    } else if (first.isEmpty()) {
      first.putAll(second);
    } else {
      Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result = first;
      for (Boolean refd : second.keySet()) {
        if (!first.containsKey(refd)) {
          first.put(refd, second.get(refd));
        } else {
          mergeMaps(first.get(refd), second.get(refd));
        }
      }
      return;
    }
  }

  /**
   * This method is used to merge specific types of maps
   * @param first of two maps to be merged, contains merge result, must not be null
   * @param second map
   * @return map with the elements of both input maps
   */
  public void mergeMaps(
          @Nonnull HashMap<CType, HashMap<CType, HashSet<String>>> first,
          HashMap<CType, HashMap<CType, HashSet<String>>> second) {

    assert(first != null);

    if (second == null || second.isEmpty()){
      return;
    } else if (first.isEmpty()) {
      first.putAll(second);
    } else {
      Set<CType> sourceTypeSet = second.keySet();

      //add all new keys with values to first map
      Set<CType> notPresent = new HashSet<>(sourceTypeSet);
      notPresent.removeAll(first.keySet());

      for (CType type : notPresent) {
        first.put(type, second.get(type));
      }

      //from now on all of the keys are present in first map
      Set<CType> present = new HashSet<>(sourceTypeSet);
      present.removeAll( notPresent );

      for (CType type : present) {
        //same idea
        Set<CType> keySet = first.get(type).keySet();
        Map<CType, HashSet<String>> map = second.get(type);

        Set<CType> nextNotPresent = new HashSet<>(map.keySet());
        nextNotPresent.removeAll(keySet);

        for (CType npType : nextNotPresent) {
          first.get(type).put(npType, map.get(npType));
        }

        Set<CType> nextPresent = new HashSet<>(map.keySet());
        nextPresent.removeAll(nextNotPresent);

        for (CType pType : nextPresent) {
          first.get(type).get(pType).addAll(map.get(pType));
        }
      }
      return;
    }
  }
}
