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

@Options(prefix="analysis")
public class BlockConfiguration {

  @Option(secure=true, name = "traversal.blockResourceLimit",
      description = "resource limit for the block")
  int blockResourceLimit = 1000;

  @Option(secure=true, name = "traversal.entryResourceLimit",
      description = "resource limit for the entry block")
  int entryResourceLimit = 100000;

  @Option(secure=true, name = "traversal.blockSaveResources",
      description = "save resources for the block if it is empty")
  boolean blockSaveResources = true;

  @Option(secure=true, name="traversal.blockFunctionPatterns", description = "Patterns for detecting block starts (ldv_ like functions)")
  ImmutableSet<String> blockFunctionPatterns = ImmutableSet.of("ldv_%_instance_%");

  public BlockConfiguration(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

}
