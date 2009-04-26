/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;
import programtesting.simple.QDPTCompositeCPA.CFAEdgeEdge;
import programtesting.simple.QDPTCompositeCPA.Edge;

/**
 *
 * @author holzera
 */
public class AcyclicPathProgramExtractor {
  public static class AcyclicPathProgram {
    private Graph<QDPTCompositeElement, CFAEdgeEdge> mDAGRepresentation;
    private Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> mBasicBlockGraph;
    private BasicBlock mInitialBasicBlock;
    private Map<QDPTCompositeElement, BasicBlock> mElementToBasicBlockMap;
    private Map<Integer, BasicBlock> mIdToBasicBlockMap;
    private Map<FunctionDefinitionNode, TreeSet<BasicBlock>> mFunctionSeparatedBasicBlocks;
    private Map<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge, Integer> mEdgesBetweenBasicBlocksToIdMap;
    private Map<Integer, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge> mIdToEdgesBetweenBasicBlocksMap;
    private CFAMap mCFAMap;

    private AcyclicPathProgram() {
      
    }

    private void setDAG(Graph<QDPTCompositeElement, CFAEdgeEdge> pDAG) {
      assert(pDAG != null);
      assert(mDAGRepresentation == null);

      mDAGRepresentation = pDAG;
    }

    public Graph<QDPTCompositeElement, CFAEdgeEdge> getDAG() {
      assert(mDAGRepresentation != null);

      return mDAGRepresentation;
    }

    private void setBasicBlockGraph(Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> pBasicBlocksGraph) {
      assert(pBasicBlocksGraph != null);
      assert(mBasicBlockGraph == null);

      mBasicBlockGraph = pBasicBlocksGraph;
    }

    public Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> getBasicBlockGraph() {
      return mBasicBlockGraph;
    }

    private void setInitialBasicBlock(BasicBlock pInitialBasicBlock) {
      assert(pInitialBasicBlock != null);

      mInitialBasicBlock = pInitialBasicBlock;
    }

    public BasicBlock getInitialBasicBlock() {
      assert(mInitialBasicBlock != null);

      return mInitialBasicBlock;
    }

    public Set<BasicBlock> getBasicBlocks() {
      assert(mBasicBlockGraph != null);

      return mBasicBlockGraph.getNodes();
    }

    public void setElementToBasicBlockMap(Map<QDPTCompositeElement, BasicBlock> pElementToBasicBlockMap) {
      assert(pElementToBasicBlockMap != null);
      assert(mElementToBasicBlockMap == null);

      mElementToBasicBlockMap = pElementToBasicBlockMap;
    }

    public Map<QDPTCompositeElement, BasicBlock> getElementToBasicBlockMap() {
      assert(mElementToBasicBlockMap != null);

      return mElementToBasicBlockMap;
    }

    public void setIdToBasicBlockMap(Map<Integer, BasicBlock> pIdToBasicBlockMap) {
      assert(pIdToBasicBlockMap != null);
      assert(mIdToBasicBlockMap == null);

      mIdToBasicBlockMap = pIdToBasicBlockMap;
    }

    public Map<Integer, BasicBlock> getIdToBasicBlockMap() {
      assert(mIdToBasicBlockMap != null);

      return mIdToBasicBlockMap;
    }

    public void setFunctionSeparatedBasicBlocks(Map<FunctionDefinitionNode, TreeSet<BasicBlock>> pFunctionSeparatedBasicBlocks) {
      assert(pFunctionSeparatedBasicBlocks != null);
      assert(mFunctionSeparatedBasicBlocks == null);

      mFunctionSeparatedBasicBlocks = pFunctionSeparatedBasicBlocks;
    }

    public Map<FunctionDefinitionNode, TreeSet<BasicBlock>> getFunctionSeparatedBasicBlocks() {
      assert(mFunctionSeparatedBasicBlocks != null);

      return mFunctionSeparatedBasicBlocks;
    }

