// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;

public class ExtendedWitnessFactory extends WitnessFactory {
  ExtendedWitnessFactory(
      WitnessOptions pOptions,
      CFA pCfa,
      LogManager pLogger,
      VerificationTaskMetaData pMetaData,
      ExpressionTreeFactory<Object> pFactory,
      Simplifier<Object> pSimplifier,
      @Nullable String pDefaultSourceFileName,
      WitnessType pGraphType,
      InvariantProvider pInvariantProvider) {
    super(
        pOptions,
        pCfa,
        pLogger,
        pMetaData,
        pFactory,
        pSimplifier,
        pDefaultSourceFileName,
        pGraphType,
        pInvariantProvider);
  }

  @Override
  protected Map<ARGState, CFAEdgeWithAdditionalInfo> getAdditionalInfo(
      Optional<CounterexampleInfo> pCounterExample) {
    if (pCounterExample.isPresent()) {
      return pCounterExample.orElseThrow().getAdditionalInfoMapping();
    }
    return ImmutableMap.of();
  }

  @Override
  public Set<AdditionalInfoConverter> getAdditionalInfoConverters(
      Optional<CounterexampleInfo> pCounterExample) {
    if (pCounterExample.isPresent()) {
      return pCounterExample.orElseThrow().getAdditionalInfoConverters();
    }
    return ImmutableSet.of();
  }

  @Override
  protected TransitionCondition addAdditionalInfo(
      TransitionCondition pCondition, CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    TransitionCondition result = pCondition;
    if (pAdditionalInfo != null) {
      for (Entry<ConvertingTags, Object> addInfo : pAdditionalInfo.getInfos()) {
        ConvertingTags tag = addInfo.getKey();
        Object value = addInfo.getValue();
        for (AdditionalInfoConverter converter : additionalInfoConverters) {
          result = converter.convert(result, tag, value);
        }
      }
    }
    return result;
  }

  @Override
  protected boolean handleAsEpsilonEdge(CFAEdge pEdge, CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    return AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge, pAdditionalInfo);
  }

  @Override
  protected boolean isEmptyTransitionPossible(CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    return pAdditionalInfo == null || pAdditionalInfo.getInfos().isEmpty();
  }

  @Override
  protected Collection<TransitionCondition> extractTransitionForStates(
      String pFrom,
      String pTo,
      CFAEdge pEdge,
      Collection<ARGState> pFromStates,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      CFAEdgeWithAdditionalInfo pAdditionalInfo,
      TransitionCondition pTransitionCondition,
      boolean pGoesToSink,
      boolean pIsDefaultCase) {
    TransitionCondition result = pTransitionCondition;
    if (pEdge instanceof CDeclarationEdge) {
      CDeclarationEdge declEdge = (CDeclarationEdge) pEdge;
      CDeclaration decl = declEdge.getDeclaration();
      if (decl instanceof CVariableDeclaration || decl instanceof CFunctionDeclaration) {
        result = result.putAndCopy(KeyDef.DECL, "true");
      }
    }
    return super.extractTransitionForStates(
        pFrom,
        pTo,
        pEdge,
        pFromStates,
        pValueMap,
        pAdditionalInfo,
        result,
        pGoesToSink,
        pIsDefaultCase);
  }
}
