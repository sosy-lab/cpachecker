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
package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "analysis.traversal")
public class BlockConfiguration {

  @Option(secure = true, description = "resource limit for the block")
  private int blockResourceLimit = 1000;

  @Option(secure = true, description = "resource limit for the entry block")
  private int entryResourceLimit = 100000;

  @Option(secure = true, description = "save resources for the block if it is empty")
  private boolean blockSaveResources = true;

  @Option(secure = true, description = "Patterns for detecting block starts (ldv_ like functions)")
  private ImmutableSet<String> blockFunctionPatterns = ImmutableSet.of("ldv_%_instance_%");

  public BlockConfiguration(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  int getBlockResourceLimit() {
    return blockResourceLimit;
  }

  int getEntryResourceLimit() {
    return entryResourceLimit;
  }

  boolean shouldSaveBlockResources() {
    return blockSaveResources;
  }

  ImmutableSet<String> getBlockFunctionPatterns() {
    return blockFunctionPatterns;
  }
}
