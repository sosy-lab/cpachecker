// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public enum BitVectorAccessType {
  NONE(SeqSyntax.EMPTY_STRING, SeqSyntax.EMPTY_STRING),
  /** For both read and write. */
  ACCESS(SeqToken.a, SeqToken.ACCESS),
  READ(SeqToken.r, SeqToken.READ),
  WRITE(SeqToken.w, SeqToken.WRITE);

  public final String shortName;

  public final String longName;

  BitVectorAccessType(String pShortName, String pLongName) {
    shortName = pShortName;
    longName = pLongName;
  }
}
