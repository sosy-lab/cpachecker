package fql.backend.targetgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;

import common.Pair;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAExitNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import fql.backend.testgoals.DefaultTestGoalVisitor;
import fql.backend.testgoals.EdgeSequence;
import fql.backend.testgoals.TestGoal;
import fql.frontend.ast.DefaultASTVisitor;
import fql.frontend.ast.coverage.ConditionalCoverage;
import fql.frontend.ast.coverage.Coverage;
import fql.frontend.ast.coverage.Edges;
import fql.frontend.ast.coverage.Paths;
import fql.frontend.ast.coverage.States;
import fql.frontend.ast.filter.Complement;
import fql.frontend.ast.filter.Compose;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.filter.Function;
import fql.frontend.ast.filter.FunctionCall;
import fql.frontend.ast.filter.FunctionCalls;
import fql.frontend.ast.filter.FunctionEntry;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.filter.Intersection;
import fql.frontend.ast.filter.Line;
import fql.frontend.ast.filter.SetMinus;
import fql.frontend.ast.filter.Union;
import fql.frontend.ast.predicate.Predicate;
import fql.frontend.ast.predicate.Predicates;

public class TargetGraph {
  private class FilterEvaluator extends DefaultASTVisitor<TargetGraph> {

    private Map<Filter, TargetGraph> mCache;
    
    public FilterEvaluator() {
      mCache = new HashMap<Filter, TargetGraph>();
    }
    
    @Override
    public TargetGraph visit(Complement pComplement) {
      assert(pComplement != null);
      
      if (mCache.containsKey(pComplement)) {
        return mCache.get(pComplement);
      }
      else {
        Filter lFilter = pComplement.getFilter();
        
        TargetGraph lResult = TargetGraph.applyMinusFilter(mSelf, mSelf.apply(lFilter)); 
        
        mCache.put(pComplement, lResult);
        
        return lResult;
      }
    }

    @Override
    public TargetGraph visit(Compose pCompose) {
      assert(pCompose != null);
      
      if (mCache.containsKey(pCompose)) {
        return mCache.get(pCompose);
      }
      else {
        TargetGraph lResult = mSelf.apply(pCompose.getFilterAppliedFirst()).apply(pCompose.getFilterAppliedSecond());
        
        mCache.put(pCompose, lResult);
        
        return lResult;
      }
    }

    @Override
    public TargetGraph visit(Identity pIdentity) {
      return mSelf;
    }

    @Override
    public TargetGraph visit(Intersection pIntersection) {
      assert(pIntersection != null);
      
      if (mCache.containsKey(pIntersection)) {
        return mCache.get(pIntersection);
      }
      else {
        TargetGraph lGraph1 = mSelf.apply(pIntersection.getFirstFilter());
        TargetGraph lGraph2 = mSelf.apply(pIntersection.getSecondFilter());
        
        TargetGraph lResult = TargetGraph.applyIntersectionFilter(lGraph1, lGraph2);
        
        mCache.put(pIntersection, lResult);
        
        return lResult;
      }
    }

    @Override
    public TargetGraph visit(SetMinus pSetMinus) {
      assert(pSetMinus != null);
      
      if (mCache.containsKey(pSetMinus)) {
        return mCache.get(pSetMinus);
      }
      else {
        TargetGraph lGraph1 = mSelf.apply(pSetMinus.getFirstFilter());
        TargetGraph lGraph2 = mSelf.apply(pSetMinus.getSecondFilter());
        
        TargetGraph lResult = TargetGraph.applyMinusFilter(lGraph1, lGraph2);

        mCache.put(pSetMinus, lResult);
        
        return lResult;
      }
    }

    @Override
    public TargetGraph visit(Union pUnion) {
      assert(pUnion != null);
      
      if (mCache.containsKey(pUnion)) {
        return mCache.get(pUnion);
      }
      else {
        TargetGraph lGraph1 = mSelf.apply(pUnion.getFirstFilter());
        TargetGraph lGraph2 = mSelf.apply(pUnion.getSecondFilter());
        
        TargetGraph lResult = TargetGraph.applyUnionFilter(lGraph1, lGraph2);
        
        mCache.put(pUnion, lResult);
        
        return lResult;
      }
    }

