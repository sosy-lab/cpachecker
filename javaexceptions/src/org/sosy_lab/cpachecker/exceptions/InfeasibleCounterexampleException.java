// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/** Exception raised when the refinement procedure fails, or was abandoned. */
public class InfeasibleCounterexampleException extends CPAException {

  private static final long serialVersionUID = 513019120577549278L;

  private final List<ARGPath> paths;

  public InfeasibleCounterexampleException(String msg, List<ARGPath> pInfeasibleErrorPaths) {
    super(msg);
    paths = pInfeasibleErrorPaths;
  }

  public InfeasibleCounterexampleException(String msg, List<ARGPath> p, Throwable t) {
    super(msg, checkNotNull(t));
    paths = p;
  }

  /** Return the path that caused the failure */
  public @Nullable List<ARGPath> getErrorPaths() {
    return paths;
  }
}
