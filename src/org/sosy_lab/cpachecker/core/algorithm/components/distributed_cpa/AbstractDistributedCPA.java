// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class AbstractDistributedCPA implements ConfigurableProgramAnalysis {

  protected final BlockNode block;
  protected final UpdatedTypeMap typeMap;
  protected final AnalysisDirection direction;
  protected final String id;
  protected final AnalysisOptions analysisOptions;
  protected ConfigurableProgramAnalysis parentCPA;
  protected Message firstMessage;
  protected Precision precision;

  protected Message latestOwnPostConditionMessage;

  public AbstractDistributedCPA(
      String pId,
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection,
      AnalysisOptions pOptions) throws
                                CPAException {
    block = pNode;
    typeMap = pTypeMap;
    direction = pDirection;
    id = pId;
    precision = pPrecision;
    analysisOptions = pOptions;
  }

  public abstract AbstractState deserialize(Message pPayload)
      throws InterruptedException;

  public abstract Payload serialize(AbstractState pState);

  protected abstract MessageProcessing proceedForward(Message newMessage)
      throws InterruptedException, SolverException;

  protected abstract MessageProcessing proceedBackward(Message newMessage)
      throws SolverException, InterruptedException;

  public abstract AbstractState combine(AbstractState pState1, AbstractState pState2)
      throws InterruptedException, CPAException;

  public MessageProcessing proceed(Message newMessage)
      throws SolverException, InterruptedException {
    return direction == AnalysisDirection.FORWARD ? proceedForward(newMessage)
                                                  : proceedBackward(newMessage);
  }

  public final AbstractState combine(List<AbstractState> pStates)
      throws InterruptedException, CPAException {
    if (pStates.isEmpty()) {
      return getInitialState(
          direction == AnalysisDirection.FORWARD ? block.getStartNode() : block.getLastNode(),
          StateSpacePartition.getDefaultPartition());
    }
    if (pStates.size() == 1) {
      return pStates.get(0);
    }

    List<AbstractState> ordered = new ArrayList<>(pStates);
    AbstractState state = ordered.remove(0);

    for (AbstractState abstractState : ordered) {
      state = combine(state, abstractState);
    }

    return state;
  }

  public abstract boolean doesOperateOn(Class<? extends AbstractState> pClass);

  public void setFirstMessage(Message pFirstMessage) {
    if (firstMessage != null) {
      throw new AssertionError("First message can only be set once: " + firstMessage);
    }
    firstMessage = pFirstMessage;
  }

  public AnalysisDirection getDirection() {
    return direction;
  }

  public ConfigurableProgramAnalysis getParentCPA() {
    return parentCPA;
  }

  public void setParentCPA(ConfigurableProgramAnalysis pParentCPA) throws CPAException {
    parentCPA = pParentCPA;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return parentCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return parentCPA.getStopOperator();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return parentCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return parentCPA.getTransferRelation();
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return parentCPA.getInitialState(node, partition);
  }

  public void synchronizeKnowledge(AbstractDistributedCPA pDCPA) {
    assert pDCPA.getClass().equals(getClass()) :
        "Can only synchronize knowledge between equal classes of DCPAs but got " + pDCPA.getClass()
            + " and " + getClass();
    assert pDCPA.direction != direction
        : "Can only exchange data from DCPAs operating in distinct directions (cannot override values)";
    if (direction == AnalysisDirection.BACKWARD) {
      latestOwnPostConditionMessage = pDCPA.latestOwnPostConditionMessage;
    }
  }

  public CFANode getStartNode() {
    return direction == AnalysisDirection.FORWARD ? block.getStartNode() : block.getLastNode();
  }

  public void setLatestOwnPostConditionMessage(Message m) {
    latestOwnPostConditionMessage = m;
  }
}
