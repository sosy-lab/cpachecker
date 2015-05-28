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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.ci.translators.AbstractRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.IntervalRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.PredicateRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.SignRequirementsTranslator;
import org.sosy_lab.cpachecker.util.ci.translators.ValueRequirementsTranslator;

public class CustomInstructionRequirementsWriter {

  private final String filePrefix;
  private int fileID;
  private final Class<?> requirementsState;
  private AbstractRequirementsTranslator<? extends AbstractState> abstractReqTranslator;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  public CustomInstructionRequirementsWriter(final String pFilePrefix, final Class<?> reqirementsState,
      final Configuration config, final ShutdownNotifier shutdownNotifier, final LogManager log)
      throws CPAException{
    filePrefix = pFilePrefix;
    fileID = 0;
    this.requirementsState = reqirementsState;
    this.config = config;
    this.shutdownNotifier = shutdownNotifier;
    logger = log;
    createRequirementTranslator();
  }

  public void writeCIRequirement(final ARGState pState, final Collection<ARGState> pSet,
      final AppliedCustomInstruction pACI) throws IOException, CPAException {
    // TODO Tanja: delete index from convertRequirements interface
    Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertedRequirements
      = abstractReqTranslator.convertRequirements(pState, pSet, pACI.getIndicesForReturnVars(), 0);

    Pair<List<String>, String> fakeSMTDesc = pACI.getFakeSMTDescription();
    Collection<String> set = removeDuplicates(convertedRequirements.getFirst().getFirst(), convertedRequirements.getSecond().getFirst(), fakeSMTDesc.getFirst());
    fileID++;

    try (Writer br = Files.openOutputFile(Paths.get(filePrefix+fileID+".smt"))) {
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

  private Collection<String> removeDuplicates(final List<String> pre, final List<String> post,
      final List<String> ci) {
    Set<String> duplicateFreeSet = new HashSet<>();
    duplicateFreeSet.addAll(pre);
    duplicateFreeSet.addAll(post);
    duplicateFreeSet.addAll(ci);
    return duplicateFreeSet;
  }

  private void createRequirementTranslator() throws CPAException {
    if (requirementsState.equals(SignState.class)) {
      abstractReqTranslator = new SignRequirementsTranslator(config, shutdownNotifier, logger);
    } else if (requirementsState.equals(ValueAnalysisState.class)) {
      abstractReqTranslator = new ValueRequirementsTranslator(config, shutdownNotifier, logger);
    } else if (requirementsState.equals(IntervalAnalysisState.class)) {
      abstractReqTranslator = new IntervalRequirementsTranslator(config, shutdownNotifier, logger);
    } else if (requirementsState.equals(PredicateAbstractState.class)) {
      abstractReqTranslator = new PredicateRequirementsTranslator();
    } else {
      throw new CPAException("There is no suitable requirementTranslator available.");
    }
  }
}
