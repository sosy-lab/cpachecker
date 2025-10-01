// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGAndSMGValues {

  private final SMG smg;

  private final Set<SMGValue> values;

  private SMGAndSMGValues(SMG pSmg, Set<SMGValue> pSMGValues) {
    Preconditions.checkNotNull(pSmg);
    Preconditions.checkNotNull(pSMGValues);
    smg = pSmg;
    values = pSMGValues;
  }

  public static SMGAndSMGValues of(SMG pSmg, Set<SMGValue> pSMGValues) {
    return new SMGAndSMGValues(pSmg, pSMGValues);
  }

  public SMG getSMG() {
    return smg;
  }

  public Set<SMGValue> getSMGValues() {
    return values;
  }
}
