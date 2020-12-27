// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The class <code>PredicatePartitionRevImplication</code> represents a concrete partition of predicates and hence it
 * extends {@link PredicatePartition}.
 * <p/>
 * It is used by the {@link AbstractionManager} to generate a variable ordering for BDDs where a BDD variable
 * represents a predicate.
 */
public class PredicatePartitionRevImplication extends PredicatePartition {

  public PredicatePartitionRevImplication(FormulaManagerView fmgr, Solver solver, LogManager logger) {
    super(fmgr, solver, logger);
  }

  /**
   * Inserts a new predicate before the most left predicate of the partition that implies the new predicate.
   *
   * @param newPred the predicate that should be inserted.
   */
  @Override
  public void insertPredicate(AbstractionPredicate newPred) {
    this.varIDToPredicate.put(newPred.getVariableNumber(), newPred);
    // solver does caching
    // find lowest position of a predicate that is implied by newPred, insert newPred before that predicate
    int lowestImplier = this.predicates.size();
    int elementIndex = this.predicates.size() - 1;
    for (AbstractionPredicate oldPred : Lists.reverse(ImmutableList.copyOf(this.predicates))) {
      try {
        if (this.solver.implies(oldPred.getSymbolicAtom(), newPred.getSymbolicAtom())) {
          lowestImplier = elementIndex;
        }
        if (this.solver.implies(newPred.getSymbolicAtom(), oldPred.getSymbolicAtom())) {
          break;
        }

        elementIndex--;
      } catch (SolverException | InterruptedException e) {
        this.logger.log(java.util.logging.Level.WARNING, "Error while adding the predicate ", newPred, " by " +
            "implications to the list of predicates");
      }
    }

    this.predicates.add(lowestImplier, newPred);
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