// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic;

import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;

public class SMGUnknownAbstractionCandidate implements SMGGenericAbstractionCandidate {

  private final static SMGUnknownAbstractionCandidate INSTANCE = new SMGUnknownAbstractionCandidate();

  private SMGUnknownAbstractionCandidate() {}

  @Override
  public int getScore() {
    return 0;
  }

  @Override
  public SMG execute(SMG pSMG) {
    throw new UnsupportedOperationException("Unknown abstraction cannot be executed");
  }

  @Override
  public boolean isUnknown() {
    return true;
  }

  public static SMGUnknownAbstractionCandidate getInstance() {
    return INSTANCE;
  }

}
