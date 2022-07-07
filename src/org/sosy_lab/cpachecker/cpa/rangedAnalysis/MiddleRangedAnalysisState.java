// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysis;

public class MiddleRangedAnalysisState extends RangedAnalysisState {

  private static final long serialVersionUID = 6769991514691078996L;

  public MiddleRangedAnalysisState() {
    super(null, null);
  }

  @Override
  public String toDOTLabel() {
    return "MIDDLE";
  }
}
