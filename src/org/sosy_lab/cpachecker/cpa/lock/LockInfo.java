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
package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;



public class LockInfo {
  public final String lockName;
  public final ImmutableMap<String, Integer> LockFunctions;  /* integer: 0 - if we don't use parameter as identifier */
  public final ImmutableMap<String, Integer> UnlockFunctions;/*          i - we use parameter number i as identifier */
  public final ImmutableMap<String, Integer> ResetFunctions;
  public final ImmutableSet<String> Variables;
  public final String setLevel;
  public final int maxLock;

  public LockInfo(String name, Map<String, Integer> lock, Map<String, Integer> unlock, Map<String, Integer> reset
      , Set<String> vars, String level, int max) {
    lockName = name;
    LockFunctions = ImmutableMap.copyOf(lock);
    UnlockFunctions = ImmutableMap.copyOf(unlock);
    ResetFunctions = ImmutableMap.copyOf(reset);
    Variables = ImmutableSet.copyOf(vars);
    setLevel = level;
    maxLock = max;
  }

}