    @Override
    public TargetGraph visit(Function pFunc) {
      assert(pFunc != null);
      
      if (mCache.containsKey(pFunc)) {
        return mCache.get(pFunc);
      }
      else {
        TargetGraph lResult = TargetGraph.applyFunctionNameFilter(mSelf, pFunc.getFunctionName());
        
        mCache.put(pFunc, lResult);
        
        return lResult;
      }
    }
    
    @Override
    public TargetGraph visit(FunctionCall pFunctionCall) {
      assert(pFunctionCall != null);
      
      if (mCache.containsKey(pFunctionCall)) {
        return mCache.get(pFunctionCall);
      }
      else {
        MaskFunctor<Node, Edge> lMaskFunctor = new FunctionCallMaskFunctor(pFunctionCall.getFunctionName());
        
        TargetGraph lResult = TargetGraph.applyStandardEdgeBasedFilter(mSelf, lMaskFunctor);
        
        mCache.put(pFunctionCall, lResult);
        
        return lResult;
      }
    }
    
    @Override
    public TargetGraph visit(FunctionCalls pFunctionCalls) {
      assert(pFunctionCalls != null);
      
      if (mCache.containsKey(pFunctionCalls)) {
        return mCache.get(pFunctionCalls);
      }
      else {
        TargetGraph lResult = TargetGraph.applyStandardEdgeBasedFilter(mSelf, FunctionCallsMaskFunctor.getInstance());
        
        mCache.put(pFunctionCalls, lResult);
        
        return  lResult;
      }
    }

    @Override
    public TargetGraph visit(FunctionEntry pEntry) {
      assert(pEntry != null);
      
      if (mCache.containsKey(pEntry)) {
        return mCache.get(pEntry);
      }
      else {
        MaskFunctor<Node, Edge> lMaskFunctor = new FunctionEntryMaskFunctor(pEntry.getFunctionName());
        
        TargetGraph lResult = TargetGraph.applyStandardEdgeBasedFilter(mSelf, lMaskFunctor);
        
        mCache.put(pEntry, lResult);
        
        return lResult;
      }
    }

    @Override
    public TargetGraph visit(Line pLine) {
      assert(pLine != null);
      
      if (mCache.containsKey(pLine)) {
        return mCache.get(pLine);
      }
      else {
        MaskFunctor<Node, Edge> lMaskFunctor = new LineNumberMaskFunctor(pLine.getLine());
        
        TargetGraph lResult = TargetGraph.applyStandardEdgeBasedFilter(mSelf, lMaskFunctor);
        
        mCache.put(pLine, lResult);
        
        return lResult;
      }
    }
    
  }
  
  private class CoverageSpecificationEvaluator extends DefaultASTVisitor<Set<? extends TestGoal>> {

    private class TestGoalPredicator extends DefaultTestGoalVisitor<TestGoal> {

      private Predicates mPreconditions;
      private Predicates mPostconditions;
      
      public TestGoalPredicator(Predicates pPreconditions, Predicates pPostconditions) {
        assert(pPreconditions != null);
        assert(pPostconditions != null);
        
        mPreconditions = pPreconditions;
        mPostconditions = pPostconditions;
      }
      
      private Node getPredicatedNode(Node pNode, Predicates pPredicates) {
        assert(pNode != null);
        assert(pPredicates != null);
        
        if (pPredicates.isEmpty()) {
          return pNode;
        }
        else {
          Node lNode = new Node(pNode);
          
          for (Predicate lPredicate : pPredicates) {
            lNode.addPredicate(lPredicate, true);
          }
          
          return lNode;
        }
      }
      
