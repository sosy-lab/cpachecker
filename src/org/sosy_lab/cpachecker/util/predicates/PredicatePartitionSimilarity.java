// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * The class <code>PredicatePartitionSimilartiy</code> represents a concrete partition of predicates and hence it
 * extends {@link PredicatePartition}.
 * <p/>
 * It is used by the {@link AbstractionManager} to generate a variable ordering for BDDs where a BDD variable
 * represents a predicate.
 */
public class PredicatePartitionSimilarity extends PredicatePartition {
  private Map<Integer, Map<Integer, Integer>> similarityRelation;

  public PredicatePartitionSimilarity(FormulaManagerView fmgr, Solver solver, LogManager logger) {
    super(fmgr, solver, logger);
    this.similarityRelation = new HashMap<>();
  }

  /**
   * Inserts a new predicate next to the predicate of the partition that is most similar to the new one.
   *
   * @param newPred the new predicate that should be inserted.
   */
  @Override
  public void insertPredicate(AbstractionPredicate newPred) {
    this.varIDToPredicate.put(newPred.getVariableNumber(), newPred);
    // first update the predicate similarities
    updatePredicateSimilarities(newPred);

    // calculate the predicate that is most similar to the new one
    Map<Integer, Integer> similarities = similarityRelation.get(newPred.getVariableNumber());
    Map.Entry<Integer, Integer> entryWithMaxSimilarity = null;
    for (Map.Entry<Integer, Integer> entry : similarities.entrySet()) {
      if (entryWithMaxSimilarity == null || entry.getValue().compareTo(entryWithMaxSimilarity.getValue()) > 0) {
        entryWithMaxSimilarity = entry;
      }
    }

    if (entryWithMaxSimilarity != null) {
      AbstractionPredicate mostSimilarPredicate = varIDToPredicate.get(entryWithMaxSimilarity.getKey());
      int indexSimilarPred = predicates.indexOf(mostSimilarPredicate);

      // the new predicate is inserted before the most similar if it contains more variables else it's the other way round
      if (fmgr.extractVariableNames(mostSimilarPredicate.getSymbolicAtom()).size() <
          fmgr.extractVariableNames(newPred.getSymbolicAtom()).size()) {
        predicates.add(indexSimilarPred, newPred);
      } else {
        predicates.add(indexSimilarPred + 1, newPred);
      }
    } else {
      predicates.add(newPred);
    }
  }

  @Override
  public PredicatePartition merge(PredicatePartition newPreds) {
    if (this.partitionID != newPreds.getPartitionID() && newPreds instanceof PredicatePartitionSimilarity) {
      PredicatePartitionSimilarity newPartition = (PredicatePartitionSimilarity) newPreds;
      // merge the mappings varIDToPredicate of the two partitions.
      // this has to be done no matter which insertion strategy is used.
      this.varIDToPredicate.putAll(newPreds.getVarIDToPredicate());

      // place the partition with more predicates first and merge similarity relations.
      if (newPreds.predicates.size() > this.predicates.size()) {
        this.predicates.addAll(0, newPreds.predicates);
      } else {
        this.predicates.addAll(newPreds.predicates);
      }
      this.similarityRelation.putAll(newPartition.getSimilarityRelation());
    }

    return this;
  }

  public Map<Integer, Map<Integer, Integer>> getSimilarityRelation() {
    return similarityRelation;
  }

  /**
   * Calculates the similarity of the new predicate and old predicates in the partition and updates the similarity
   * relationship.
   *
   * @param newPredicate the new predicated that arrived.
   */
  private void updatePredicateSimilarities(AbstractionPredicate newPredicate) {
    int varIDNewPredicate = newPredicate.getVariableNumber();
    Map<Integer, Integer> similarities = new HashMap<>();
    Set<String> varsInNewPred = fmgr.extractVariableNames(newPredicate.getSymbolicAtom());

    // calculate for each of the old predicates the number of variables the old and the new predicate have in common
    for (AbstractionPredicate predInPartition : predicates) {
      Set<String> varsInPrevPred = fmgr.extractVariableNames(predInPartition.getSymbolicAtom());
      int index = predInPartition.getVariableNumber();
      // the similarity is equal to the number of variables the predicates have in common
      Integer similarity = Sets.intersection(varsInPrevPred, varsInNewPred).size();
      Map<Integer, Integer> similaritiesPrevPred = similarityRelation.get(index);
      similaritiesPrevPred.put(varIDNewPredicate, similarity);
      similarities.put(index, similarity);
    }

    similarityRelation.put(varIDNewPredicate, similarities);
  }
}