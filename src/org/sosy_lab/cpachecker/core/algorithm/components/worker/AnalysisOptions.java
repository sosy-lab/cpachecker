// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "worker")
public class AnalysisOptions {

  @Option(description = "forces the precondition of fault localization workers to be true")
  private boolean faultLocalizationPreconditionAlwaysTrue = false;

  @Option(description = "whether analysis worker abstract at block entries or exits")
  private boolean abstractAtTargetLocation = false;

  @Option(description = "whether analysis worker store circular post conditions")
  private boolean doStoreCircularPostConditions = false;

  public AnalysisOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public boolean isFaultLocalizationPreconditionAlwaysTrue() {
    return faultLocalizationPreconditionAlwaysTrue;
  }

  public boolean doAbstractAtTargetLocations() {
    return abstractAtTargetLocation;
  }

  public boolean doStoreCircularPostConditions() {
    return doStoreCircularPostConditions;
  }
}