      @Override
      public Edge visit(Edge pEdge) {
        assert(pEdge != null);
        
        Node lSource = pEdge.getSource();
        Node lTarget = pEdge.getTarget();
        CFAEdge lCFAEdge = pEdge.getCFAEdge();
        
        Node lNewSource = getPredicatedNode(lSource, mPreconditions);
        
        Node lNewTarget = getPredicatedNode(lTarget, mPostconditions);
        
        return new Edge(lNewSource, lNewTarget, lCFAEdge);
      }

      @Override
      public EdgeSequence visit(EdgeSequence pEdgeSequence) {
        assert(pEdgeSequence != null);
        
        assert(pEdgeSequence.size() > 1);
        
        Edge lFirstEdge = pEdgeSequence.get(0);
        Edge lLastEdge = pEdgeSequence.get(pEdgeSequence.size() - 1);

        
        Edge lNewFirstEdge;
        
        if (mPreconditions.isEmpty()) {
          lNewFirstEdge = lFirstEdge;
        }
        else {
          Node lFirstNode = lFirstEdge.getSource();
          
          Node lNewFirstNode = getPredicatedNode(lFirstNode, mPreconditions);
          
          lNewFirstEdge = new Edge(lNewFirstNode, lFirstEdge.getTarget(), lFirstEdge.getCFAEdge());
        }
        
        Edge lNewLastEdge;
        
        if (mPostconditions.isEmpty()) {
          lNewLastEdge = lLastEdge;
        }
        else {
          Node lLastNode = lLastEdge.getTarget();
          
          Node lNewLastNode = getPredicatedNode(lLastNode, mPostconditions);
          
          lNewLastEdge = new Edge(lLastEdge.getSource(), lNewLastNode, lLastEdge.getCFAEdge());
        }
        
        List<Edge> lEdgeSequence = new LinkedList<Edge>();
        
        lEdgeSequence.add(lNewFirstEdge);
        
        for (int lIndex = 1; lIndex < pEdgeSequence.size() - 1; lIndex++) {
          lEdgeSequence.add(pEdgeSequence.get(lIndex));
        }
        
        lEdgeSequence.add(lNewLastEdge);
        
        return new EdgeSequence(lEdgeSequence);
      }

      @Override
      public Node visit(Node pNode) {
        assert(pNode != null);
        
        Node lNode = new Node(pNode);
        
        for (Predicate lPredicate : mPreconditions) {
          lNode.addPredicate(lPredicate, true);
        }
        
        for (Predicate lPredicate : mPostconditions) {
          lNode.addPredicate(lPredicate, true);
        }
        
        return lNode;
      }
      
    }
    
    @Override
    public Set<? extends TestGoal> visit(ConditionalCoverage pConditionalCoverage) {
      assert(pConditionalCoverage != null);
      
      Predicates lPreconditions = pConditionalCoverage.getPreconditions();
      Predicates lPostconditions = pConditionalCoverage.getPostconditions();
      
      Coverage lCoverage = pConditionalCoverage.getCoverage();
      
      Set<? extends TestGoal> lTestGoals = mSelf.apply(lCoverage);
      
      Set<TestGoal> lPredicatedTestGoals = new HashSet<TestGoal>();
      
      TestGoalPredicator lPredicator = new TestGoalPredicator(lPreconditions, lPostconditions);
      
      for (TestGoal lTestGoal : lTestGoals) {
        lPredicatedTestGoals.add(lTestGoal.accept(lPredicator));
      }
      
      return lPredicatedTestGoals;
    }

    @Override
    public Set<? extends TestGoal> visit(Edges pEdges) {
      assert(pEdges != null);
      
      Filter lFilter = pEdges.getFilter();
      Predicates lPredicates = pEdges.getPredicates();
      
      TargetGraph lFilteredTargetGraph = mSelf.apply(lFilter);
      TargetGraph lPredicatedTargetGraph = TargetGraph.applyPredication(lFilteredTargetGraph, lPredicates);
  
      return lPredicatedTargetGraph.mGraph.edgeSet();
    }

