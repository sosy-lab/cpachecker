// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

// Name subject to change; i just need the exception right now
public class SMG2Exception extends CPATransferException {
  private static final long serialVersionUID = -1677699207895867889L;

  // Null for String msgs only
  private final @Nullable SMGState errorState;

  public SMG2Exception(SMGState pErrorState) {
    super(pErrorState.getErrorInfo().toString());
    errorState = pErrorState;
  }

  public SMG2Exception(String errorMsg) {
    super(errorMsg);
    errorState = null;
  }

  public boolean hasState() {
    return errorState != null;
  }

  /**
   * @return the {@link SMGState} that is the error state. Careful, might be null! Check with
   *     hasState().
   */
  public SMGState getErrorState() {
    return errorState;
  }
}
