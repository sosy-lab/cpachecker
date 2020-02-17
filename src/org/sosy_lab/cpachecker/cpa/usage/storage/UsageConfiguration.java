/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
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