    public CFAMap getCFAMap() {
      assert(mCFAMap != null);

      return mCFAMap;
    }

    private void setCFAMap(CFAMap pCFAMap) {
      assert(pCFAMap != null);
      assert(mCFAMap == null);

      mCFAMap = pCFAMap;
    }

    private void setEdgesBetweenBasicBlocksToIdMap(Map<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge, Integer> pEdgesBetweenBasicBlocksToIndexMap) {
      assert(pEdgesBetweenBasicBlocksToIndexMap != null);
      assert(mEdgesBetweenBasicBlocksToIdMap == null);

      mEdgesBetweenBasicBlocksToIdMap = pEdgesBetweenBasicBlocksToIndexMap;
    }

    public Map<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge, Integer> getEdgesBetweenBasicBlocksToIdMap() {
      assert(mEdgesBetweenBasicBlocksToIdMap != null);
      
      return mEdgesBetweenBasicBlocksToIdMap;
    }

    private void setIdToEdgesBetweenBasicBlocksMap(Map<Integer, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge> pIdToEdgesBetweenBasicBlocksMap) {
      assert(pIdToEdgesBetweenBasicBlocksMap != null);
      assert(mIdToEdgesBetweenBasicBlocksMap == null);

      mIdToEdgesBetweenBasicBlocksMap = pIdToEdgesBetweenBasicBlocksMap;
    }

    public Map<Integer, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge> getIdToEdgesBetweenBasicBlocksMap() {
      assert(mIdToEdgesBetweenBasicBlocksMap != null);

      return mIdToEdgesBetweenBasicBlocksMap;
    }
  }

