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

public class SMGException extends CPATransferException {
  private static final long serialVersionUID = -1677699207895867889L;

  // Null for String msgs only
  private final @Nullable SMGState errorState;

  public SMGException(SMGState pErrorState) {
    super(pErrorState.getErrorInfo().toString());
    errorState = pErrorState;
  }

  public SMGException(String errorMsg) {
    super(errorMsg);
    errorState = null;
  }

  /**
   * Returns the {@link SMGState} that is the error state. Careful, might be null! Check with
   * hasState().
   */
  public SMGState getErrorState() {
    return errorState;
  }
}