    @Override
    public Set<? extends TestGoal> visit(Paths pPaths) {
      assert(pPaths != null);
      
      Filter lFilter = pPaths.getFilter();
      Predicates lPredicates = pPaths.getPredicates();
      
      TargetGraph lFilteredTargetGraph = mSelf.apply(lFilter);
      TargetGraph lPredicatedTargetGraph = TargetGraph.applyPredication(lFilteredTargetGraph, lPredicates);
  
      Iterator<Node> lInitialNodesIterator = lPredicatedTargetGraph.getInitialNodes();
      
      HashSet<TestGoal> lTestGoals = new HashSet<TestGoal>();
      
      while (lInitialNodesIterator.hasNext()) {
        Node lInitialNode = lInitialNodesIterator.next();
        
        Set<Edge> lOutgoingEdges = lPredicatedTargetGraph.mGraph.outgoingEdgesOf(lInitialNode);
        
        if (lOutgoingEdges.size() == 0) {
          lTestGoals.add(lInitialNode);
        }
        else {
          // enumerate all k-bounded paths starting in lInitialNode
          HashMap<Node, Integer> lNumberOfOccurrences = new HashMap<Node, Integer>();
          
          lNumberOfOccurrences.put(lInitialNode, 1);
          
          LinkedList<Iterator<Edge>> lIteratorStack = new LinkedList<Iterator<Edge>>();
          
          lIteratorStack.addFirst(lOutgoingEdges.iterator());
          
          LinkedList<Edge> lEdgeStack = new LinkedList<Edge>();
          
          while (!lIteratorStack.isEmpty()) {
            Iterator<Edge> lIterator = lIteratorStack.getLast();
            
            if (lIterator.hasNext()) {
              Edge lEdge = lIterator.next();
              
              lEdgeStack.addLast(lEdge);
              
              Node lTarget = lEdge.getTarget();
              
              int lOccurrences;
              
              if (lNumberOfOccurrences.containsKey(lTarget)) {
                lOccurrences = lNumberOfOccurrences.get(lTarget);
                
                lOccurrences++;
              }
              else {
                lOccurrences = 1;
              }
              
              if (lOccurrences > pPaths.getBound()) {
                lEdgeStack.removeLast();
                lNumberOfOccurrences.put(lTarget, lOccurrences - 1);
                
                switch (lEdgeStack.size()) {
                case 0:
                  lTestGoals.add(lInitialNode);
                  break;
                case 1:
                  lTestGoals.add(lEdgeStack.getFirst());
                  break;
                default:
                  lTestGoals.add(new EdgeSequence(lEdgeStack));
                  break;
                }
              }
              else {
                // add iterator to stack
                Iterator<Edge> lNextIterator = lPredicatedTargetGraph.mGraph.outgoingEdgesOf(lTarget).iterator();
                
                if (lNextIterator.hasNext()) {
                  // update occurrences
                  lNumberOfOccurrences.put(lTarget, lOccurrences);
                  
                  // add to stack
                  lIteratorStack.addLast(lNextIterator);
                }
                else {
                  assert(lEdgeStack.size() > 0);
                  
                  if (lEdgeStack.size() == 1) {
                    lTestGoals.add(lEdge);
                  }
                  else {
                    lTestGoals.add(new EdgeSequence(lEdgeStack));
                  }
                  
                  lEdgeStack.removeLast();
                }
              }
            }
            else {
              lIteratorStack.removeLast();
              
              // first iterator is not associated with an edge,
              // so remove only edge if it is not the first one
              if (lIteratorStack.size() > 0) {
                Edge lEdge = lEdgeStack.getLast();
                Node lTarget = lEdge.getTarget();
                int lOccurrences = lNumberOfOccurrences.get(lTarget);
                lNumberOfOccurrences.put(lTarget, lOccurrences - 1);
                
                lEdgeStack.removeLast();
              }
            }
          }
        }
      }
      
      return lTestGoals;
    }

