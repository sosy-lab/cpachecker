/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;

public class ExtendedWitnessFactory extends WitnessFactory {
  ExtendedWitnessFactory(
      WitnessOptions pOptions,
      CFA pCfa,
      VerificationTaskMetaData pMetaData,
      ExpressionTreeFactory<Object> pFactory,
      Simplifier<Object> pSimplifier,
      @Nullable String pDefaultSourceFileName,
      WitnessType pGraphType,
      InvariantProvider pInvariantProvider) {
    super(
        pOptions,
        pCfa,
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
}
