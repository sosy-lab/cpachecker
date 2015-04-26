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

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.ci.translators.AbstractRequirementsTranslator;

public class CustomInstructionRequirementsWriter {

  private final String filePrefix;
  private int fileID;
  private final Class<?> requirementsState;
  private AbstractRequirementsTranslator<AbstractState> abstractReqTranslator;

  public CustomInstructionRequirementsWriter(final String pFilePrefix, final Class<?> pClass,
      final AbstractRequirementsTranslator<AbstractState> pabstractReqTranslator) {
    filePrefix = pFilePrefix;
    fileID = 0;
    requirementsState = pClass;
    abstractReqTranslator = pabstractReqTranslator;
  }

  public void writeCIRequirement(final ARGState pState, final Collection<ARGState> pSet,
      final AppliedCustomInstruction pACI) throws IOException {

//    convertRequirements(abstractReqTranslator); TODO

//    Collection<String> set = removeDuplicates(pre, post, ci); TODO

    fileID++;
    try (Writer br = Files.openOutputFile(Paths.get(filePrefix+fileID+".smt"))) {
      // TODO custom instruction schreiben

      // TODO pre, post schreiben
    }
  }

  private Collection<String> removeDuplicates(List<String> pre, List<String> post, List<String> ci) {
    Set<String> duplicateFreeSet = new HashSet<>();
    duplicateFreeSet.addAll(pre);
    duplicateFreeSet.addAll(post);
    duplicateFreeSet.addAll(ci);
    return duplicateFreeSet; // TODO
  }

  private void createRequirementTranslator() throws CPAException {
//    if (requirementsState ... ?) {
//      abstractReqTranslator is PredicateRequirementsTranslator
//    } else {
      throw new CPAException("There is no suitable requirementTranslator available.");
//    }
  }
}
