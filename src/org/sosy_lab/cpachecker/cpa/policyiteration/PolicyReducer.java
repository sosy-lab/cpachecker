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
package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.templates.Template;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * BAM reduction for LPI.
 */
public class PolicyReducer implements Reducer {

  private final LogManager logger;

  public PolicyReducer(LogManager pLogger) {
    logger = pLogger;
  }

  /**
   * Remove all information from the {@code expandedState} which is not
   * relevant to {@code context}.
   */
  @Override
  public PolicyAbstractedState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) {
    PolicyState pState = (PolicyState) expandedState;
    Preconditions.checkState(pState.isAbstract());

    PolicyAbstractedState aState = pState.asAbstracted();
    Set<String> blockVars = getBlockVariables(context);

    Map<Template, PolicyBound> newAbstraction = Maps.filterKeys(
        aState.getAbstraction(),
        template -> blockVars.containsAll(
            template.getUsedVars().collect(Collectors.toSet()))
    );
    return aState.withNewAbstractionAndSSA(
        newAbstraction,
        SSAMap.emptySSAMap().withDefault(1)
    );
  }

  @Override
  public PolicyState getVariableExpandedState(
      AbstractState entryState,
      Block reducedContext,
      AbstractState returnState) {
    PolicyState pEntryState = (PolicyState) entryState;
    PolicyState pReturnState = (PolicyState) returnState;

    if (!pReturnState.isAbstract()) {
      // BAM-specific hack: intermediate states come from target states,
      // but this information can not be expressed by having only a PolicyState.
      return pReturnState;
    }
    Preconditions.checkState(pEntryState.isAbstract());
    Preconditions.checkState(pReturnState.isAbstract());

    PolicyAbstractedState aEntryState = pEntryState.asAbstracted();
    PolicyAbstractedState aReturnState = pReturnState.asAbstracted();

    // Enrich the {@code pReturnState} with bounds obtained from {@code
    // pEntryState} which were dropped during the reduction.
    Map<Template, PolicyBound> rootAbstraction =
        aEntryState.getAbstraction();
    Map<Template, PolicyBound> fullAbstraction =
        new HashMap<>(aReturnState.getAbstraction());
    for (Entry<Template, PolicyBound> e : rootAbstraction.entrySet()) {
      Template t = e.getKey();

      if (staysInvariantUnderBlock(t, aReturnState)) {
        fullAbstraction.put(t, e.getValue());
      } else {
        logger.log(Level.INFO, "Not inserting the bound for the template",
            t, "which is", e.getValue().getBound());
      }
    }

    return pReturnState.asAbstracted().withNewAbstraction(fullAbstraction);
  }

  private boolean staysInvariantUnderBlock(
      Template t,
      PolicyAbstractedState returnState) {

    // todo: unsound handling for pointed-to variables.
    return t.getUsedVars().allMatch(v ->
        !returnState.getBound(t).isPresent()
        && !(returnState.getSSA().getIndex(v) > 1));
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision precision, Block context) {
    // Currently, precision is a singleton.
    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    // Currently, precision is a singleton.
    return rootPrecision;
  }

  @Override
  public Object getHashCodeForState(
      AbstractState stateKey, Precision precisionKey) {
    PolicyState pState = (PolicyState) stateKey;

    // Discard all the meta-information attached to the bounds.
    return pState.asAbstracted().getAbstraction().entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey(),
            e -> e.getValue().getBound()
        ));
  }

  /**
   * Take root state,
   * remove all bounds associated with global variables,
   * add all globals from the expandedState,
   * add assignment to return function value from expandedState.
   *
   * TODO: this function was not tested yet.
   */
  @Override
  public PolicyAbstractedState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    PolicyState pRootState = (PolicyState) rootState;
    PolicyState pExpandedState = (PolicyState) expandedState;
    PolicyState pEntryState = (PolicyState) entryState;
    Preconditions.checkState(pRootState.isAbstract());
    Preconditions.checkState(pEntryState.isAbstract());
    Preconditions.checkState(pExpandedState.isAbstract());

    // Remove all global values from root state.
    Map<Template, PolicyBound> rootAbstraction
        = new HashMap<>(pRootState.asAbstracted().getAbstraction());
    Map<Template, PolicyBound> expandedAbstraction
        = new HashMap<>(pExpandedState.asAbstracted().getAbstraction());
    Map<Template, PolicyBound> noGlobals = Maps.filterKeys(
        rootAbstraction,
        t -> !t.hasGlobals()
    );

    // Re-add globals from expanded state.
    noGlobals.putAll(
      Maps.filterKeys(
          expandedAbstraction,
          t -> !t.getUsedVars()
              .filter(s -> !s.contains("::")).findAny().isPresent()
      ));

    Optional<String> retName = exitLocation.getEntryNode().getReturnVariable()
        .flatMap(t -> Optional.of(t.getQualifiedName()));

    Map<Template, PolicyBound> out;
    if (retName.isPresent()) {
      String retVarName = retName.get();

      // Drop all templates which contain the return variable
      // name.
      // TODO: probably need to call simplex at this point to figure out the
      // new bounds.
      Map<Template, PolicyBound> noRetVar = Maps.filterKeys(
          noGlobals,
          t -> t.getUsedVars()
                .filter(v -> v.equals(retVarName)).findAny().isPresent()
      );

      // Re-add the template length 1 from {@code expandedState} if exists.
      expandedAbstraction.keySet().stream().filter(
          t -> t.getLinearExpression().size() == 1
            && t.getUsedVars().filter(v -> v.equals(retVarName)).findAny().isPresent()
      ).forEach(
          t -> noRetVar.put(t, expandedAbstraction.get(t))
      );
      out = noRetVar;
    } else {
      out = noGlobals;
    }
    return pExpandedState.asAbstracted().withNewAbstraction(out);

  }

  private Set<String> getBlockVariables(Block pBlock) {
    return pBlock.getReferencedVariables().stream()
        .map(ReferencedVariable::getName).collect(Collectors.toSet());
  }
}
