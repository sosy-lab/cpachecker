/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import programtesting.simple.AcyclicPathProgramExtractor.AcyclicPathProgram;
import programtesting.simple.Graph.Edge;
import programtesting.simple.QDPTCompositeCPA.CFAEdgeEdge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 *
 * @author holzera
 */
public class InfeasibleElementsUtility {
  /*
   * Given an infeasible acyclic path program we return an underapproximation of
   * the infeasible elements.
   */
  public static Set<QDPTCompositeElement> getInfeasibleElements(AcyclicPathProgram pProgram) {
    assert(pProgram != null);

    HashSet<QDPTCompositeElement> lInfeasibleElements = new HashSet<QDPTCompositeElement>();

    // initialize infeasible elements with final element (which has to be surely
    // infeasible since the whole program is infeasible
    lInfeasibleElements.add(pProgram.getFinalElement());


    Graph<QDPTCompositeElement, CFAEdgeEdge> lDAG = pProgram.getDAG();


    LinkedList<QDPTCompositeElement> lWorklist = new LinkedList<QDPTCompositeElement>();

    lWorklist.add(pProgram.getFinalElement());

    while (!lWorklist.isEmpty()) {
      // get and remove first element from worklist
      QDPTCompositeElement lElement = lWorklist.poll();

      for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lEdge : lDAG.getIncomingEdges(lElement)) {
        CFAEdgeEdge lCFAEdgeEdge = lEdge.getAnnotation();

        CFAEdge lCFAEdge = lCFAEdgeEdge.getCFAEdge();

        if (!lCFAEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
          lWorklist.add(lEdge.getSource());
        }

        // Here, we assume that at some point every "merged" target gets caught
        // by some edge (TODO formulate more precisely)
        lInfeasibleElements.add(lCFAEdgeEdge.getChild());

        // Is this really necessary ? There is predicate refinement on infeasible
        // test goals
        if (lElement != lCFAEdgeEdge.getChild()) {
          // TODO implement leafs and special predicate refinement
          System.out.println("Special predicate refinement necessary!");
        }
      }
    }

    
    // TODO remove
    if (lInfeasibleElements.size() > 1) {
      System.out.println(lInfeasibleElements);
    }


    return lInfeasibleElements;
  }
}
