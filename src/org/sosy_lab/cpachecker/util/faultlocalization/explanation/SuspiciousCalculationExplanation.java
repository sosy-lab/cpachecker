// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.explanation;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultExplanation;

public class SuspiciousCalculationExplanation implements FaultExplanation {

  @Override
  public String explanationFor(Fault subset) {
    String offByOne =
        FluentIterable.from(subset)
            .filter(
                fc ->
                    fc.correspondingEdge().getDescription().contains("+ 1")
                        || fc.correspondingEdge().getDescription().contains("- 1"))
            .transform(fc -> fc.correspondingEdge().getDescription())
            .join(Joiner.on(", "));
    if (offByOne.isBlank()) {
      return "";
    }
    return "Found suspicious lines indicating a possible off-by-one error: " + offByOne;
  }
}
