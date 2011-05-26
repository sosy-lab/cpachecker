package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;


/**
 * Interface for the computation of (ir-)relevant predicates of a given block.
 * @author dwonisch
 *
 */
public interface RelevantPredicatesComputer {
  public Collection<AbstractionPredicate> getIrrelevantPredicates(Block context, Collection<AbstractionPredicate> predicates);
  public Collection<AbstractionPredicate> getRelevantPredicates(Block context, Collection<AbstractionPredicate> predicates);
}
