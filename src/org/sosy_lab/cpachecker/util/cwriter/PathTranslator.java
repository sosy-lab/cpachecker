// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;

public abstract class PathTranslator {

  protected static CFunctionEntryNode extractFunctionCallLocation(ARGState state) {
    // We assume, that each node has one location.
    // TODO: the location is invalid for all concurrent programs,
    //       because interleaving threads are not handled.
    return FluentIterable.from(extractLocations(state))
        .filter(CFunctionEntryNode.class)
        .first()
        .orNull();
  }

  protected final List<String> mGlobalDefinitionsList = new ArrayList<>();
  protected final List<String> mFunctionDecls = new ArrayList<>();
  private int mFunctionIndex = 0;

  // list of functions
  protected final List<FunctionBody> mFunctionBodies = new ArrayList<>();

  protected PathTranslator() {}

  /**
   * Gets the piece of code that should appear at the target state; e.g. <code>assert(0)</code> or
   * <code>exit(-1)</code>
   *
   * @return Line of code for target state
   */
  protected abstract String getTargetState();

  protected Appender generateCCode() {
    return Appenders.forIterable(
        Joiner.on('\n'), concat(mGlobalDefinitionsList, mFunctionDecls, mFunctionBodies));
  }

  /**
   * Translate a single linear path to code.
   *
   * @param pPath the path to translate
   * @param callback A callback that receives each <code>ARGState</code> along with their edges and
   *     can then determine what code to generate from it. The default behavior of a <code>
   *     ProcessEdgeFunction</code> is to call {@link #processEdge(ARGState, CFAEdge, Deque)}
   */
  protected void translateSinglePath0(ARGPath pPath, EdgeVisitor callback) {
    assert pPath.size() >= 1;

    PathIterator pathIt = pPath.fullPathIterator();
    ARGState firstElement = pathIt.getAbstractState();

    Deque<FunctionBody> functionStack = new ArrayDeque<>();

    // create the first function and put in into the stack
    startFunction(firstElement, functionStack, extractFunctionCallLocation(firstElement));

    while (pathIt.hasNext()) {
      pathIt.advance();

      CFAEdge currentCFAEdge = pathIt.getIncomingEdge();
      ARGState childElement;
      if (pathIt.isPositionWithState()) {
        childElement = pathIt.getAbstractState();
      } else {
        childElement = pathIt.getPreviousAbstractState();
      }

      callback.visit(childElement, currentCFAEdge, functionStack);
    }
  }

  protected final void translatePaths0(
      final ARGState firstElement, Set<ARGState> elementsOnPath, EdgeVisitor callback) {
    // waitlist for the edges to be processed
    List<Edge> waitlist = new ArrayList<>();

    // map of nodes to check end of a condition
    Map<Integer, MergeNode> mergeNodes = new HashMap<>();

    // create initial element
    {
      Deque<FunctionBody> newStack = new ArrayDeque<>();

      // create the first function and put in into newStack
      startFunction(firstElement, newStack, extractFunctionCallLocation(firstElement));

      waitlist.addAll(getRelevantChildrenOfState(firstElement, newStack, elementsOnPath));
    }

    while (!waitlist.isEmpty()) {
      // we need to sort the list based on arg element id because we have to process
      // the edges in topological sort
      Collections.sort(waitlist);

      // get the first element in the list (this is the smallest element when topologically sorted)
      Edge nextEdge = waitlist.remove(0);

      waitlist.addAll(handleEdge(nextEdge, mergeNodes, elementsOnPath, callback));
    }
  }

