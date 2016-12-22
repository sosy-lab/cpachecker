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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import java.util.Objects;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

/** A container for all PDR related options. */
@Options(prefix = "pdr")
public class PDROptions {

  @Option(
    secure = true,
    description =
        "Simplifies the global transition relation by keeping only"
            + " those transitions whose corresponding blocks are backwards reachable from the target"
            + " locations."
  )
  private boolean removeRedundantTransitions = false;

  /**
   * Creates a new instance and injects all relevant options from the provided configuration.
   *
   * @param pConfig The configuration containing the options.
   * @throws InvalidConfigurationException If the configuration file is incorrect.
   */
  public PDROptions(Configuration pConfig) throws InvalidConfigurationException {
    Objects.requireNonNull(pConfig).inject(this);
  }

  /**
   * Returns whether the configuration file set the option to remove all redundant block transitions
   * from the global transition relation.
   *
   * @return True, if redundant transitions are to be removed. False, if not.
   */
  public boolean shouldRemoveRedundantTransitions() {
    return removeRedundantTransitions;
  }
}
