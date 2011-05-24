package de.upb.agw.cpachecker.cpa.abm.util;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/**
 * Helper class that provides efficient methods to compare <code>PredicatePrecision</code>s with each other with respect to the relevant predicates of a given block context.
 * @author dwonisch
 *
 */
public class PrecisionUtils {
  public static boolean relevantComparePrecisions(PredicatePrecision precision, CachedSubtree context, PredicatePrecision otherPrecision, CachedSubtree otherContext, RelevantPredicatesComputer relevantPredicatesComputer, CachedSubtreeManager csmgr) {
   if(!context.equals(otherContext))
     return false;
   
   Set<CFANode> functionNodes = context.getNodes();   
   
   Collection<AbstractionPredicate> globalPreds1 = relevantPredicatesComputer.getRelevantPredicates(context, precision.getGlobalPredicates());
   Collection<AbstractionPredicate> globalPreds2 = relevantPredicatesComputer.getRelevantPredicates(otherContext, otherPrecision.getGlobalPredicates());
   if(!globalPreds1.equals(globalPreds2)) {
     return false;
   }
   
   for(CFANode node : functionNodes) {
     if(precision.getPredicateMap().keySet().contains(node) || otherPrecision.getPredicateMap().keySet().contains(node)) {
       Collection<AbstractionPredicate> set1 = precision.getPredicates(node);
       Collection<AbstractionPredicate> set2 = otherPrecision.getPredicates(node);
       if(csmgr.isCallNode(node) || csmgr.isReturnNode(node)) {
         set1 = relevantPredicatesComputer.getRelevantPredicates(context, precision.getPredicates(node));
         set2 = relevantPredicatesComputer.getRelevantPredicates(otherContext, otherPrecision.getPredicates(node)); 
       } 
       
       if(!set1.equals(set2)) {
         return false;
       }
     }          
   }
   return true;
  }  
  
  public static int relevantComputeHashCode(PredicatePrecision precision, CachedSubtree context, RelevantPredicatesComputer relevantPredicatesComputer, CachedSubtreeManager csmgr) {   
    int h = 1;
    Set<CFANode> functionNodes = context.getNodes();
    
    Set<AbstractionPredicate> globalPredicates = precision.getGlobalPredicates();
    globalPredicates = (Set<AbstractionPredicate>) relevantPredicatesComputer.getRelevantPredicates(context, globalPredicates);
    h += globalPredicates.hashCode();
    
    for(CFANode node : precision.getPredicateMap().keySet()) {
      if(functionNodes.contains(node)) {
        Collection<AbstractionPredicate> set = precision.getPredicates(node);
        if(csmgr.isCallNode(node) || csmgr.isReturnNode(node)) {
          set = relevantPredicatesComputer.getRelevantPredicates(context, set);
        }
        
        for(AbstractionPredicate predicate : set) {
          if(!globalPredicates.contains(predicate))
            h += set.hashCode();
        }
      }
    }
    return h;
  }
}
