package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.Maps;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block. 
 * @author dwonisch
 *
 */

public class OccurrenceComputer implements RelevantPredicatesComputer {

  private final Map<Pair<Block, AbstractionPredicate>, Boolean> relevantPredicates = Maps.newHashMap();
  
  @Override
  public Collection<AbstractionPredicate> getRelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
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
  
  @Override
  public Collection<AbstractionPredicate> getIrrelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Collection<AbstractionPredicate> newPredicates = getRelevantPredicates(context, predicates);
    
    Collection<AbstractionPredicate> removePredicates = new HashSet<AbstractionPredicate>();
    removePredicates.addAll(predicates);
    removePredicates.removeAll(newPredicates);
    
    return removePredicates;
  }
}
