// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

/** Interface represent a reached set, which provide information about verification result. */
public interface ResultProviderReachedSet {

  /** Determine verification result for the given reached set. */
  public Result getOverallResult();

  /** Print additional information on verification result. */
  public void printResults(PrintStream out);
}
