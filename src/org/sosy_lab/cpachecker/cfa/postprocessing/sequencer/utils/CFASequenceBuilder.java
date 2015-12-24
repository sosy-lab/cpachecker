package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Preconditions;

/**
 * <p>
 * CFASequenceBuilder is cfa building utility which is capable of building a cfa
 * sequence starting from a given node. It is also possible to branch the
 * sequence by adding multiple edges to a node and continue building those with
 * a new CFASequenceBuilder.
 * </p>
 *
 * <p>
 * The CFASequenceBuilder can only build sequences within a function as it uses
 * the function name to create new nodes in the sequence which was given from
 * the first node added to the builder.
 * </p>
 */
public class CFASequenceBuilder {
  @Deprecated
  private CFANode priorNode = null;

  private boolean lockBuilder = false;
  private CFANode chainHeadNode;
  private String functionName;
  private CFANode latestChainLink;
  private CFAEdge lastAttachedEdge = null;
  private MutableCFA cfa;

  /**
   * All edges from the cfa sequence which the sequence builder.
   * This list is only used for the toString method
   */
  private List<CFAEdge> buildedSequenceEdges = new LinkedList<CFAEdge>();

  /**
   * An empty node. This can be used to create new edges for sequence builder,
   * because the nodes of these edges will be replaced anyway
   */
  public final static CFANode DUMMY_NODE = new CFANode("dummy! dont use this in cfa!");



  /**
   * A sequence builder by which edges can be appended to create a cfa sequence.
   *
   * @param chainHead
   *          - the start node of the sequence
   * @param cfa
   *          - the cfa in which the sequence is in
   */
  public CFASequenceBuilder(CFANode chainHead, MutableCFA cfa) {
    this.chainHeadNode = chainHead;
    this.latestChainLink = chainHead;
    this.functionName = chainHead.getFunctionName();
    this.cfa = cfa;
    cfa.addNode(chainHead);
  }

  /**
   * Appends a new edge to the cfa sequence. A new successor node for the edge
   * will be created
   *
   * @param newEdge
   *          - the edge which will be attached
   * @return the cfa node which is used as target node for the edge
   */
  public CFANode addChainLink(CFAEdge newEdge) {
    Preconditions.checkArgument(!lockBuilder, "Chain builder is closed!");
    Preconditions.checkArgument(newEdge != null, "Chain builder cannot append null");

    wireEdge(newEdge);

    return latestChainLink;
  }

  /**
   * Appends a new edge to the cfa sequence
   *
   * <p>
   * Note if you enter a {@link FunctionEntryNode} or {@link FunctionExitNode}
   * as target node then the sequence Builder will be closed automatically
   * because the sequence builder builds only sequences within a function
   * </p>
   *
   * @param newEdge
   *          - the edge which will be attached
   * @param targetNode
   *          - the node which will be used as successor node for the edge
   * @return the cfa node which is used as target node for the edge. This is the
   *         given target node
   */
  public CFANode addChainLink(CFAEdge newEdge, CFANode targetNode) {
    Preconditions.checkArgument(!lockBuilder, "Chain builder is closed!");
    Preconditions.checkArgument(newEdge != null && targetNode != null, "Chain builder cannot append null");

    wireEdgeTo(targetNode, newEdge);

//    if(targetNode instanceof FunctionEntryNode || targetNode instanceof FunctionExitNode) {
//      lockSequenceBuilder();
//    }

    return latestChainLink;
  }

  private void wireEdge(CFAEdge edge) {
    CFANode nextNode = new CFANode(functionName);
    wireEdgeTo(nextNode, edge);
  }

  private void wireEdgeTo(CFANode nextNode, CFAEdge edge) {
    CFAEdge newEdgeInSequence = CFAEdgeUtils.copyCEdge(edge, latestChainLink, nextNode);
    buildedSequenceEdges.add(newEdgeInSequence);
    lastAttachedEdge = newEdgeInSequence;
    cfa.addNode(nextNode);

    priorNode = latestChainLink;
    latestChainLink = nextNode;
  }

