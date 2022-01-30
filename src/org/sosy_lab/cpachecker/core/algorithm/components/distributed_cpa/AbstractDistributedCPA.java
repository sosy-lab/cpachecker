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
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class AbstractDistributedCPA implements ConfigurableProgramAnalysis {

  protected final BlockNode block;
  protected final SSAMap typeMap;
  protected final AnalysisDirection direction;
  protected final String id;
  protected ConfigurableProgramAnalysis parentCPA;
  protected Message firstMessage;
  protected Precision precision;

  public AbstractDistributedCPA(
      String pId,
      BlockNode pNode,
      SSAMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection) throws
                                    CPAException {
    block = pNode;
    typeMap = pTypeMap;
    direction = pDirection;
    id = pId;
    precision = pPrecision;
  }

  public abstract AbstractState deserialize(Payload pPayload, CFANode location)
      throws InterruptedException;

  public abstract Payload serialize(AbstractState pState);

  protected abstract MessageProcessing proceedForward(Message newMessage);

  protected abstract MessageProcessing proceedBackward(Message newMessage)
      throws SolverException, InterruptedException;

  public abstract AbstractState combine(AbstractState pState1, AbstractState pState2)
      throws InterruptedException;

  public MessageProcessing proceed(Message newMessage)
      throws SolverException, InterruptedException {
    return direction == AnalysisDirection.FORWARD ? proceedForward(newMessage)
                                                  : proceedBackward(newMessage);
  }

  public final AbstractState combine(List<AbstractState> pStates)
      throws InterruptedException, CPAException {
    if (pStates.size() < 1) {
      throw new AssertionError("Merging requires at least one state: " + pStates);
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

  public void setParentCPA(ConfigurableProgramAnalysis pParentCPA) throws CPAException {
    parentCPA = pParentCPA;
  }

  public AnalysisDirection getDirection() {
    return direction;
  }

  public ConfigurableProgramAnalysis getParentCPA() {
    return parentCPA;
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

  public CFANode getStartNode() {
    return direction == AnalysisDirection.FORWARD ? block.getStartNode() : block.getLastNode();
  }
}
