// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "cpa.usage")
public class ProbeFilter extends CallstackFilter {

  public ProbeFilter(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(pWrapper, pConfig);
  }

  @Override
  protected String getPathCore(ExtendedARGPath pPath) {
    String originName = super.getPathCore(pPath);
    if (originName != null) {
      if (originName.contains("probe")) {
        return "probe";
      } else if (originName.contains("disconnect")) {
        return "disconnect";
      }
    }
    return originName;
  }
}