  /**
   * <p>
   * This method adds another edge to the prior node. This is required to add
   * multiple edges to a node. The method will return a new builder which allows
   * to build edges on the new branch
   * </p>
   *
   * <p>
   * Due to concept of cfa no program code will produce a node with several
   * leaving edges except ones with two assume edges. Although the cfa automata
   * might agree with several leaving edges it is recommended to use the ...
   * function to create a more reasonable cfa instead.
   * </p>
   *
   * @param edge
   *          - the edge which will be attached to the prior edge
   * @return a new instance of CfaSequenceBuilder by which edges can be appended
   *         to the new 'created branch'.
   */
  @Deprecated
  public CFASequenceBuilder addAnotherEdgeToPriorNode(CFAEdge edge) {
    assert !lockBuilder;
    if (lockBuilder) {
      throw new IllegalStateException("Chain builder is closed!");
    }

    assert edge != null;

    if (priorNode == null) {
      throw new IllegalStateException("Cannot add edge to previous node, because it hasn't one");
    }

    CFASequenceBuilder newBranch = new CFASequenceBuilder(priorNode, cfa);
    newBranch.addChainLink(edge);

    return newBranch;
  }

  /**
   * <p>
   * This method adds another edge to the prior node. This is required to add
   * multiple edges to a node. The method will return a new builder which allows
   * to build edges on the new branch
   * </p>
   *
   * <p>
   * Due to concept of cfa no program code will produce a node with several
   * leaving edges except ones with two assume edges. Although the cfa automata
   * might agree with several leaving edges it is recommended to use the ...
   * function to create a more reasonable cfa instead.
   * </p>
   *
   * @param edge
   *          - the edge which will be attached to the prior edge
   * @param targetNode
   *          - the target node on witch the appended edge will be pointing at
   * @return a new instance of CfaSequenceBuilder by which edges can be appended
   *         to the new 'created branch'.
   */
  @Deprecated
  public CFASequenceBuilder addAnotherEdgeToPriorNode(CFAEdge edge, CFANode targetNode) {
    assert !lockBuilder;
    if (lockBuilder) {
      throw new IllegalStateException("Chain builder is closed!");
    }

    assert !(targetNode instanceof FunctionEntryNode);
    assert edge != null;

    if (priorNode == null) {
      throw new IllegalStateException("Cannot add edge to previous node, because it hasn't one");
    }

    CFASequenceBuilder newBranch = new CFASequenceBuilder(priorNode, cfa);
    newBranch.addChainLink(edge, targetNode);

    return newBranch;
  }


  @Deprecated
  public CFASequenceBuilder addMultipleEdge(CFAEdge edge, CFANode targetNode) {
    Preconditions.checkNotNull(edge, "Cannot attache null");
    Preconditions.checkArgument(!lockBuilder, "Chain builder is closed!");

    CFASequenceBuilder builder;
    builder = new CFASequenceBuilder(latestChainLink, cfa);
    builder.addChainLink(edge, targetNode);

    return builder;
  }

  @Deprecated
  public CFASequenceBuilder addMultipleEdge(CFAEdge edge) {
    return addMultipleEdge(edge, new CFANode(functionName));
  }

