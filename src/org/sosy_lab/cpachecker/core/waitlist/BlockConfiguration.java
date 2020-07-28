// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
