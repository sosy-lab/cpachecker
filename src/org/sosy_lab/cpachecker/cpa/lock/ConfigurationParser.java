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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix="cpa.lock")
public class ConfigurationParser {
  private Configuration config;

  @Option(name="lockinfo",
      description="contains all lock names")
  private Set<String> lockinfo;

  @Option(name="annotate",
      description=" annotated functions, which are known to works right")
  private Set<String> annotated;

  ConfigurationParser(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
  }

  @SuppressWarnings("deprecation")
  public ImmutableSet<LockInfo> parseLockInfo() {
    Set<LockInfo> tmpInfo = new HashSet<>();
    Map<String, Integer> lockFunctions;
    Map<String, Integer> unlockFunctions;
    Map<String, Integer> resetFunctions;
    Set<String> variables;
    LockInfo tmpLockInfo;
    Set<String> tmpStringSet;
    String tmpString;
    int num;

    for (String lockName : lockinfo) {
      tmpString = config.getProperty(lockName + ".lock");
      lockFunctions = new HashMap<>();
      if (tmpString != null) {
        tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
        for (String funcName : tmpStringSet) {
          try {
            num = Integer.parseInt(config.getProperty(lockName + "." + funcName + ".parameters"));
          } catch (NumberFormatException e) {
            num = 0;
          }
          lockFunctions.put(funcName, num);
        }
      }
      unlockFunctions = new HashMap<>();
      tmpString = config.getProperty(lockName + ".unlock");
      if (tmpString != null) {
        tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
        for (String funcName : tmpStringSet) {
          try {
            num = Integer.parseInt(config.getProperty(lockName + "." + funcName + ".parameters"));
          } catch (NumberFormatException e) {
            num = 0;
          }
          unlockFunctions.put(funcName, num);
        }
      }
      variables = new HashSet<>();
      tmpString = config.getProperty(lockName + ".variable");
      if (tmpString != null) {
        variables = new HashSet<>(Arrays.asList(tmpString.split(", *")));
      }
      resetFunctions = new HashMap<>();
      tmpString = config.getProperty(lockName + ".reset");
      if (tmpString != null) {
        tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
        for (String funcName : tmpStringSet) {
          try {
            num = Integer.parseInt(config.getProperty(lockName + "." + funcName + ".parameters"));
          } catch (NumberFormatException e) {
            num = 0;
          }
          resetFunctions.put(funcName, num);
        }
      }
      tmpString = config.getProperty(lockName + ".setlevel");
      try {
        num = Integer.parseInt(config.getProperty(lockName + ".maxDepth"));
      } catch (NumberFormatException e) {
        num = 100;
      }
      tmpLockInfo = new LockInfo(lockName, lockFunctions, unlockFunctions, resetFunctions, variables, tmpString, num);
      tmpInfo.add(tmpLockInfo);
    }
    return ImmutableSet.copyOf(tmpInfo);
  }

  @SuppressWarnings("deprecation")
  public ImmutableMap<String, AnnotationInfo> parseAnnotatedFunctions() {
    Map<String, String> freeLocks;
    Map<String, String> restoreLocks;
    Map<String, String> resetLocks;
    Map<String, String> captureLocks;
    Set<String> tmpStringSet;
    String tmpString;
    AnnotationInfo tmpAnnotationInfo;
    Map<String, AnnotationInfo> annotatedfunctions = new HashMap<>();

    if (annotated != null) {
      for (String fName : annotated) {
        tmpString = config.getProperty("annotate." + fName + ".free");
        freeLocks = null;
        if (tmpString != null) {
          tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
          freeLocks = new HashMap<>();
          for (String fullName : tmpStringSet) {
            if (fullName.matches(".*\\(.*")) {
              String[] stringArray = fullName.split("\\(");
              assert stringArray.length == 2;
              freeLocks.put(stringArray[0], stringArray[1]);
            } else {
              freeLocks.put(fullName, "");
            }
          }
        }
        tmpString = config.getProperty("annotate." + fName + ".restore");
        restoreLocks = null;
        if (tmpString != null) {
          tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
          restoreLocks = new HashMap<>();
          for (String fullName : tmpStringSet) {
            if (fullName.matches(".*\\(.*")) {
              String[] stringArray = fullName.split("\\(");
              assert stringArray.length == 2;
              restoreLocks.put(stringArray[0], stringArray[1]);
            } else {
              restoreLocks.put(fullName, "");
            }
          }
        }
        tmpString = config.getProperty("annotate." + fName + ".reset");
        resetLocks = null;
        if (tmpString != null) {
          tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
          resetLocks = new HashMap<>();
          for (String fullName : tmpStringSet) {
            if (fullName.matches(".*\\(.*")) {
              String[] stringArray = fullName.split("\\(");
              assert stringArray.length == 2;
              resetLocks.put(stringArray[0], stringArray[1]);
            } else {
              resetLocks.put(fullName, "");
            }
          }
        }
        tmpString = config.getProperty("annotate." + fName + ".lock");
        captureLocks = null;
        if (tmpString != null) {
          tmpStringSet = new HashSet<>(Arrays.asList(tmpString.split(", *")));
          captureLocks = new HashMap<>();
          for (String fullName : tmpStringSet) {
            if (fullName.matches(".*\\(.*")) {
              String[] stringArray = fullName.split("\\(");
              assert stringArray.length == 2;
              captureLocks.put(stringArray[0], stringArray[1]);
            } else {
              captureLocks.put(fullName, "");
            }
          }
        }
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
}
