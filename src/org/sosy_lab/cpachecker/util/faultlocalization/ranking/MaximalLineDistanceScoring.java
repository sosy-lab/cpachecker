// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

public class MaximalLineDistanceScoring implements FaultScoring {

  private int errorLocation;

  /**
   * Sorts the result set by absolute distance to the error location based on the linenumber
   *
   * @param pErrorLocation the error location
   */
  public MaximalLineDistanceScoring(CFAEdge pErrorLocation) {
    errorLocation = pErrorLocation.getFileLocation().getStartingLineInOrigin();
  }

  @Override
  public RankInfo scoreFault(Fault fault) {
    int max =
        fault.stream()
            .mapToInt(
                fc ->
                    Math.abs(
                        fc.correspondingEdge().getFileLocation().getStartingLineInOrigin()
                            - errorLocation))
            .max()
            .orElse(0);
    return FaultInfo.rankInfo("This line is " + max + " line(s) away from the error location", max);
  }
}
