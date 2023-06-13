// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
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
import org.sosy_lab.cpachecker.util.ci.translators.ApronRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.IntervalRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.PredicateRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.SignRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.ValueRequirementsTranslator;

public class CustomInstructionRequirementsWriter {

  private final Class<?> requirementsState;
  private AbstractRequirementsTranslator<? extends AbstractState> abstractReqTranslator;
  private final LogManager logger;
  private final boolean enableRequirementSlicing;
  private final PathCounterTemplate fileTemplate;

  public CustomInstructionRequirementsWriter(
      final PathCounterTemplate ciReqFiles,
      final Class<?> reqirementsState,
      final LogManager log,
      final ConfigurableProgramAnalysis cpa,
      boolean enableRequirementSlicing)
      throws CPAException {
    fileTemplate = ciReqFiles;
    requirementsState = reqirementsState;
    logger = log;
    this.enableRequirementSlicing = enableRequirementSlicing;
    createRequirementTranslator(cpa);
  }

  public void writeCIRequirement(
      final ARGState pState, final Collection<ARGState> pSet, final AppliedCustomInstruction pACI)
      throws IOException, CPAException {
    Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertedRequirements;
    if (enableRequirementSlicing) {
      convertedRequirements =
          abstractReqTranslator.convertRequirements(
              pState,
              pSet,
              pACI.getIndicesForReturnVars(),
              pACI.getInputVariables(),
              pACI.getOutputVariables());
    } else {
      convertedRequirements =
          abstractReqTranslator.convertRequirements(
              pState, pSet, pACI.getIndicesForReturnVars(), null, null);
    }
    if (convertedRequirements
        .getSecond()
        .getSecond()
        .matches("\\(define-fun post \\(\\) Bool(\\s)+true\\)")) {
      // post condition true, do not need to consider this requirement
      return;
    }

    Pair<List<String>, String> fakeSMTDesc = pACI.getFakeSMTDescription();
    List<String> set =
        removeDuplicates(
            convertedRequirements.getFirst().getFirst(),
            convertedRequirements.getSecond().getFirst(),
            fakeSMTDesc.getFirst());

    try (Writer br = IO.openOutputFile(fileTemplate.getFreshPath(), Charset.defaultCharset())) {
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

  private List<String> removeDuplicates(
      final List<String> pre, final List<String> post, final List<String> ci) {
    int sumSize = pre.size() + post.size() + ci.size();
    List<String> duplicateFreeSet = new ArrayList<>(sumSize);
    Set<String> set = Sets.newHashSetWithExpectedSize(sumSize);

    addNonMembersToList(pre, duplicateFreeSet, set);
    addNonMembersToList(post, duplicateFreeSet, set);
    addNonMembersToList(ci, duplicateFreeSet, set);
    return duplicateFreeSet;
  }

  private void addNonMembersToList(
      final List<String> candidates, final List<String> list, final Set<String> listElems) {
    for (String next : candidates) {
      if (listElems.add(next)) {
        list.add(next);
      }
    }
  }

  private void createRequirementTranslator(final ConfigurableProgramAnalysis cpa)
      throws CPAException {
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
      if (pCpa == null) {
        throw new CPAException(
            "Cannot extract analysis which was responsible for construction PredicateAbstract"
                + " States");
      }
      abstractReqTranslator =
          new PredicateRequirementsTranslator(pCpa.getSolver().getFormulaManager());
    } else if (requirementsState.equals(ApronState.class)) {
      abstractReqTranslator = new ApronRequirementsTranslator(ApronState.class, logger);
    } else {
      throw new CPAException("There is no suitable requirementTranslator available.");
    }
  }
}
