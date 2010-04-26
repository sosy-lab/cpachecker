package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.TestGoal;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.TestGoalVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;

public class Node implements TestGoal {
  
  private CFANode mCFANode;
  
  private ArrayList<Predicate> mPredicates;
  private ArrayList<Boolean> mEvaluation;
  
  // TODO: add reference to list of predicates (or predicate map)
  // TODO: add evaluation of predicates
  
  
  public Node(CFANode pCFANode) {
    assert(pCFANode != null);
    
    mCFANode = pCFANode;
    
    mPredicates = new ArrayList<Predicate>();
    mEvaluation = new ArrayList<Boolean>();
  }
  
  public Node(Node pNode) {
    assert(pNode != null);

    mCFANode = pNode.mCFANode;
    
    mPredicates = new ArrayList<Predicate>(pNode.mPredicates);
    mEvaluation = new ArrayList<Boolean>(pNode.mEvaluation); 
  }
  
  public void addPredicate(Predicate pPredicate, Boolean pEvaluation) {
    assert(pPredicate != null);
    assert(pEvaluation != null);
    
    // TODO check whether pPredicate is already in mPredicates?
    
    mPredicates.add(pPredicate);
    mEvaluation.add(pEvaluation);
  }
  
  public List<Predicate> getPredicates() {
    return mPredicates;
  }
  
  public boolean getEvaluation(int pIndex) {
    assert(pIndex >= 0);
    assert(pIndex < mPredicates.size());
    
    return mEvaluation.get(pIndex);
  }
  
  public CFANode getCFANode() {
    return mCFANode;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Node lNode = (Node)pOther;
      
      return lNode.mCFANode == mCFANode && lNode.mPredicates.equals(mPredicates) && lNode.mEvaluation.equals(mEvaluation);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 293421 + mCFANode.hashCode() + mPredicates.hashCode() + mEvaluation.hashCode();
  }
  
  @Override
  public String toString() {
    return "(cfa node: " + mCFANode.toString() + ", predicates: " + mPredicates.toString() + ", evaluation: " + mEvaluation.toString() + ")";
  }
 
  @Override
  public <T> T accept(TestGoalVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }
  
}