  /**
   * Start the function, puts another body on the function stack.
   *
   * @param firstFunctionElement the first state inside the function
   * @param functionStack the current callstack
   * @param predecessor the previous node
   */
  protected String startFunction(
      ARGState firstFunctionElement, Deque<FunctionBody> functionStack, CFANode predecessor) {
    // create the first stack element using the first element of the function
    CFunctionEntryNode functionStartNode = extractFunctionCallLocation(firstFunctionElement);
    String freshFunctionName = getFreshFunctionName(functionStartNode);

    String lFunctionHeader =
        functionStartNode.getFunctionDefinition().getType().toASTString(freshFunctionName);
    // lFunctionHeader is for example "void foo_99(int a)"

    // create a new function
    FunctionBody newFunction = new FunctionBody(firstFunctionElement.getStateId(), lFunctionHeader);

    // register function
    mFunctionDecls.add(lFunctionHeader + ";");
    mFunctionBodies.add(newFunction);
    functionStack.push(newFunction); // add function to current stack
    return freshFunctionName;
  }

  /**
   * Processes an edge of the CFA and will write code to the output function body.
   *
   * @param childElement the state after the given edge
   * @param edge the edge to process
   * @param functionStack the current callstack
   */
  void processEdge(ARGState childElement, CFAEdge edge, Deque<FunctionBody> functionStack) {
    FunctionBody currentFunction = functionStack.peek();

    if (childElement.isTarget()) {
      currentFunction.write(getTargetState());
    }

    // handle the edge

    if (edge instanceof CFunctionCallEdge) {
      // if this is a function call edge we need to create a new state and push
      // it to the topmost stack to represent the function

      // create function and put in onto stack
      String freshFunctionName = startFunction(childElement, functionStack, edge.getPredecessor());

      // write summary edge to the caller site (with the new unique function name)
      currentFunction.write(processFunctionCall(edge, freshFunctionName));

    } else if (edge instanceof CFunctionReturnEdge) {
      functionStack.pop();

    } else {
      currentFunction.write(processSimpleEdge(edge, currentFunction.getCurrentBlock()));
    }
  }

  private Collection<Edge> handleEdge(
      Edge nextEdge,
      Map<Integer, MergeNode> mergeNodes,
      Set<ARGState> elementsOnPath,
      EdgeVisitor callback) {
    ARGState childElement = nextEdge.getChildState();
    CFAEdge edge = nextEdge.getEdge();
    Deque<FunctionBody> functionStack = nextEdge.getStack();

    // clone stack to have a different representation of the function calls and conditions
    // for every element
    functionStack = cloneStack(functionStack);

    // we do not have a single edge, instead a dynamic multi-edge
    if (edge == null) {
      List<CFAEdge> edges = nextEdge.getParentState().getEdgesToChild(childElement);
      for (CFAEdge inner : edges) {
        callback.visit(childElement, inner, functionStack);
      }
    } else {
      callback.visit(childElement, edge, functionStack);
    }

    // how many parents does the child have?
    // ignore parents not on the error path
    int noOfParents = from(childElement.getParents()).filter(in(elementsOnPath)).size();
    assert noOfParents >= 1;

    // handle merging if necessary
    if (noOfParents > 1) {
      assert !((edge instanceof CFunctionCallEdge) || childElement.isTarget());

      // this is the end of a condition, determine whether we should continue or backtrack

      int elemId = childElement.getStateId();
      FunctionBody currentFunction = functionStack.peek();
      currentFunction.write("goto label_" + elemId + ";");

      // get the merge node for that node
      MergeNode mergeNode = mergeNodes.get(elemId);
      // if null create new and put in the map
      if (mergeNode == null) {
        mergeNode = new MergeNode(elemId);
        mergeNodes.put(elemId, mergeNode);
      }

      // this tells us the number of edges (entering that node) processed so far
      int noOfProcessedBranches = mergeNode.addBranch(currentFunction);

      // if all edges are processed
      if (noOfParents == noOfProcessedBranches) {
        // all branches are processed, now decide which nodes to remove from the stack
        List<FunctionBody> incomingStacks = mergeNode.getIncomingStates();

        FunctionBody newFunction = processIncomingStacks(incomingStacks);

        // replace the current function body with the right one
        functionStack.pop();
        functionStack.push(newFunction);

        newFunction.write("label_" + elemId + ": ;");

      } else {
        return ImmutableSet.of();
      }
    }

    return getRelevantChildrenOfState(childElement, functionStack, elementsOnPath);
  }

