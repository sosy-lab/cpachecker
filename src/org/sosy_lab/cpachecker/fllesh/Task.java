package org.sosy_lab.cpachecker.fllesh;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp.CoverageSpecificationTranslator;

public class Task implements Iterable<ElementaryCoveragePattern> {

  private Set<ElementaryCoveragePattern> mTestGoals;
  private ElementaryCoveragePattern mPassingClause;
  
  public Task(Set<ElementaryCoveragePattern> pTestGoals) {
    mTestGoals = pTestGoals;
    mPassingClause = null;
  }
  
  public Task(Set<ElementaryCoveragePattern> pTestGoals, ElementaryCoveragePattern pPassingClause) {
    this(pTestGoals);
    mPassingClause = pPassingClause;
  }
  
  public int getNumberOfTestGoals() {
    return mTestGoals.size();
  }
  
  public boolean hasPassingClause() {
    return (mPassingClause != null);
  }

  @Override
  public Iterator<ElementaryCoveragePattern> iterator() {
    return mTestGoals.iterator();
  }
  
  public ElementaryCoveragePattern getPassingClause() {
    return mPassingClause;
  }
  
  public static Task create(FQLSpecification pSpecification, CFANode pInitialNode) {
    /*Set<CFAEdge> lBasicBlockEntries = TargetGraphUtil.getBasicBlockEntries(pInitialNode);
    
    TargetGraph lTargetGraph = TargetGraphUtil.cfa(pInitialNode);
    
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(lTargetGraph, lBasicBlockEntries);*/
    CoverageSpecificationTranslator lSpecificationTranslator = new CoverageSpecificationTranslator(pInitialNode);
    Set<ElementaryCoveragePattern> lGoals = lSpecificationTranslator.translate(pSpecification.getCoverageSpecification());
    
    if (pSpecification.hasPassingClause()) {
      ElementaryCoveragePattern lPassing = lSpecificationTranslator.translate(pSpecification.getPathPattern());
      
      return new Task(lGoals, lPassing);
    }
    else {
      return new Task(lGoals);
    }
  }
  
  public Queue<Goal> toGoals(Wrapper pWrapper) {
    LinkedList<Goal> lGoals = new LinkedList<Goal>();
    
    for (ElementaryCoveragePattern lGoalPattern : this) {
      Goal lGoal = new Goal(lGoalPattern, pWrapper);
      lGoals.add(lGoal);
    }
    
    return lGoals;
  }
  
}
