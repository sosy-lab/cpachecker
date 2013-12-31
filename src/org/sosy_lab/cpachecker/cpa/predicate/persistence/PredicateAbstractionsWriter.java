/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.io.IOException;
import java.io.Writer;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;


public class PredicateAbstractionsWriter {

  private final LogManager logger;
  private final AbstractionManager absmgr;
  private final FormulaManagerView fmgr;

  public PredicateAbstractionsWriter(LogManager pLogger, AbstractionManager pAbsMgr,
      FormulaManagerView pFmMgr) {
    this.logger = pLogger;
    this.absmgr = pAbsMgr;
    this.fmgr = pFmMgr;
  }

  private static final Predicate<AbstractState> IS_ABSTRACTION_STATE = new Predicate<AbstractState>() {
    @Override
    public boolean apply(AbstractState pArg0) {
      PredicateAbstractState e = AbstractStates.extractStateByType(pArg0, PredicateAbstractState.class);
      return e.isAbstractionState();
    }
  };

  private int getAbstractionId(ARGState state) {
    PredicateAbstractState paState = AbstractStates.extractStateByType(state, PredicateAbstractState.class);
    return paState.getAbstractionFormula().getId();
  }

  public void writeAbstractions(Path abstractionsFile, ReachedSet reached) {
    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<ARGState, String> stateToAssert = Maps.newHashMap();

    // Get list of all abstraction states in the set reached
    ARGState rootState = AbstractStates.extractStateByType(reached.getFirstState(), ARGState.class);
    SetMultimap<ARGState, ARGState> successors = ARGUtils.projectARG(rootState,
        ARGUtils.CHILDREN_OF_STATE, IS_ABSTRACTION_STATE);

    Set<ARGState> done = Sets.newHashSet();
    Deque<ARGState> worklist = Queues.newArrayDeque();

    worklist.add(rootState);

    // Write abstraction formulas of the abstraction states to the file
    try (Writer writer = Files.openOutputFile(abstractionsFile)) {
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
        PredicateAbstractState predicateState = checkNotNull(extractStateByType(state, PredicateAbstractState.class));
        Region region = predicateState.getAbstractionFormula().asRegion();
        BooleanFormula formula = absmgr.toConcrete(region);

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
      for (ARGState state : stateToAssert.keySet()) {
        StringBuilder stateSuccessorsSb = new StringBuilder();
        for (ARGState successor : successors.get(state)) {
          if (stateSuccessorsSb.length() > 0) {
            stateSuccessorsSb.append(",");
          }
          stateSuccessorsSb.append(getAbstractionId(successor));
        }

        writer.append(String.format("%d (%s) @%d:\n",
            getAbstractionId(state),
            stateSuccessorsSb.toString(),
            AbstractStates.extractLocation(state).getNodeNumber()));
        writer.append(stateToAssert.get(state));
        writer.append("\n\n");
      }


    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write abstractions to file");
    }
  }

}
