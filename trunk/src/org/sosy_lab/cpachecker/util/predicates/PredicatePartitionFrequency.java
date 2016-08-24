/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The class <code>PredicatePartitionFrequency</code> represents a concrete partition of predicates and hence it
 * extends {@link PredicatePartition}.
 * <p/>
 * It is used by the {@link AbstractionManager} to generate a variable ordering for BDDs where a BDD variable
 * represents a predicate.
 */
public class PredicatePartitionFrequency extends PredicatePartition {
  private PriorityQueue<AbstractionPredicate> predicatesSortedByVarFrequency;

  public PredicatePartitionFrequency(FormulaManagerView fmgr, Solver solver, LogManager logger) {
    super(fmgr, solver, logger);
    predicatesSortedByVarFrequency =
        new PriorityQueue<>(
            1, Comparator.comparingInt(AbstractionPredicate::getVariableNumber).reversed());
  }

  /**
   * Inserts a new predicate such that predicates with a higher number of variables are left
   * and predicates with a lower number of variables are right of the new predicate.
   *
   * @param newPred the new predicate to be inserted in the partition
   */
  @Override
  public void insertPredicate(AbstractionPredicate newPred) {
    this.varIDToPredicate.put(newPred.getVariableNumber(), newPred);
    predicatesSortedByVarFrequency.add(newPred);
  }

  @Override
  public PredicatePartition merge(PredicatePartition newPreds) {
    if (this.partitionID != newPreds.getPartitionID()) {
      // merge the mappings varIDToPredicate of the two partitions.
      // this has to be done no matter which insertion strategy is used.
      this.varIDToPredicate.putAll(newPreds.getVarIDToPredicate());

      // insert all predicates of the other partition at the right place in the priority queue of this partition
      this.predicatesSortedByVarFrequency.addAll(newPreds.getPredicates());
    }

    return this;
  }

  @Override
  public List<AbstractionPredicate> getPredicates() {
    if (predicatesSortedByVarFrequency.size() != 0) {
      while (!predicatesSortedByVarFrequency.isEmpty()) {
        predicates.add(predicatesSortedByVarFrequency.poll());
      }
    }
    return predicates;
  }
}