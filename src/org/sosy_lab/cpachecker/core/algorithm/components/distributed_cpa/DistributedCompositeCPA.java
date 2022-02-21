// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.StatTimerSum.StatTimerType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload.PayloadBuilder;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedCompositeCPA extends AbstractDistributedCPA {

  private final Map<Class<? extends ConfigurableProgramAnalysis>, Class<? extends AbstractDistributedCPA>>
      lookup;

  private final Map<Class<? extends ConfigurableProgramAnalysis>, AbstractDistributedCPA>
      registered;

  private final StatTimer forwardProceed = new StatTimer("Time forward proceed");
  private final StatTimer backwardProceed = new StatTimer("Time backward proceed");
  private final StatTimer serializeTimer = new StatTimer("Time serialize");
  private final StatTimer deserializeTimer = new StatTimer("Time deserialize");
  private final StatTimer combineTimer = new StatTimer("Time deserialize");

  public DistributedCompositeCPA(
      String pId,
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection,
      AnalysisOptions pOptions)
      throws CPAException {
    super(pId, pNode, pTypeMap, pPrecision, pDirection, pOptions);
    lookup = new ConcurrentHashMap<>();
    lookup.put(PredicateCPA.class, DistributedPredicateCPA.class);
    lookup.put(CallstackCPA.class, DistributedCallstackCPA.class);
    lookup.put(FunctionPointerCPA.class, DistributedFunctionPointerCPA.class);
    registered = new ConcurrentHashMap<>();
  }

  @Override
  public AbstractState deserialize(Message pMessage)
      throws InterruptedException {
    deserializeTimer.start();
    CFANode location = block.getNodeWithNumber(pMessage.getTargetNodeNumber());
    CompositeCPA compositeCPA = (CompositeCPA) parentCPA;
    List<AbstractState> states = new ArrayList<>();
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      if (registered.containsKey(wrappedCPA.getClass())) {
        AbstractDistributedCPA entry = registered.get(wrappedCPA.getClass());
        states.add(entry.deserialize(pMessage));
      } else {
        states.add(
            wrappedCPA.getInitialState(location, StateSpacePartition.getDefaultPartition()));
      }
    }
    deserializeTimer.stop();
    return new CompositeState(states);
  }

  @Override
  public Payload serialize(AbstractState pState) {
    serializeTimer.start();
    CompositeState compositeState = (CompositeState) pState;
    PayloadBuilder payload = new PayloadBuilder();
    for (AbstractState wrappedState : compositeState.getWrappedStates()) {
      for (AbstractDistributedCPA value : registered.values()) {
        if (value.doesOperateOn(wrappedState.getClass())) {
          payload = payload.putAll(value.serialize(wrappedState));
          break;
        }
      }
    }
    serializeTimer.stop();
    return payload.build();
  }

  public void register(Class<? extends ConfigurableProgramAnalysis> clazz)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException,
             IllegalAccessException {
    if (registered.containsKey(clazz)) {
      return;
    }
    Class<? extends AbstractDistributedCPA> cpaClass = lookup.get(clazz);
    if (cpaClass == null) {
      return;
    }
    AbstractDistributedCPA cpa =
        cpaClass.getConstructor(String.class, BlockNode.class, UpdatedTypeMap.class,
                Precision.class,
                AnalysisDirection.class, AnalysisOptions.class)
            .newInstance(id, block, typeMap, precision, direction, analysisOptions);
    registered.put(clazz, cpa);
  }

  @Override
  public MessageProcessing proceedForward(Message newMessage)
      throws SolverException, InterruptedException {
    forwardProceed.start();
    MessageProcessing processing = MessageProcessing.proceed();
    for (AbstractDistributedCPA value : registered.values()) {
      processing = processing.merge(value.proceedForward(newMessage), true);
    }
    forwardProceed.stop();
    return processing;
  }

  @Override
  public MessageProcessing proceedBackward(Message newMessage)
      throws SolverException, InterruptedException {
    backwardProceed.start();
    MessageProcessing processing = MessageProcessing.proceed();
    for (AbstractDistributedCPA value : registered.values()) {
      processing = processing.merge(value.proceedBackward(newMessage), true);
    }
    backwardProceed.stop();
    return processing;
  }

  public <T extends ConfigurableProgramAnalysis> T getOriginalCPA(Class<T> pCPA) {
    return pCPA.cast(registered.get(pCPA).getParentCPA());
  }

  public <T extends ConfigurableProgramAnalysis> AbstractDistributedCPA getDistributedAnalysis(Class<T> pCPA) {
    return registered.get(pCPA);
  }

  @Override
  public void setParentCPA(ConfigurableProgramAnalysis cpa) throws CPAException {
    super.setParentCPA(cpa);
    CompositeCPA compositeCPA = (CompositeCPA) cpa;
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      try {
        register(wrappedCPA.getClass());
      } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException pE) {
        throw new CPAException("Could not create distributed CPA for " + wrappedCPA);
      }
    }
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      AbstractDistributedCPA analysis = registered.get(wrappedCPA.getClass());
      if (analysis == null) {
        continue;
      }
      analysis.setParentCPA(wrappedCPA);
    }
  }

  @Override
  public AbstractState combine(AbstractState pState1, AbstractState pState2)
      throws InterruptedException, CPAException {
    combineTimer.start();
    CompositeState state1 = (CompositeState) pState1;
    CompositeState state2 = (CompositeState) pState2;

    if (state1.getWrappedStates().size() != state2.getWrappedStates().size()) {
      combineTimer.stop();
      throw new AssertionError("CompositeStates have to have the same size");
    }
    List<AbstractState> combined = new ArrayList<>();
    for (int i = 0; i < state1.getWrappedStates().size(); i++) {
      boolean found = false;
      AbstractState state1I = state1.get(i);
      AbstractState state2I = state2.get(i);
      for (AbstractDistributedCPA value : registered.values()) {
        if (value.doesOperateOn(state1I.getClass()) && value.doesOperateOn(state2I.getClass())) {
          combined.add(value.combine(state1I, state2I));
          found = true;
          break;
        }
      }
      // merge sep
      if (!found) {
        combined.add(state2I);
      }
    }
    combineTimer.stop();
    return new CompositeState(combined);
  }

  @Override
  public void synchronizeKnowledge(AbstractDistributedCPA pDCPA) {
    super.synchronizeKnowledge(pDCPA);
    DistributedCompositeCPA distributed = (DistributedCompositeCPA) pDCPA;
    for (Entry<Class<? extends ConfigurableProgramAnalysis>, AbstractDistributedCPA> entry : registered.entrySet()) {
      if (distributed.registered.containsKey(entry.getKey())) {
        entry.getValue().synchronizeKnowledge(distributed.registered.get(entry.getKey()));
      }
    }
  }

  @Override
  public void setFirstMessage(Message pFirstMessage) {
    registered.values().forEach(cpa -> cpa.setFirstMessage(pFirstMessage));
  }

  @Override
  public boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return pClass.equals(CompositeState.class);
  }

  @Override
  public void setLatestOwnPostConditionMessage(Message m) {
    super.setLatestOwnPostConditionMessage(m);
    registered.values().forEach(dcpa -> dcpa.setLatestOwnPostConditionMessage(m));
  }

  public void registerTimer(StatTimerSum pStatTimerSum, StatTimerType pType) {
    switch (pType) {
      case COMBINE:
        pStatTimerSum.register(combineTimer);
        break;
      case SERIALIZE:
        pStatTimerSum.register(serializeTimer);
        break;
      case DESERIALIZE:
        pStatTimerSum.register(deserializeTimer);
        break;
      case PROCEED_F:
        pStatTimerSum.register(forwardProceed);
        break;
      case PROCEED_B:
        pStatTimerSum.register(backwardProceed);
        break;
      default:
        throw new AssertionError("Unknown type: " + pType);
    }
  }

}
