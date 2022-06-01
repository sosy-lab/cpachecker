// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

/**
 * A Fault is a set of FaultContributions. The set has to be obtained by a fault localizing
 * algorithm. FaultReasons can be appended to a Fault to explain why this set of FaultContributions
 * caused an error. The score of a Fault is used to rank the Faults. The higher the score the higher
 * the rank.
 */
public class Fault extends ForwardingSet<FaultContribution> implements Comparable<Fault> {

  private final Set<FaultContribution> errorSet;
  private final List<FaultInfo> infos;
  private int intendedIndex;

  /**
   * The recommended way is to calculate the score based on the likelihoods of the appended reasons.
   * However, the implementation can be arbitrary.
   *
   * @see FaultRankingUtils#assignScoreTo(Fault)
   */
  private double score;

  /**
   * Error Indicators indicate a subset of all edges that most likely contain an error.
   *
   * @param pErrorSet set to forward
   */
  public Fault(Collection<FaultContribution> pErrorSet) {
    this(pErrorSet, 0);
  }

  public Fault() {
    this(new LinkedHashSet<>(), 0);
  }

  /**
   * Creates a mutable set with only one member
   *
   * @param singleton a FaultContribution that is transformed into a singleton set
   */
  public Fault(FaultContribution singleton) {
    this(Collections.singleton(singleton), 0);
  }

  public Fault(FaultContribution pContribs, double pScore) {
    this(Collections.singleton(pContribs), pScore);
  }

  public Fault(Collection<FaultContribution> pContribs, double pScore) {
    errorSet = new LinkedHashSet<>(pContribs);
    infos = new ArrayList<>();
    score = pScore;
  }

  /**
   * Add a reason to explain why this set indicates an error. Appending reasons in heuristics is the
   * recommended way.
   *
   * @param reason Fix, Hint or Reason why this might be an error or why a ranking did add a
   *     FaultInfo to this set.
   */
  public void addInfo(FaultInfo reason) {
    infos.add(reason);
  }

  public List<FaultInfo> getInfos() {
    return infos;
  }

  public double getScore() {
    return score;
  }

  /**
   * Set the score for this Fault. There exists a default implementation of calculating the score
   * for this class.
   *
   * @see FaultRankingUtils#assignScoreTo(Fault)
   * @param pScore the score
   */
  public void setScore(double pScore) {
    score = pScore;
  }

  @Override
  public String toString() {
    List<FaultInfo> copy = ImmutableList.sortedCopyOf(infos);

    StringBuilder out =
        new StringBuilder("Error suspected on line(s): " + listDistinctLinesAndJoin() + ".\n");
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

  private String listDistinctLinesAndJoin() {
    List<String> lines =
        errorSet.stream()
            .mapToInt(l -> l.correspondingEdge().getFileLocation().getStartingLineInOrigin())
            .sorted()
            .distinct()
            .mapToObj(String::valueOf)
            .collect(ImmutableList.toImmutableList());

    String result;
    if (lines.size() <= 2) {
      result = String.join(" and ", lines);
    } else {
      int lastIndex = lines.size() - 1;
      result = String.join(", ", lines.subList(0, lastIndex) + " and " + lines.get(lastIndex));
    }
    return result + " (Score: " + (int) (score * 100) + ")";
  }

  /**
   * Set an intended index. Call sortIntended on FaultLocalizationInfo to sort ascending by intended
   * index
   *
   * @param pIntendedIndex the intended place in the final list for this fault
   */
  public void setIntendedIndex(int pIntendedIndex) {
    intendedIndex = pIntendedIndex;
  }

  public int getIntendedIndex() {
    return intendedIndex;
  }

  @Override
  public boolean equals(Object q) {
    if (!(q instanceof Fault)) {
      return false;
    }

    Fault comp = (Fault) q;
    return errorSet.equals(comp.errorSet) && infos.equals(comp.infos);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorSet, infos);
  }

  @Override
  protected Set<FaultContribution> delegate() {
    return errorSet;
  }

  @Override
  public int compareTo(Fault o) {
    // higher score means higher rank
    return Double.compare(o.score, score);
  }

  public void replaceErrorSet(Set<FaultContribution> pContributions) {
    errorSet.clear();
    errorSet.addAll(pContributions);
  }
}
