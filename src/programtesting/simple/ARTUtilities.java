/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.testgoal.TestGoalCPA;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import programtesting.simple.QDPTCompositeCPA.Edge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 *
 * @author holzera
 */
public class ARTUtilities {
  public static Set<QDPTCompositeElement> getReachedElements(Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    HashSet<QDPTCompositeElement> lReachedElements = new HashSet<QDPTCompositeElement>();
    
    for (QDPTCompositeElement lInitialElement : pInitialElementsMap.keySet()) {
      accumulateReachedElements(lInitialElement, lReachedElements);
    }
    
    return lReachedElements;
  }
  
  public static void accumulateReachedElements(QDPTCompositeElement pElement, Set<QDPTCompositeElement> pReachedElements) {
    assert(pElement != null);
    assert(pReachedElements != null);
    
    pReachedElements.add(pElement);
    
    for (Edge lEdge : pElement.getChildren()) {
      accumulateReachedElements(lEdge.getChild(), pReachedElements);
    }
  }
  
  public static void propagate(QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    if (isPropagateable(pElement)) {
      // pElement will be no initial element anymore
      pInitialElementsMap.remove(pElement);
    }
    else {
      if (!pInitialElementsMap.containsKey(pElement)) {
        // set as initial element
      
        Set<CFAEdge> lEdges = getOutgoingCFAEdges(pElement);
        
        lEdges.removeAll(getVisitedCFAEdges(pElement));
                
        pInitialElementsMap.put(pElement, lEdges);
      }
    }
    
    for (Edge lEdge : pElement.getChildren()) {
      propagate(lEdge.getChild(), pInitialElementsMap);
    }
  }
  
  public static boolean isPropagateable(QDPTCompositeElement pElement) {
    assert(pElement != null);

    if (pElement.getNumberOfChildren() == 0) {
      return false;
    }
    
    CFANode lCFANode = pElement.getLocationNode();
    
    return (lCFANode.getNumLeavingEdges() == getVisitedCFAEdges(pElement).size());
  }

  public static Set<CFAEdge> getOutgoingCFAEdges(QDPTCompositeElement pElement) {
    assert(pElement != null);
    
    return getOutgoingCFAEdges(pElement.getLocationNode());
  }
  
  public static Set<CFAEdge> getOutgoingCFAEdges(CFANode pCFANode) {
    assert(pCFANode != null);
    
    HashSet<CFAEdge> lOutgoingEdges = new HashSet<CFAEdge>();

    for (int lIndex = 0; lIndex < pCFANode.getNumLeavingEdges(); lIndex++) {
      lOutgoingEdges.add(pCFANode.getLeavingEdge(lIndex));
    }

    return lOutgoingEdges;
  }
  
  public static Set<CFAEdge> getVisitedCFAEdges(QDPTCompositeElement pElement) {
    assert(pElement != null);
    
    HashSet<CFAEdge> lVisitedCFAEdges = new HashSet<CFAEdge>();
    
    for (Edge lEdge : pElement.getChildren()) {
      addVisitedCFAEdges(lEdge, lVisitedCFAEdges);
    }
    
    return lVisitedCFAEdges;
  }
  
  private static void addVisitedCFAEdges(Edge pEdge, Set<CFAEdge> pVisitedCFAEdges) {
    assert(pEdge != null);
    assert(pVisitedCFAEdges != null);
    
    if (pEdge instanceof QDPTCompositeCPA.CFAEdgeEdge) {
      pVisitedCFAEdges.add(((QDPTCompositeCPA.CFAEdgeEdge) pEdge).getCFAEdge());
    } 
    else {
      assert (pEdge instanceof QDPTCompositeCPA.SubpathsEdge);

      QDPTCompositeCPA.SubpathsEdge lSubpathsEdge = (QDPTCompositeCPA.SubpathsEdge) pEdge;

      for (List<Edge> lSubpath : lSubpathsEdge.getSubpaths()) {
        Edge lFirstEdge = lSubpath.get(0);
        
        addVisitedCFAEdges(lFirstEdge, pVisitedCFAEdges);
      }
    }
  }
  
