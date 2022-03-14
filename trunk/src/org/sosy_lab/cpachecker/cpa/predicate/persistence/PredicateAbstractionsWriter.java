// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.LINE_JOINER;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.splitFormula;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateAbstractionsWriter {

  private final LogManager logger;
  private final FormulaManagerView fmgr;

  public PredicateAbstractionsWriter(LogManager pLogger, FormulaManagerView pFmMgr) {
    logger = pLogger;
    fmgr = pFmMgr;
  }

  private int getAbstractionId(ARGState state) {
    return PredicateAbstractState.getPredicateState(state).getAbstractionFormula().getId();
  }

  public void writeAbstractions(Path abstractionsFile, UnmodifiableReachedSet reached) {
    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = new LinkedHashSet<>();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<ARGState, String> stateToAssert = new LinkedHashMap<>();

    // Get list of all abstraction states in the set reached
    Deque<ARGState> worklist = new ArrayDeque<>();
    SetMultimap<ARGState, ARGState> successors;
    if (!reached.isEmpty()) {
      ARGState rootState =
          AbstractStates.extractStateByType(reached.getFirstState(), ARGState.class);
      if (rootState != null) {
        successors =
            ARGUtils.projectARG(
                rootState, ARGState::getChildren, PredicateAbstractState::containsAbstractionState);
        worklist.add(rootState);
      } else {
        successors = ImmutableSetMultimap.of();
      }
    } else {
      successors = ImmutableSetMultimap.of();
    }
    Set<ARGState> done = new HashSet<>();

    // Write abstraction formulas of the abstraction states to the file
    try (Writer writer = IO.openOutputFile(abstractionsFile, Charset.defaultCharset())) {
      while (!worklist.isEmpty()) {
        ARGState state = worklist.pop();

        if (done.contains(state)) {
          continue;
        }

        // Handle successors
        worklist.addAll(successors.get(state));

        // Abstraction formula
        PredicateAbstractState predicateState = PredicateAbstractState.getPredicateState(state);
        BooleanFormula formula = predicateState.getAbstractionFormula().asFormula();

        Pair<String, List<String>> p = splitFormula(fmgr, formula);
        String formulaString = p.getFirst();
        definitions.addAll(p.getSecond());

        stateToAssert.put(state, formulaString);

        done.add(state);
      }

      // Write it to the file
      // -- first the definitions
      LINE_JOINER.appendTo(writer, definitions);
      writer.append("\n\n");

      // -- then the assertions
      for (Map.Entry<ARGState, String> entry : stateToAssert.entrySet()) {
        ARGState state = entry.getKey();
        StringBuilder stateSuccessorsSb = new StringBuilder();
        for (ARGState successor : successors.get(state)) {
          if (stateSuccessorsSb.length() > 0) {
            stateSuccessorsSb.append(",");
          }
          stateSuccessorsSb.append(getAbstractionId(successor));
        }

        String locationString;
        CFANode locationNode = AbstractStates.extractLocation(state);
        if (locationNode != null) {
          locationString = Integer.toString(locationNode.getNodeNumber());
        } else {
          locationString = AbstractStates.extractLocations(state).toString();
        }
        writer.append(
            String.format(
                "%d (%s) @%s:",
                getAbstractionId(state), stateSuccessorsSb.toString(), locationString));
        writer.append("\n");
        writer.append(entry.getValue());
        writer.append("\n\n");
      }

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write abstractions to file");
    }
  }
}
