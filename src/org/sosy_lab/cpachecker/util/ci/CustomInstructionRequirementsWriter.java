/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import com.google.common.collect.Sets;

import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ci.translators.AbstractRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.IntervalRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.PredicateRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.SignRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.ValueRequirementsTranslator;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CustomInstructionRequirementsWriter {

  private final Class<?> requirementsState;
  private AbstractRequirementsTranslator<? extends AbstractState> abstractReqTranslator;
  private final LogManager logger;
  private final boolean enableRequirementSlicing;
  private final PathCounterTemplate fileTemplate;

  public CustomInstructionRequirementsWriter(final PathCounterTemplate ciReqFiles,
      final Class<?> reqirementsState, final LogManager log, final ConfigurableProgramAnalysis cpa,
      boolean enableRequirementSlicing) throws CPAException {
    fileTemplate = ciReqFiles;
    this.requirementsState = reqirementsState;
    logger = log;
    this.enableRequirementSlicing = enableRequirementSlicing;
    createRequirementTranslator(cpa);
  }

  public void writeCIRequirement(final ARGState pState, final Collection<ARGState> pSet,
      final AppliedCustomInstruction pACI) throws IOException, CPAException {
    Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertedRequirements;
    if (enableRequirementSlicing) {
      convertedRequirements = abstractReqTranslator.convertRequirements(pState, pSet, pACI.getIndicesForReturnVars(), pACI.getInputVariables(), pACI.getOutputVariables());
    } else {
      convertedRequirements = abstractReqTranslator.convertRequirements(pState, pSet, pACI.getIndicesForReturnVars(), null, null);
    }
    if(convertedRequirements.getSecond().getSecond().matches("\\(define-fun post \\(\\) Bool(\\s)+true\\)")) {
      // post condition true, do not need to consider this requirement
      return;
    }

    Pair<List<String>, String> fakeSMTDesc = pACI.getFakeSMTDescription();
    List<String> set = removeDuplicates(convertedRequirements.getFirst().getFirst(), convertedRequirements.getSecond().getFirst(), fakeSMTDesc.getFirst());

    try (Writer br =
        MoreFiles.openOutputFile(fileTemplate.getFreshPath(), Charset.defaultCharset())) {
      for (String element : set) {
        br.write(element);
        br.write("\n");
      }

      br.write("\n;Custom Instruction\n");
      br.write(pACI.getFakeSMTDescription().getSecond());

      br.write("\n\n;Pre and Post Conditions\n");
        br.write(convertedRequirements.getFirst().getSecond());
        br.write("\n");
        br.write(convertedRequirements.getSecond().getSecond());
        br.write("\n");
    }
  }

  private List<String> removeDuplicates(final List<String> pre, final List<String> post,
      final List<String> ci) {
    int sumSize =pre.size()+post.size()+ci.size();
    List<String> duplicateFreeSet = new ArrayList<>(sumSize);
    Set<String> set = Sets.newHashSetWithExpectedSize(sumSize);

    addNonMembersToList(pre, duplicateFreeSet, set);
    addNonMembersToList(post, duplicateFreeSet, set);
    addNonMembersToList(ci, duplicateFreeSet, set);
    return duplicateFreeSet;
  }

  private void addNonMembersToList(final List<String> candidates, final List<String> list, final Set<String> listElems) {
    for (String next : candidates) {
      if (listElems.add(next)) {
        list.add(next);
      }
    }
  }

  private void createRequirementTranslator(final ConfigurableProgramAnalysis cpa) throws CPAException {
    if (requirementsState.equals(SignState.class)) {
      abstractReqTranslator = new SignRequirementsTranslator(logger);
    } else if (requirementsState.equals(ValueAnalysisState.class)) {
      abstractReqTranslator = new ValueRequirementsTranslator(logger);
    } else if (requirementsState.equals(IntervalAnalysisState.class)) {
      abstractReqTranslator = new IntervalRequirementsTranslator(logger);
    } else if (requirementsState.equals(PredicateAbstractState.class)) {
      PredicateCPA pCpa = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      if (pCpa == null) {
        pCpa = CPAs.retrieveCPA(cpa, BAMPredicateCPA.class);
      }
      if (pCpa == null) { throw new CPAException(
          "Cannot extract analysis which was responsible for construction PredicateAbstract States"); }
      abstractReqTranslator = new PredicateRequirementsTranslator(pCpa);
    } else {
      throw new CPAException("There is no suitable requirementTranslator available.");
    }
  }
}
