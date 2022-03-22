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

public interface SMGAbstractionCandidate {

  boolean isEmpty();

  UnmodifiableCLangSMG execute(CLangSMG pSMG, SMGState pSmgState) throws SMGInconsistentException;

  int getLength();

  int getScore();

  SMGAbstractionBlock createAbstractionBlock(UnmodifiableSMGState pSmgState);
}
