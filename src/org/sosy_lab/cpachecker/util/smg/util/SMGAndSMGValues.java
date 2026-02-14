// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGAndSMGValues {

  private final SMG smg;

  private final Map<SMGValue, SMGValue> values;

  private SMGAndSMGValues(SMG pSmg, Map<SMGValue, SMGValue> pSMGValues) {
    Preconditions.checkNotNull(pSmg);
    Preconditions.checkNotNull(pSMGValues);
    smg = pSmg;
    values = pSMGValues;
  }

  public static SMGAndSMGValues of(SMG pSmg, Map<SMGValue, SMGValue> pSMGValues) {
    return new SMGAndSMGValues(pSmg, pSMGValues);
  }

  public SMG getSMG() {
    return smg;
  }

  /** oldValue -> newValue */
  public Map<SMGValue, SMGValue> getSMGValues() {
    return values;
  }
}
