// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings;

import com.google.common.base.Splitter;
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class InformationProvider {

  /**
   * First raw implementation of additional information search. Search for iteration variables and
   * for calculations in array brackets.
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
            .map(e -> e.getDescription())
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
              FaultInfo.hint(
                  "Detected suspicious calculation within the array subscript using an iteration"
                      + " variable. Have a closer look to this line."));
          break;
        }
        if (hasIter) {
          fault.addInfo(
              FaultInfo.hint(
                  "This line uses an iteration variable. This may be especially prone to errors."));
          break;
        }
        if (hasCalc) {
          fault.addInfo(
              FaultInfo.hint(
                  "Detected suspicious calculation within the array subscript. This may be"
                      + " especially prone to errors"));
          break;
        }
      }
    }
  }

  public static void propagatePreCondition(
      Collection<Fault> rankedList, TraceFormula traceFormula, FormulaManagerView fmgr) {
    if (!traceFormula.getPrecondition().toString().contains("_VERIFIER_nondet_")) {
      return;
    }
    Set<BooleanFormula> preconditions =
        fmgr.getBooleanFormulaManager().toConjunctionArgs(traceFormula.getPrecondition(), true);

    Map<String, String> mapFormulaToValue = new HashMap<>();
    List<String> assignments = new ArrayList<>();

    for (BooleanFormula precondition : preconditions) {
      String formulaString = precondition.toString();
      formulaString = formulaString.replaceAll("\\(", "").replaceAll("\\)", "");
      List<String> operatorAndOperands = Splitter.on("` ").splitToList(formulaString);
      if (operatorAndOperands.size() != 2) {
        return;
      }
      String withoutOperator = operatorAndOperands.get(1);
      List<String> operands = Splitter.on(" ").splitToList(withoutOperator);
      if (operands.size() != 2) {
        return;
      }
      if (operands.get(0).contains("__VERIFIER_nondet_")
          || (operands.get(0).contains("::") && operands.get(0).contains("@"))) {
        if ((operands.get(0).contains("::") && operands.get(0).contains("@"))) {
          assignments.add(
              Splitter.on("@").splitToList(operands.get(0)).get(0) + " = " + operands.get(1));
        } else {
          mapFormulaToValue.put(operands.get(0), operands.get(1));
        }
      } else {
        if ((operands.get(1).contains("::") && operands.get(1).contains("@"))) {
          assignments.add(
              Splitter.on("@").splitToList(operands.get(1)).get(0) + " = " + operands.get(0));
        } else {
          mapFormulaToValue.put(operands.get(1), operands.get(0));
        }
      }
    }

    List<BooleanFormula> atoms = traceFormula.getEntries().toAtomList();
    for (Entry<String, String> entry : mapFormulaToValue.entrySet()) {
      for (BooleanFormula atom : atoms) {
        if (atom.toString().contains(entry.getKey())) {
          atom = fmgr.uninstantiate(atom);
          String assignment =
              BooleanFormulaParser.parse(
                      atom.toString().replaceAll(entry.getKey(), entry.getValue()))
                  .toString();
          assignments.add(assignment);
        }
      }
    }

    String allAssignments = String.join(", ", assignments);
    for (Fault fault : rankedList) {
      fault.addInfo(
          FaultInfo.hint(
              "The program fails for the following initial variable assignment: "
                  + allAssignments));
    }
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
