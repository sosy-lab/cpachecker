/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractMemoryRegionManager implements MemoryRegionManager {
  private Map<Pair<CFAEdge,String>, Integer> targetStats = new HashMap<>();

  @Override
  public void addTargetToStats(CFAEdge pEdge, String pUfName, PointerTarget pTarget) {
    Pair<CFAEdge,String> key = Pair.of(pEdge, pUfName);
    Integer v = targetStats.get(key);
    if(v!=null) {
      targetStats.put(key, v+1);
    } else {
      targetStats.put(key, 1);
    }
  }

  @Override
  public void printStatistics(PrintStream out) {
    int totalTargets = 0;
    Map<String,Integer> perUf = new HashMap<>();
    for(Map.Entry<Pair<CFAEdge,String>, Integer> entry : targetStats.entrySet()) {
      String key = entry.getKey().getSecond();
      Integer v = entry.getValue();
      if(v!=null) {
        perUf.put(key, v+1);
        totalTargets += v;
      } else {
        perUf.put(key, 1);
      }
    }
    out.println("Total number of created targets for pointer analysis: " + totalTargets);
    for(Map.Entry<String, Integer> entry : perUf.entrySet()) {
      out.println("   Number of created targets for uf: " + entry.getKey() + " is " + entry.getValue());
    }
    out.println();
  }
}
