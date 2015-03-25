/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.codeGen;

import java.util.*;
import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class CFromPathGenerator {

  private final ARGState rootState;
  private final ARGState errorState;
  private final Set<ARGState> errorPathStates;

  private List<String> includes;
  private List<CDeclarationEdge> globalDeclarations;
  private List<Function> functions;

  public CFromPathGenerator(ARGState pRootState, ARGState pErrorState, Set<ARGState> pErrorPathStates) {
    this.rootState = pRootState;
    this.errorState = pErrorState;
    this.errorPathStates = pErrorPathStates;
    this.globalDeclarations = new ArrayList<>();
    this.functions = new ArrayList<>();
    this.includes = new ArrayList<>(Arrays.asList("#include <stdlib.h>"));
  }

  public Appender generateSourceCode() {
    Deque<Function> callStack = new ArrayDeque<>();
    Deque<Edge> waitStack = new ArrayDeque<>();

    newFunction(rootState, callStack);
    List<Edge> edgesToChildrenOnErrorPath = edgesToChildrenOnErrorPath(rootState, callStack);
    for (Edge state : edgesToChildrenOnErrorPath) {
      waitStack.offerFirst(state);
    }

    while (!waitStack.isEmpty()) {
      Edge currentState = waitStack.pollFirst();
      Iterable<Edge> newEdges = handleEdge(currentState);

      for (Edge newEdge : newEdges) {
        waitStack.offerFirst(newEdge);
      }
    }

    postprocessing();

    Appender includes = Appenders.forIterable(Joiner.on(System.lineSeparator()), this.includes);
    Appender emptyLine = Appenders.fromToStringMethod(System.lineSeparator() + System.lineSeparator());

    List<String> globalDeclStrings = new ArrayList<>(globalDeclarations.size());

    for (CDeclarationEdge globalDeclaration : globalDeclarations) {
      globalDeclStrings.add(globalDeclaration.getCode());
    }

    List<String> prototypeStrings = new ArrayList<>(functions.size());

    for (Function function : functions) {
      String prototype = function.getPrototype();

      if (!globalDeclStrings.contains(prototype)) {
        prototypeStrings.add(prototype);
      }
    }

    Appender prototypes = Appenders.forIterable(Joiner.on(System.lineSeparator()), prototypeStrings);
    Appender globals = Appenders.forIterable(Joiner.on(System.lineSeparator()), globalDeclStrings);
    Appender functions = Appenders.forIterable(Joiner.on(System.lineSeparator()), this.functions);

    return Appenders.concat(includes, emptyLine, globals, emptyLine, prototypes, emptyLine, functions);
  }

  private void postprocessing() {
    Map<String, List<Function>> equivClasses = new HashMap<>();

    for (Function f : functions) {
      String name = f.getName(false);

      if (equivClasses.containsKey(name)) {
        equivClasses.get(name).add(f);
      } else {
        equivClasses.put(name, new ArrayList<>(Arrays.asList(f)));
      }
    }

    for (Map.Entry<String, List<Function>> entry : equivClasses.entrySet()) {
      Map<String, List<Function>> bodyToFunctions = new HashMap<>();

      for (Function f : entry.getValue()) {
        String body = f.getBody("");

        if (bodyToFunctions.containsKey(body)) {
          bodyToFunctions.get(body).add(f);
        } else {
          bodyToFunctions.put(body, new ArrayList<>(Arrays.asList(f)));
        }
      }

      for (Map.Entry<String, List<Function>> toMerge : bodyToFunctions.entrySet()) {
        if (toMerge.getValue().size() == 1) {
          continue;
        }

        List<Function> fToMerge = toMerge.getValue();
        Function keep = fToMerge.remove(0);

        functions.removeAll(fToMerge);

        for (Function function : functions) {
          for (Function toReplace : fToMerge) {
            function.replaceFunction(toReplace, keep);
          }
        }
      }
    }
  }

  private Function newFunction(ARGState rootState, Deque<Function> callStack) {
    Function f = new Function(rootState);

    callStack.offerFirst(f);
    functions.add(f); // TODO what if function already in list?

    return f;
  }

  private Iterable<Edge> handleEdge(Edge edge) {
    Deque<Function> callStack = edge.getCallStack();
    Function currentFunction = callStack.peekFirst();
    CFAEdge cfaEdge = edge.getEdge();
    ARGState child = edge.getChild();

    if (errorState.equals(child)) {
      currentFunction.add(new SimpleStatement("exit(-1337);"));
    }

    if (cfaEdge instanceof CFunctionCallEdge) {
      Function newFunc = newFunction(child, callStack);
      currentFunction.add(functionCall((CFunctionCallEdge) cfaEdge, newFunc.getName()));
    } else if (cfaEdge instanceof CFunctionReturnEdge) {
      callStack.pollFirst(); // currentFunction is removed from callstack
    } else {
      simpleEdge(edge.getEdge(), callStack);
    }

    return edgesToChildrenOnErrorPath(child, callStack);
  }

  private SimpleStatement functionCall(CFunctionCallEdge fCallEdge, String functionName) {
    List<String> lArguments = Lists.transform(fCallEdge.getArguments(),

            new com.google.common.base.Function<CExpression, String>() {

              @Nullable
              @Override
              public String apply(@Nullable CExpression pCExpression) {
                return (pCExpression == null) ? " " : pCExpression.toASTString();
              }
            });

    String lArgumentString = "(" + Joiner.on(", ").join(lArguments) + ")";

    CFunctionSummaryEdge summaryEdge = fCallEdge.getSummaryEdge();
    String fCall = functionName + lArgumentString + ";";
    if (summaryEdge == null) {

      // no summary edge, i.e., no return to this function (CFA was pruned)
      // we don't need to care whether this was an assignment or just a function call
      return new SimpleStatement(fCall);
    }

    CFunctionCall expressionOnSummaryEdge = summaryEdge.getExpression();
    if (expressionOnSummaryEdge instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp = (CFunctionCallAssignmentStatement) expressionOnSummaryEdge;
      String assignedVarName = assignExp.getLeftHandSide().toASTString();

      return new SimpleStatement(assignedVarName + " = " + fCall);
    } else {
      assert expressionOnSummaryEdge instanceof CFunctionCallStatement;

      return new SimpleStatement(fCall);
    }
  }

  private void simpleEdge(CFAEdge edge, Deque<Function> callStack) {
    Function currentFunction = callStack.peekFirst();

    String code = edge.getCode();
    switch (edge.getEdgeType()) {

      case BlankEdge:
      case StatementEdge:
      case ReturnStatementEdge:
        currentFunction.add(new SimpleStatement(code));

        break;
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) edge;
        SimpleStatement return0 = new SimpleStatement("exit(0); // error path left");
        currentFunction.enterBlock("if (!(" + assumeEdge.getCode() + "))").add(return0);
        break;
      case DeclarationEdge:
        // TODO global declarations, re-declaration

        CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        boolean global = declarationEdge.getDeclaration().isGlobal();

        if (global) {
          globalDeclarations.add(declarationEdge);
        } else {
          currentFunction.addDeclaration(declarationEdge, globalDeclarations);
        }

        break;
      case MultiEdge:

        for (CFAEdge e : (MultiEdge) edge) {
          simpleEdge(e, callStack);
        }
        break;
    }
  }

  private List<Edge> edgesToChildrenOnErrorPath(final ARGState state, Deque<Function> callStack) {
    assert state.getChildren().size() <= 2;

    List<Edge> edges = new LinkedList<>();
    List<ARGState> childrenOnErrorPath = new ArrayList<>();

    for (ARGState child : state.getChildren()) {
      if (errorPathStates.contains(child)) {
        childrenOnErrorPath.add(child);
      }
    }

    if (childrenOnErrorPath.size() == 1) {
      edges.add(new Edge(state, childrenOnErrorPath.get(0), callStack));
    } else if (childrenOnErrorPath.size() == 2) {
      boolean isFirstChild = true;
      for (ARGState child : childrenOnErrorPath) {
        Deque<Function> clonedStack = cloneStack(callStack);
        Function currentFunction = clonedStack.peek();

        // multiple children should only occur after if statements
        CAssumeEdge assumeEdge = (CAssumeEdge) state.getEdgeToChild(child);
        boolean truthAssumption = assumeEdge.getTruthAssumption();

        StringBuilder condition = new StringBuilder();

        if (isFirstChild) {
          condition.append("if ");
          isFirstChild = false;
        } else {
          condition.append("else if ");
        }

        if (truthAssumption) {
          condition.append("(").append(assumeEdge.getExpression().toASTString()).append(")");
        } else {
          condition.append("(!(").append(assumeEdge.getExpression().toASTString()).append("))");
        }

        // create a new block starting with this condition
        BlockStatement openedBlock = currentFunction.enterBlock(condition.toString());

        edges.add(new Edge(state, child, clonedStack));
      }
    }

    return edges;
  }

  private Deque<Function> cloneStack(Deque<Function> callStack) {
      Deque<Function> cloneStack = new ArrayDeque<>();

      for (Function function : callStack) {
        Function clone = function.clone();
        functions.add(clone); // TODO this does not work this way!
        cloneStack.offerLast(clone);
      }

      return cloneStack;
    }
  }
