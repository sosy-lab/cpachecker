// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

@Options(prefix = "cpa.lock")
public class ConfigurationParser {
  private Configuration config;

  @Option(name = "lockinfo", description = "contains all lock names", secure = true)
  private Set<String> lockinfo = ImmutableSet.of();

  @Option(
      name = "annotate",
      description = " annotated functions, which are known to works right",
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
    String tmpString;

    for (String lockName : lockinfo) {
      int num = getValue(lockName + ".maxDepth", 10);
      functionEffects.putAll(createMap(lockName, "lock", AcquireLockEffect.getInstance()));
      functionEffects.putAll(createMap(lockName, "unlock", ReleaseLockEffect.getInstance()));
      functionEffects.putAll(createMap(lockName, "reset", ResetLockEffect.getInstance()));

      tmpString = config.getProperty(lockName + ".variable");
      if (tmpString != null) {
        tmpString = CharMatcher.whitespace().removeFrom(tmpString);
        Splitter.on(",")
            .splitToList(tmpString)
            .forEach(k -> variableEffects.put(k, LockIdentifier.of(lockName)));
      }

      tmpString = config.getProperty(lockName + ".setlevel");
      if (!isNullOrEmpty(tmpString)) {
        functionEffects.put(
            tmpString, Pair.of(SetLockEffect.getInstance(), new LockIdUnprepared(lockName, 0)));
      }
      tmpInfo.put(lockName, num);
    }
    return new LockInfo(functionEffects, variableEffects, tmpInfo);
  }

  @SuppressWarnings("deprecation")
  private Map<String, Pair<LockEffect, LockIdUnprepared>> createMap(
      String lockName, String target, LockEffect effect) {

    String tmpString = config.getProperty(lockName + "." + target);
    if (tmpString != null) {

      tmpString = CharMatcher.whitespace().removeFrom(tmpString);
      return from(Splitter.on(",").splitToList(tmpString))
          .toMap(
              f ->
                  Pair.of(
                      effect,
                      new LockIdUnprepared(
                          lockName, getValue(lockName + "." + f + ".parameters", 0))));
    }
    return ImmutableMap.of();
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
    Set<LockIdentifier> freeLocks;
    Set<LockIdentifier> restoreLocks;
    Set<LockIdentifier> resetLocks;
    Set<LockIdentifier> captureLocks;
    ImmutableMap.Builder<String, AnnotationInfo> annotatedfunctions = ImmutableMap.builder();

    if (annotated != null) {
      for (String fName : annotated) {
        freeLocks = createAnnotationMap(fName, "free");
        restoreLocks = createAnnotationMap(fName, "restore");
        resetLocks = createAnnotationMap(fName, "reset");
        captureLocks = createAnnotationMap(fName, "lock");
        annotatedfunctions.put(
            fName, new AnnotationInfo(freeLocks, restoreLocks, resetLocks, captureLocks));
      }
    }
    return annotatedfunctions.buildOrThrow();
  }

  @SuppressWarnings("deprecation")
  private Set<LockIdentifier> createAnnotationMap(String function, String target) {
    Set<LockIdentifier> result = new TreeSet<>();

    String property = config.getProperty("annotate." + function + "." + target);
    if (property != null) {
      property = CharMatcher.whitespace().removeFrom(property);
      List<String> lockNames = Splitter.on(",").splitToList(property);
      LockIdentifier parsedId;
      for (String fullName : lockNames) {
        if (fullName.matches(".*\\(.*")) {
          List<String> stringArray = Splitter.on("(").splitToList(fullName);
          assert stringArray.size() == 2;
          parsedId = LockIdentifier.of(stringArray.get(0), stringArray.get(1));
        } else {
          parsedId = LockIdentifier.of(fullName, "");
        }
        result.add(parsedId);
      }
    }
    return result;
  }
}
