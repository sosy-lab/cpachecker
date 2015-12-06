package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;


import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtilsTest.IsSameEdgeCheckedByClassAndDescritpion.isSameEdge;
import static org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtilsTest.IsSameNodeCheckedByClassAndDescritpion.isSameNode;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtils.NodeReachable;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

//import static org.hamcrest.Matchers.contains;

public class CFAEdgeUtilsTest {

  @Test
  public void testInjectEasySequnceInBetween() {
    MutableCFA cfa = Mockito.mock(MutableCFA.class);
    
    // origin code
    CFANode startOriginNode = new CFANode("Node 1");
    CFANode middleOriginNode = new CFANode("Node 2");
    CFANode endOriginNode = new CFANode("Node 3");
    CFAEdge originFirstEdge = new BlankEdge("", FileLocation.DUMMY, startOriginNode, middleOriginNode, "Edge 1");
    CFAEdge originSecondEdge = new BlankEdge("", FileLocation.DUMMY, middleOriginNode, endOriginNode, "Edge 2");
    CFACreationUtils.addEdgeUnconditionallyToCFA(originFirstEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originSecondEdge);
    
    // injected sequence
    CFANode startSequenceNode = new CFANode("Node 4");
    CFANode middleSequenceNode = new CFANode("Node 5");
    CFANode endSequenceNode = new CFANode("Node 6");
    CFAEdge firstSequenceEdge = new BlankEdge("", FileLocation.DUMMY, startSequenceNode, middleSequenceNode, "Edge 3");
    CFAEdge secondSequenceEdge = new BlankEdge("", FileLocation.DUMMY, middleSequenceNode, endSequenceNode, "Edge 4");
    CFACreationUtils.addEdgeUnconditionallyToCFA(firstSequenceEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(secondSequenceEdge);
    
    // BEFORE INJECTING
    //connections which should still stay
    assertSuccessorOf(originFirstEdge, startOriginNode);
    assertSuccessorOf(originSecondEdge, middleOriginNode);
    assertSuccessorOf(endOriginNode, originSecondEdge);
    
    assertSuccessorOf(firstSequenceEdge, startSequenceNode);
    assertSuccessorOf(middleSequenceNode, firstSequenceEdge);
    assertSuccessorOf(secondSequenceEdge, middleSequenceNode);
    
    //connection which should desire
    assertSuccessorOf(middleOriginNode, originFirstEdge);
    assertSuccessorOf(endSequenceNode, secondSequenceEdge);
    
    // desired connections should be replaced by
    assertNotSuccessorOf(startSequenceNode, originFirstEdge);
    assertNotSuccessorOf(middleOriginNode, secondSequenceEdge);
    
    
    // INJECTING
    CFAEdgeUtils.injectInBetween(middleOriginNode, startSequenceNode, endSequenceNode, cfa);

    // The originFirstEdge is immutable so it will still contain the original
    // nodes. But this edge is not use anymore! Check by traversing from start
    // or get test edges by traversing
    // --> get edges to test by traversing through unchanged node.
    CFAEdge newOriginFirstEdge = startOriginNode.getLeavingEdge(0);
    CFAEdge newSecondSequenceEdge = middleSequenceNode.getLeavingEdge(0);
    CFAEdge newOriginSecondEdge = endOriginNode.getEnteringEdge(0);
    CFAEdge newFirstSequenceEdge = startSequenceNode.getLeavingEdge(0);
    
    // AFTER INJECTING
    //connections which should still stay
    assertSuccessorOf(newOriginFirstEdge, startOriginNode);
    assertSuccessorOf(newOriginSecondEdge, middleOriginNode);
    assertSuccessorOf(endOriginNode, newOriginSecondEdge);
    
    assertSuccessorOf(newFirstSequenceEdge, startSequenceNode);
    assertSuccessorOf(middleSequenceNode, newFirstSequenceEdge);
    assertSuccessorOf(newSecondSequenceEdge, middleSequenceNode);
    
    //connection which should desire
    assertNotSuccessorOf(middleOriginNode, newOriginFirstEdge);
    assertNotSuccessorOf(endSequenceNode, newSecondSequenceEdge);
    
    // desired connections should be replaced by
    assertSuccessorOf(startSequenceNode, newOriginFirstEdge);
    assertSuccessorOf(middleOriginNode, newSecondSequenceEdge);
    
  }
  
  @Test
  public void testSimpleBypassCEdgeNodes() {
    // origin code
    CFANode startOriginNode = new CFANode("Node 1");
    CFANode endOriginNode = new CFANode("Node 2");
    CFANode newEndNode = new CFANode("Node 3");
    CFAEdge originEdge = new BlankEdge("", FileLocation.DUMMY, startOriginNode, endOriginNode, "Edge 1");
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge);
    
    CFAEdge resultEdge = CFAEdgeUtils.bypassCEdgeNodes(originEdge, startOriginNode, newEndNode);
    
    Assert.assertEquals(endOriginNode.getNumEnteringEdges(), 0);
    Assert.assertEquals(startOriginNode.getNumLeavingEdges(), 1);
    Assert.assertNotEquals(startOriginNode.getLeavingEdge(0), originEdge);
    
    
    Assert.assertEquals(resultEdge.getPredecessor(), startOriginNode);
    Assert.assertEquals(resultEdge.getSuccessor(), newEndNode);
    
    assertSuccessorOf(resultEdge, startOriginNode);
    assertSuccessorOf(newEndNode, resultEdge);
  }
  
