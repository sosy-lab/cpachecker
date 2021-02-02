// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariantsampling;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "invariantsampling")
public class TestCPA extends AbstractCPA {

  @Option(
      secure = true,
      // name = "whichStopType",
      description = "Determines which stop operator to use for the invariant sampling analysis.")
  private String stopType = "sep";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TestCPA.class);
  }

  protected TestCPA(Configuration pConfig) throws InvalidConfigurationException {
    super("sep", "sep", new InvSamplingTransferRelation());
    pConfig.inject(this);
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new InvSamplingState();
  }

  private static class InvSamplingState implements AbstractState {
    public InvSamplingState() {}
  }

  private static class InvSamplingTransferRelation extends SingleEdgeTransferRelation implements TransferRelation {

    public InvSamplingTransferRelation() {}

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
        AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
        throws CPATransferException, InterruptedException {
      // TODO Auto-generated method stub
      return ImmutableList.of(pState);
    }
  }
}