  /**
   * <p>
   * This method adds a new assume branch to the cfa sequence. The new branch
   * can be attached by the new returned CFASequenceBuilder. The inverted assume
   * edge will be attached also to continue the sequence of this builder to the
   * prior edge. Adding several assume edges (with disjunct assume expression)
   * to one sequence builder will appear like adding of several assume edges to
   * the same node (only if assume expressions are disjunct)
   * </p>
   *
   * <p>
   *
   * <pre>
   * Example 1:
   * {@code
   * if(a == 1) {...}
   * else if(a == 2) {...}
   * else {...}
   * }
   *
   * Example 2:
   * {@code
   * if(a == 1) {
   *   ...
   *   } else {
   *     if(a == 2) {
   *       ...
   *     } else {
   *       ...
   *   }
   * }
   * </pre>
   *
   * The previous code will appear in the cfa equally. This is due to the fact,
   * that for cfa creation the first code will be interpreted like the second
   * code. The second code has a simpler structure as it uses no 'else if'. This
   * allows a simpler representation which manifest in a cfa which has only
   * branchings of maximum two (assume-)edges.
   * </p>
   *
   * <p>
   * This method provides a way to add assume edges in a way thats similar to
   * the first code as it is obviously more easy to read
   * </p>
   *
   *
   * @param appendEdge
   *          - the assume edge which will be appended to the previous node
   * @return a new instance of CfaSequenceBuilder by which edges can be appended
   *         to the new 'created branch'.
   */
  public CFASequenceBuilder addAssumeEdge(AssumeEdge appendEdge) {
    assert !lockBuilder;
    Preconditions.checkArgument(!lockBuilder, "Chain builder is closed!");
    Preconditions.checkArgument(appendEdge != null, "Cannot append null to chain");

    CFASequenceBuilder newBranch = new CFASequenceBuilder(latestChainLink, cfa);
    newBranch.addChainLink(appendEdge);
    this.addChainLink(CFAEdgeUtils.getAssumeEdgeInvertedExpression(appendEdge));

    return newBranch;
  }

  /**
   *
   * <p>
   * This method adds a new assume branch to the cfa sequence. The new branch
   * can be attached by the new returned CFASequenceBuilder. The inverted assume
   * edge will be attached also to continue the sequence of this builder to the
   * prior edge. Adding several assume edges (with disjunct assume expression)
   * to one sequence builder will appear like adding of several assume edges to
   * the same node (only if assume expressions are disjunct)
   * </p>
   *
   * <p>
   * Note if you enter a {@link FunctionEntryNode} or {@link FunctionExitNode}
   * as target node then the sequence Builder will be closed automatically
   * because the sequence builder builds only sequences within a function
   * </p>
   *
   * <p>
   *
   * <pre>
   * Example 1:
   * {@code
   * if(a == 1) {...}
   * else if(a == 2) {...}
   * else {...}
   * }
   *
   * Example 2:
   * {@code
   * if(a == 1) {
   *   ...
   *   } else {
   *     if(a == 2) {
   *       ...
   *     } else {
   *       ...
   *   }
   * }
   * </pre>
   *
   * The previous code will appear in the cfa equally. This is due to the fact,
   * that for cfa creation the first code will be interpreted like the second
   * code. The second code has a simpler structure as it uses no 'else if'. This
   * allows a simpler representation which manifest in a cfa which has only
   * branchings of maximum two (assume-)edges.
   * </p>
   *
   * <p>
   * This method provides a way to add assume edges in a way thats similar to
   * the first code as it is obviously more easy to read
   * </p>
   *
   *
   * @param appendEdge
   *          - the assume edge which will be appended to the previous node
   * @param targetNodeForAssume
   *          - the node in which the given assume edge will point at
   * @param targetNodeForInvertedAssume
   *          - the node in which the inverse assume edge of given assume edge
   *          will point at
   * @return a new instance of CfaSequenceBuilder by which edges can be appended
   *         to the new 'created branch'.
   *
   * @throws IllegalArgumentException if the given AssumeEdge or CFANode's are null
   * @throws IllegalStateException if the builder is locked
   */
  public CFASequenceBuilder addAssumeEdge(AssumeEdge appendEdge, CFANode targetNodeForAssume, CFANode targetNodeForInvertedAssume) {
    assert !lockBuilder;
    Preconditions.checkArgument(!lockBuilder, "Chain builder is closed!");
    Preconditions.checkArgument(appendEdge != null && targetNodeForAssume != null && targetNodeForInvertedAssume != null, "Cannot append null to chain");

    CFASequenceBuilder newBranch = new CFASequenceBuilder(latestChainLink, cfa);
    newBranch.addChainLink(appendEdge, targetNodeForAssume);
    addChainLink(CFAEdgeUtils.getAssumeEdgeInvertedExpression(appendEdge), targetNodeForInvertedAssume);

    if(targetNodeForAssume instanceof FunctionEntryNode || targetNodeForAssume instanceof FunctionExitNode) {
     newBranch.lockSequenceBuilder();
    }

    if(targetNodeForInvertedAssume instanceof FunctionEntryNode || targetNodeForInvertedAssume instanceof FunctionExitNode) {
      this.lockSequenceBuilder();
    }
    return newBranch;
  }