  private Collection<Edge> getRelevantChildrenOfState(
      ARGState currentElement, Deque<FunctionBody> functionStack, Set<ARGState> elementsOnPath) {
    // find the next elements to add to the waitlist

    List<ARGState> relevantChildrenOfElement =
        from(currentElement.getChildren()).filter(in(elementsOnPath)).toList();
    relevantChildrenOfElement = chooseIfArbitrary(currentElement, relevantChildrenOfElement);

    switch (relevantChildrenOfElement.size()) {
      case 0:
        return ImmutableList.of();

      case 1:
        { // If there is only one child on the path, get the next ARG state, create a new edge using
          // the same stack and add it to the waitlist.
          ARGState elem = Iterables.getOnlyElement(relevantChildrenOfElement);
          CFAEdge e = currentElement.getEdgeToChild(elem);
          return ImmutableList.of(new Edge(elem, currentElement, e, functionStack));
        }

      case 2:
        { // If there are more than one relevant child, then this is a condition.
          // We need to update the stack.
          ARGState child1 = relevantChildrenOfElement.get(0);
          ARGState child2 = relevantChildrenOfElement.get(1);
          CAssumeEdge edge1 = (CAssumeEdge) currentElement.getEdgeToChild(child1);
          CAssumeEdge edge2 = (CAssumeEdge) currentElement.getEdgeToChild(child2);
          verify(edge1.getExpression().equals(edge2.getExpression()));
          verify(edge1.getTruthAssumption() != edge2.getTruthAssumption());

          if (edge2.getTruthAssumption()) {
            // swap edges such that edge1 is the positive one
            ARGState tmpState = child1;
            CAssumeEdge tmpEdge = edge1;
            child1 = child2;
            edge1 = edge2;
            child2 = tmpState;
            edge2 = tmpEdge;
          }

          String cond = "if (" + edge1.getExpression().toASTString() + ")";

          return ImmutableList.of(
              createNewBasicBlock(currentElement, child1, edge1, cond, functionStack),
              createNewBasicBlock(currentElement, child2, edge2, "else", functionStack));
        }
      default:
        throw new AssertionError();
    }
  }

  private List<ARGState> chooseIfArbitrary(
      ARGState parent, List<ARGState> pRelevantChildrenOfElement) {
    if (pRelevantChildrenOfElement.size() <= 1) {
      return pRelevantChildrenOfElement;
    }
    final Comparator<ARGState> childCountComparator =
        Comparator.<ARGState>comparingInt(s -> s.getChildren().size()).reversed();
    List<ARGState> relevantChildrenOfElement =
        ImmutableList.sortedCopyOf(childCountComparator, pRelevantChildrenOfElement);
    List<ARGState> result = new ArrayList<>(2);
    for (ARGState candidate : relevantChildrenOfElement) {
      boolean valid = true;
      if (!result.isEmpty()) {
        Set<AbstractState> candidateChildren =
            transformedImmutableSetCopy(candidate.getChildren(), ARGState::getWrappedState);
        for (ARGState chosen : result) {
          if (parent.getEdgesToChild(chosen).equals(parent.getEdgesToChild(candidate))) {
            Set<AbstractState> chosenChildren =
                transformedImmutableSetCopy(chosen.getChildren(), ARGState::getWrappedState);
            if (chosenChildren.containsAll(candidateChildren)) {
              valid = false;
              break;
            }
          }
        }
      }
      if (valid) {
        result.add(candidate);
      }
    }
    return result;
  }

