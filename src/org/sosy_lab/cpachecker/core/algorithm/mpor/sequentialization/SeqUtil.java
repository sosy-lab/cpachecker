// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

public class SeqUtil {

  public static final String tab = initTab();

  private static String initTab() {
    String rTab = SeqSyntax.EMPTY_STRING.getString();
    for (int s = 0; s < Sequentialization.TAB_SIZE; s++) {
      rTab += SeqSyntax.SPACE.getString();
    }
    return rTab;
  }
}
