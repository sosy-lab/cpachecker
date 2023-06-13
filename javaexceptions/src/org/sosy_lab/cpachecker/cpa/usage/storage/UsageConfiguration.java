// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector.UnsafeMode;

@Options(prefix = "cpa.usage")
public final class UsageConfiguration {

  @Option(description = "output only true unsafes", secure = true)
  private boolean printOnlyTrueUnsafes = false;

  @Option(
      name = "unsafedetector.ignoreEmptyLockset",
      description = "ignore unsafes only with empty callstacks",
      secure = true)
  private boolean ignoreEmptyLockset = true;

  @Option(name = "unsafedetector.unsafeMode", description = "defines what is unsafe", secure = true)
  private UnsafeMode unsafeMode = UnsafeMode.RACE;

  @Option(
      name = "unsafedetector.intLock",
      description = "A name of interrupt lock for checking deadlock free",
      secure = true)
  private String intLockName = null;

  public UsageConfiguration(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  boolean printOnlyTrueUnsafes() {
    return printOnlyTrueUnsafes;
  }

  boolean ignoreEmptyLockset() {
    return ignoreEmptyLockset;
  }

  UnsafeMode getUnsafeMode() {
    return unsafeMode;
  }

  String getIntLockName() {
    return intLockName;
  }
}