  public static boolean mergeAtElement(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pCPA != null);
    assert(pTestGoalCPA != null);
    assert(pElement != null);
    assert(pInitialElementsMap != null);
    
    // ensure that pElement is really a candidate for merging
    assert(pElement.getNumberOfChildren() > 1);
    
    boolean lMergeDone = false;
    
    ArrayList<Edge> lChildren = new ArrayList<Edge>();

    for (Edge lEdge : pElement.getChildren()) {
      lChildren.add(lEdge);
    }

    for (int lOuterPathIndex = 0; lOuterPathIndex < lChildren.size() - 1; lOuterPathIndex++) {
      Edge lOuterPathEdge = lChildren.get(lOuterPathIndex);

      LinkedList<Edge> lOuterPath = getSubpath(lOuterPathEdge, pInitialElementsMap);

      if (lOuterPath.size() > 1) {
        for (int lInnerPathIndex = lOuterPathIndex + 1; lInnerPathIndex < lChildren.size(); lInnerPathIndex++) {
          Edge lInnerPathEdge = lChildren.get(lInnerPathIndex);

          // TODO this can be precalculated
          LinkedList<Edge> lInnerPath = getSubpath(lInnerPathEdge, pInitialElementsMap);

          if (lInnerPath.size() > 1) {
            // TODO use different data structure
            Vector<LinkedList<Edge>> lPaths = new Vector<LinkedList<Edge>>(2);

            lPaths.add(lOuterPath);
            lPaths.add(lInnerPath);

            Set<List<Edge>> lMergeSubpaths = getMergeSubpaths(pTestGoalCPA, lPaths);

            if (lMergeSubpaths.size() > 1) {
              QDPTCompositeElement lMergeElement = merge(pCPA, pTestGoalCPA, pElement, lMergeSubpaths, pInitialElementsMap);

              if (pInitialElementsMap.containsKey(lMergeElement)) {
                propagate(lMergeElement, pInitialElementsMap);
              }

              lMergeDone = true;

              int lOldSize = lChildren.size();

              // TODO optimize this
              lChildren.clear();

              for (Edge lEdge : pElement.getChildren()) {
                lChildren.add(lEdge);
              }

              assert (lOldSize - 1 == lChildren.size());

              lOuterPathIndex = 0;

              break;
            }
          }
        }
      }
    }
    
