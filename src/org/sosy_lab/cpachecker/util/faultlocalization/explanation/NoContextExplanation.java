// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.explanation;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultExplanation;

public class NoContextExplanation implements FaultExplanation {

  private static final NoContextExplanation instance = new NoContextExplanation();

  public static NoContextExplanation getInstance() {
    return instance;
  }

  private NoContextExplanation() {}

  /**
   * Make a suggestion for a bug-fix based on the EdgeType.
   *
   * @param subset set of FaultLocalizationOutputs.
   * @return explanation of what might be a fix
   * @see org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo#possibleFixFor(Fault)
   */
  @Override
  public String explanationFor(Fault subset) {
    return Joiner.on(" ").join(transformedImmutableSetCopy(subset, this::explain));
  }

  private String explain(FaultContribution faultContribution) {
    CFAEdge pEdge = faultContribution.correspondingEdge();
    String description = pEdge.getDescription();
    return switch (pEdge.getEdgeType()) {
      case AssumeEdge -> {
        String[] ops = {"<=", "!=", "==", ">=", "<", ">"};
        String op = "";
        for (String o : ops) {
          if (description.contains(o)) {
            op = o;
            break;
          }
        }
        yield "Try to replace \""
            + op
            + "\" in \""
            + description
            + "\" with another boolean operator (<, >, <=, !=, ==, >=).";
      }
      case StatementEdge ->
          "Try to change the assigned value of \""
              + Iterables.get(Splitter.on(" ").split(description), 0)
              + "\" in \""
              + description
              + "\" to another value.";
      case DeclarationEdge -> "Try to declare the variable in \"" + description + "\" differently.";
      case ReturnStatementEdge ->
          "Try to change the return-value of \"" + description + "\" to another value.";
      case FunctionCallEdge ->
          "The function call \""
              + description
              + "\" may have unwanted side effects or a wrong return value.";
      case FunctionReturnEdge -> {
        String functionName = ((CFunctionReturnEdge) pEdge).getFunctionEntry().getFunctionName();
        yield "The function " + functionName + "(...) may have an unwanted return value.";
      }
      case CallToReturnEdge, BlankEdge -> {
        yield "No proposal found for the statement: \"" + description + "\".";
      }
    };
  }
}
