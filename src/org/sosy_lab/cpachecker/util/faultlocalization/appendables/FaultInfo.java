// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

import java.util.Comparator;
import java.util.Objects;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.NoContextExplanation;

public abstract class FaultInfo implements Comparable<FaultInfo> {

  /**
   * Represents the type of FaultInfo. The HTML report sorts FaultInfos according to the definition
   * in the enum InfoType.
   */
  public enum InfoType {
    /** The reason why a fault localization algorithm created the fault */
    REASON,
    /** Provides a possible fix */
    FIX,
    /** Information provided by the rankings */
    RANK_INFO
  }

  protected double score;
  protected String description;
  private final InfoType type;

  protected FaultInfo(InfoType pType) {
    type = pType;
  }

  /**
   * Returns a possible fix for pSet. It may be a guess. The set has to have size 1 because
   * NoContextExplanation is designed to explain singletons only.
   *
   * @param pFaultContribution find an explanation for this fault contribution
   * @return Explanation for pSet
   */
  public static FaultInfo possibleFixFor(FaultContribution pFaultContribution) {
    return new PotentialFix(
        NoContextExplanation.getInstance().explanationFor(new Fault(pFaultContribution)));
  }

  public static PotentialFix fix(String pDescription) {
    return new PotentialFix(pDescription);
  }

  public static RankInfo rankInfo(String pDescription, double pLikelihood) {
    return new RankInfo(pDescription, pLikelihood);
  }

  public static FaultReason justify(String pDescription) {
    return new FaultReason(pDescription);
  }

  public double getScore() {
    return score;
  }

  public String getDescription() {
    return description;
  }

  public InfoType getType() {
    return type;
  }

  /**
   * Sort by InfoType then by score.
   *
   * @param info FaultInfo for comparison
   * @return Is this object smaller equal or greater than info
   */
  @Override
  public int compareTo(FaultInfo info) {
    return Comparator.comparing(FaultInfo::getType)
        .thenComparingDouble(FaultInfo::getScore)
        .compare(this, info);
  }

  @Override
  public int hashCode() {
    return Objects.hash(31, description, score, type);
  }

  @Override
  public boolean equals(Object q) {
    if (q instanceof FaultInfo) {
      FaultInfo r = (FaultInfo) q;
      if (type.equals(r.type)) {
        return r.description.equals(description) && score == r.score;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    String percent = ((int) (score * 10000)) / 100d + "%";
    return type + ": " + description + " (" + percent + ")";
  }
}
