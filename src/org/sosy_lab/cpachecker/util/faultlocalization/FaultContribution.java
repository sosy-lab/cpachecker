// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

/**
 * Every FaultContribution represents an edge in the program that contains a fault or is responsible
 * for a fault in combination with the other edges in the set. Reasons can be added to a
 * FaultContribution object. They should explain why this object is responsible for the error. The
 * assigned score is used to rank the FaultContributions.
 */
public class FaultContribution {

  protected final List<FaultInfo> infos;
  private final CFAEdge correspondingEdge;

  /**
   * The calculation of the score of FaultContribution is not implemented. The score is used to rank
   * FaultContributions. The recommended way is to calculate the score based on the likelihood of
   * the appended RankInfos instead of setting it to an value manually. The score will be printed to
   * the user as an indicator of how likely this edge is to fix the error when changed. However,
   * there exists an example method for calculating the score. For more details see
   * FaultRankingUtils.
   *
   * @see FaultRankingUtils#assignScoreTo(FaultContribution)
   */
  private double score;

  public FaultContribution(CFAEdge pCorrespondingEdge) {
    infos = new ArrayList<>();
    correspondingEdge = pCorrespondingEdge;
    score = 0;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double pScore) {
    score = pScore;
  }

  public void addInfo(FaultInfo pFaultInfo) {
    infos.add(pFaultInfo);
  }

  public List<FaultInfo> getInfos() {
    return infos;
  }

  @Override
  public String toString() {
    List<FaultInfo> copy = ImmutableList.sortedCopyOf(infos);

    StringBuilder out =
        new StringBuilder(
            "Error suspected on line "
                + correspondingEdge().getFileLocation().getStartingLineInOrigin()
                + ".\n");

    for (FaultInfo faultInfo : copy) {
      switch (faultInfo.getType()) {
        case RANK_INFO:
          out.append(" ".repeat(2));
          break;
        case REASON:
          out.append(" ".repeat(5));
          break;
        case FIX:
          out.append(" ".repeat(8));
          break;
      }
      out.append(faultInfo).append("\n");
    }

    return out.toString();
  }

  public boolean hasReasons() {
    return !infos.isEmpty();
  }

  public CFAEdge correspondingEdge() {
    return correspondingEdge;
  }

  @Override
  public boolean equals(Object q) {
    if (q instanceof FaultContribution) {
      FaultContribution casted = (FaultContribution) q;
      if (correspondingEdge.equals(casted.correspondingEdge())) {
        if (casted.getInfos().size() == getInfos().size()) {
          return ImmutableList.sortedCopyOf(infos).equals(ImmutableList.sortedCopyOf(casted.infos));
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 5;
    result = Objects.hash(correspondingEdge, result);
    return result;
  }
}
