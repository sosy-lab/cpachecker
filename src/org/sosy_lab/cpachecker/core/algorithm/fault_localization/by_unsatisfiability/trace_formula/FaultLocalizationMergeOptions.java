// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo.FaultLocalizationInfoMergeStrategy;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo.FaultSelectionStrategy;

@Options(prefix = "faultLocalization.merge")
public class FaultLocalizationMergeOptions {

  @Option(description = "how to merge lists of faults")
  private FaultLocalizationInfoMergeStrategy mergeStrategy;

  @Option(description = "which faults to use")
  private FaultSelectionStrategy selectionStrategy;

  public FaultLocalizationMergeOptions(Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  public FaultLocalizationInfoMergeStrategy getMergeStrategy() {
    return mergeStrategy;
  }

  public FaultSelectionStrategy getSelectionStrategy() {
    return selectionStrategy;
  }
}