  private Edge createNewBasicBlock(
      ARGState parent,
      ARGState child,
      CAssumeEdge edge,
      String cond,
      Deque<FunctionBody> functionStack) {
    Deque<FunctionBody> newStack = cloneStack(functionStack);
    FunctionBody currentFunction = newStack.peek();
    currentFunction.enterBlock(parent.getStateId(), edge, cond);

    // Create dummy edge as next edge, otherwise we would get a __CPROVER_assume() statement that
    // just duplicates the condition.
    BlankEdge dummyEdge =
        new BlankEdge(
            edge.getRawStatement(),
            edge.getFileLocation(),
            edge.getPredecessor(),
            edge.getSuccessor(),
            "");
    return new Edge(child, parent, dummyEdge, newStack);
  }

  private static FunctionBody processIncomingStacks(List<FunctionBody> pIncomingStacks) {

    FunctionBody maxStack = null;
    int maxSizeOfStack = 0;

    for (FunctionBody stack : pIncomingStacks) {
      while (stack.getCurrentBlock().isClosedBefore()) {
        stack.leaveBlock();
      }
      if (stack.size() > maxSizeOfStack) {
        maxStack = stack;
        maxSizeOfStack = maxStack.size();
      }
    }

    return maxStack;
  }

  protected String processSimpleEdge(CFAEdge pCFAEdge, BasicBlock currentBlock) {

    switch (pCFAEdge.getEdgeType()) {
      case BlankEdge:
      case StatementEdge:
      case ReturnStatementEdge:
        return pCFAEdge.getCode();

      case AssumeEdge:
        {
          CAssumeEdge lAssumeEdge = (CAssumeEdge) pCFAEdge;
          return ("__CPROVER_assume(" + lAssumeEdge.getCode() + ");");
          //    return ("if(! (" + lAssumptionString + ")) { return (0); }");
        }

      case DeclarationEdge:
        {
          CDeclarationEdge lDeclarationEdge = (CDeclarationEdge) pCFAEdge;

          if (lDeclarationEdge.getDeclaration().isGlobal()) {
            mGlobalDefinitionsList.add(lDeclarationEdge.getCode());
            return "";
          }

          // avoid having the same declaration edge twice in one basic block
          if (currentBlock.hasDeclaration(lDeclarationEdge)) {
            return "";
          } else {
            currentBlock.addDeclaration(lDeclarationEdge);
            return lDeclarationEdge.getCode();
          }
        }

      default:
        throw new AssertionError(
            "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType());
    }
  }

  protected final String processFunctionCall(CFAEdge pCFAEdge, String functionName) {

    CFunctionCallEdge lFunctionCallEdge = (CFunctionCallEdge) pCFAEdge;

    List<String> lArguments =
        Lists.transform(lFunctionCallEdge.getArguments(), CExpression::toASTString);
    String lArgumentString = "(" + Joiner.on(", ").join(lArguments) + ")";

    CFunctionSummaryEdge summaryEdge = lFunctionCallEdge.getSummaryEdge();
    if (summaryEdge == null) {
      // no summary edge, i.e., no return to this function (CFA was pruned)
      // we don't need to care whether this was an assignment or just a function call
      return functionName + lArgumentString + ";";
    }

    CFunctionCall expressionOnSummaryEdge = summaryEdge.getExpression();
    if (expressionOnSummaryEdge instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp =
          (CFunctionCallAssignmentStatement) expressionOnSummaryEdge;
      String assignedVarName = assignExp.getLeftHandSide().toASTString();
      return assignedVarName + " = " + functionName + lArgumentString + ";";

    } else {
      assert expressionOnSummaryEdge instanceof CFunctionCallStatement;
      return functionName + lArgumentString + ";";
    }
  }

  protected final String getFreshFunctionName(FunctionEntryNode functionStartNode) {
    return functionStartNode.getFunctionName() + "_" + mFunctionIndex++;
  }

  private Deque<FunctionBody> cloneStack(Deque<FunctionBody> pStack) {

    Deque<FunctionBody> ret = new ArrayDeque<>();
    for (FunctionBody functionBody : pStack) {
      ret.add(new FunctionBody(functionBody));
    }
    return ret;
  }
}
