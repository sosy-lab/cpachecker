// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BaseStrategy implements Strategy {

  @Override
  public Optional<GhostCFA> summarize(CFANode pLoopStartNode) {
    return Optional.empty();
  }
}
