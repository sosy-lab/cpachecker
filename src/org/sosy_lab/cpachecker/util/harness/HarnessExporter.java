/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "testHarnessExport")
public class HarnessExporter {

  private static final String RETVAL_NAME = "retval";
  private static final int ERR_REACHED_CODE = 107;

  private final CFA cfa;

  private final LogManager logger;

  private final HackyOptions hackyOptions = new HackyOptions();

  @Option(secure = true, description = "Use the counterexample model to provide test-vector values")
  private boolean useModel = true;

  /**
   * This is a temporary hack to easily obtain verification tasks. TODO: Obtain the values without
   * this hack.
   */
  @Options
  private static class HackyOptions {

    @Option(
      secure = true,
      name = "analysis.programNames",
      description = "A String, denoting the programs to be analyzed"
    )
    private String programs;
  }

  public HarnessExporter(Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    cfa = pCFA;
    logger = pLogger;
    pConfig.inject(hackyOptions);
    pConfig.inject(this);
  }

  public void writeHarness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      CounterexampleInfo pCounterexampleInfo)
      throws IOException {

    // Find a path with sufficient test vector info
    Optional<TestVector> testVector =
        extractTestVector(
            pRootState, pIsRelevantState, pIsRelevantEdge, getValueMap(pCounterexampleInfo));
    if (testVector.isPresent()) {

      // #include <stdlib.h> for exit function
      appendln(pTarget, "#include <stdlib.h>");
      // implement __VERIFIER_error with exit(EXIT_FAILURE)
      appendln(pTarget, "void __VERIFIER_error(void) { exit(" + ERR_REACHED_CODE + "); }");
      // implement __VERIFIER_assume with exit (EXIT_SUCCESS)
      appendln(pTarget, "void __VERIFIER_assume(int cond) { if (!(cond)) { exit(EXIT_SUCCESS); }}");

      // implement actual harness
      TestVector vector = testVector.get();
      for (AFunctionDeclaration inputFunction : vector.getInputFunctions()) {
        List<ALiteralExpression> inputValues = vector.getInputValues(inputFunction);
        String inputFunctionVectorIndexName = inputFunction.getName() + "_index__";
        if (inputValues.size() > 1) {
          appendVectorIndexDeclaration(pTarget, inputFunctionVectorIndexName);
        }
        pTarget.append(declare(inputFunction));
        appendln(pTarget, " {");
        Type returnType = inputFunction.getType().getReturnType();
        pTarget.append("  ");
        appendDeclaration(pTarget, returnType, RETVAL_NAME);
        if (inputValues.size() == 1) {
          pTarget.append("  ");
          appendAssignment(pTarget, RETVAL_NAME, inputValues.iterator().next());
          appendln(pTarget, ";");
        } else if (inputValues.size() > 1) {
          pTarget.append("  switch (");
          pTarget.append(inputFunctionVectorIndexName);
          appendln(pTarget, ") {");
          int i = 0;
          for (ALiteralExpression value : inputValues) {
            pTarget.append("    case ");
            pTarget.append(Integer.toString(i));
            pTarget.append(": ");
            appendAssignment(pTarget, RETVAL_NAME, value);
            appendln(pTarget, "; break;");
            ++i;
          }
          appendln(pTarget, "}");
          pTarget.append("  ++");
          pTarget.append(inputFunctionVectorIndexName);
          appendln(pTarget, ";");
        }
        pTarget.append("  return ");
        pTarget.append(RETVAL_NAME);
        appendln(pTarget, ";");
        appendln(pTarget, "}");
      }
    } else {
      logger.log(
          Level.INFO, "Could not export a test harness, some test-vector values are missing.");
    }
  }

  private String declare(AFunctionDeclaration pInputFunction) {
    return enforceParameterNames(pInputFunction).toASTString(pInputFunction.getName());
  }

  private AFunctionType enforceParameterNames(AFunctionDeclaration pInputFunction) {
    IAFunctionType functionType = pInputFunction.getType();
    if (functionType instanceof CFunctionType) {
      CFunctionType cFunctionType = (CFunctionType) functionType;
      return new CFunctionTypeWithNames(
          cFunctionType.isConst(),
          cFunctionType.isVolatile(),
          cFunctionType.getReturnType(),
          FluentIterable.from(enforceParameterNames(pInputFunction.getParameters()))
              .filter(CParameterDeclaration.class)
              .toList(),
          functionType.takesVarArgs());
    }
    if (functionType instanceof JMethodType) {
      JMethodType methodType = (JMethodType) functionType;
      return new JMethodType(
          methodType.getReturnType(),
          FluentIterable.from(enforceParameterNames(pInputFunction.getParameters()))
              .filter(JType.class)
              .toList(),
          functionType.takesVarArgs());
    }
    throw new AssertionError("Unsupported function type: " + functionType.getClass());
  }

  private List<AParameterDeclaration> enforceParameterNames(
      List<? extends AParameterDeclaration> pParameters) {
    Set<String> usedNames = Sets.newHashSetWithExpectedSize(pParameters.size());
    int i = 0;
    List<AParameterDeclaration> result = Lists.newArrayListWithCapacity(pParameters.size());
    for (AParameterDeclaration parameter : pParameters) {
      AParameterDeclaration declaration = parameter;
      if (!declaration.getName().isEmpty()) {
        usedNames.add(declaration.getName());
      } else {
        String name;
        while (!usedNames.add(name = "p" + i)) {
          ++i;
        }
        if (declaration instanceof CParameterDeclaration) {
          declaration =
              new CParameterDeclaration(FileLocation.DUMMY, (CType) declaration.getType(), name);
        } else if (declaration instanceof JParameterDeclaration) {
          JParameterDeclaration jDecl = (JParameterDeclaration) declaration;
          declaration =
              new JParameterDeclaration(
                  FileLocation.DUMMY,
                  jDecl.getType(),
                  name,
                  jDecl.getQualifiedName() + name,
                  jDecl.isFinal());
        } else {
          throw new AssertionError(
              "Unsupported parameter declaration type: " + declaration.getClass());
        }
      }
      result.add(declaration);
    }
    return result;
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions> getValueMap(
      CounterexampleInfo pCounterexampleInfo) {
    if (useModel && pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    }
    return ImmutableMultimap.of();
  }

  private Optional<TestVector> extractTestVector(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    Set<State> visited = Sets.newHashSet();
    Deque<State> stack = Queues.newArrayDeque();
    stack.push(State.of(pRootState, TestVector.newTestVector()));
    visited.addAll(stack);
    while (!stack.isEmpty()) {
      State previous = stack.pop();
      if (AbstractStates.isTargetState(previous.argState)) {
        return Optional.of(previous.testVector);
      }
      ARGState parent = previous.argState;
      Iterable<CFANode> parentLocs = AbstractStates.extractLocations(parent);
      for (ARGState child : parent.getChildren()) {
        if (pIsRelevantState.apply(child) && pIsRelevantEdge.apply(Pair.of(parent, child))) {
          Iterable<CFANode> childLocs = AbstractStates.extractLocations(child);
          for (CFANode parentLoc : parentLocs) {
            for (CFANode childLoc : childLocs) {
              if (parentLoc.hasEdgeTo(childLoc)) {
                CFAEdge edge = parentLoc.getEdgeTo(childLoc);
                Optional<State> nextState = computeNextState(previous, child, edge, pValueMap);
                if (nextState.isPresent() && visited.add(nextState.get())) {
                  stack.push(nextState.get());
                }
              }
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  private Optional<State> computeNextState(
      State pPrevious,
      ARGState pChild,
      CFAEdge pEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    if (pEdge instanceof AStatementEdge) {
      AStatement statement = ((AStatementEdge) pEdge).getStatement();
      if (statement instanceof AFunctionCall) {
        AFunctionCall functionCall = (AFunctionCall) statement;
        AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();

        if (!(functionCallExpression.getExpressionType() instanceof CVoidType)
            && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {

          AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
          if (nameExpression instanceof AIdExpression) {

            AIdExpression idExpression = (AIdExpression) nameExpression;
            String name = idExpression.getDeclaration().getQualifiedName();
            if (cfa.getFunctionHead(name) == null) {
              if (functionCall instanceof AFunctionCallStatement) {
                return handlePlainFunctionCall(pPrevious, pChild, functionCallExpression);
              }
              AFunctionCallAssignmentStatement assignment =
                  (AFunctionCallAssignmentStatement) functionCall;
              return handleFunctionCallAssignment(
                  pEdge, pPrevious, pChild, functionCallExpression, assignment, pValueMap);
            }
          }
        }
      }
    }
    return Optional.of(State.of(pChild, pPrevious.testVector));
  }

  private Optional<State> handleFunctionCallAssignment(
      CFAEdge pEdge,
      State pPrevious,
      ARGState pChild,
      AFunctionCallExpression functionCallExpression,
      AFunctionCallAssignmentStatement assignment,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    ALeftHandSide leftHandSide = assignment.getLeftHandSide();
    Iterable<AutomatonState> automatonStates =
        AbstractStates.asIterable(pChild).filter(AutomatonState.class);
    for (AutomatonState automatonState : automatonStates) {
      for (AExpression assumption : automatonState.getAssumptions()) {
        Optional<ALiteralExpression> value = getOther(assumption, leftHandSide);
        if (value.isPresent()) {
          return extendTestVector(pPrevious, pChild, functionCallExpression, value);
        }
      }
    }
    Collection<CFAEdgeWithAssumptions> assumptions = pValueMap.get(pPrevious.argState);
    if (assumptions != null) {
      for (AExpression assumption :
          FluentIterable.from(assumptions)
              .filter(e -> e.getCFAEdge().equals(pEdge))
              .transformAndConcat(CFAEdgeWithAssumptions::getExpStmts)
              .transform(AExpressionStatement::getExpression)) {
        Optional<ALiteralExpression> value = getOther(assumption, leftHandSide);
        if (value.isPresent()) {
          return extendTestVector(pPrevious, pChild, functionCallExpression, value);
        }
      }
    }
    return Optional.empty();
  }

  private Optional<State> extendTestVector(
      State pPrevious,
      ARGState pChild,
      AFunctionCallExpression functionCallExpression,
      Optional<ALiteralExpression> value) {
    TestVector newTestVector =
        pPrevious.testVector.addInputValue(functionCallExpression.getDeclaration(), value.get());
    return Optional.of(State.of(pChild, newTestVector));
  }

  private Optional<State> handlePlainFunctionCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression functionCallExpression) {
    TestVector newTestVector =
        pPrevious.testVector.addInputValue(
            functionCallExpression.getDeclaration(),
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO));
    return Optional.of(State.of(pChild, newTestVector));
  }

  private Optional<ALiteralExpression> getOther(
      AExpression pAssumption, ALeftHandSide pLeftHandSide) {
    if (!(pAssumption instanceof ABinaryExpression)) {
      return Optional.empty();
    }
    ABinaryExpression binOp = (ABinaryExpression) pAssumption;
    if (binOp.getOperator() != BinaryOperator.EQUALS
        && binOp.getOperator()
            != org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.EQUALS) {
      return Optional.empty();
    }
    if (binOp.getOperand2() instanceof ALiteralExpression
        && binOp.getOperand1().equals(pLeftHandSide)) {
      return Optional.of((ALiteralExpression) binOp.getOperand2());
    }
    if (binOp.getOperand1() instanceof ALiteralExpression
        && binOp.getOperand2().equals(pLeftHandSide)) {
      return Optional.of((ALiteralExpression) binOp.getOperand1());
    }
    return Optional.empty();
  }

  private static class State {

    private final ARGState argState;

    private final TestVector testVector;

    private State(ARGState pARGState, TestVector pTestVector) {
      this.argState = Objects.requireNonNull(pARGState);
      this.testVector = Objects.requireNonNull(pTestVector);
    }

    @Override
    public int hashCode() {
      return Objects.hash(argState, testVector);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof State) {
        State other = (State) pObj;
        return argState.equals(other.argState) && testVector.equals(other.testVector);
      }
      return false;
    }

    @Override
    public String toString() {
      return "(" + argState + ", " + testVector + ")";
    }

    public static State of(ARGState pARGState, TestVector pTestVector) {
      return new State(pARGState, pTestVector);
    }
  }

  private void appendVectorIndexDeclaration(
      Appendable pTarget, String pInputFunctionVectorIndexName) throws IOException {
    pTarget.append("unsigned int ");
    pTarget.append(pInputFunctionVectorIndexName);
    appendln(pTarget, " = 0;");
  }

  private static void appendDeclaration(Appendable pTarget, Type pType, String pName)
      throws IOException {
    pTarget.append(pType.toASTString(pName));
    appendln(pTarget, ";");
  }

  private void appendAssignment(Appendable pTarget, String pRetvalName, ALiteralExpression pValue)
      throws IOException {
    pTarget.append(pRetvalName);
    pTarget.append(" = ");
    pTarget.append(pValue.toASTString());
  }

  private static void appendln(Appendable pAppendable, String pLine) throws IOException {
    pAppendable.append(pLine);
    pAppendable.append(System.lineSeparator());
  }
}
