// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedCPA extends AbstractDistributedCPA<CompositeCPA, CompositeState> {

  private final Map<Class<? extends ConfigurableProgramAnalysis>, Class<? extends AbstractDistributedCPA<?, ?>>>
      lookup;

  private final Map<Class<? extends AbstractState>, Class<? extends ConfigurableProgramAnalysis>> lookupState;

  private final Map<Class<? extends ConfigurableProgramAnalysis>, AbstractDistributedCPA<? extends ConfigurableProgramAnalysis, ? extends AbstractState>> registered;

  public DistributedCPA(
      String pId,
      BlockNode pNode, SSAMap pTypeMap, Precision pPrecision, AnalysisDirection pDirection) throws CPAException {
    super(pId, pNode, pTypeMap, pPrecision, pDirection);
    lookup = new ConcurrentHashMap<>();
    lookup.put(PredicateCPA.class, DistributedPredicateCPA.class);
    registered = new ConcurrentHashMap<>();
    lookupState = new ConcurrentHashMap<>();
  }

  public void register(Class<? extends ConfigurableProgramAnalysis> clazz)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException,
             IllegalAccessException {
    if (registered.containsKey(clazz)) {
      return;
    }
    Class<? extends AbstractDistributedCPA<?, ?>> cpaClass = lookup.get(clazz);
    if (cpaClass == null) {
      return;
    }
    AbstractDistributedCPA<?, ?> cpa = cpaClass.getConstructor(String.class, BlockNode.class, SSAMap.class, Precision.class, AnalysisDirection.class).newInstance(id, block, typeMap, precision, direction);
    registered.put(clazz, cpa);
    lookupState.put(cpa.getAbstractStateClass(), cpa.getParentCPAClass());
  }

  @Override
  public Payload encode(Collection<CompositeState> statesAtBlockEntry) {
    Multimap<Class<? extends ConfigurableProgramAnalysis>, AbstractState> states = ArrayListMultimap.create();
    for (CompositeState compositeState : statesAtBlockEntry) {
      for (AbstractState state : compositeState.getWrappedStates()) {
        Class<?> superClasses = state.getClass();
        while (superClasses != null) {
          if (lookupState.containsKey(superClasses)) {
            if (registered.containsKey(lookupState.get(superClasses))) {
              states.put(lookupState.get(superClasses), state);
              break;
            }
          }
          superClasses = superClasses.getSuperclass();
        }
      }
    }
    Payload result = Payload.empty();
    for (Class<? extends ConfigurableProgramAnalysis> aClass : states.keySet()) {
      result.putAll(registered.get(aClass).safeEncode(states.get(aClass)));
    }
    return result;
  }

  @Override
  public CompositeState decode(
      Collection<Payload> messages, CompositeState previousAbstractState) {
    List<AbstractState> states = new ArrayList<>();
    boolean found = false;
    for (AbstractState state : previousAbstractState.getWrappedStates()) {
      Class<?> superClasses = state.getClass();
      while (superClasses != null) {
        if (lookupState.containsKey(superClasses)) {
          if (registered.containsKey(lookupState.get(superClasses))) {
            states.add(registered.get(lookupState.get(superClasses)).safeDecode(messages, state));
            found = true;
            break;
          }
        }
        superClasses = superClasses.getSuperclass();
      }
      if (!found) {
        states.add(state);
      }
      found = false;
    }
    return new CompositeState(states);
  }

  @Override
  public MessageProcessing stopForward(Message newMessage) {
    MessageProcessing processing = MessageProcessing.proceed();
    for (AbstractDistributedCPA<? extends ConfigurableProgramAnalysis, ? extends AbstractState> value : registered.values()) {
      processing = processing.merge(value.stopForward(newMessage), true);
    }
    return processing;
  }

  @Override
  public MessageProcessing stopBackward(Message newMessage) throws SolverException, InterruptedException {
    MessageProcessing processing = MessageProcessing.proceed();
    for (AbstractDistributedCPA<? extends ConfigurableProgramAnalysis, ? extends AbstractState> value : registered.values()) {
      processing = processing.merge(value.stopBackward(newMessage), true);
    }
    return processing;
  }

  public <T extends ConfigurableProgramAnalysis> T getOriginalCPA(Class<T> pCPA) {
    return pCPA.cast(registered.get(pCPA).getParentCPA());
  }

  public <T extends ConfigurableProgramAnalysis> AbstractDistributedCPA<?, ?> getDistributedAnalysis(Class<T> pCPA) {
    return registered.get(pCPA);
  }

  @Override
  public void setParentCPA(CompositeCPA cpa) throws CPAException{
    super.setParentCPA(cpa);
    for (ConfigurableProgramAnalysis wrappedCPA : cpa.getWrappedCPAs()) {
      try {
        register(wrappedCPA.getClass());
      } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException pE) {
        throw new CPAException("Could not create distributed CPA for " + wrappedCPA);
      }
    }
    for (ConfigurableProgramAnalysis wrappedCPA : cpa.getWrappedCPAs()) {
      AbstractDistributedCPA<?, ?> analysis = registered.get(wrappedCPA.getClass());
      if (analysis == null) {
        continue;
      }
      analysis.safeSetParentCPA(wrappedCPA);
    }
  }

  @Override
  public CompositeState getTop(CFANode pNode) throws InterruptedException {
    List<AbstractState> states = new ArrayList<>();
    for (AbstractDistributedCPA<? extends ConfigurableProgramAnalysis, ? extends AbstractState> value : registered.values()) {
      states.add(value.getTop(pNode));
    }
    return new CompositeState(states);
  }

  @Override
  public void setFirstMessage(Message pFirstMessage) {
    registered.values().forEach(cpa -> cpa.setFirstMessage(pFirstMessage));
  }

  @Override
  public Class<CompositeCPA> getParentCPAClass() {
    return CompositeCPA.class;
  }

  @Override
  public Class<CompositeState> getAbstractStateClass() {
    return CompositeState.class;
  }
}
