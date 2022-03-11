// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

public abstract class AbstractStatValue {

  private final String title;
  private StatKind mainStatisticKind;

  protected AbstractStatValue(StatKind pMainStatisticKind, String pTitle) {
    title = pTitle;
    mainStatisticKind = pMainStatisticKind;
  }

  public String getTitle() {
    return title;
  }

  /**
   * How many times was this statistical value updated.
   *
   * @return A nonnegative number.
   */
  public abstract int getUpdateCount();

  public StatKind getMainStatisticKind() {
    return mainStatisticKind;
  }
}
