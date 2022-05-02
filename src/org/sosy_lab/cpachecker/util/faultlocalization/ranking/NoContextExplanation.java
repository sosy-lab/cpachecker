// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import static com.google.common.base.Preconditions.checkArgument;

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
   * This method relies on singleton sets otherwise an error is thrown. Make a suggestion for a bug
   * fix based on the EdgeType.
   *
   * @param subset set of FaultLocalizationOutputs.
   * @return explanation of what might be a fix
   * @see
   *     org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo#possibleFixFor(FaultContribution)
   */
  @Override
  public String explanationFor(Fault subset) {
    checkArgument(subset.size() == 1, "NoContextExplanation requires exactly one edge in Fault");
    FaultContribution faultContribution = subset.iterator().next();
    CFAEdge pEdge = faultContribution.correspondingEdge();
    String description = pEdge.getDescription();
    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        {
          String[] ops = {"<=", "!=", "==", ">=", "<", ">"};
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
              + "\" with another boolean operator (<, >, <=, !=, ==, >=). This line may be prone to"
              + " off-by-one errors!";
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
          return "The function call \""
              + description
              + "\" may have unwanted side effects or a wrong return value.";
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
