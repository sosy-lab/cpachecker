/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class InvariantsWriter {

  private static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
  private static final Joiner LINE_JOINER = Joiner.on('\n');

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final String splitInvariantsForExport;

  public InvariantsWriter(
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      String pSplitInvariantsForExport) {
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    splitInvariantsForExport = pSplitInvariantsForExport;
  }

  /** write all invariants for the whole reached-set. */
  public void write(UnmodifiableReachedSet pReachedSet, Appendable pAppendable) throws IOException {
    SetMultimap<CFANode, InvariantsState> locationPredicates = HashMultimap.create();
    for (AbstractState state : pReachedSet) {
      CFANode location = extractLocation(state);
      if (location != null) {
        InvariantsState invariantsState = extractStateByType(state, InvariantsState.class);
        locationPredicates.put(location, invariantsState);
      }
    }
    write(locationPredicates, pAppendable);
  }

  /** write all invariants for a single program location. */
  public void write(final CFANode pCfaNode, UnmodifiableReachedSet pReachedSet, Appendable pAppendable) throws IOException {
    SetMultimap<CFANode, InvariantsState> statesToNode = HashMultimap.create();
    statesToNode.putAll(pCfaNode, projectToType(filterLocation(pReachedSet, pCfaNode), InvariantsState.class));
    write(statesToNode, pAppendable);
  }

  private void write(SetMultimap<CFANode, InvariantsState> pStates, Appendable pAppendable) throws IOException {

    // (global) definitions used for predicates
    final Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    final Multimap<CFANode, String> cfaNodeToPredicate = HashMultimap.create();

    // fill the above set and map
    for (CFANode cfaNode : pStates.keySet()) {
      List<BooleanFormula> formulas = getFormulasForNode(pStates.get(cfaNode), cfaNode);
      extractPredicatesAndDefinitions(cfaNode, definitions, cfaNodeToPredicate, formulas);
    }

    writeInvariants(pAppendable, definitions, cfaNodeToPredicate);
  }

  /** get formulas representing the abstract states at the cfaNode. */
  private List<BooleanFormula> getFormulasForNode(Set<InvariantsState> states, CFANode cfaNode) {
    final List<BooleanFormula> formulas = new ArrayList<>();
    final BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    List<BooleanFormula> stateFormulas = new ArrayList<>();
    for (InvariantsState state : states) {
      stateFormulas.add(state.getFormulaApproximation(fmgr, pfmgr));
    }

    switch (splitInvariantsForExport) {
    case "LOCATION":
      // create the disjunction of the found states for the current location
      formulas.add(bfmgr.or(stateFormulas));
      break;
    case "STATE":
      // do not merge different location-formulas
      formulas.addAll(stateFormulas);
      break;
    case "ATOM":
      // atomize formulas
      for (BooleanFormula f : stateFormulas) {
        formulas.addAll(fmgr.extractAtoms(f, false));
      }
      break;
    default:
      throw new AssertionError("unknown option");
    }

    // filter out formulas with no information
    final List<BooleanFormula> filtered = new ArrayList<>();
    for (BooleanFormula f : formulas) {
      if (!bfmgr.isTrue(f) && !bfmgr.isFalse(f)) {
        filtered.add(f);
      }
    }

    return filtered;
  }

  /** dump each formula and split it into the predicate and some utility-stuff (named definition)
   *  that consists of symbol-declarations and solver-specific queries. */
  private void extractPredicatesAndDefinitions(
      CFANode cfaNode,
      Set<String> definitions,
      Multimap<CFANode, String> cfaNodeToPredicate,
      List<BooleanFormula> predicates) throws IOException {

    for (BooleanFormula formula : predicates) {
      String s = fmgr.dumpFormula(formula).toString();
      List<String> lines = Lists.newArrayList(LINE_SPLITTER.split(s));
      assert !lines.isEmpty();

      // Get the predicate from the last line
      String predString = lines.get(lines.size() - 1);

      // Remove the predicate from the dump
      lines.remove(lines.size() - 1);

      // Check that the dump format is correct
      if (!(predString.startsWith("(assert ") && predString.endsWith(")"))) {
        throw new AssertionError("Writing invariants is only supported for solvers "
            + "that support the Smtlib2 format, please try using Mathsat5.");
      }

      // Add the definition part of the dump to the set of definitions
      definitions.addAll(lines);

      // Record the predicate to write it later at t
      cfaNodeToPredicate.put(cfaNode, predString);
    }
  }

  /** write the definitions and predicates in the commonly used precision-format
   *  (that is defined somewhere else...)*/
  private void writeInvariants(Appendable pAppendable, Set<String> definitions,
      Multimap<CFANode, String> cfaNodeToPredicate) throws IOException {

    // write definitions to file
    LINE_JOINER.appendTo(pAppendable, definitions);
    pAppendable.append("\n\n");

    // write states to file
    for (CFANode cfaNode : cfaNodeToPredicate.keySet()) {
      pAppendable.append(toKey(cfaNode));
      pAppendable.append(":\n");
      LINE_JOINER.appendTo(pAppendable, cfaNodeToPredicate.get(cfaNode));
      pAppendable.append("\n\n");
    }
  }

  private static String toKey(CFANode pCfaNode) {
    return pCfaNode.getFunctionName() + " " + pCfaNode.toString();
  }

}
