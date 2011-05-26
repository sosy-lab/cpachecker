package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import java.util.Collection;

import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;

/**
 * Interface for the computation of (ir-)relevant predicates of a given block.
 * @author dwonisch
 *
 */
public interface RelevantPredicatesComputer {
  public Collection<AbstractionPredicate> getIrrelevantPredicates(CachedSubtree context, Collection<AbstractionPredicate> predicates);
  public Collection<AbstractionPredicate> getRelevantPredicates(CachedSubtree context, Collection<AbstractionPredicate> predicates);
}
