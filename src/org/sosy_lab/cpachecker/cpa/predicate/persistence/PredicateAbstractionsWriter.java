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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.LINE_JOINER;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.splitFormula;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;


public class PredicateAbstractionsWriter {

  private final LogManager logger;
  private final FormulaManagerView fmgr;

  public PredicateAbstractionsWriter(LogManager pLogger,
      FormulaManagerView pFmMgr) {
    this.logger = pLogger;
    this.fmgr = pFmMgr;
  }

  private int getAbstractionId(ARGState state) {
    return PredicateAbstractState.getPredicateState(state).getAbstractionFormula().getId();
  }

  public void writeAbstractions(Path abstractionsFile, UnmodifiableReachedSet reached) {
    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<ARGState, String> stateToAssert = Maps.newHashMap();

    // Get list of all abstraction states in the set reached
    Deque<ARGState> worklist = Queues.newArrayDeque();
    SetMultimap<ARGState, ARGState> successors;
    if (!reached.isEmpty()) {
      ARGState rootState =
          AbstractStates.extractStateByType(reached.getFirstState(), ARGState.class);
      successors =
          ARGUtils.projectARG(
              rootState, ARGState::getChildren, PredicateAbstractState.CONTAINS_ABSTRACTION_STATE);

      worklist.add(rootState);
    } else {
      successors = ImmutableSetMultimap.of();
    }
    Set<ARGState> done = Sets.newHashSet();

    // Write abstraction formulas of the abstraction states to the file
    try (Writer writer = MoreFiles.openOutputFile(abstractionsFile, Charset.defaultCharset())) {
      while (!worklist.isEmpty()) {
        ARGState state = worklist.pop();

        if (done.contains(state)) {
          continue;
        }

        // Handle successors
        for (ARGState successor : successors.get(state)) {
          worklist.add(successor);
        }

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

        writer.append(String.format("%d (%s) @%d:",
            getAbstractionId(state),
            stateSuccessorsSb.toString(),
            AbstractStates.extractLocation(state).getNodeNumber()));
        writer.append("\n");
        writer.append(entry.getValue());
        writer.append("\n\n");
      }


    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write abstractions to file");
    }
  }

}
