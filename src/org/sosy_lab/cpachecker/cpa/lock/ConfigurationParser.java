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
package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ReleaseLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ResetLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SetLockEffect;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix="cpa.lock")
public class ConfigurationParser {
  private Configuration config;

  @Option(name="lockinfo",
      description="contains all lock names",
      secure = true)
  private Set<String> lockinfo;

  @Option(name="annotate",
      description=" annotated functions, which are known to works right",
      secure = true)
  private Set<String> annotated;

  ConfigurationParser(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
  }

  @SuppressWarnings("deprecation")
  public LockInfo parseLockInfo() {
    Map<String, Integer> tmpInfo = new HashMap<>();
    Map<String, Pair<LockEffect, LockIdUnprepared>> functionEffects = new HashMap<>();
    Map<String, LockIdentifier> variableEffects = new HashMap<>();
    Set<String> variables;
    String tmpString;


    for (String lockName : lockinfo) {
      int num = getValue(lockName + ".maxDepth", 10);
      functionEffects.putAll(createMap(lockName, "lock", AcquireLockEffect.getInstance()));
      functionEffects.putAll(createMap(lockName, "unlock", ReleaseLockEffect.getInstance()));
      functionEffects.putAll(createMap(lockName, "reset", ResetLockEffect.getInstance()));

      tmpString = config.getProperty(lockName + ".variable");
      if (tmpString != null) {
        variables = new HashSet<>(Arrays.asList(tmpString.split(", *")));
        variables.forEach(
            k -> variableEffects.put(k, LockIdentifier.of(lockName)));
      } else {
        variables = new HashSet<>();
      }

      tmpString = config.getProperty(lockName + ".setlevel");
      if (tmpString != null && !tmpString.isEmpty()) {
        functionEffects.put(tmpString, Pair.of(SetLockEffect.getInstance(), new LockIdUnprepared(lockName, 0)));
      }
      tmpInfo.put(lockName, num);
    }
    return new LockInfo(functionEffects, variableEffects, tmpInfo);
  }

  @SuppressWarnings("deprecation")
  private Map<String, Pair<LockEffect, LockIdUnprepared>> createMap(String lockName, String target, LockEffect effect) {

    String tmpString = config.getProperty(lockName + "." + target);
    if (tmpString != null) {

      return from(Arrays.asList(tmpString.split(", *")))
            .toMap(f ->
                Pair.of(effect,
                    new LockIdUnprepared(lockName, getValue(lockName + "." + f + ".parameters", 0))
                ));
    }
    return Maps.newHashMap();
  }

  @SuppressWarnings("deprecation")
  private int getValue(String property, int defaultValue) {
    int num;
    try {
      num = Integer.parseInt(config.getProperty(property));
    } catch (NumberFormatException e) {
      num = defaultValue;
    }
    return num;
  }

  public ImmutableMap<String, AnnotationInfo> parseAnnotatedFunctions() {
    Map<String, String> freeLocks;
    Map<String, String> restoreLocks;
    Map<String, String> resetLocks;
    Map<String, String> captureLocks;
    AnnotationInfo tmpAnnotationInfo;
    Map<String, AnnotationInfo> annotatedfunctions = new HashMap<>();

    if (annotated != null) {
      for (String fName : annotated) {
        freeLocks = createAnnotationMap(fName, "free");
        restoreLocks = createAnnotationMap(fName, "restore");
        resetLocks = createAnnotationMap(fName, "reset");
        captureLocks = createAnnotationMap(fName, "lock");
        if (restoreLocks == null && freeLocks == null && resetLocks == null && captureLocks == null) {
          //we don't specify the annotation. Restore all locks.
          tmpAnnotationInfo = new AnnotationInfo(fName, null, new HashMap<String, String>(), null, null);
        } else {
          tmpAnnotationInfo = new AnnotationInfo(fName, freeLocks, restoreLocks, resetLocks, captureLocks);
        }
        annotatedfunctions.put(fName, tmpAnnotationInfo);
      }
    }
    return ImmutableMap.copyOf(annotatedfunctions);
  }

  @SuppressWarnings("deprecation")
  private Map<String, String> createAnnotationMap(String function, String target) {
    Map<String, String> result = null;

    String property = config.getProperty("annotate." + function + "." + target);
    if (property != null) {
      Set<String> lockNames = new HashSet<>(Arrays.asList(property.split(", *")));
      result = new HashMap<>();
      for (String fullName : lockNames) {
        if (fullName.matches(".*\\(.*")) {
          String[] stringArray = fullName.split("\\(");
          assert stringArray.length == 2;
          result.put(stringArray[0], stringArray[1]);
        } else {
          result.put(fullName, "");
        }
      }
    }
    return result;
  }
}
