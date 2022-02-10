// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Tuple for {@link SMGValue} and {@link SymbolicProgramConfiguration}. */
public class SMGValueAndSPC {

  private final SMGValue value;
  private final SymbolicProgramConfiguration spc;

  private SMGValueAndSPC(SMGValue pValue, SymbolicProgramConfiguration pSPC) {
    value = pValue;
    spc = pSPC;
  }

  public static SMGValueAndSPC of(SMGValue pValue, SymbolicProgramConfiguration pSPC) {
    Preconditions.checkNotNull(pValue);
    Preconditions.checkNotNull(pSPC);
    return new SMGValueAndSPC(pValue, pSPC);
  }

  public SMGValue getSMGValue() {
    return value;
  }

  public SymbolicProgramConfiguration getSPC() {
    return spc;
  }
}