  public static AcyclicPathProgram extract(QDPTCompositeElement pRootOfART, QDPTCompositeElement pTargetElement, CFAMap pCFAs) {
    assert(pRootOfART != null);
    assert(pTargetElement != null);
    assert(pCFAs != null);


    AcyclicPathProgram lAcyclicPathProgram = new AcyclicPathProgram();


    // I) extract DAG
    Graph<QDPTCompositeElement, CFAEdgeEdge> lDAG = extractDAG(pRootOfART, pTargetElement);
    lAcyclicPathProgram.setDAG(lDAG);


    // II) create basic block graph
    Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> lBasicBlocks = new Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>();

    BasicBlock lFirstBasicBlock = new BasicBlock(pRootOfART);
    lBasicBlocks.addNode(lFirstBasicBlock);

    HashMap<QDPTCompositeElement, BasicBlock> lBasicBlockMap = new HashMap<QDPTCompositeElement, BasicBlock>();
    lBasicBlockMap.put(pRootOfART, lFirstBasicBlock);

    LinkedList<BasicBlock> lBasicBlockWorklist = new LinkedList<BasicBlock>();
    lBasicBlockWorklist.add(lFirstBasicBlock);

    HashSet<BasicBlock> lVisitedBlocks = new HashSet<BasicBlock>();

    while (!lBasicBlockWorklist.isEmpty()) {
      BasicBlock lBasicBlock = lBasicBlockWorklist.removeFirst();

      if (lVisitedBlocks.contains(lBasicBlock)) {
        continue;
      }

      lVisitedBlocks.add(lBasicBlock);

      boolean lFinishLoop = false;

      while (!lFinishLoop) {
        QDPTCompositeElement lLastElement = lBasicBlock.getLastElement();

        Set<Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> lOutgoingDAGEdges = lDAG.getOutgoingEdges(lLastElement);

        switch (lOutgoingDAGEdges.size()) {
          case 0: {
            // no successor, so we are done

            lFinishLoop = true;

            break;
          }
          case 1: {
            // exaclty one successor

            Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lEdge = lOutgoingDAGEdges.iterator().next();

            CFAEdge lCFAEdge = lEdge.getAnnotation().getCFAEdge();

            QDPTCompositeElement lTarget = lEdge.getTarget();

            if ((lCFAEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) ||
                    (lCFAEdge.getEdgeType() == CFAEdgeType.ReturnEdge) ||
                    (lDAG.getIncomingEdges(lTarget).size() > 1)) {
              BasicBlock lSuccessorBlock = null;

              if (lBasicBlockMap.containsKey(lTarget)) {
                lSuccessorBlock = lBasicBlockMap.get(lTarget);
              } else {
                lSuccessorBlock = new BasicBlock(lTarget);
                lBasicBlockMap.put(lTarget, lSuccessorBlock);
                lBasicBlockWorklist.add(lSuccessorBlock);
              }

              lBasicBlocks.addEdge(lBasicBlock, lSuccessorBlock, lEdge);

              lFinishLoop = true;
            } else if (lCFAEdge.getEdgeType() == CFAEdgeType.MultiStatementEdge) {
              // currently, we do not support multi statement edges.
              assert (false);
            } else if ((lCFAEdge.getEdgeType() == CFAEdgeType.StatementEdge) &&
                    (lCFAEdge.isJumpEdge())) {
              // a return edge
              assert (lCFAEdge.getRawStatement().startsWith("return ") ||
                      lCFAEdge.getRawStatement().startsWith("return;"));

              BasicBlock lSuccessorBlock = null;

              if (lBasicBlockMap.containsKey(lTarget)) {
                lSuccessorBlock = lBasicBlockMap.get(lTarget);
              } else {
                lSuccessorBlock = new BasicBlock(lTarget);
                lBasicBlockMap.put(lTarget, lSuccessorBlock);
                lBasicBlockWorklist.add(lSuccessorBlock);
              }

              lBasicBlocks.addEdge(lBasicBlock, lSuccessorBlock, lEdge);

              lFinishLoop = true;
            } else {
              // add edge to basic block
              lBasicBlock.addEdge(lEdge);
            }

            break;
          }
          default: {
            // more than one successor

            for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lEdge : lOutgoingDAGEdges) {
              QDPTCompositeElement lTarget = lEdge.getTarget();

              BasicBlock lSuccessorBlock = null;

              if (lBasicBlockMap.containsKey(lTarget)) {
                lSuccessorBlock = lBasicBlockMap.get(lTarget);
              } else {
                lSuccessorBlock = new BasicBlock(lTarget);
                lBasicBlockMap.put(lTarget, lSuccessorBlock);
                lBasicBlockWorklist.add(lSuccessorBlock);
              }

              lBasicBlocks.addEdge(lBasicBlock, lSuccessorBlock, lEdge);
            }

            lFinishLoop = true;

            break;
          }
        }
      }
    }

    lAcyclicPathProgram.setBasicBlockGraph(lBasicBlocks);
    lAcyclicPathProgram.setInitialBasicBlock(lFirstBasicBlock);
    lAcyclicPathProgram.setElementToBasicBlockMap(lBasicBlockMap);

    
    // do topological sorting
    Map<Integer, BasicBlock> lIdToBasicBlockMap = createIdToBasicBlockMap(lBasicBlocks, lFirstBasicBlock);
    lAcyclicPathProgram.setIdToBasicBlockMap(lIdToBasicBlockMap);


    // for every function get the corresponding subset of basic blocks
    Map<FunctionDefinitionNode, TreeSet<BasicBlock>> lFunctionSeparatedBasicBlocks = createFunctionSeparatedBasicBlocks(lAcyclicPathProgram.getBasicBlocks(), pCFAs);
    lAcyclicPathProgram.setFunctionSeparatedBasicBlocks(lFunctionSeparatedBasicBlocks);


    lAcyclicPathProgram.setCFAMap(pCFAs);


    // create an id for every edge
    int lEdgeIndex = 0;

    Map<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge, Integer> lEdgesBetweenBasicBlocksToIdMap = new HashMap<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge, Integer>();
    Map<Integer, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge> lIdToEdgesBetweenBasicBlocksMap = new HashMap<Integer, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge>();

