package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block. 
 * @author dwonisch
 *
 */

public class OccurrenceComputer implements RelevantPredicatesComputer {
  private Map<Pair<Block, Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>> removedCache;
  private Map<Pair<Block, Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>> relevantCache;
  private Map<Pair<Block, AbstractionPredicate>, Boolean> relevantPredicates;
  
  public OccurrenceComputer() {    
    this.removedCache = new HashMap<Pair<Block,Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>>();
    this.relevantCache = new HashMap<Pair<Block,Collection<AbstractionPredicate>>, Collection<AbstractionPredicate>>();
    this.relevantPredicates = new HashMap<Pair<Block,AbstractionPredicate>, Boolean>();
  }
  
  @Override
  public Collection<AbstractionPredicate> getIrrelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Pair<Block,Collection<AbstractionPredicate>> pair = Pair.of(context, predicates);
    if(!removedCache.containsKey(pair)) {
      removedCache.put(pair, computeRemovePredicates(context, predicates));
    } 
    return removedCache.get(pair);
  }
  
  @Override
  public Collection<AbstractionPredicate> getRelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Pair<Block,Collection<AbstractionPredicate>> pair = Pair.of(context, predicates);
    if(!relevantCache.containsKey(pair)) {
      relevantCache.put(pair, computeRelevantPredicates(context, predicates));
    } 
    return relevantCache.get(pair);
  }
  
  private Collection<AbstractionPredicate> computeRelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Collection<AbstractionPredicate> relevantPredicates = new HashSet<AbstractionPredicate>(predicates.size());
    for(AbstractionPredicate predicate : predicates) {
      if(isRelevant(context, predicate)) {
        relevantPredicates.add(predicate);
      }
    }
    return relevantPredicates;
  }
  
  private boolean isRelevant(Block context, AbstractionPredicate predicate) {
    Pair<Block, AbstractionPredicate> pair = Pair.of(context, predicate);
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
  
  private Collection<AbstractionPredicate> computeRemovePredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Collection<AbstractionPredicate> newPredicates = getRelevantPredicates(context, predicates);
    
    Collection<AbstractionPredicate> removePredicates = new HashSet<AbstractionPredicate>();
    removePredicates.addAll(predicates);
    removePredicates.removeAll(newPredicates);
    
    return removePredicates;
  }
}
