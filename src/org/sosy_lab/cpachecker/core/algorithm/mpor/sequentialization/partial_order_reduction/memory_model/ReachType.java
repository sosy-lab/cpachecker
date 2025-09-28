// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public enum ReachType {
  DIRECT(SeqToken.d, SeqToken.DIRECT),
  REACHABLE(SeqToken.r, SeqToken.REACHABLE);

  public final String shortName;

  public final String longName;

  ReachType(String pShortName, String pLongName) {
    shortName = pShortName;
    longName = pLongName;
  }
}
