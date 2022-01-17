// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class AbstractDistributedCPA<T extends ConfigurableProgramAnalysis, S extends AbstractState> {

  protected final BlockNode block;
  protected final SSAMap typeMap;
  protected final AnalysisDirection direction;
  protected  final String id;
  protected T parentCPA;
  protected Message firstMessage;
  protected Precision precision;

  public AbstractDistributedCPA(String pId, BlockNode pNode, SSAMap pTypeMap, Precision pPrecision, AnalysisDirection pDirection) throws
                                                                                                            CPAException {
    block = pNode;
    typeMap = pTypeMap;
    direction = pDirection;
    id = pId;
    precision = pPrecision;
  }

  public abstract Payload encode(Collection<S> statesAtBlockEntry);

  public abstract S decode(Collection<Payload> messages, S previousAbstractState);

  public abstract MessageProcessing stopForward(Message newMessage);

  public abstract MessageProcessing stopBackward(Message newMessage)
      throws SolverException, InterruptedException;

  public abstract Class<T> getParentCPAClass();

  public abstract Class<S> getAbstractStateClass();

  public Payload safeEncode(Collection<AbstractState> pStatesAtBlockEntry) {
    return encode(transform(pStatesAtBlockEntry));
  }

  public S safeDecode(Collection<Payload> messages, AbstractState previousAbstractState) {
    return decode(messages, transform(previousAbstractState));
  }

  private Collection<S> transform(Collection<AbstractState> pAbstractStates) {
    return pAbstractStates.stream().map(this::transform).collect(ImmutableList.toImmutableList());
  }

  private S transform(AbstractState pAbstractStates) {
    if (!getAbstractStateClass().isAssignableFrom(pAbstractStates.getClass())) {
      throw new AssertionError("expected " + getAbstractStateClass() + " but got " + pAbstractStates.getClass());
    }
    return getAbstractStateClass().cast(pAbstractStates);
  }

  public void setFirstMessage(Message pFirstMessage) {
    if (firstMessage != null) {
      throw new AssertionError("First message can only be set once: " + firstMessage);
    }
    firstMessage = pFirstMessage;
  }

  public void setParentCPA(T pParentCPA) throws CPAException {
    parentCPA = pParentCPA;
  }

  public void safeSetParentCPA(ConfigurableProgramAnalysis pCPA) throws CPAException {
    if (!getParentCPAClass().equals(pCPA.getClass())) {
      throw new AssertionError("expected " + getParentCPAClass() + " but got " + pCPA.getClass());
    }
    setParentCPA(getParentCPAClass().cast(pCPA));
  }

  public S getTop(CFANode pNode) throws InterruptedException {
    return getAbstractStateClass().cast(parentCPA.getInitialState(pNode, StateSpacePartition.getDefaultPartition()));
  }

  public AnalysisDirection getDirection() {
    return direction;
  }

  public T getParentCPA() {
    return parentCPA;
  }
}