  /**
   *
   * <p>
   * This method adds a new assume branch to the cfa sequence. The new branch
   * can be attached by the new returned CFASequenceBuilder. The inverted assume
   * edge will be attached also to continue the sequence of this builder to the
   * prior edge. Adding several assume edges (with disjunct assume expression)
   * to one sequence builder will appear like adding of several assume edges to
   * the same node (only if assume expressions are disjunct)
   * </p>
   *
   * <p>
   * Note if you enter a {@link FunctionEntryNode} or {@link FunctionExitNode}
   * as target node then the sequence Builder will be closed automatically
   * because the sequence builder builds only sequences within a function
   * </p>
   *
   * <p>
   *
   * <pre>
   * Example 1:
   * {@code
   * if(a == 1) {...}
   * else if(a == 2) {...}
   * else {...}
   * }
   *
   * Example 2:
   * {@code
   * if(a == 1) {
   *   ...
   *   } else {
   *     if(a == 2) {
   *       ...
   *     } else {
   *       ...
   *   }
   * }
   * </pre>
   *
   * The previous code will appear in the cfa equally. This is due to the fact,
   * that for cfa creation the first code will be interpreted like the second
   * code. The second code has a simpler structure as it uses no 'else if'. This
   * allows a simpler representation which manifest in a cfa which has only
   * branchings of maximum two (assume-)edges.
   * </p>
   *
   * <p>
   * This method provides a way to add assume edges in a way thats similar to
   * the first code as it is obviously more easy to read
   * </p>
   *
   *
   * @param appendEdge
   *          - the assume edge which will be appended to the previous node
   * @param targetNodeForAssume
   *          - the node in which the given assume edge will point at
   * @return a new instance of CfaSequenceBuilder by which edges can be appended
   *         to the new 'created branch'.
   *
   * @throws IllegalArgumentException if the given AssumeEdge or CFANode's are null
   * @throws IllegalStateException if the builder is locked
   */
  public CFASequenceBuilder addAssumeEdge(AssumeEdge appendEdge, CFANode targetNodeForAssume) {
    return addAssumeEdge(appendEdge, targetNodeForAssume, new CFANode(functionName));
  }


  /**
   * Returns the function where the sequence builder is working in. The function
   * name is the same which is used in the chain head node
   *
   * @return the function name this sequence builder is working in.
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Returns the first node, which was used to create the sequence.
   *
   * @return the first node, which was used to create the sequence.
   */
  public CFANode getFirstNode() {
    return chainHeadNode;
  }

  /**
   * locks the sequence builder. Any use of an locked sequence builder will
   * cause in a runtime exception
   *
   * @return the last node in the sequence.
   */
  public CFANode lockSequenceBuilder() {
    lockBuilder = true;

    return latestChainLink;
  }

  /**
   * @return last target node of the last attached edge
   */
  public CFANode getLastNode() {
    return latestChainLink;
  }

  /**
   * @return Returns true if builder is locked. A locked builder can not be used
   * anymore. Every function call will result in an IllegalStateException
   */
  public boolean isLocked() {
    return lockBuilder;
  }

