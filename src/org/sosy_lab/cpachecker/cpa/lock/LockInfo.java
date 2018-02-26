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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.util.Pair;

public class LockInfo {

  private final ImmutableMap<String, Pair<LockEffect, LockIdUnprepared>> functionEffectDescription;
  private final ImmutableMap<String, LockIdentifier> variableEffectDescription;
  private final ImmutableMap<String, Integer> maxLevel;

  public LockInfo(
      Map<String, Pair<LockEffect, LockIdUnprepared>> functionEffects,
      Map<String, LockIdentifier> varEffects,
      Map<String, Integer> max) {
    functionEffectDescription = ImmutableMap.copyOf(functionEffects);
    variableEffectDescription = ImmutableMap.copyOf(varEffects);
    maxLevel = ImmutableMap.copyOf(max);
  }

  public ImmutableMap<String, Pair<LockEffect, LockIdUnprepared>> getFunctionEffectDescription() {
    return functionEffectDescription;
  }

  public ImmutableMap<String, LockIdentifier> getVariableEffectDescription() {
    return variableEffectDescription;
  }

  public int getMaxLevel(String lockName) {
    // The max level must be in map (do not use default)
    return maxLevel.get(lockName);
  }

}
