package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.Maps;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that a auxiliary to the block. 
 * @author dwonisch
 *
 */
public class AuxiliaryComputer implements RelevantPredicatesComputer {
  
  private final Map<Pair<Collection<String>, AbstractionPredicate>, Boolean> relevantPredicates = Maps.newHashMap();
  
  @Override
  public Collection<AbstractionPredicate> getRelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Collection<AbstractionPredicate> relevantPredicates = new HashSet<AbstractionPredicate>(predicates.size());
    
    Collection<String> relevantVariables = computeRelevantVariables(context, predicates);
    
    for(AbstractionPredicate predicate : predicates) {
      if(isRelevant(relevantVariables, predicate)) {
        relevantPredicates.add(predicate);
      }
    }
    return relevantPredicates;
  }
  
  private Collection<String> computeRelevantVariables(Block pContext, Collection<AbstractionPredicate> pPredicates) {
    Collection<String> relevantVars = new HashSet<String>();
    Collection<ReferencedVariable> unknownVars = new ArrayList<ReferencedVariable>();
    
    for(ReferencedVariable var : pContext.getReferencedVariables()) {
      if(var.occursInCondition()) {
        relevantVars.add(var.getName());        
      }
      else if(var.occursOnLhs()) {
        if(occursInPredicate(var, pPredicates)) {
          relevantVars.add(var.getName());
        }
      }
      else {
        unknownVars.add(var);
      }
    }
    
    boolean changed = true;
    while(changed) {
      changed = false;
      Collection<ReferencedVariable> yetUnknownVars = new ArrayList<ReferencedVariable>();
      
      for(ReferencedVariable var : unknownVars) {
        if(relevantVars.contains(var.getLhsVariable().getName())) {
          relevantVars.add(var.getName());
          changed = true;
        }
        else {
          yetUnknownVars.add(var);
        }
      }      
      unknownVars = yetUnknownVars;
    }   
    
    return relevantVars;
  }

  private boolean occursInPredicate(ReferencedVariable pVar, Collection<AbstractionPredicate> pPredicates) {
    for(AbstractionPredicate predicate : pPredicates) {
      if(predicate.getSymbolicAtom().toString().contains(pVar.getName())) {
        return true;
      }
    }
    return false;
  }

  private boolean isRelevant(Collection<String> relevantVariables, AbstractionPredicate predicate) {
    Pair<Collection<String>, AbstractionPredicate> pair = Pair.of(relevantVariables, predicate);
    if(relevantPredicates.containsKey(pair)) {
      return relevantPredicates.get(pair);
    }
    
    String predicateString = predicate.getSymbolicAtom().toString();
    if(predicateString.contains("false") || predicateString.contains("retval")  || predicateString.contains("nondet")) {
      relevantPredicates.put(pair, true);
      return true;
    }
    else {
      for(String var : relevantVariables) {
        if(predicateString.contains(var)) {
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
