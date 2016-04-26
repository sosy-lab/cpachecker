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

public class BnBMapMerger {

  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> mergeMaps(
          Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> target,
          Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> source) {

    if (source == null || source.isEmpty()){
      return target;
    } else if (target == null || target.isEmpty()) {
      return source;
    } else {
      Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result = target;
      for (Boolean refd : source.keySet()) {
        if (!target.containsKey(refd)) {
          result.put(refd, source.get(refd));
        } else {
          result.put(refd, mergeMaps(target.get(refd), source.get(refd)));
        }
      }
      return result;
    }
  }

  public HashMap<CType, HashMap<CType, HashSet<String>>> mergeMaps(
          HashMap<CType, HashMap<CType, HashSet<String>>> target,
          HashMap<CType, HashMap<CType, HashSet<String>>> source) {

    if (source == null || source.isEmpty()){
      return target;
    } else if (target == null || target.isEmpty()) {
      return source;
    } else {
      Set<CType> targetTypeSet = target.keySet();
      Set<CType> sourceTypeSet = source.keySet();

      //add all new keys with values to target
      Set<CType> notPresent = new HashSet<>(sourceTypeSet);
      notPresent.removeAll(targetTypeSet);

      for (CType type : notPresent) {
        target.put(type, source.get(type));
      }

      //from now on all of the keys are present in target
      Set<CType> present = new HashSet<>(sourceTypeSet);
      present.removeAll( notPresent );

      for (CType type : present) {
        //same idea
        Set<CType> keySet = target.get(type).keySet();
        Map<CType, HashSet<String>> map = source.get(type);

        Set<CType> nextNotPresent = new HashSet<>(map.keySet());
        nextNotPresent.removeAll(keySet);

        for (CType npType : nextNotPresent) {
          target.get(type).put(npType, map.get(npType));
        }

        Set<CType> nextPresent = new HashSet<>(map.keySet());
        nextPresent.removeAll(nextNotPresent);

        for (CType pType : nextPresent) {
          target.get(type).get(pType).addAll(map.get(pType));
        }
      }
      return target;
    }
  }
}
