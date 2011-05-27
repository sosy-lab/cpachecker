package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur in the block. 
 * @author dwonisch
 *
 */

public class OccurrenceComputer extends AbstractRelevantPredicatesComputer<Block> {

  @Override
  protected Block precompute(Block pContext, Collection<AbstractionPredicate> pPredicates) {
    return pContext;
  }
  
  @Override
  protected boolean isRelevant(Block context, AbstractionPredicate predicate) {
    String predicateString = predicate.getSymbolicAtom().toString();

    for (ReferencedVariable var : context.getReferencedVariables()) {
      if (predicateString.contains(var.getName())) {
        //var occurs in the predicate, so better trace it
        //TODO: contains is a quite rough approximation; for example "foo <= 5" also contains "f", although the variable f does in fact not occur in the predicate.
        return true;
      }
    }
    return false;
  }
}