    for (Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lEdge : lBasicBlocks.getEdges()) {
      // assign id to edge
      lEdgesBetweenBasicBlocksToIdMap.put(lEdge, lEdgeIndex);
      lIdToEdgesBetweenBasicBlocksMap.put(lEdgeIndex, lEdge);

      lEdgeIndex++;
    }

    lAcyclicPathProgram.setEdgesBetweenBasicBlocksToIdMap(lEdgesBetweenBasicBlocksToIdMap);
    lAcyclicPathProgram.setIdToEdgesBetweenBasicBlocksMap(lIdToEdgesBetweenBasicBlocksMap);

    return lAcyclicPathProgram;
  }

  /*
   * For every element lBasicBlock in pBasicBlocks we assume that
   * lBasicBlock.getId() returns the topological index of lBasicBlock.
   *
   */
  private static Map<FunctionDefinitionNode, TreeSet<BasicBlock>> createFunctionSeparatedBasicBlocks(Set<BasicBlock> pBasicBlocks, CFAMap pCFAs) {
    Map<FunctionDefinitionNode, TreeSet<BasicBlock>> lFunctionSeparatedBasicBlocks = new HashMap<FunctionDefinitionNode, TreeSet<BasicBlock>>();

    Comparator<BasicBlock> lTopologicalComparator = new Comparator<BasicBlock>() {

      @Override
      public int compare(BasicBlock pBlock0, BasicBlock pBlock1) {
        assert (pBlock0 != null);
        assert (pBlock1 != null);

        // do topological sorting
        return (pBlock0.getId() - pBlock1.getId());
      }
    };

    
    // Add every basic block in pBasicBlocks to the set of basic blocks
    // corresponding to the same function definition node. These sets are
    // sorted topological.
    //
    for (BasicBlock lBasicBlock : pBasicBlocks) {
      CFAFunctionDefinitionNode lCFAFunctionDefinitionNode = pCFAs.getCFA(lBasicBlock.getFirstElement().getLocationNode().getFunctionName());

      assert (lCFAFunctionDefinitionNode instanceof FunctionDefinitionNode);

      FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode) lCFAFunctionDefinitionNode;

      if (!lFunctionSeparatedBasicBlocks.containsKey(lFunctionDefinitionNode)) {
        lFunctionSeparatedBasicBlocks.put(lFunctionDefinitionNode, new TreeSet<BasicBlock>(lTopologicalComparator));
      }

      Set<BasicBlock> lBlocks = lFunctionSeparatedBasicBlocks.get(lFunctionDefinitionNode);

      assert (lBlocks != null);

      lBlocks.add(lBasicBlock);
    }

    return lFunctionSeparatedBasicBlocks;
  }

  /*
   * This method performs
   * 1) a topological sorting of the basic blocks (the method getId() of a basic
   *    block returns after applying this method the topological id of the block), and
   * 2) creates a map that maps ids to basic blocks.
   */
  private static Map<Integer, BasicBlock> createIdToBasicBlockMap(Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> pBasicBlocks, BasicBlock pInitialBasicBlock) {
    assert(pBasicBlocks != null);
    assert(pInitialBasicBlock != null);

    // preinitialize ids
    // ids are a little misused here to improve performance and
    // because I (andi) am lazy.
    for (BasicBlock lBasicBlock : pBasicBlocks.getNodes()) {
      // initialize id with number of unprocessed predecessors
      lBasicBlock.setId(pBasicBlocks.getIncomingEdges(lBasicBlock).size());
    }

    
    Map<Integer, BasicBlock> lIdToBasicBlockMap = new HashMap<Integer, BasicBlock>();


    LinkedList<BasicBlock> lTopologicalOrderWorklist = new LinkedList<BasicBlock>();

    lTopologicalOrderWorklist.add(pInitialBasicBlock);

    int lTopologicalOrderId = 0;

    while (!lTopologicalOrderWorklist.isEmpty()) {
      BasicBlock lCurrentBasicBlock = lTopologicalOrderWorklist.removeFirst();

      assert (lCurrentBasicBlock.getId() == 0);

      lCurrentBasicBlock.setId(lTopologicalOrderId);

      lIdToBasicBlockMap.put(lTopologicalOrderId, lCurrentBasicBlock);

      lTopologicalOrderId++;

      for (Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lOutgoingEdge : pBasicBlocks.getOutgoingEdges(lCurrentBasicBlock)) {
        BasicBlock lSuccessorBasicBlock = lOutgoingEdge.getTarget();

        // successor was not processed before
        assert (lSuccessorBasicBlock.getId() > 0);

        int lSuccessorId = lSuccessorBasicBlock.getId() - 1;

        lSuccessorBasicBlock.setId(lSuccessorId);

        if (lSuccessorId == 0) {
          lTopologicalOrderWorklist.add(lSuccessorBasicBlock);
        }
      }
    }

    
    return lIdToBasicBlockMap;
  }

  private static Graph<QDPTCompositeElement, CFAEdgeEdge> extractDAG(QDPTCompositeElement pRootOfART, QDPTCompositeElement pTargetElement) {
    assert(pRootOfART != null);
    assert(pTargetElement != null);

    Graph<QDPTCompositeElement, CFAEdgeEdge> lDAG = new Graph<QDPTCompositeElement, CFAEdgeEdge>();

    LinkedList<QDPTCompositeElement> lWorklist = new LinkedList<QDPTCompositeElement>();
    lWorklist.add(pTargetElement);

    HashSet<QDPTCompositeElement> lVisitedElements = new HashSet<QDPTCompositeElement>();

    while (!lWorklist.isEmpty()) {
      QDPTCompositeElement lElement = lWorklist.poll();

      if (!lVisitedElements.contains(lElement)) {
        lVisitedElements.add(lElement);

        if (lElement.hasParent()) {
          QDPTCompositeElement lParent = lElement.getParent();

          Edge lEdgeToParent = lElement.getEdgeToParent();

          assert (lEdgeToParent instanceof CFAEdgeEdge);

          lDAG.addEdge(lParent, lElement, (CFAEdgeEdge) lEdgeToParent);

          lWorklist.add(lParent);
        }

        if (lElement.equals(pTargetElement)) {
          // we have to ensure reachability of this element, not of a
          // similar one (see coverage)
          // TODO improve this
          continue;
        }

        if (lElement.hasCoveringElement()) {
          QDPTCompositeElement lCoveringElement = lElement.getCoveringElement();

          if (lCoveringElement.hasParent()) {
            QDPTCompositeElement lParent = lCoveringElement.getParent();

            Edge lEdgeToParent = lCoveringElement.getEdgeToParent();

            assert (lEdgeToParent instanceof CFAEdgeEdge);

            lDAG.addEdge(lParent, lElement, (CFAEdgeEdge) lEdgeToParent);

            lWorklist.add(lParent);
          }
        }

        for (QDPTCompositeElement lCoveredElement : lElement.getCoveredElements()) {
          if (lCoveredElement.isSuccessor(lElement)) {
            // otherwise we would create a loop
            continue;
          }

          if (lCoveredElement.hasParent()) {
            // TODO: An additional stop test is missing,
            // currently this is not an issue because of
            // basic block coverage, but, for more
            // complex criteria this will be an issue!

            QDPTCompositeElement lParent = lCoveredElement.getParent();

            Edge lEdgeToParent = lCoveredElement.getEdgeToParent();

            assert (lEdgeToParent instanceof CFAEdgeEdge);

            lDAG.addEdge(lParent, lElement, (CFAEdgeEdge) lEdgeToParent);

            lWorklist.add(lParent);
          }
        }
      }
    }

    return lDAG;
  }
}
