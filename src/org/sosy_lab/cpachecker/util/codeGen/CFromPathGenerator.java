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

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.*;

import java.util.*;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

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
    includes = Arrays.asList("#include <stdlib.h>");
  }

  public Appender generateSourceCode() {
    Deque<Function> callStack = new ArrayDeque<>();
    Deque<Edge> waitStack = new ArrayDeque<>();

    newFunction(rootState, callStack);
    for (Edge state : edgesOnErrorPath(rootState)) {
      waitStack.offerFirst(state);
    }

    while (!waitStack.isEmpty()) {
      Edge currentState = waitStack.pollFirst();
      Iterable<Edge> newEdges = handleEdge(currentState, callStack);

      for (Edge newEdge : newEdges) {
        waitStack.offerFirst(newEdge);
      }
    }

    Appender includes = Appenders.forIterable(Joiner.on(System.lineSeparator()), this.includes);
    Appender emptyLine = Appenders.fromToStringMethod(System.lineSeparator() + System.lineSeparator());
    Appender globals = Appenders.forIterable(Joiner.on(System.lineSeparator()), Lists.transform(this.globalDeclarations,
        new com.google.common.base.Function<CDeclarationEdge, String>() {
          
          @Nullable
          @Override
          public String apply(@Nullable CDeclarationEdge pCDeclarationEdge) {
            assert pCDeclarationEdge != null : "null was added to the globalDeclarations List";
            return pCDeclarationEdge.getCode();
          }
        }));
    Appender functions = Appenders.forIterable(Joiner.on(System.lineSeparator()), this.functions);
    
    return Appenders.concat(includes, emptyLine, globals, emptyLine, functions);
  }
  
  private Function newFunction(ARGState rootState, Deque<Function> callStack) {
    Function f = new Function(rootState);

    callStack.offerFirst(f);
    functions.add(f); // TODO what if function already in list?

    return f;
  }

  private Iterable<Edge> handleEdge(Edge edge, Deque<Function> callStack) {
    Function currentFunction = callStack.peekFirst();
    CFAEdge cfaEdge = edge.getEdge();
    ARGState child = edge.getChild();

    if (errorState.equals(child)) {
      currentFunction.add(new SimpleStatement("exit(-1337);"));
    }

    if (cfaEdge instanceof CFunctionCallEdge) {
      Function newFunc = newFunction(child, callStack);
      currentFunction.add(functionCall((CFunctionCallEdge)cfaEdge, newFunc.getName()));
    } else if (cfaEdge instanceof CFunctionReturnEdge) {
      callStack.pollFirst(); // currentFunction is removed from callstack
    } else {
      simpleEdge(edge.getEdge(), callStack);
    }

    return edgesOnErrorPath(child);
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
        SimpleStatement return0 = new SimpleStatement("exit(0);");
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

  private Iterable<Edge> edgesOnErrorPath(final ARGState state) {
    List<ARGState> children = from(state.getChildren()).filter(in(errorPathStates)).toList();

    return transform(children, new com.google.common.base.Function<ARGState, Edge>() {

      @Nullable
      @Override
      public Edge apply(ARGState pARGState) {
        return new Edge(state, pARGState);
      }
    });
  }
}
