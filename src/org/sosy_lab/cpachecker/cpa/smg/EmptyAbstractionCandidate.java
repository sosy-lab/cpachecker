// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;

enum EmptyAbstractionCandidate implements SMGAbstractionCandidate {
  INSTANCE;

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public int getScore() {
    return 0;
  }

  @Override
  public UnmodifiableCLangSMG execute(CLangSMG pSMG, SMGState pSmgState) {
    return pSMG;
  }

  @Override
  public SMGAbstractionBlock createAbstractionBlock(UnmodifiableSMGState pSmgState) {
    throw new IllegalArgumentException(
        "Can't create abstraction block of empty abstraction candidate.");
  }
}
