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
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
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

  public void writeAbstractions(Path abstractionsFile, ReachedSet reached) {

    // Get list of all abstraction states in the set reached
    ARGState rootState = AbstractStates.extractStateByType(reached.getFirstState(), ARGState.class);
    SetMultimap<ARGState, ARGState> successors = ARGUtils.projectARG(rootState,
        ARGUtils.CHILDREN_OF_STATE, IS_ABSTRACTION_STATE);

    Set<ARGState> written = Sets.newHashSet();
    Deque<ARGState> toWrite = Queues.newArrayDeque();

    toWrite.add(rootState);

    // Write abstraction formulas of the abstraction states to the file
    try (Writer writer = Files.openOutputFile(abstractionsFile)) {
      while (!toWrite.isEmpty()) {
        ARGState state = toWrite.pop();
        Set<ARGState> stateSuccessors = successors.get(state);

        if (written.contains(state)) {
          continue;
        }

        // Successors
        StringBuilder stateSuccessorsSb = new StringBuilder();
        for (ARGState successor : stateSuccessors) {
          if (stateSuccessorsSb.length() == 0) {
            stateSuccessorsSb.append(",");
          }
          stateSuccessorsSb.append(successor.getStateId());
        }

        // Abstraction formula
        PredicateAbstractState predicateState = checkNotNull(extractStateByType(state, PredicateAbstractState.class));
        Region region = predicateState.getAbstractionFormula().asRegion();
        BooleanFormula formula = absmgr.toConcrete(region);
        Appender abstractionFormula = fmgr.dumpFormula(formula);

        // Write it to the file
        writer.append(String.format("%d (%s):\n",
            state.getStateId(),
            stateSuccessorsSb.toString()));
        abstractionFormula.appendTo(writer);
        writer.append("\n\n");

        written.add(state);
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write abstractions to file");
    }
  }

}
