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
        "The maximum number of literals that should be dropped during the inductive generalization"
            + " of states."
  )
  private int maxLiteralsToDropDuringGeneralization = 5;

  @Option(
    secure = true,
    description =
        "The maximum number attempts at dropping literals during the inductive generalization of"
            + " states."
  )
  private int maxAttemptsToDropLiteralsDuringGeneralization = 10;

  @Option(secure = true, description = "Subsumes redundant abstraction predicates.")
  private boolean subsumeRedundantAbstractionPredicates = false;

  @Option(
    secure = true,
    description =
        "Try to shorten the lifted state further by checking if the lifting query is still valid "
            + "after dropping each literal in turn."
  )
  private boolean dropLiteralsBeyondUnsatCoreAfterLifting = false;

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
   * Returns the maximum number of attempts at dropping literals during generalization as specified
   * by the configuration file.
   *
   * @return The maximum number of attempts at dropping literals.
   */
  public int maxAttemptsAtDroppingLiterals() {
    return maxAttemptsToDropLiteralsDuringGeneralization;
  }

  /**
   * Returns the maximum number of literals that should be dropped during generalization as
   * specified by the configuration file.
   *
   * @return The maximum number of literals to be dropped.
   */
  public int maxLiteralsToDrop() {
    return maxLiteralsToDropDuringGeneralization;
  }

  /**
   * Returns whether the configuration file set the option to subsume redundant abstraction
   * predicates.
   *
   * @return If redundant abstraction predicates are to be removed.
   */
  public boolean shouldSubsumeRedundantAbstractionPredicates() {
    return subsumeRedundantAbstractionPredicates;
  }

  /**
   * Returns whether the lifted state should be further reduced by trying to manually drop literals.
   *
   * @return If literals should be manually dropped after lifting.
   */
  public boolean shouldDropLiteralsAfterLiftingWithUnsatCore() {
    return dropLiteralsBeyondUnsatCoreAfterLifting;
  }

}
