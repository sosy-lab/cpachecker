// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

/** This class collects some configurations options for the C-to-formula encoding process. */
@Options(prefix = "cpa.predicate.svlib")
public class SvLibFormulaEncodingOptions {

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

  public SvLibFormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this, SvLibFormulaEncodingOptions.class);
  }

  public boolean ignoreIrrelevantVariables() {
    return ignoreIrrelevantVariables;
  }

  @SuppressWarnings("unused")
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
