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

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;


public class AnnotationInfo {
  public final String funcName;
  public final ImmutableMap<String, String> freeLocks;
  public final ImmutableMap<String, String> restoreLocks;
  public final ImmutableMap<String, String> resetLocks;
  public final ImmutableMap<String, String> captureLocks;

  public AnnotationInfo(String name, Map<String, String> free, Map<String, String> restore, Map<String, String> reset
      , Map<String, String> capture) {
    funcName = name;
    freeLocks = (free == null ? ImmutableMap.copyOf(new HashMap<String, String>()) : ImmutableMap.copyOf(free));
    restoreLocks = (restore == null ? ImmutableMap.copyOf(new HashMap<String, String>()) : ImmutableMap.copyOf(restore));
    resetLocks = (reset == null ? ImmutableMap.copyOf(new HashMap<String, String>()) : ImmutableMap.copyOf(reset));
    captureLocks = (capture == null ? ImmutableMap.copyOf(new HashMap<String, String>()) : ImmutableMap.copyOf(capture));
  }

}