    @Override
    public Set<? extends TestGoal> visit(States pStates) {
      assert(pStates != null);
      
      Filter lFilter = pStates.getFilter();
      Predicates lPredicates = pStates.getPredicates();
      
      TargetGraph lFilteredTargetGraph = mSelf.apply(lFilter);
      TargetGraph lPredicatedTargetGraph = TargetGraph.applyPredication(lFilteredTargetGraph, lPredicates);
  
      return lPredicatedTargetGraph.mGraph.vertexSet();
    }

    @Override
    public Set<? extends TestGoal> visit(fql.frontend.ast.coverage.Union pUnion) {
      assert(pUnion != null);
      
      Coverage lLeftCoverage = pUnion.getLeftCoverage();
      Coverage lRightCoverage = pUnion.getRightCoverage();
      
      assert(lLeftCoverage != null);
      assert(lRightCoverage != null);
      
      Set<? extends TestGoal> lFirstTestGoalSet = mSelf.apply(lLeftCoverage);
      Set<? extends TestGoal> lSecondTestGoalSet = mSelf.apply(lRightCoverage);
      
      Set<TestGoal> lResult = new HashSet<TestGoal>();
      
      lResult.addAll(lFirstTestGoalSet);
      lResult.addAll(lSecondTestGoalSet);
      
      return lResult;
    }

    @Override
    public Set<? extends TestGoal> visit(fql.frontend.ast.coverage.Intersection pIntersection) {
      // TODO Auto-generated method stub
      return super.visit(pIntersection);
    }

    @Override
    public Set<? extends TestGoal> visit(fql.frontend.ast.coverage.SetMinus pSetMinus) {
      // TODO Auto-generated method stub
      return super.visit(pSetMinus);
    }

  }
  
  public TargetGraph apply(Filter pFilter) {
    assert(pFilter != null);
    
    return pFilter.accept(mFilterEvaluator);
  }
  
  public Set<? extends TestGoal> apply(Coverage pCoverageSpecification) {
    assert(pCoverageSpecification != null);
    
    return pCoverageSpecification.accept(mCoverageSpecificationEvaluator);
  }
  
  private FilterEvaluator mFilterEvaluator = new FilterEvaluator();
  private CoverageSpecificationEvaluator mCoverageSpecificationEvaluator = new CoverageSpecificationEvaluator();
  
  private Set<Node> mInitialNodes;
  private Set<Node> mFinalNodes;
  private DirectedGraph<Node, Edge> mGraph;
  private TargetGraph mSelf = this;
  
  private TargetGraph(Set<Node> pInitialNodes, Set<Node> pFinalNodes, DirectedGraph<Node, Edge> pGraph) {
    assert(pInitialNodes != null);
    assert(pFinalNodes != null);
    assert(pGraph != null);
    
    mInitialNodes = pInitialNodes;
    mFinalNodes = pFinalNodes;
    mGraph = pGraph;
  }
  
  /* copy constructor */
  private TargetGraph(TargetGraph pTargetGraph) {
    assert(pTargetGraph != null);
    
    mInitialNodes = new HashSet<Node>(pTargetGraph.mInitialNodes);
    mFinalNodes = new HashSet<Node>(pTargetGraph.mFinalNodes);
    mGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);
    
    for (Node lNode : pTargetGraph.mGraph.vertexSet()) {
      mGraph.addVertex(lNode);
    }
    
