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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.sosy_lab.common.Pair;

public class AliasingMap {

  private final String prefix;

  private final AtomicInteger nextIndex = new AtomicInteger(0);
  private final HashMap<Pair<String, Integer>, Integer> map;

  /**
   * @param prefix the letter you want all new variables to start with, e.g. "v"
   */
  public AliasingMap(String prefix) {
    this.prefix = prefix;
    map = new HashMap<>();
  }

  public int size() {
    return map.size();
  }

  /**
   * Alias the TemplateVariable x. If its (name,index) pair is already
   * present in map, then use the existing index stored there. Otherwise
   * make a new index, and use that, also storing it in the map.
   */
  public void alias(TemplateVariable x) {
    String s = x.getName();
    Integer i = x.getIndex();
    Pair<String, Integer> p =  Pair.<String, Integer>of(s, i);
    Integer j;
    if (map.containsKey(p)) {
      j = map.get(p);
    } else {
      j = Integer.valueOf(nextIndex.incrementAndGet());
      map.put(p, j);
    }
    x.setAlias(prefix, j);
  }

}
