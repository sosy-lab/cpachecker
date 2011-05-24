package de.upb.agw.cpachecker.cpa.abm.util.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.ReferencedVariable;
import de.upb.agw.cpachecker.cpa.abm.util.RelevantPredicatesComputer;

/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block. 
 * @author dwonisch
 *
 */

public class OccurrenceComputer implements RelevantPredicatesComputer {
  private Map<Pair<CachedSubtree, Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>> removedCache;
  private Map<Pair<CachedSubtree, Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>> relevantCache;
  private Map<Pair<CachedSubtree, AbstractionPredicate>, Boolean> relevantPredicates;
  
  public OccurrenceComputer() {    
    this.removedCache = new HashMap<Pair<CachedSubtree,Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>>();
    this.relevantCache = new HashMap<Pair<CachedSubtree,Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>>();
    this.relevantPredicates = new HashMap<Pair<CachedSubtree,AbstractionPredicate>, Boolean>();
  }
  
  @Override
  public Collection<AbstractionPredicate> getIrrelevantPredicates(CachedSubtree context, Collection<AbstractionPredicate> predicates) {
    Pair<CachedSubtree,Collection<AbstractionPredicate>> pair = Pair.of(context, predicates);
    if(!removedCache.containsKey(pair)) {
      removedCache.put(pair, computeRemovePredicates(context, predicates));
    } 
    return removedCache.get(pair);
  }
  
  @Override
  public Collection<AbstractionPredicate> getRelevantPredicates(CachedSubtree context, Collection<AbstractionPredicate> predicates) {
    Pair<CachedSubtree,Collection<AbstractionPredicate>> pair = Pair.of(context, predicates);
    if(!relevantCache.containsKey(pair)) {
      relevantCache.put(pair, computeRelevantPredicates(context, predicates));
    } 
    return relevantCache.get(pair);
  }
  
  private Collection<AbstractionPredicate> computeRelevantPredicates(CachedSubtree context, Collection<AbstractionPredicate> predicates) {
    Collection<AbstractionPredicate> relevantPredicates = new HashSet<AbstractionPredicate>(predicates.size());
    for(AbstractionPredicate predicate : predicates) {
      if(isRelevant(context, predicate)) {
        relevantPredicates.add(predicate);
      }
    }
    return relevantPredicates;
  }
  
  private boolean isRelevant(CachedSubtree context, AbstractionPredicate predicate) {
    Pair<CachedSubtree, AbstractionPredicate> pair = Pair.of(context, predicate);
    if(relevantPredicates.containsKey(pair)) {
      return relevantPredicates.get(pair);
    }
    
    String predicateString = predicate.getSymbolicAtom().toString();
    if(predicateString.contains("false") || predicateString.contains("retval")  || predicateString.contains("nondet")) {
      relevantPredicates.put(pair, true);
      return true;
    }
    else {
      for(ReferencedVariable var : context.getReferencedVariables()) {
        if(predicateString.contains(var.getName())) {
          //var occurs in the predicate, so better trace it
          //TODO: contains is a quite rough approximation; for example "foo <= 5" also contains "f", although the variable f does in fact not occur in the predicate.
          relevantPredicates.put(pair, true);
          return true;
        }
      }      
    }
    relevantPredicates.put(pair, false);
    return false;    
  }
  
  private Collection<AbstractionPredicate> computeRemovePredicates(CachedSubtree context, Collection<AbstractionPredicate> predicates) {
    Collection<AbstractionPredicate> newPredicates = getRelevantPredicates(context, predicates);
    
    Collection<AbstractionPredicate> removePredicates = new HashSet<AbstractionPredicate>();
    removePredicates.addAll(predicates);
    removePredicates.removeAll(newPredicates);
    
    return removePredicates;
  }
}
