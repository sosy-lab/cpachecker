// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.ExpressionNode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.FormulaNode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.LiteralNode;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class InformationProvider {

  /**
   * Search for iteration variables and for suspicious
   * calculations within the array subscript.
   *
   * @param faults ranked faults
   * @param edges counterexample as list of edges
   */
  public static void searchForAdditionalInformation(Collection<Fault> faults, List<CFAEdge> edges) {
    // matches eg "x = x + 1", "test = test4    - 3" but not "test = 3 + test4"
    final Pattern matchIteration = Pattern.compile(".+=.+[+\\-/*][ 1-9]+[0-9]+");
    // matches eg "x = 4 + arr[c + 3]
    final Pattern matchArrayOperation = Pattern.compile(".*\\[.*[+\\-/*]+.*].*");

    // Find iteration variables
    Map<Object, Long> counts =
        edges.stream()
            .map(CFAEdge::getDescription)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    Set<String> iterationVariables = new HashSet<>();
    for (Entry<Object, Long> entry : counts.entrySet()) {
      if (entry.getValue() > 3) {
        String curr = (String) entry.getKey();
        Matcher itMatch = matchIteration.matcher(curr);
        if (itMatch.matches()) {
          List<String> parts = Splitter.on(" ").splitToList(curr);
          parts.removeIf(String::isBlank);
          if (parts.size() > 2) {
            if (parts.get(0).equals(parts.get(2))) {
              iterationVariables.add(parts.get(0));
            }
          }
        }
      }
    }

    for (Fault fault : faults) {
      for (FaultContribution faultContribution : fault) {
        String description = faultContribution.correspondingEdge().getDescription();
        boolean hasIter = false;
        for (String iterationVariable : iterationVariables) {
          if (description.contains(iterationVariable)) {
            hasIter = true;
            break;
          }
        }
        boolean hasCalc = matchArrayOperation.matcher(description).matches();
        if (hasIter && hasCalc) {
          fault.addInfo(
              FaultInfo.fix(
                  "Detected suspicious calculation within the array subscript using an iteration"
                      + " variable. Have a closer look to this line."));
          break;
        }
        if (hasIter) {
          fault.addInfo(
              FaultInfo.fix(
                  "This line uses an iteration variable. This may be especially prone to errors."));
          break;
        }
        if (hasCalc) {
          fault.addInfo(
              FaultInfo.fix(
                  "Detected suspicious calculation within the array subscript. This may be"
                      + " especially prone to errors"));
          break;
        }
      }
    }
  }

  public static String prettyPrecondition(BooleanFormula precondition, List<BooleanFormula> atoms) {
    FormulaNode root = BooleanFormulaParser.parse(precondition);
    List<FormulaNode> conjuncts = BooleanFormulaParser.toConjunctionArgs(root);

    Map<String, String> nondetToName = new HashMap<>();

    for (BooleanFormula atom : atoms) {
      FormulaNode atomRoot = BooleanFormulaParser.parse(atom);
      if (atomRoot.getType().equals(FormulaNode.FormulaNodeType.ExpressionNode)) {
        ExpressionNode exNode = (ExpressionNode) atomRoot;
        if (exNode.getOperands().size() == 2 &&
            exNode.getOperator().contains("=")) {
          FormulaNode left = exNode.getOperands().get(0);
          FormulaNode right = exNode.getOperands().get(1);
          if (left.toString().contains("__VERIFIER_nondet")) {
            nondetToName.put(left.toString(), right.toString());
          }
          if (right.toString().contains("__VERIFIER_nondet")) {
            nondetToName.put(right.toString(), left.toString());
          }
        }
      }
    }


    for (FormulaNode conjunct : conjuncts) {
      if (conjunct.getSuccessors().size() == 2 &&
          conjunct.getType().equals(FormulaNode.FormulaNodeType.ExpressionNode) &&
          conjunct.toString().contains("__VERIFIER_nondet") &&
          conjunct.getSuccessors().get(0).getType().equals(FormulaNode.FormulaNodeType.LiteralNode) &&
          conjunct.getSuccessors().get(1).getType().equals(FormulaNode.FormulaNodeType.LiteralNode)) {
          LiteralNode n1 = (LiteralNode) conjunct.getSuccessors().get(0);
          if (n1.toString().contains("__VERIFIER_nondet")) {
            conjunct.getSuccessors().remove(0);
            conjunct.getSuccessors().add(0, new LiteralNode(nondetToName.getOrDefault(n1.toString(), n1.toString())));
            continue;
          }
          LiteralNode n2 = (LiteralNode) conjunct.getSuccessors().get(1);
          if (n1.toString().contains("__VERIFIER_nondet")) {
            conjunct.getSuccessors().remove(1);
            conjunct.getSuccessors().add(new LiteralNode(nondetToName.getOrDefault(n2.toString(), n2.toString())));
          }
      }
    }

    return root.toString();
  }

  public static void addDefaultPotentialFixesToFaults(
      Collection<Fault> result, int maxNumberOfHints) {
    boolean maxNumberOfHintsNegative = maxNumberOfHints < 0;
    Set<FaultContribution> alreadyAttached = new HashSet<>();
    for (Fault faultLocalizationOutputs : result) {
      int hints = 0;
      for (FaultContribution faultContribution : faultLocalizationOutputs) {
        FaultInfo potFix = FaultInfo.possibleFixFor(faultContribution);
        if (maxNumberOfHintsNegative || hints < maxNumberOfHints) {
          faultLocalizationOutputs.addInfo(potFix);
        }
        // Prevent attaching the same hint twice
        if (!alreadyAttached.contains(faultContribution)) {
          faultContribution.addInfo(potFix);
          alreadyAttached.add(faultContribution);
        }
        hints++;
      }
    }
  }
}
