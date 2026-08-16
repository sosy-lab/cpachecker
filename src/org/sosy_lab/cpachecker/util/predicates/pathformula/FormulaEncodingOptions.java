// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

/*
 * This class collects some configurations options for the encoding process.
 *
 * <p>This class is not specific to any programming language, and should therefore never be
 * instantiated, please use the language-specific subclasses instead.
 */
@Options(prefix = "cpa.predicate")
public abstract class FormulaEncodingOptions {

  @Option(
      secure = true,
      description = "Ignore variables that are not relevant for reachability properties.")
  private boolean ignoreIrrelevantVariables = true;

  @Option(
      secure = true,
      description =
          "Whether to give up immediately if a very large array is encountered (heuristic, often we"
              + " would just waste time otherwise)")
  private boolean abortOnLargeArrays = true;

  @Option(
      secure = true,
      description =
          "Insert tmp-variables for parameters at function-entries. "
              + "The variables are similar to return-variables at function-exit.")
  private boolean useParameterVariables = false;

  @Option(
      secure = true,
      description =
          "Insert tmp-parameters for global variables at function-entries. "
              + "The global variables are also encoded with return-variables at function-exit.")
  private boolean useParameterVariablesForGlobals = false;

  public FormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this, FormulaEncodingOptions.class);
  }

  public boolean ignoreIrrelevantVariables() {
    return ignoreIrrelevantVariables;
  }

  public boolean shouldAbortOnLargeArrays() {
    return abortOnLargeArrays;
  }

  @SuppressWarnings("unused")
  public boolean useParameterVariables() {
    return useParameterVariables;
  }

  @SuppressWarnings("unused")
  public boolean useParameterVariablesForGlobals() {
    return useParameterVariablesForGlobals;
  }
}
