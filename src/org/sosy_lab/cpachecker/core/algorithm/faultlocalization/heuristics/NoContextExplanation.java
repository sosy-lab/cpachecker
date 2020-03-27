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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationExplanation;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;

public class NoContextExplanation implements FaultLocalizationExplanation {

  /**
   * possible implementation of a function that maps a FaultLocalizationOutput object to a
   * description (as string) this function relies on singleton sets otherwise an error is thrown.
   * based on the edge type a suggestion for fixing the bug is made. A sample usage can be found
   * here: FaultLocalizationHeuristicsImpl.rankByCountingSubsetOccurrences
   *
   * @param subset set of FaultLocalizationOutputs.
   * @return explanation of what might be a fix
   */
  @Override
  public String explanationFor(Set<? extends FaultLocalizationOutput> subset) {
    if (subset.size() != 1) {
      throw new IllegalArgumentException("reason without context requires exactly one edge");
    }
    FaultLocalizationOutput object = new ArrayList<>(subset).get(0);
    CFAEdge pEdge = object.correspondingEdge();
    String description = pEdge.getDescription();
    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
      {
        String[] ops = {"<", ">", "<=", "!=", "==", ">="};
        String op = "";
        for (String o : ops) {
          if (description.contains(o)) {
            op = o;
            break;
          }
        }
        return "Try to replace \""
            + op
            + "\" in \""
            + description
            + "\" with another boolean operator (<, >, <=, !=, ==, >=).";
      }
      case StatementEdge:
      {
        return "Try to change the assigned value of \""
            + Iterables.get(Splitter.on(" ").split(description), 0)
            + "\" in \""
            + description
            + "\" to another value.";
      }
      case DeclarationEdge:
      {
        return "Try to declare the variable in \"" + description + "\" differently.";
      }
      case ReturnStatementEdge:
      {
        return "Try to change the return-value of \"" + description + "\" to another value.";
      }
      case FunctionCallEdge:
      {
        return "The function call \"" + description + "\" may have unwanted side effects.";
      }
      case FunctionReturnEdge:
      {
        String functionName = ((CFunctionReturnEdge) pEdge).getFunctionEntry().getFunctionName();
        return "The function " + functionName + "(...) may have an unwanted return value.";
      }
      case CallToReturnEdge:
      case BlankEdge:
      default:
        return "No proposal found for the statement: \"" + description + "\".";
    }
  }
}