  @Test
  public void testFullBypassCEdgeNodes() {
    //    * *
    //     *
    //     * 
    //    * *
    
    // origin code
    CFANode leftStartOriginNode = new CFANode("Node 1");
    CFANode rightStartOriginNode = new CFANode("Node 2");
    CFANode firstMiddleNode = new CFANode("Node 3");
    CFANode secondMiddleNode = new CFANode("Node 4");
    CFANode leftEndOriginNode = new CFANode("Node 5");
    CFANode rightEndOriginNode = new CFANode("Node 6");
    
    CFAEdge originEdge1 = new BlankEdge("", FileLocation.DUMMY, leftStartOriginNode, firstMiddleNode, "Edge 1");
    CFAEdge originEdge2 = new BlankEdge("", FileLocation.DUMMY, rightStartOriginNode, firstMiddleNode, "Edge 2");
    CFAEdge originEdge3 = new BlankEdge("", FileLocation.DUMMY, firstMiddleNode, secondMiddleNode, "Edge 3");  // will be replaced
    CFAEdge originEdge4 = new BlankEdge("", FileLocation.DUMMY, secondMiddleNode, leftEndOriginNode, "Edge 4");
    CFAEdge originEdge5 = new BlankEdge("", FileLocation.DUMMY, secondMiddleNode, rightEndOriginNode, "Edge 5");
    
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge1);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge2);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge3);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge4);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge5);
    
    CFANode newPredesessor = new CFANode("Node 7");
    CFANode newSuccessor = new CFANode("Node 8");
    
    CFAEdge resultEdge = CFAEdgeUtils.bypassCEdgeNodes(originEdge3, newPredesessor, newSuccessor);
    
    Assert.assertEquals(firstMiddleNode.getNumLeavingEdges(), 0);
    Assert.assertEquals(firstMiddleNode.getNumEnteringEdges(), 2);

    Assert.assertEquals(secondMiddleNode.getNumEnteringEdges(), 0);
    Assert.assertEquals(secondMiddleNode.getNumLeavingEdges(), 2);
    
    
    Assert.assertEquals(resultEdge.getPredecessor(), newPredesessor);
    Assert.assertEquals(resultEdge.getSuccessor(), newSuccessor);
    
    assertSuccessorOf(resultEdge, newPredesessor);
    assertSuccessorOf(newSuccessor, resultEdge);
  
  }
  
  @Test
  public void testXBypassCEdgeNodes() {
    //    * *
    //     *
    //    * *
    
    // origin code
    CFANode leftStartOriginNode = new CFANode("Node 1");
    CFANode rightStartOriginNode = new CFANode("Node 2");
    CFANode middleNode = new CFANode("Node 3");
    CFANode leftEndOriginNode = new CFANode("Node 5");
    CFANode rightEndOriginNode = new CFANode("Node 6");
    
    CFAEdge originEdge1 = new BlankEdge("", FileLocation.DUMMY, leftStartOriginNode, middleNode, "Edge 1");
    CFAEdge originEdge2 = new BlankEdge("", FileLocation.DUMMY, rightStartOriginNode, middleNode, "Edge 2");
    CFAEdge originEdge3 = new BlankEdge("", FileLocation.DUMMY, middleNode, leftEndOriginNode, "Edge 3");
    CFAEdge originEdge4 = new BlankEdge("", FileLocation.DUMMY, middleNode, rightEndOriginNode, "Edge 4");
    
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge1);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge2);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge3);
    CFACreationUtils.addEdgeUnconditionallyToCFA(originEdge4);
    
    CFANode newPredesessor = new CFANode("Node 5");
    
    CFAEdge resultEdge = CFAEdgeUtils.bypassCEdgeNodes(originEdge3, newPredesessor, rightEndOriginNode);
    
    Assert.assertEquals(middleNode.getNumLeavingEdges(), 1);
    Assert.assertEquals(middleNode.getNumEnteringEdges(), 2);

    Assert.assertEquals(resultEdge.getPredecessor(), newPredesessor);
    Assert.assertEquals(resultEdge.getSuccessor(), rightEndOriginNode);
    
    Assert.assertEquals(resultEdge.getPredecessor(), newPredesessor);
    Assert.assertEquals(resultEdge.getSuccessor(), rightEndOriginNode);
    
    assertSuccessorOf(resultEdge, newPredesessor);
    assertSuccessorOf(rightEndOriginNode, resultEdge);
  }
  
  private static void assertSuccessorOf(CFANode successor, CFAEdge of) {
    Assert.assertThat(of.getSuccessor(), isSameNode(successor));
    Assert.assertThat(CFAUtils.enteringEdges(successor), hasItem(isSameEdge(of)));
  }

  private static void assertSuccessorOf(CFAEdge successor, CFANode of) {
    Assert.assertThat(successor.getPredecessor(), isSameNode(of));
    Assert.assertThat(CFAUtils.allLeavingEdges(of), hasItem(isSameEdge(successor)));
  }
  
  private static void assertNotSuccessorOf(CFANode successor, CFAEdge of) {
    Assert.assertThat(of.getSuccessor(), not(isSameNode(successor)));
    Assert.assertThat(CFAUtils.enteringEdges(successor), everyItem(not(isSameEdge(of))));
  }
  
  private static void assertNotSuccessorOf(CFAEdge successor, CFANode of) {
    Assert.assertThat(successor.getPredecessor(), not(isSameNode(of)));
    Assert.assertThat(CFAUtils.allLeavingEdges(of), everyItem(not(isSameEdge(successor))));
  }
  
  
  /**
   * This checks equality of an edge via it's class and it's description. A edge
   * with an null description will never match another edge
   */
  public static class IsSameEdgeCheckedByClassAndDescritpion extends BaseMatcher<CFAEdge> {

    private CFAEdge edge;

    public IsSameEdgeCheckedByClassAndDescritpion(CFAEdge edge) {
      this.edge = edge;
    }

    @Override
    public boolean matches(Object arg0) {
      if (arg0 == null) {
        return false;
      }
      if (!(arg0 instanceof CFAEdge)) {
        return false;
      }
      if (arg0.getClass() != edge.getClass()) {
        return false;
      }
      CFAEdge compareEdge = (CFAEdge) arg0;
      if (compareEdge.getDescription() == null) {
        return false;
      }
      if (compareEdge.getDescription().equals(edge.getDescription())) {
        return true;
      }
      return false;
    }

    @Override
    public void describeTo(Description arg0) {

    }
    
    public static Matcher<CFAEdge> isSameEdge(CFAEdge edge) {
      return new IsSameEdgeCheckedByClassAndDescritpion(edge);
    }

  }
  
  
  /**
   * This checks equality of an edge via it's class and it's function name. A edge
   * with an null description will never match another edge
   */
  public static class IsSameNodeCheckedByClassAndDescritpion extends BaseMatcher<CFANode> {

    private CFANode node;

    public IsSameNodeCheckedByClassAndDescritpion(CFANode node) {
      this.node = node;
    }

    @Override
    public boolean matches(Object arg0) {
      if (arg0 == null) {
        return false;
      }
      if (!(arg0 instanceof CFANode)) {
        return false;
      }
      if (arg0.getClass() != node.getClass()) {
        return false;
      }
      CFANode compareEdge = (CFANode) arg0;
      if (node.getFunctionName() == null) {
        return false;
      }
      if (node.getFunctionName().equals(compareEdge.getFunctionName())) {
        return true;
      }
      return false;
    }

    @Override
    public void describeTo(Description arg0) {

    }

    @Factory
    public static Matcher<CFANode> isSameNode(CFANode node) {
      return new IsSameNodeCheckedByClassAndDescritpion(node);
    }
  }
  
  /**
   * This matcher will check if the node is reachable by an start node
   */
  public static class IsNodeReachableBy extends BaseMatcher<CFANode> {

    private CFANode node;

    public IsNodeReachableBy(CFANode node) {
      this.node = node;
    }

    @Override
    public boolean matches(Object arg0) {
      if (arg0 == null) {
        return false;
      }
      if(arg0 instanceof CFANode) {
        CFANode node = (CFANode) arg0;
        return reachableBy(node);
      } else if(arg0 instanceof CFAEdge) {
        CFAEdge edge = (CFAEdge) arg0;
        return reachableBy(edge);
      }
      return false;
    }
    
    public boolean reachableBy(CFANode startNode) {
      NodeReachable reachableChecker = new NodeReachable(this.node);
      CFATraversal.dfs().traverseOnce(startNode, reachableChecker);
      return reachableChecker.isReachable();
    }
    
    public boolean reachableBy(CFAEdge edge) {
      return reachableBy(edge.getSuccessor());
    }

    @Override
    public void describeTo(Description arg0) {

    }

    @Factory
    public static Matcher<CFANode> isReachable(CFANode node) {
      return new IsNodeReachableBy(node);
    }
  }
  
  public static class IsReachable extends BaseMatcher<CFAEdge> {

    private CFAEdge edge = null;
    private CFANode node = null;

    public IsReachable(CFAEdge edge) {
      this.edge = edge;
    }
    
    public IsReachable(CFANode node) {
      this.node = node;
    }    

    @Override
    public boolean matches(Object arg0) {
      if (arg0 == null) {
        return false;
      }
      if (!(arg0 instanceof CFAEdge)) {
        return false;
      }
      if (arg0.getClass() != edge.getClass()) {
        return false;
      }
      CFAEdge compareEdge = (CFAEdge) arg0;
      return false;
    }

    @Override
    public void describeTo(Description arg0) {

    }
    
    public static Matcher<CFAEdge> isEdgeReachable(CFAEdge edge) {
      return new IsReachable(edge);
    }
    
    public static Matcher<CFAEdge> isNodeReachable(CFANode node) {
      return new IsReachable(node);
    }

  }
}
