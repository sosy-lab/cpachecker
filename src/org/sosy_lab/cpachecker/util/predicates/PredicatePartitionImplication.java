/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.Collections;
import java.util.LinkedList;

/**
 * The class <code>PredicatePartitionImplication</code> represents a concrete partition of predicates and hence it
 * extends {@link PredicatePartition}.
 * <p/>
 * It is used by the {@link AbstractionManager} to generate a variable ordering for BDDs where a BDD variable
 * represents a predicate.
 */
public class PredicatePartitionImplication extends PredicatePartition {

  public PredicatePartitionImplication(FormulaManagerView fmgr, Solver solver, LogManager logger) {
    super(fmgr, solver, logger);
  }

  /**
   * Inserts a new predicate before the most left predicate of the partition that is implied by the new predicate.
   *
   * @param newPred the predicate that should be inserted.
   */
  @Override
  public void insertPredicate(AbstractionPredicate newPred) {
    this.varIDToPredicate.put(newPred.getVariableNumber(), newPred);
    // solver does caching
    // find lowest position of a predicate that is implied by newPred, insert newPred before that predicate
    int lowestImplied = this.predicates.size();
    int elementIndex = this.predicates.size() - 1;
    LinkedList<AbstractionPredicate> predicatesCopy = new LinkedList<>(this.predicates);
    Collections.reverse(predicatesCopy);
    for (AbstractionPredicate oldPred : predicatesCopy) {
      try {
        if (this.solver.implies(newPred.getSymbolicAtom(), oldPred.getSymbolicAtom())) {
          lowestImplied = elementIndex;
        }

        if (this.solver.implies(oldPred.getSymbolicAtom(), newPred.getSymbolicAtom())) {
          break;
        }

        elementIndex--;
      } catch (SolverException | InterruptedException e) {
        this.logger.log(java.util.logging.Level.WARNING, "Error while adding the predicate ", newPred, " by implications to "
            + "the list of predicates");
      }
    }

    this.predicates.add(lowestImplied, newPred);
  }

  @Override
  public PredicatePartition merge(PredicatePartition newPreds) {
    if (this.partitionID != newPreds.getPartitionID()) {
      // merge the mappings varIDToPredicate of the two partitions.
      this.varIDToPredicate.putAll(newPreds.getVarIDToPredicate());

      // insert every predicate on its own, insertion takes care of the sorting
      for (AbstractionPredicate newPred : newPreds.getPredicates()) {
       this.insertPredicate(newPred);
      }
    }

    return this;
  }
}