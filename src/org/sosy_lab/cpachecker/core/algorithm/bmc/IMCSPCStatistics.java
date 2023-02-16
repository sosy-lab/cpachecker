// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.io.PrintStream;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class IMCSPCStatistics implements Statistics {

  final Timer itpQuery = new Timer();
  int numUnroll = 0;
  int numItp = 0;

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Interpolation time: " + itpQuery);
    pOut.println("Number of unrolled edges: " + numUnroll);
    pOut.println("Number of interpolation queries: " + numItp);
  }

  @Override
  public String getName() {
    return "IMC-SPC algorithm";
  }
}
