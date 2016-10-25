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
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class HarnessExporter {

  private static final String RETVAL_NAME = "retval";

  private final CFA cfa;

  private final LogManager logger;

  private final HackyOptions hackyOptions = new HackyOptions();

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
  }

  public void writeHarness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge)
      throws IOException {

    // Find a path with sufficient test vector info
    Optional<TestVector> testVector =
        extractTestVector(pRootState, pIsRelevantState, pIsRelevantEdge);
    if (testVector.isPresent()) {

      // #include <stdlib.h> for exit function
      appendln(pTarget, "#include <stdlib.h>");
      // implement __VERIFIER_error with exit(EXIT_FAILURE)
      appendln(pTarget, "void __VERIFIER_error(void) { exit(EXIT_FAILURE); }");
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
        pTarget.append(inputFunction.getType().toASTString(inputFunction.getName()));
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

  private Optional<TestVector> extractTestVector(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge) {
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
                Optional<State> nextState = computeNextState(previous, child, edge);
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

  private Optional<State> computeNextState(State pPrevious, ARGState pChild, CFAEdge pEdge) {
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
                TestVector newTestVector =
                    pPrevious.testVector.addInputValue(
                        functionCallExpression.getDeclaration(),
                        new CIntegerLiteralExpression(
                            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO));
                return Optional.of(State.of(pChild, newTestVector));
              }
              AFunctionCallAssignmentStatement assignment =
                  (AFunctionCallAssignmentStatement) functionCall;
              ALeftHandSide leftHandSide = assignment.getLeftHandSide();
              Iterable<AutomatonState> automatonStates =
                  AbstractStates.asIterable(pChild).filter(AutomatonState.class);
              for (AutomatonState automatonState : automatonStates) {
                for (AExpression assumption : automatonState.getAssumptions()) {
                  Optional<ALiteralExpression> value = getOther(assumption, leftHandSide);
                  if (value.isPresent()) {
                    TestVector newTestVector =
                        pPrevious.testVector.addInputValue(
                            functionCallExpression.getDeclaration(), value.get());
                    return Optional.of(State.of(pChild, newTestVector));
                  }
                }
              }
              return Optional.empty();
            }
          }
        }
      }
    }
    return Optional.of(State.of(pChild, pPrevious.testVector));
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
