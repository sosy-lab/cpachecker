/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Summary manager for the {@link CompositeCPA}.
 */
public class CompositeSummaryManager implements SummaryManager {

  private final List<SummaryManager> managers;

  CompositeSummaryManager(List<ConfigurableProgramAnalysis> pCpas) {
    managers = pCpas.stream().map(
        cpa -> ((UseSummaryCPA) cpa).getSummaryManager()
    ).collect(Collectors.toList());
  }

  @Override
  public List<? extends AbstractState> getAbstractSuccessorsForSummary(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<Summary> pSummary,
      Block pBlock,
      CFAEdge pCallEdge)
      throws CPAException, InterruptedException {

    CompositePrecision cPrecision = (CompositePrecision) pCallPrecision;
    CompositeState cState = (CompositeState) pCallState;
    Preconditions.checkState(cState.getNumberOfStates() == managers.size());

    List<List<? extends AbstractState>> contained = new ArrayList<>(managers.size());
    for (int i=0; i<managers.size(); i++) {
      int idx = i;
      List<Summary> projectedSummaries = pSummary.stream()
          .map(c -> ((CompositeSummary) c).get(idx)).collect(Collectors.toList());

      List<? extends AbstractState> successors =
          managers.get(idx).getAbstractSuccessorsForSummary(
              cState.get(idx), cPrecision.get(idx), projectedSummaries, pBlock, pCallEdge
          );
      contained.add(successors);
    }

    return Lists.cartesianProduct(contained).stream()
        .map(l -> new CompositeState(l)).collect(Collectors.toList());
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pCallState,
      Precision pPrecision,
      CFAEdge pCallEdge,
      Block pBlock) {
    CompositeState cState = (CompositeState) pCallState;
    CompositePrecision cPrecision = (CompositePrecision) pPrecision;
    List<AbstractState> weakened = IntStream.range(0, managers.size())
        .mapToObj(i ->
            managers.get(i).getWeakenedCallState(
                cState.get(i), cPrecision.get(i), pCallEdge, pBlock
            )).collect(Collectors.toList());
    return new CompositeState(weakened);
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pJoinPrecisions,
      CFANode pCallNode,
      Block pBlock) throws CPATransferException {
    CompositeState cEntryState = (CompositeState) pCallState;
    CompositePrecision cEntryPrecision = (CompositePrecision) pCallPrecision;

    List<List<? extends Summary>> computed = new ArrayList<>(managers.size());
    for (int i=0; i<managers.size(); i++) {
      final int idx = i;
      computed.add(managers.get(i).generateSummaries(
            cEntryState.get(i),
            cEntryPrecision.get(i),
            pReturnStates.stream()
                .map(s -> ((CompositeState) s).get(idx)).collect(Collectors.toList()),
            pJoinPrecisions.stream()
                .map(s -> ((CompositePrecision) s).get(idx)).collect(Collectors.toList()),
          pCallNode,
            pBlock
        ));
    }

    List<List<Summary>> product = Lists.cartesianProduct(computed);
    return product.stream().map(l -> new CompositeSummary(l)).collect(Collectors.toList());
  }


  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {

    CompositeSummary cSummary1 = (CompositeSummary) pSummary1;
    CompositeSummary cSummary2 = (CompositeSummary) pSummary2;
    for (int i = 0; i < managers.size(); i++) {
      if (!managers.get(i).isDescribedBy(
          cSummary1.get(i),
          cSummary2.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public CompositeSummary merge(
      Summary pSummary1, Summary pSummary2) throws CPAException, InterruptedException {

    CompositeSummary cSummary1 = (CompositeSummary) pSummary1;
    CompositeSummary cSummary2 = (CompositeSummary) pSummary2;

    boolean identicalStates = true;
    List<Summary> mergedSummaries = new ArrayList<>(managers.size());
    for (int i = 0; i < managers.size(); i++) {
      SummaryManager mgr = managers.get(i);
      Summary s1 = cSummary1.get(i);
      Summary s2 = cSummary2.get(i);
      Summary merged = mgr.merge(s1, s2);

      if (!mgr.isDescribedBy(s1, merged)) {

        // The result does not cover s1 => might as well perform splitting on the
        // entire state-space.
        return cSummary2;
      }

      if (merged != s2) {
        identicalStates = false;
      }
      mergedSummaries.add(merged);
    }
    if (identicalStates) {
      return cSummary2;
    }
    return new CompositeSummary(mergedSummaries);
  }

  @Override
  public boolean isCallsiteLessThanSummary(
      AbstractState pCallsite, Summary pSummary) {

    CompositeSummary cSummary = (CompositeSummary) pSummary;
    CompositeState cState = (CompositeState) pCallsite;

    for (int i=0; i<managers.size(); i++) {
      if (!managers.get(i).isCallsiteLessThanSummary(cState.get(i), cSummary.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getSummaryPartition(Summary pSummary) {
    CompositeSummary cSummary = (CompositeSummary) pSummary;
    return IntStream.range(0, managers.size())
        .mapToObj(i -> managers.get(i).getSummaryPartition(cSummary.get(i)))
        .reduce(String::concat).get();
  }

  @Override
  public String getCallstatePartition(AbstractState pAbstractState) {
    CompositeState cState = (CompositeState) pAbstractState;
    return IntStream.range(0, managers.size())
        .mapToObj(i -> managers.get(i).getCallstatePartition(cState.get(i)))
        .reduce(String::concat).get();
  }

  private static class CompositeSummary implements Summary {

    private final List<Summary> summaries;

    private CompositeSummary(Collection<? extends Summary> pSummaries) {
      summaries = ImmutableList.copyOf(pSummaries);
    }

    public Summary get(int idx) {
      return summaries.get(idx);
    }

    @Override
    public boolean equals(@Nullable Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      CompositeSummary that = (CompositeSummary) pO;
      return Objects.equals(summaries, that.summaries);
    }

    @Override
    public int hashCode() {
      return summaries.hashCode();
    }

    @Override
    public String toString() {
      return "CompositeSummary{" +
          "summaries=" + Joiner.on('\n').join(summaries) + '}';
    }
  }
}
