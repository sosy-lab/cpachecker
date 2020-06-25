/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.testcase;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.harness.PredefinedTypes;

public class TestCaseExporter {

  private TestCaseExporter() {}

  public static final TestValuesToFormat LINE_SEPARATED =
      valueList -> {
        StringBuilder strB = new StringBuilder();
        Joiner.on("\n").appendTo(strB, valueList);
        return strB.toString();
      };

  public static Optional<String> writeTestInputNondetValues(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      final CounterexampleInfo pCounterexampleInfo,
      final CFA pCfa,
      final TestValuesToFormat formatter) {

    Preconditions.checkArgument(pCounterexampleInfo.isPreciseCounterExample());
    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap =
        pCounterexampleInfo.getExactVariableValues();

    List<String> values = new ArrayList<>();
    Set<ARGState> visited = new HashSet<>();
    Deque<ARGState> stack = new ArrayDeque<>();
    Deque<CFAEdge> lastEdgeStack = new ArrayDeque<>();
    stack.push(pRootState);
    visited.addAll(stack);
    Optional<String> value;
    while (!stack.isEmpty()) {
      ARGState previous = stack.pop();
      CFAEdge lastEdge = null;
      if (!lastEdgeStack.isEmpty()) {
        lastEdge = lastEdgeStack.pop();
      }
      if (AbstractStates.isTargetState(previous)) {
        // end of cex path reached, write test values
        assert lastEdge != null
            : "Expected target state to be different from root state, but was not";
        return Optional.of(formatter.convertToOutput(values));
      }
      ARGState parent = previous;
      Iterable<CFANode> parentLocs = AbstractStates.extractLocations(parent);
      for (ARGState child : parent.getChildren()) {
        if (pIsRelevantState.apply(child) && pIsRelevantEdge.test(parent, child)) {
          Iterable<CFANode> childLocs = AbstractStates.extractLocations(child);
          for (CFANode parentLoc : parentLocs) {
            for (CFANode childLoc : childLocs) {
              if (parentLoc.hasEdgeTo(childLoc)) {
                CFAEdge edge = parentLoc.getEdgeTo(childLoc);

                // add the required values for external non-void functions
                if (edge instanceof AStatementEdge) {
                  AStatementEdge statementEdge = (AStatementEdge) edge;
                  if (statementEdge.getStatement() instanceof AFunctionCall) {
                    value =
                        getReturnValueForExternalFunction(
                            (AFunctionCall) statementEdge.getStatement(),
                            edge,
                            valueMap.get(previous),
                            pCfa);
                    if (value.isPresent()) {
                      values.add(value.get());
                    }
                  }
                }

                if (visited.add(child)) {
                  stack.push(child);
                  lastEdgeStack.push(edge);
                }
              }
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<String> getReturnValueForExternalFunction(
      final AFunctionCall functionCall,
      final CFAEdge edge,
      final @Nullable Collection<CFAEdgeWithAssumptions> pAssumptions,
      final CFA pCfa) {
    AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

    if (!PredefinedTypes.isKnownTestFunction(functionDeclaration)
        && !(functionCallExpression.getExpressionType() instanceof CVoidType)
        && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {
      // only write if not predefined like e.g. malloc and is non-void

      AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
      if (nameExpression instanceof AIdExpression) {

        ASimpleDeclaration declaration = ((AIdExpression) nameExpression).getDeclaration();
        if (declaration != null && pCfa.getFunctionHead(declaration.getQualifiedName()) == null) {
          // external function with return value

          if (functionCall instanceof AFunctionCallAssignmentStatement) {
            AFunctionCallAssignmentStatement assignment =
                (AFunctionCallAssignmentStatement) functionCall;

            if (pAssumptions != null) {
              // get return value from assumptions in counterexample
              for (AExpression assumption :
                  FluentIterable.from(pAssumptions)
                      .filter(e -> e.getCFAEdge().equals(edge))
                      .transformAndConcat(CFAEdgeWithAssumptions::getExpStmts)
                      .transform(AExpressionStatement::getExpression)) {

                if (assumption instanceof ABinaryExpression
                    && ((ABinaryExpression) assumption).getOperator() == BinaryOperator.EQUALS) {

                  ABinaryExpression binExp = (ABinaryExpression) assumption;

                  if (binExp.getOperand2() instanceof ALiteralExpression
                      && binExp.getOperand1().equals(assignment.getLeftHandSide())) {
                    return Optional.of(
                        String.valueOf(((ALiteralExpression) binExp.getOperand2()).getValue()));
                  }
                  if (binExp.getOperand1() instanceof ALiteralExpression
                      && binExp.getOperand2().equals(assignment.getLeftHandSide())) {
                    return Optional.of(
                        String.valueOf(((ALiteralExpression) binExp.getOperand1()).getValue()));
                  }
                }
              }
            }

            // could not find any value
            // or value is irrelevant (case of function call statement)
            // use default value
            Type returnType = functionDeclaration.getType().getReturnType();
            if (returnType instanceof CType) {
              returnType = ((CType) returnType).getCanonicalType();

              if (returnType instanceof CSimpleType
                  && ((CSimpleType) returnType).getType() == CBasicType.CHAR) {
                return Optional.of(" ");
              }

              if (!(returnType instanceof CCompositeType
                  || returnType instanceof CArrayType
                  || returnType instanceof CBitFieldType
                  || (returnType instanceof CElaboratedType
                      && ((CElaboratedType) returnType).getKind() != ComplexTypeKind.ENUM))) {

                return Optional.of(
                    String.valueOf(
                        ((ALiteralExpression)
                                ((CInitializerExpression)
                                        CDefaults.forType((CType) returnType, FileLocation.DUMMY))
                                    .getExpression())
                            .getValue()
                            .toString()));
              } else {
                throw new AssertionError("Cannot write test case value (not a literal)");
              }
            } else {
              throw new AssertionError("Cannot write test case value (not a CType)");
            }
          }
        }
      }
    }
    return Optional.empty();
  }

}
