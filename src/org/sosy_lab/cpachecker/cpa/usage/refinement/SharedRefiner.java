// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.local.LocalState;
import org.sosy_lab.cpachecker.cpa.local.LocalTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class SharedRefiner extends GenericSinglePathRefiner {

  private LocalTransferRelation transferRelation;

  // Debug counter
  private StatCounter counter = new StatCounter("Number of cases with empty successors");
  // private final StatInt totalFalseConditions = new StatInt(StatKind.COUNT, "Number of false
  // conditions that were detected by SharedRefiner");
  private StatCounter numOfFalseResults = new StatCounter("Number of false results");

  public SharedRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      LocalTransferRelation RelationForSharedRefiner) {
    super(pWrapper);
    transferRelation = RelationForSharedRefiner;
  }

  @Override
  protected RefinementResult call(ExtendedARGPath pPath) throws CPAException, InterruptedException {
    RefinementResult result = RefinementResult.createUnknown();
    List<CFAEdge> edges = pPath.getFullPath();
    SingletonPrecision emptyPrecision = SingletonPrecision.getInstance();

    LocalState lastState =
        AbstractStates.extractStateByType(pPath.getLastState(), LocalState.class);
    LocalState initialState = LocalState.createInitialLocalState(lastState);

    Collection<LocalState> successors = Collections.singleton(initialState);
    UsageInfo sharedUsage = pPath.getUsageInfo();
    SingleIdentifier usageId = pPath.getUsageInfo().getId();

    for (CFAEdge edge : edges) {
      assert (successors.size() <= 1);
      Iterator<LocalState> sharedIterator = successors.iterator();
      if (sharedUsage.getCFANode().equals(edge.getSuccessor())) {
        LocalState usageState = sharedIterator.next();
        assert usageState != null;

        if (usageState.getType(usageId) == LocalState.DataType.LOCAL) {
          result = RefinementResult.createFalse();
          numOfFalseResults.inc();
        } else {

          result = RefinementResult.createTrue();
        }
        break;
      } else {
        // TODO Important! Final state is not a state of usage. Think about.
        if (sharedIterator.hasNext()) {
          LocalState usageState = sharedIterator.next();

          successors =
              transferRelation.getAbstractSuccessorsForEdge(usageState, emptyPrecision, edge);
        } else {
          // Strange situation
          counter.inc();
          result = RefinementResult.createUnknown();
          break;
        }
      }
    }

    // totalFalseConditions.setNextValue(numOfFalseResults.getValue());
    return result;
  }

  @Override
  protected void printAdditionalStatistics(StatisticsWriter pOut) {
    pOut.beginLevel().put(counter).put(numOfFalseResults);
    // pOut.println(totalFalseConditions);
  }
}