    return lMergeDone;
  }
  
  public static void mergePathsRevisited(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pCPA != null);
    assert(pTestGoalCPA != null);
    assert(pElement != null);
    assert(pInitialElementsMap != null);
    
    
    Map<QDPTCompositeElement, Iterator<Edge>> lRemainingChildren = new HashMap<QDPTCompositeElement, Iterator<Edge>>();
    
    LinkedList<QDPTCompositeElement> lWorklist = new LinkedList<QDPTCompositeElement>();
    
    lWorklist.add(pElement);
    
    QDPTCompositeElement lElementBeforeBacktracking = null;
    
    while (!lWorklist.isEmpty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.getFirst();
      
      if (lRemainingChildren.containsKey(lCurrentElement) && (lElementBeforeBacktracking == null)) {
              
        Iterator<Edge> lIterator = lRemainingChildren.get(lCurrentElement);
        
        if (lIterator.hasNext()) {
          lWorklist.addFirst(lIterator.next().getChild());
        }
        else {
          // TODO is this necessary, does it harm?
          lRemainingChildren.remove(lCurrentElement);
          
          lWorklist.removeFirst();
        }
        
        System.out.println(lWorklist);
      } else if (lCurrentElement.getNumberOfChildren() == 1) {
        assert(lElementBeforeBacktracking == null);
        
        lWorklist.removeFirst();
        
        lWorklist.addFirst(lCurrentElement.getChildren().iterator().next().getChild());
      }
      else if (lCurrentElement.getNumberOfChildren() > 1) {
        //lCurrentElement is a candidate for a merging point

        if (mergeAtElement(pCPA, pTestGoalCPA, lCurrentElement, pInitialElementsMap)) {
          // we merged something
          
          if (lElementBeforeBacktracking != null) {
            lElementBeforeBacktracking = null;
            lRemainingChildren.remove(lCurrentElement);
          }
          
          if (lCurrentElement.getNumberOfChildren() == 1) {

            QDPTCompositeElement lBacktrackCandidate = lCurrentElement; // is equivalent to lWorklist.getFirst();

            while (!isBacktrackElement(lBacktrackCandidate, pInitialElementsMap)) {
              lBacktrackCandidate = lBacktrackCandidate.getParent();
            }

            if (lBacktrackCandidate != lCurrentElement) {
              assert(lWorklist.get(1) == lBacktrackCandidate);

              lWorklist.removeFirst();

              lElementBeforeBacktracking = lCurrentElement;
            }
          }
        }
        else {
          // we have not merged anything
          
          if (lElementBeforeBacktracking == null) {
            assert(!lRemainingChildren.containsKey(lCurrentElement));

            lRemainingChildren.put(lCurrentElement, lCurrentElement.getChildren().iterator());
          }
          else {
            lWorklist.addFirst(lElementBeforeBacktracking);
            
            lElementBeforeBacktracking = null;
          }
        }
      }
      else {
        // no successors
        
        lWorklist.removeFirst();
      }
    }
  }
  
  public static boolean mergePaths(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pCPA != null);
    assert(pTestGoalCPA != null);
    assert(pElement != null);
    assert(pInitialElementsMap != null);
    
    boolean lARTHasBeenUpdated = false;
    
    if (pElement.getNumberOfChildren() > 1) {
      // pElement is a candidate for a merging point
      
      ArrayList<Edge> lChildren = new ArrayList<Edge>();
      
      for (Edge lEdge : pElement.getChildren()) {
        lChildren.add(lEdge);
      }
      
      for (int lOuterPathIndex = 0; lOuterPathIndex < lChildren.size() - 1; lOuterPathIndex++) {
        Edge lOuterPathEdge = lChildren.get(lOuterPathIndex);
        
        LinkedList<Edge> lOuterPath = getSubpath(lOuterPathEdge, pInitialElementsMap);
        
        if (lOuterPath.size() > 1) {
          for (int lInnerPathIndex = lOuterPathIndex + 1; lInnerPathIndex < lChildren.size(); lInnerPathIndex++) {
            Edge lInnerPathEdge = lChildren.get(lInnerPathIndex);

            // TODO this can be precalculated
            LinkedList<Edge> lInnerPath = getSubpath(lInnerPathEdge, pInitialElementsMap);

            if (lInnerPath.size() > 1) {
              // TODO use different data structure
              Vector<LinkedList<Edge>> lPaths = new Vector<LinkedList<Edge>>(2);
              
              lPaths.add(lOuterPath);
              lPaths.add(lInnerPath);
              
              Set<List<Edge>> lMergeSubpaths = getMergeSubpaths(pTestGoalCPA, lPaths);
        
              if (lMergeSubpaths.size() > 1) {
                QDPTCompositeElement lMergeElement = merge(pCPA, pTestGoalCPA, pElement, lMergeSubpaths, pInitialElementsMap);

                if (pInitialElementsMap.containsKey(lMergeElement)) {
                  propagate(lMergeElement, pInitialElementsMap);
                }

                lARTHasBeenUpdated = true;
                
                int lOldSize = lChildren.size();
                
                // TODO optimize this
                lChildren.clear();
                
                for (Edge lEdge : pElement.getChildren()) {
                  lChildren.add(lEdge);
                }
                
                assert(lOldSize - 1 == lChildren.size());
                        
                lOuterPathIndex = 0;
                
                break;
              }
            }
          }
        }        
      }
    }
    
    
    if (lARTHasBeenUpdated) {
      // we have merged and maybe we are able to merge at a higher point again,
      // so let's try backtracking
      
      QDPTCompositeElement lBacktrackElement = getBacktrackElement(pElement, pInitialElementsMap);
      
      mergePaths(pCPA, pTestGoalCPA, lBacktrackElement, pInitialElementsMap);
    }
    else {
      /* we have not merged anything, so continue at children */
      Set<QDPTCompositeElement> lChildren = new HashSet<QDPTCompositeElement>(pElement.getNumberOfChildren());
      
      for (Edge lEdge : pElement.getChildren()) {
        lChildren.add(lEdge.getChild());
      }
      
      for (QDPTCompositeElement lChild : lChildren) {
        // merge paths in subtrees
        lARTHasBeenUpdated |= mergePaths(pCPA, pTestGoalCPA, lChild, pInitialElementsMap);
        
        assert(lChildren.size() == pElement.getNumberOfChildren());
      }
    }
    
    
    return lARTHasBeenUpdated;
  }
  
  public static boolean isBacktrackElement(QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pElement != null);
    assert(pInitialElementsMap != null);
    
    /* root ? */
    if (!pElement.hasParent()) {
      return true;
    }
    
    /* initial element ? */
    if (pInitialElementsMap.containsKey(pElement)) {
      return true;
    }
    
    /* branching in ART ? */
    if (pElement.getNumberOfChildren() > 1) {
      return true;
    }
    
    /* we know there is a parent */
    QDPTCompositeElement lParent = pElement.getParent();
    
    /* change in callstack ? */
    if (!pElement.getCallStack().equals(lParent.getCallStack())) {
      return true;
    }
    
    return false;
  }
  
  public static QDPTCompositeElement getBacktrackElement(QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pElement != null);
    assert(pInitialElementsMap != null);
    
    /* root ? */
    if (!pElement.hasParent()) {
      return pElement;
    }
    
    /* initial element ? */
    if (pInitialElementsMap.containsKey(pElement)) {
      return pElement;
    }
    
    /* branching in ART ? */
    if (pElement.getNumberOfChildren() > 1) {
      return pElement;
    }
    
    /* we know there is a parent */
    QDPTCompositeElement lParent = pElement.getParent();
    
    /* change in callstack ? */
    if (!pElement.getCallStack().equals(lParent.getCallStack())) {
      return pElement;
    }
    
    return getBacktrackElement(lParent, pInitialElementsMap);
  }
  
  public static LinkedList<Edge> getSubpath(Edge pEdge, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pEdge != null);
    assert(pInitialElementsMap != null);
    
    LinkedList<Edge> lPath = new LinkedList<Edge>();
    
    Edge lCurrentEdge = pEdge;
    
    do {
      QDPTCompositeElement lParent = lCurrentEdge.getParent();
      QDPTCompositeElement lChild = lCurrentEdge.getChild();

      // we do only intraprocedural merging
      // TODO allow interprocedural merging
      if (!lParent.getCallStack().equals(lChild.getCallStack())) {
        break;
      }

      lPath.add(lCurrentEdge);

      // new merge candidate
      if (lChild.getNumberOfChildren() != 1) {
        break;
      }

      if (pInitialElementsMap.containsKey(lChild)) {
        break;
      }

      // we know there is only one successor edge
      lCurrentEdge = lChild.getChildren().iterator().next();
    } while (true);
    
    return lPath;
  }
  
  public static QDPTCompositeElement merge(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement, Set<List<Edge>> pSubpaths, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pCPA != null);
    assert(pTestGoalCPA != null);
    assert(pElement != null);
    assert(pSubpaths != null);
    assert(pSubpaths.size() > 1);
    assert(pInitialElementsMap != null);
    
    List<Edge> lFirstSubpath = pSubpaths.iterator().next();
    
    assert(lFirstSubpath != null);
    
    Edge lLastEdge = lFirstSubpath.get(lFirstSubpath.size() - 1);
    
    QDPTCompositeElement lLastElement = lLastEdge.getChild();
    
    
    
    // create merge element
    List<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();
    // location cpa
    lAbstractElements.add(lLastElement.get(QueryDrivenProgramTesting.mLocationCPAIndex));
    // scope restriction cpa
    // TODO this has to be changed to handle scope restriction analysis correctly!
    lAbstractElements.add(lLastElement.get(QueryDrivenProgramTesting.mScopeRestrictionCPAIndex));
    // test goal cpa
    Set<Automaton<CFAEdge>.State> lNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();

    for (Automaton<CFAEdge>.State lState : pTestGoalCPA.getAbstractDomain().castToStateSetElement(lLastElement.get(QueryDrivenProgramTesting.mTestGoalCPAIndex)).getStates()) {
      if (!lState.isFinal()) {
        lNonAcceptingStates.add(lState);
      }
    }

    AutomatonCPADomain<CFAEdge>.StateSetElement lStateSetElement = pTestGoalCPA.getAbstractDomain().createStateSetElement(pTestGoalCPA.getAbstractDomain(), lNonAcceptingStates);
    lAbstractElements.add(lStateSetElement);

    //pElement.hideChildren();
    // TODO problem here
    // we know we have only two children that we merge here
    for (List<Edge> lSubpath : pSubpaths) {
      Edge lEdge = lSubpath.get(0);
      
      pElement.hideChild(lEdge);
    }
    
    QDPTCompositeElement lMergeElement = pCPA.createElement(lAbstractElements, pElement.getCallStack(), pElement, pSubpaths);

    Set<Edge> lEdgeSet = new HashSet<Edge>();

    
    for (List<Edge> lSubpath : pSubpaths) {
      Edge lEdge = lSubpath.get(lSubpath.size() - 1);
      
      QDPTCompositeElement lLastSubpathElement = lEdge.getChild();
      
      for (Edge lChildEdge : lLastSubpathElement.getChildren()) {
        lEdgeSet.add(lChildEdge);
      }
      
      // handle initial elements
      if (pInitialElementsMap.containsKey(lLastSubpathElement)) {
        Set<CFAEdge> lCFAEdges = pInitialElementsMap.get(lLastSubpathElement);
        
        pInitialElementsMap.remove(lLastSubpathElement);
        
        if (pInitialElementsMap.containsKey(lMergeElement)) {
          Set<CFAEdge> lCurrentCFAEdges = pInitialElementsMap.get(lMergeElement);
          
          lCurrentCFAEdges.addAll(lCFAEdges);
        }
        else {
          pInitialElementsMap.put(lMergeElement, lCFAEdges);
        }
      }
    }
          
    for (Edge lEdge : lEdgeSet) {
      lEdge.setParent(lMergeElement);
    }
    
    // merging could invalidate our minimality invariant for inital elements
    if (pInitialElementsMap.containsKey(lMergeElement)) {
      // we have to ensure that we will only visit previously unvisited edges
      Set<CFAEdge> lCFAEdges = pInitialElementsMap.get(lMergeElement);
      
      Set<CFAEdge> lVisitedCFAEdges = getVisitedCFAEdges(lMergeElement);
      
      lCFAEdges.removeAll(lVisitedCFAEdges);
      
      // NOTE: If there is no edge remaining we still need to have lMergeElements
      // in pInitialElementsMap for later propagation of initial elements status.
    }
    
    return lMergeElement;
  }

  public static boolean areElementsMergeable(TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement1, QDPTCompositeElement pElement2) {
    assert(pElement1 != null);
    assert(pElement2 != null);
    
    // TODO include equivalence test for scope restriction automaton
    
    if (!(pElement1.get(QueryDrivenProgramTesting.mLocationCPAIndex).equals(pElement2.get(QueryDrivenProgramTesting.mLocationCPAIndex)))) {
      return false;
    }
    
    if (!pElement1.getCallStack().equals(pElement2.getCallStack())) {
      return false;
    }

    
    AutomatonCPADomain<CFAEdge>.StateSetElement lCurrentStateSetElement = pTestGoalCPA.getAbstractDomain().castToStateSetElement(pElement1.get(QueryDrivenProgramTesting.mTestGoalCPAIndex));
    Set<Automaton<CFAEdge>.State> lCurrentStates = lCurrentStateSetElement.getStates();
    // TODO: provide this sets by automaton or test goal cpa element
    Set<Automaton<CFAEdge>.State> lCurrentNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();

    for (Automaton<CFAEdge>.State lState : lCurrentStates) {
      if (!lState.isFinal()) {
        lCurrentNonAcceptingStates.add(lState);
      }
    }

    
    AutomatonCPADomain<CFAEdge>.StateSetElement lCandidateStateSetElement = pTestGoalCPA.getAbstractDomain().castToStateSetElement(pElement2.get(QueryDrivenProgramTesting.mTestGoalCPAIndex));
    Set<Automaton<CFAEdge>.State> lCandidateStates = lCandidateStateSetElement.getStates();
    Set<Automaton<CFAEdge>.State> lCandidateNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();

    for (Automaton<CFAEdge>.State lState : lCandidateStates) {
      if (!lState.isFinal()) {
        lCandidateNonAcceptingStates.add(lState);
      }
    }
    
          
    return lCurrentNonAcceptingStates.equals(lCandidateNonAcceptingStates);
  }
  
  public static Set<List<Edge>> getMergeSubpaths(TestGoalCPA pTestGoalCPA, Vector<LinkedList<Edge>> pPaths) {
    assert(pPaths != null);
    
    
    // detect merging points
    LinkedList<QDPTCompositeElement[]>  lMatchingElementsTuples = new LinkedList<QDPTCompositeElement[]>();
    
    LinkedList<Edge> lFirstPath = pPaths.get(0);
    
    for (Edge lCurrentEdge : lFirstPath) {
      QDPTCompositeElement lCurrentElement = lCurrentEdge.getChild();
      
      QDPTCompositeElement[] lMatchingElementsTuple = new QDPTCompositeElement[pPaths.size()];
      
      lMatchingElementsTuple[0] = lCurrentElement;
      
      boolean lOk = true;
      
      for (int lPathIndex = 1; lPathIndex < pPaths.size(); lPathIndex++) {
        LinkedList<Edge> lCurrentPath = pPaths.get(lPathIndex);
        
        for (Edge lCandidateEdge : lCurrentPath) {
          QDPTCompositeElement lCandidateElement = lCandidateEdge.getChild();
          
          if (areElementsMergeable(pTestGoalCPA, lCurrentElement, lCandidateElement)) {
            lMatchingElementsTuple[lPathIndex] = lCandidateElement;
            
            break;
          }
        }
        
        if (lMatchingElementsTuple[lPathIndex] == null) {
          lOk = false;
          
          break;
        }
      }
      
      if (lOk) {
        lMatchingElementsTuples.add(lMatchingElementsTuple);
      }
    }
    
    
    if (lMatchingElementsTuples.size() <= 0) {
      // nothing to merge so return
      return Collections.EMPTY_SET;
    }
    
    
    int lMinDepth = Integer.MAX_VALUE;

    QDPTCompositeElement[] lFinalTuple = null;

    for (QDPTCompositeElement[] lTuple : lMatchingElementsTuples) {
      int lCurrentMinDepth = Integer.MAX_VALUE;

      for (QDPTCompositeElement lElement : lTuple) {
        int lTmpDepth = lElement.getDepth();

        if (lElement.getDepth() < lCurrentMinDepth) {
          lCurrentMinDepth = lTmpDepth;
        }
      }

      if (lMinDepth > lCurrentMinDepth) {
        lFinalTuple = lTuple;
        lMinDepth = lCurrentMinDepth;
      }
    }

    assert (lFinalTuple != null);
            
    
    // determine paths
    HashSet<List<Edge>> lMergePaths = new HashSet<List<Edge>>();    

    for (int lMergePathIndex = 0; lMergePathIndex < pPaths.size(); lMergePathIndex++) {
      LinkedList<Edge> lTmpPath = new LinkedList<Edge>();

      for (Edge lMergeEdge : pPaths.get(lMergePathIndex)) {
        if (lMergeEdge.getChild().equals(lFinalTuple[lMergePathIndex])) {
          lTmpPath.add(lMergeEdge);

          break;
        }

        lTmpPath.addLast(lMergeEdge);
      }

      lMergePaths.add(lTmpPath);
    }
    
    return lMergePaths;
  }
}
