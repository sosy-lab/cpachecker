// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class FaultUtil {

  public static Fault fromEdgesAnnotated(
      Collection<CFAEdge> pEdges, Function<CFAEdge, Collection<FaultInfo>> pAnnotations) {
    Fault result =
        FluentIterable.from(pEdges)
            .transform(
                edge -> {
                  FaultContribution fc = new FaultContribution(edge);
                  pAnnotations.apply(edge).forEach(fc::addInfo);
                  return fc;
                })
            .copyInto(new Fault());
    result.forEach(fc -> fc.getInfos().forEach(result::addInfo));
    return result;
  }

  public static Fault fromEdges(Collection<CFAEdge> pEdges) {
    return FluentIterable.from(pEdges).transform(FaultContribution::new).copyInto(new Fault());
  }

  /**
   * Return a new fault that represents the intersection of both input faults. The intersection is
   * build upon the corresponding edges contained in the fault contributions of the fault. The hints
   * for fault contributions are merged as well.
   *
   * @param fault1 the first fault
   * @param fault2 the second fault
   * @return the intersection of the first and the second fault.
   */
  public static Fault intersection(Fault fault1, Fault fault2) {
    // if there are 2 entries in the map, both faults
    // contain a fault contribution with the same edge,
    // i.e., we can build the intersection
    return faultMergeHelper(fault1, fault2, collection -> collection.size() > 1);
  }

  /**
   * Merge all fault contributions contained in the two faults to one new faults. In case, both
   * faults contain fault contributions mapping to the same CFA edge, the fault contributions are
   * merged by appending the hints for of both contributions.
   *
   * @param fault1 the first fault
   * @param fault2 the second fault
   * @return the union of the first and the second fault.
   */
  public static Fault union(Fault fault1, Fault fault2) {
    // we take all edges of all fault contributions and combine it to one fault.
    return faultMergeHelper(fault1, fault2, Predicates.alwaysTrue());
  }

  private static Fault faultMergeHelper(
      Fault fault1, Fault fault2, Predicate<Collection<FaultContribution>> pCheck) {
    // Note that merging changes the scores, and they will not sum up to 1 anymore
    Multimap<CFAEdge, FaultContribution> edgeToFaultContributions = ArrayListMultimap.create();
    fault1.forEach(fc -> edgeToFaultContributions.put(fc.correspondingEdge(), fc));
    fault2.forEach(fc -> edgeToFaultContributions.put(fc.correspondingEdge(), fc));
    double averageScore = (fault1.getScore() + fault2.getScore()) / 2d;
    Set<FaultContribution> fcs =
        FluentIterable.from(edgeToFaultContributions.keySet())
            .filter(edge -> pCheck.test(edgeToFaultContributions.get(edge)))
            .transform(edge -> mergeFaultContributions(edge, edgeToFaultContributions.get(edge)))
            .toSet();
    Fault result = new Fault(fcs);
    fcs.forEach(fc -> fc.getInfos().forEach(result::addInfo));
    result.setScore(averageScore);
    fault1.getInfos().forEach(result::addInfo);
    fault2.getInfos().forEach(result::addInfo);
    return result;
  }

  private static FaultContribution mergeFaultContributions(
      CFAEdge edge, Collection<FaultContribution> contributions) {
    Preconditions.checkArgument(
        contributions.stream().allMatch(fc -> fc.correspondingEdge().equals(edge)),
        "Merging fault contributions is only possible if all fault contributions correspond to the"
            + " exact same CFAEdge");
    FaultContribution merged = new FaultContribution(edge);
    contributions.forEach(fc -> fc.getInfos().forEach(merged::addInfo));
    return merged;
  }
}
