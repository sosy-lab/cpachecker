/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.antlr.v4.runtime.misc.MultiMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class InformationProvider {

  /**
   * First raw implementation of additional information search.
   * Search for iteration variables and for calculations in array brackets.
   * @param faults ranked faults
   * @param edges counterexample as list of edges
   */
  public static void searchForAdditionalInformation(Collection<Fault> faults, List<CFAEdge> edges){
    Set<String> iterationVariables = new HashSet<>();
    Set<CFAEdge> arrayEdges = new HashSet<>();
    Set<String> operators = ImmutableSet.of("+", "-", "*", "/", "%");
    Set<String> iterationPatterns = ImmutableSet.of("++", "--", "/", "%");

    MultiMap<String, CFAEdge> descToEdges = new MultiMap<>();
    edges.forEach(e -> descToEdges.map(e.getRawStatement(), e));

    for (Entry<String, List<CFAEdge>> entry : descToEdges.entrySet()) {
      String description = entry.getKey();
      for (CFAEdge cfaEdge : entry.getValue()) {
        if (cfaEdge.getEdgeType().equals(CFAEdgeType.StatementEdge) && description.contains("[")) {
          String arrayContent = Splitter.on("[").splitToList(description).get(1);
          for (String operator : operators) {
            if (arrayContent.contains(operator)) {
              arrayEdges.add(cfaEdge);
            }
          }
        }
      }
      // CPAchecker transforms the description of edges of source code like i++ to i = i + 1
      if (description.contains("=")) {
        List<String> operands = Splitter.on("=").splitToList(description);
        if (operands.size() == 2) {
          if (entry.getValue().size() > 2 && operands.get(1).contains(operands.get(0) + " + 1")) {
            iterationVariables.add(operands.get(0).trim());
          }
          if (entry.getValue().size() > 2 && operands.get(1).contains(operands.get(0) + " - 1")) {
            iterationVariables.add(operands.get(0).trim());
          }
        }
      } else {
        iterationPatterns.forEach(p -> {
          if (description.startsWith(p) || description.endsWith(p)) {
            iterationVariables.add(description.replace(p, ""));
          }
        });
      }
    }

    for (Fault fault : faults) {
      for (FaultContribution faultContribution : fault) {
        boolean containsIterVar = false;
        for (String iterationVariable : iterationVariables) {
          if (faultContribution.correspondingEdge().getRawStatement().contains("::"+iterationVariable)) {
            containsIterVar = true;
            break;
          }
        }
        boolean isArrayVar = arrayEdges.contains(faultContribution.correspondingEdge());
        if (isArrayVar && containsIterVar) {
          fault.addInfo(FaultInfo.fix("Detected a suspicious operation within the array brackets using an iteration variable. Perhaps the iteration variable is initialized or used incorrectly."));
          break;
        }
        if (isArrayVar) {
          fault.addInfo(FaultInfo.fix("Detected a suspicious operation within the array brackets. Perhaps adapting the calculation fixes the bug."));
        }
        if (containsIterVar) {
          fault.addInfo(FaultInfo.hint("This edge uses an iteration variable. Check the initialization or the usage of it within the iteration-body."));
        }
      }
    }
  }




}
