package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.util.CFATraversal;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class CFAFunctionUtils {

  public final static String INIT_GLOBAL_VARS = "INIT GLOBAL VARS";

  public final static String DEFAULT_RETURN = "default return";

  public final static Predicate<FunctionEntryNode> isFunctionEntryName(
      final String... functionNames) {

    return new Predicate<FunctionEntryNode>() {
      private final String[] filterNames = functionNames;

      @Override
      public boolean apply(FunctionEntryNode arg0) {
        for (String filterName : filterNames) {
          return arg0.getFunctionDefinition().getName().equals(filterName);
        }
        return false;
      }
    };
  }

  public final static Predicate<AStatementEdge> isStatementEdgeName(
      final String... functionNames) {

    return new Predicate<AStatementEdge>() {
      private String[] filterNames = functionNames;

      @Override
      public boolean apply(AStatementEdge arg0) {
        if (!isFunctionCallStatement(arg0)) {
          return false;
        }

        AStatement statement = arg0.getStatement();
        AFunctionDeclaration declaration = ((AFunctionCall) statement)
            .getFunctionCallExpression().getDeclaration();

        if(declaration == null) {
          return false;
        }

        String functionCallStatementName = declaration.getName();

        for (String filterName : filterNames) {
          if (filterName.equals(functionCallStatementName)) {
            return true;
          }
        }
        return false;
      }

    };

  }

  public static Collection<AStatementEdge> filterFunctionCallStatementByNames(
      Collection<AStatementEdge> functionCalls, final String... functionNames) {


    return Collections2.filter(functionCalls, isStatementEdgeName(functionNames));
  }

  public static boolean isFunctionCallStatement(final CFAEdge cfaEdge) {
    if(cfaEdge instanceof AStatementEdge) {
      return ((AStatementEdge) cfaEdge).getStatement() instanceof AFunctionCall;
    }
    return false;
  }

  public static Collection<AStatementEdge> getAllFunctionCallEdgesReachableBy(CFA cfa, FunctionEntryNode reachableBy, int depth) {
      Collection<AStatementEdge> statementEdge = new HashSet<AStatementEdge>();

      FunctionCallCollector visitor = new FunctionCallCollector();
      CFATraversal.dfs().traverseOnce(reachableBy, visitor);

      // TODO remove! debug only
      if(depth > 1000) {
        throw new UnsupportedOperationException("For debug only. pthread createstatements reached 1000. Most likely an unsupported recursive creation");
      }

      statementEdge.addAll(PThreadUtils.filterPthreadFunctionCallStatements(visitor.getFunctionCalls()));
      for(AStatementEdge statement : visitor.getFunctionCalls()) {
        String functionName = CFAFunctionUtils.getFunctionName(statement);
        reachableBy = cfa.getFunctionHead(functionName);

        // external function cannot be explored!
        if(reachableBy != null) {
          statementEdge.addAll(getAllFunctionCallEdgesReachableBy(cfa, reachableBy, depth+1));
        }

      }
      return statementEdge;
  }

  private static void stubNodeOfChain(CFANode start, CFANode end) {
    assert start.getFunctionName().equals(end.getFunctionName());
    assert start.getNumLeavingEdges() == 1;
    assert end.getNumEnteringEdges() == 1;
    assert CFAEdgeUtils.isNodeReachableByNode(end, start);

    CFANode firstUnusedNode = start.getLeavingEdge(0).getSuccessor();

    CFAEdgeUtils.removeLeavingEdges(start);
    CFAEdgeUtils.removeEnteringEdges(end);


    BlankEdge stub = new BlankEdge("stub", FileLocation.DUMMY, start, end, "");
    CFACreationUtils.addEdgeUnconditionallyToCFA(stub);

    CFACreationUtils.removeChainOfNodesFromCFA(firstUnusedNode);
  }

  public static void stubFunction(FunctionEntryNode start) {
    stubNodeOfChain(start, start.getExitNode());
  }

  public static String getFunctionName(AStatementEdge pEdge) {
    if (CFAFunctionUtils.isFunctionCallStatement(pEdge)) {
      return getFunctionName((AFunctionCall) pEdge.getStatement());
    }
    return null;
  }

  public static boolean isExternFunction(AStatementEdge edge, CFA cfa) {
    Preconditions.checkArgument(isFunctionCallStatement(edge));

    AFunctionCall functionCall = (AFunctionCall) edge.getStatement();
    return isExternFunctionCall(functionCall, cfa);
  }

  public static boolean isExternFunctionCall(AFunctionCall functionCall, CFA cfa) {
    ADeclaration declaration = functionCall.getFunctionCallExpression().getDeclaration();
    if(declaration == null) {
      return true;
    }
    String functionName = declaration.getName();
    return !isFunctionDeclared(functionCall) || cfa.getFunctionHead(functionName) == null;
  }

  public static boolean isFunctionDeclared(AFunctionCall functionCall) {
    ADeclaration declaration = functionCall.getFunctionCallExpression().getDeclaration();
    return declaration != null;
  }

  /**
   * Returns the function name form the edge if this edge is from type statement
   * edge and has a Function call statement. Returns null if it is no function
   * call edge
   *
   * @param functionCall
   * @return
   */
  public static String getFunctionName(AFunctionCall functionCall) {
    assert functionCall.getFunctionCallExpression().getDeclaration() != null : "Function call " + functionCall + "has no declaration";
    return functionCall.getFunctionCallExpression().getDeclaration().getName();
  }


  public static CFunctionEntryNode cloneCFunction(CFunctionEntryNode startNode, String newFunctionsName, MutableCFA cfa) throws IllegalArgumentException {
    Preconditions.checkNotNull(startNode);
    Preconditions.checkNotNull(newFunctionsName);
    Preconditions.checkNotNull(cfa);

    if(cfa.getAllFunctionNames().contains(newFunctionsName)) {
      throw new IllegalArgumentException("the function name for the new cloned function is already in the cfa!");
    }

    CFunctionDeclaration originalDeclaration = startNode.getFunctionDefinition();
    List<CParameterDeclaration> originalParameters = originalDeclaration.getParameters();
    CFunctionType originalCType = originalDeclaration.getType();

    FunctionExitNode cloneExit = new FunctionExitNode(newFunctionsName);
    CFunctionDeclaration cloneDeclaration = new CFunctionDeclaration(FileLocation.DUMMY, originalCType, newFunctionsName,
        originalParameters);
    CFunctionEntryNode cloneStart = new CFunctionEntryNode(FileLocation.DUMMY, cloneDeclaration, cloneExit,
        startNode.getFunctionParameterNames(), startNode.getReturnVariable());
    cloneExit.setEntryNode(cloneStart);
    cfa.addFunction(cloneStart);

    FunctionCloner.CLONER.cloneFunction(startNode, cloneStart, cfa);

    return cloneStart;
  }

  /**
   * Use the function cloner instead {@link org.sosy_lab.cpachecker.cfa.postprocessing.global.FunctionCloner}
   */
  @Deprecated
  private static class FunctionCloner {
    protected static FunctionCloner CLONER = new FunctionCloner();
    private Map<CFANode, CFANode> oldNewNodeMapping = new HashMap<CFANode, CFANode>();
    private String newFunctionName = null;
    private MutableCFA cfa;

    public void cloneFunction(FunctionEntryNode startNode, FunctionEntryNode newFunctionEntryNode, MutableCFA cfa) {
      this.cfa = cfa;
      newFunctionName = newFunctionEntryNode.getFunctionName();
      oldNewNodeMapping.put(startNode, newFunctionEntryNode);
      oldNewNodeMapping.put(startNode.getExitNode(), newFunctionEntryNode.getExitNode());
      CFASequenceBuilder builder = new CFASequenceBuilder(newFunctionEntryNode, cfa);
      startRecursiveCopying(startNode, builder);

      // clear cloner
      oldNewNodeMapping.clear();
      newFunctionName = null;
      this.cfa = null;
    }

    private void startRecursiveCopying(CFANode predecessorNode, CFASequenceBuilder builder) {
      assert oldNewNodeMapping.containsKey(predecessorNode);
      assert !builder.isLocked();

      switch (predecessorNode.getNumLeavingEdges()) {
      case 0:
        break;
      case 1: {
        CFAEdge edge = predecessorNode.getLeavingEdge(0);
        CFANode successorNode = edge.getSuccessor();
        CFANode newSuccessorNode;

        if (oldNewNodeMapping.containsKey(successorNode)) {
          CFANode clonedNode = oldNewNodeMapping.get(successorNode);
          newSuccessorNode = builder.addChainLink(edge, clonedNode);
          assert newSuccessorNode == clonedNode;
          builder.lockSequenceBuilder();
        } else {
          newSuccessorNode = builder.addChainLink(edge);
          oldNewNodeMapping.put(successorNode, newSuccessorNode);
          startRecursiveCopying(edge.getSuccessor(), builder);
        }
      }
      break;
      case 2: {
        CFAEdge edge1 = predecessorNode.getLeavingEdge(0);
        CFAEdge edge2 = predecessorNode.getLeavingEdge(1);
        Preconditions.checkArgument((edge1 instanceof AssumeEdge) && (edge2 instanceof AssumeEdge));
        CFANode successorNode1 = edge1.getSuccessor();
        CFANode successorNode2 = edge2.getSuccessor();
        CFANode newSuccessorNode;

        if (oldNewNodeMapping.containsKey(successorNode1)) {
          CFANode clonedNode = oldNewNodeMapping.get(successorNode1);
          newSuccessorNode = builder.addChainLink(edge1, clonedNode);
          assert newSuccessorNode == clonedNode;
          // do not lock builder for second branch
          // builder.lockSequenceBuilder();
        } else {
          newSuccessorNode = builder.addChainLink(edge1);
          oldNewNodeMapping.put(successorNode1, newSuccessorNode);
          startRecursiveCopying(successorNode1, new CFASequenceBuilder(newSuccessorNode, cfa));
        }

        if (oldNewNodeMapping.containsKey(successorNode2)) {
          CFANode clonedNode = oldNewNodeMapping.get(successorNode2);

          /*
           * TODO check if an check similar to
           * Preconditions.checkArgument(assumeEdge1.equals(assumeEdge2)) is
           * possible. Note equals checks the reference!
           */
          builder = builder.addAnotherEdgeToPriorNode(edge2, clonedNode);
          builder.lockSequenceBuilder();
        } else {
          assert newFunctionName != null;
          newSuccessorNode = new CFANode(newFunctionName);
          builder = builder.addAnotherEdgeToPriorNode(edge2, newSuccessorNode);
          oldNewNodeMapping.put(successorNode2, newSuccessorNode);
          startRecursiveCopying(successorNode2, builder);
        }
      }
      break;

      default:
        assert false : "CFA is infeasable. An node cannot have more than 2 successors";
      }
    }

  }
}