  /**
   * returns a representation of the cfaSequenceBuilder class. Indeed the
   * representation is the sequence from the sequence chainHeadNode to the last node @See
   * getLastNode().
   */
  @Override
  public String toString() {
    String sequence = "[ " + chainHeadNode.toString();
    CFANode node = chainHeadNode;
    for(CFAEdge edge : buildedSequenceEdges) {
      assert CFAUtils.leavingEdges(node).contains(edge);
      node = edge.getSuccessor();

      CFANode successorNode = edge.getSuccessor();
      sequence += " -- " + edge.getDescription() + " -- ";
      sequence += successorNode.toString();
    }

    if(lockBuilder) {
      sequence += "]";
    } else {
      sequence += " -- ...]";
    }

    return sequence;
  }

  /**
   * @return the last edge which was attached to the sequence by this builder.
   * This edge is an instance of CFAEdge which appears in the builded cfa. It is
   * NOT the edge which was given to an edge appender function.
   */
  public CFAEdge getLastAttachedEdge() {
    return lastAttachedEdge;
  }

  /**
   * This method simulates the adding of an edge which is already attached to
   * the sequence. This means the predecessor node of the given edge must be
   * successor node of the last attached edge.
   *
   * With other words the sequence builder will continue building the sequence
   * at the new edge.
   *
   * @param edge
   *          a CFAEdge which is already attached to the sequence.
   *
   * @return the successor edge of the given edge
   */
  @Deprecated
  public CFANode overleapEdge(CFAEdge edge) {
    CFANode predecessor = edge.getPredecessor();
    CFANode successor = edge.getSuccessor();

    Preconditions.checkArgument(edge != null);
    Preconditions.checkArgument(latestChainLink.equals(predecessor));
    Preconditions.checkArgument(cfa.getAllNodes().contains(successor));

    assert successor != null;
    assert predecessor != null;
    assert latestChainLink.equals(predecessor);
    assert cfa.getAllNodes().contains(successor);
    assert predecessor.getFunctionName().equals(this.chainHeadNode.getFunctionName());
    assert successor.getFunctionName().equals(this.chainHeadNode.getFunctionName());

    latestChainLink = successor;

    this.lastAttachedEdge = edge;
    this.buildedSequenceEdges.add(edge);

    return latestChainLink;
  }



  @Deprecated
  public static CFASequenceBuilder mergeSequence(CFASequenceBuilder firstBuilder, CFASequenceBuilder secondBuilder, CFAEdge edge) {
    return mergeSequence(firstBuilder, secondBuilder, edge, new CFANode(firstBuilder.getFunctionName()));
  }

  /**
   * This function reunions two cfa sequences by one edge. The given build must
   * have attached at least one edge to his sequence. The given
   * {@link #CFASequenceBuilder} will be closed and a new one will be returned
   * with which the sequence at the merged path can be continued.
   *
   * @param firstBuilder
   * @param secondBuilder
   * @param edge
   * @param targetNode
   */
  @Deprecated
  public static CFASequenceBuilder mergeSequence(CFASequenceBuilder firstBuilder, CFASequenceBuilder secondBuilder, CFAEdge edge, CFANode targetNode) {
    Preconditions.checkArgument(firstBuilder.getLastAttachedEdge() != null);
    Preconditions.checkArgument(secondBuilder.getLastAttachedEdge() != null);
    Preconditions.checkArgument(!firstBuilder.isLocked());
    Preconditions.checkArgument(!secondBuilder.isLocked());

    Preconditions.checkArgument(targetNode.getFunctionName().equals(firstBuilder.getFunctionName()));
    Preconditions.checkArgument(targetNode.getFunctionName().equals(secondBuilder.getFunctionName()));


    CFANode mergePoint = firstBuilder.getLastNode();
    CFASequenceBuilder mergedPath = new CFASequenceBuilder(mergePoint, firstBuilder.cfa);

    mergedPath.addChainLink(edge, targetNode);

    CFAEdge oldEdge = secondBuilder.getLastAttachedEdge();
    CFAEdgeUtils.bypassCEdgeNodes(oldEdge, oldEdge.getSuccessor() , mergePoint);

    firstBuilder.lockSequenceBuilder();
    secondBuilder.lockSequenceBuilder();

    return mergedPath;
  }

}
