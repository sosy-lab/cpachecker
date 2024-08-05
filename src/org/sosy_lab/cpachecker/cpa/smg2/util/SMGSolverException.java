// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.java_smt.api.SolverException;

public class SMGSolverException extends CPATransferException {

  private static final long serialVersionUID = -1677699207895867889L;

  // Null for String msgs only
  private final @Nullable SMGState errorState;

  private final Exception wrappedException;

  public SMGSolverException(Exception pWrappedException, SMGState pErrorState) {
    super("If you ever read this text, the developer of SMG forgot to unwrap this exception.");
    errorState = pErrorState;
    wrappedException = pWrappedException;
  }

  /** Returns the {@link SMGState} that is the error state. */
  public SMGState getErrorState() {
    return errorState;
  }

  public boolean isInterruptedException() {
    return wrappedException instanceof InterruptedException;
  }

  public boolean isSolverException() {
    return wrappedException instanceof SolverException;
  }

  public boolean isUnrecognizedCodeException() {
    return wrappedException instanceof UnrecognizedCodeException;
  }

  public InterruptedException getInterruptedException() {
    return (InterruptedException) wrappedException;
  }

  public SolverException getSolverException() {
    return (SolverException) wrappedException;
  }
}