    for (Edge lEdge : pTargetGraph.mGraph.edgeSet()) {
      Node lSourceNode = pTargetGraph.mGraph.getEdgeSource(lEdge);
      Node lTargetNode = pTargetGraph.mGraph.getEdgeTarget(lEdge);
      
      mGraph.addEdge(lSourceNode, lTargetNode, lEdge);
    }
  }
  
  public Set<Edge> getEdges() {
    return mGraph.edgeSet(); 
  }
    
  public static TargetGraph createTargetGraphFromCFA(CFANode pInitialNode) {
    assert(pInitialNode != null);
    
    TargetGraph lTargetGraph = new TargetGraph(new HashSet<Node>(), new HashSet<Node>(), new DefaultDirectedGraph<Node, Edge>(Edge.class));
   
    // create a target graph isomorphic to the given CFA (starting in pInitialNode)
    createTargetGraphFromCFA(pInitialNode, lTargetGraph.mInitialNodes, lTargetGraph.mFinalNodes, lTargetGraph.mGraph);
    
    return lTargetGraph;
  }
  
  /* 
   * Returns a target graph that retains all nodes and edges in pTargetGraph that 
   * belong the the function given by pFunctionName. The set of initial nodes is
   * changed to the set of nodes in the resulting target graph that contain a 
   * CFAFunctionDefinitionNode. The set of final nodes is changed to the set of 
   * nodes in the resulting target graph that contain a CFAExitNode.
   */
  public static TargetGraph applyFunctionNameFilter(TargetGraph pTargetGraph, String pFunctionName) {
    assert(pTargetGraph != null);
    assert(pFunctionName != null);
    
    MaskFunctor<Node, Edge> lMaskFunctor = new FunctionNameMaskFunctor(pFunctionName);
    
    DirectedGraph<Node, Edge> lMaskedGraph = new DirectedMaskSubgraph<Node, Edge>(pTargetGraph.mGraph, lMaskFunctor);
    
    HashSet<Node> lInitialNodes = new HashSet<Node>();
    HashSet<Node> lFinalNodes = new HashSet<Node>();
    
    for (Node lNode : lMaskedGraph.vertexSet()) {
      CFANode lCFANode = lNode.getCFANode();
      
      if (lCFANode instanceof CFAFunctionDefinitionNode) {
        lInitialNodes.add(lNode);
      }
      
      if (lCFANode instanceof CFAExitNode) {
        lFinalNodes.add(lNode);
      }
    }
    
    return new TargetGraph(lInitialNodes, lFinalNodes, lMaskedGraph);
  }
  
  public static TargetGraph applyStandardEdgeBasedFilter(TargetGraph pTargetGraph, MaskFunctor<Node, Edge> pMaskFunctor) {
    assert(pTargetGraph != null);
    assert(pMaskFunctor != null);
    
    DirectedGraph<Node, Edge> lMaskedGraph = new DirectedMaskSubgraph<Node, Edge>(pTargetGraph.mGraph, pMaskFunctor);
    
    HashSet<Node> lInitialNodes = new HashSet<Node>();
    HashSet<Node> lFinalNodes = new HashSet<Node>();
    
    for (Edge lEdge : lMaskedGraph.edgeSet()) {
      lInitialNodes.add(lEdge.getSource());
      lFinalNodes.add(lEdge.getTarget());
    }
    
    return new TargetGraph(lInitialNodes, lFinalNodes, lMaskedGraph);
  }
  
  public static TargetGraph applyUnionFilter(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    assert(pTargetGraph1 != null);
    assert(pTargetGraph2 != null);
    
    TargetGraph lCopy = new TargetGraph(pTargetGraph1);
    
    lCopy.mInitialNodes.addAll(pTargetGraph2.mInitialNodes);
    lCopy.mFinalNodes.addAll(pTargetGraph2.mFinalNodes);
    
    for (Node lNode : pTargetGraph2.mGraph.vertexSet()) {
      lCopy.mGraph.addVertex(lNode);
    }
    
    for (Edge lEdge : pTargetGraph2.mGraph.edgeSet()) {
      Node lSourceNode = pTargetGraph2.mGraph.getEdgeSource(lEdge);
      Node lTargetNode = pTargetGraph2.mGraph.getEdgeTarget(lEdge);
      
      lCopy.mGraph.addEdge(lSourceNode, lTargetNode, lEdge);
    }
    
    return lCopy;
  }
  
  public static TargetGraph applyIntersectionFilter(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    assert(pTargetGraph1 != null);
    assert(pTargetGraph2 != null);
    
    TargetGraph lCopy = new TargetGraph(pTargetGraph1);
    
    lCopy.mInitialNodes.retainAll(pTargetGraph2.mInitialNodes);
    lCopy.mFinalNodes.retainAll(pTargetGraph2.mFinalNodes);
    
    HashSet<Node> lNodesToBeRemoved = new HashSet<Node>();
    
    for (Node lNode : lCopy.mGraph.vertexSet()) {
      if (!pTargetGraph2.mGraph.containsVertex(lNode)) {
        lNodesToBeRemoved.add(lNode);
      }
    }
    
    lCopy.mGraph.removeAllVertices(lNodesToBeRemoved);
    
    HashSet<Edge> lEdgesToBeRemoved = new HashSet<Edge>();
    
    for (Edge lEdge : lCopy.mGraph.edgeSet()) {
      if (!pTargetGraph2.mGraph.containsEdge(lEdge)) {
        lEdgesToBeRemoved.add(lEdge);
      }
    }
    
    lCopy.mGraph.removeAllEdges(lEdgesToBeRemoved);
    
    return lCopy;
  }
  
  public static TargetGraph applyMinusFilter(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    assert(pTargetGraph1 != null);
    assert(pTargetGraph2 != null);
    
    TargetGraph lCopy = new TargetGraph(pTargetGraph1);
    
    lCopy.mInitialNodes.removeAll(pTargetGraph2.mInitialNodes);
    lCopy.mFinalNodes.removeAll(pTargetGraph2.mFinalNodes);
    
    lCopy.mGraph.removeAllEdges(pTargetGraph2.mGraph.edgeSet());
    lCopy.mGraph.removeAllVertices(pTargetGraph2.mGraph.vertexSet());
    
    return lCopy;
  }
  
  public static TargetGraph applyPredication(TargetGraph pTargetGraph, Predicate pPredicate) {
    assert(pTargetGraph != null);
    assert(pPredicate != null);
    
    HashSet<Node> lInitialNodes = new HashSet<Node>();
    HashSet<Node> lFinalNodes = new HashSet<Node>();
    DirectedGraph<Node, Edge> lGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);
    
    // 1) duplicate vertices
    
    HashMap<Node, Pair<Node, Node>> lMap = new HashMap<Node, Pair<Node, Node>>();
    
    for (Node lNode : pTargetGraph.mGraph.vertexSet()) {
      Node lTrueNode = new Node(lNode);
      lTrueNode.addPredicate(pPredicate, true);
      lGraph.addVertex(lTrueNode);
      
      Node lFalseNode = new Node(lNode);
      lFalseNode.addPredicate(pPredicate, false);
      lGraph.addVertex(lFalseNode);
      
      Pair<Node, Node> lPair = new Pair<Node, Node>(lTrueNode, lFalseNode);
      
      lMap.put(lNode, lPair);
    }
    
    for (Node lNode : pTargetGraph.mInitialNodes) {
      Pair<Node, Node> lPair = lMap.get(lNode);
      
      lInitialNodes.add(lPair.getFirst());
      lInitialNodes.add(lPair.getSecond());
    }
    
    for (Node lNode : pTargetGraph.mFinalNodes) {
      Pair<Node, Node> lPair = lMap.get(lNode);
      
      lFinalNodes.add(lPair.getFirst());
      lFinalNodes.add(lPair.getSecond());
    }
    
    // 2) replicate edges
    
    for (Edge lEdge : pTargetGraph.mGraph.edgeSet()) {
      Node lSourceNode = lEdge.getSource();
      Pair<Node, Node> lSourcePair = lMap.get(lSourceNode);
      
      Node lTargetNode = lEdge.getTarget();
      Pair<Node, Node> lTargetPair = lMap.get(lTargetNode);
      
      Node lSourceTrueNode = lSourcePair.getFirst();
      Node lSourceFalseNode = lSourcePair.getSecond();
      
      Node lTargetTrueNode = lTargetPair.getFirst();
      Node lTargetFalseNode = lTargetPair.getSecond();
      
      new Edge(lSourceTrueNode, lTargetTrueNode, lEdge.getCFAEdge(), lGraph);
      new Edge(lSourceTrueNode, lTargetFalseNode, lEdge.getCFAEdge(), lGraph);
      new Edge(lSourceFalseNode, lTargetTrueNode, lEdge.getCFAEdge(), lGraph);
      new Edge(lSourceFalseNode, lTargetFalseNode, lEdge.getCFAEdge(), lGraph);
    }
    
    return new TargetGraph(lInitialNodes, lFinalNodes, lGraph);
  }
  
  public static TargetGraph applyPredication(TargetGraph pTargetGraph, Predicates pPredicates) {
    assert(pTargetGraph != null);
    assert(pPredicates != null);
    
    TargetGraph lResultGraph = pTargetGraph;
    
    for (Predicate lPredicate : pPredicates) {
      lResultGraph = applyPredication(lResultGraph, lPredicate);
    }
    
    return lResultGraph;
  }
  
  public Iterator<Node> getInitialNodes() {
    return mInitialNodes.iterator();
  }
  
  public Iterator<Node> getFinalNodes() {
    return mFinalNodes.iterator();
  }
  
  private static void createTargetGraphFromCFA(CFANode pInitialNode, Set<Node> pInitialNodes, Set<Node> pFinalNodes, DirectedGraph<Node, Edge> pGraph) {
    assert(pInitialNode != null);
    assert(pInitialNodes.isEmpty());
    assert(pFinalNodes.isEmpty());
    assert(pGraph != null);
    
    HashMap<CFANode, Node> lNodeMapping = new HashMap<CFANode, Node>();
    
    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();
    
    lWorklist.add(pInitialNode);
    
    Node lInitialNode = new Node(pInitialNode);
    
    pInitialNodes.add(lInitialNode);
    
    lNodeMapping.put(pInitialNode, lInitialNode);
    pGraph.addVertex(lInitialNode);
    
    while (!lWorklist.isEmpty()) {
      CFANode lCFANode = lWorklist.iterator().next();
      lWorklist.remove(lCFANode);
      
      lVisitedNodes.add(lCFANode);
      
      Node lNode = lNodeMapping.get(lCFANode);
        
      // determine successors
      int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();
      
      CallToReturnEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();
      
      if (lNumberOfLeavingEdges == 0 && lCallToReturnEdge == null) {
        assert(lCFANode instanceof CFAExitNode);
        
        pFinalNodes.add(lNode);
      }
      else {
        for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
          CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);
          CFANode lSuccessor = lEdge.getSuccessor();        
            
          Node lSuccessorNode;
          
          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);
            
            lNodeMapping.put(lSuccessor, lSuccessorNode);
            pGraph.addVertex(lSuccessorNode);
            
            lWorklist.add(lSuccessor);
          }
          
          new Edge(lNode, lSuccessorNode, lEdge, pGraph);
        }
        
        if (lCallToReturnEdge != null) {
          CFANode lSuccessor = lCallToReturnEdge.getSuccessor();
          
          Node lSuccessorNode;
          
          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);
            
            lNodeMapping.put(lSuccessor, lSuccessorNode);
            pGraph.addVertex(lSuccessorNode);
            
            lWorklist.add(lSuccessor);
          }
          
          new Edge(lNode, lSuccessorNode, lCallToReturnEdge, pGraph);
        }
      }
    }
  }
  
  @Override
  public String toString() {
    String lInitialNodes = "INITIAL NODES: " + mInitialNodes.toString() + "\n";
    String lFinalNodes = "FINAL NODES: " + mFinalNodes.toString() + "\n";
    
    StringBuffer lBuffer = new StringBuffer();
    
    lBuffer.append(lInitialNodes);
    lBuffer.append(lFinalNodes);
    
    for (Edge lEdge : mGraph.edgeSet()) {
      lBuffer.append(lEdge.toString());
      lBuffer.append("\n");
    }
    
    return lBuffer.toString();
  }
}
