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

import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.getCanonicalType;
import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.isPredefinedFunction;
import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.isPredefinedType;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
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
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
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
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;

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

      CodeAppender codeAppender = new CodeAppender(pTarget);

      // #include <stdlib.h> for exit function
      codeAppender.appendln("#include <stdlib.h>");
      // implement __VERIFIER_error with exit(EXIT_FAILURE)
      codeAppender.appendln("void __VERIFIER_error(void) { exit(" + ERR_REACHED_CODE + "); }");
      // implement __VERIFIER_assume with exit (EXIT_SUCCESS)
      codeAppender.appendln(
          "void __VERIFIER_assume(int cond) { if (!(cond)) { exit(EXIT_SUCCESS); }}");

      // implement actual harness
      TestVector vector = completeExternalFunctions(testVector.get());
      copyTypeDeclarations(codeAppender);
      for (AFunctionDeclaration inputFunction : vector.getInputFunctions()) {
        List<TestValue> inputValues = vector.getInputValues(inputFunction);
        Type returnType = inputFunction.getType().getReturnType();
        codeAppender.append(declare(inputFunction));
        codeAppender.appendln(" {");
        if (!returnType.equals(CVoidType.VOID)) {
          String inputFunctionVectorIndexName = inputFunction.getName() + "_index__";
          if (inputValues.size() > 1) {
            codeAppender.appendVectorIndexDeclaration(inputFunctionVectorIndexName);
          }
          codeAppender.append("  ");
          codeAppender.appendDeclaration(returnType, RETVAL_NAME);
          if (inputValues.size() == 1) {
            codeAppender.append("  ");
            codeAppender.appendAssignment(RETVAL_NAME, inputValues.iterator().next());
            codeAppender.appendln();
          } else if (inputValues.size() > 1) {
            codeAppender.append("  switch (");
            codeAppender.append(inputFunctionVectorIndexName);
            codeAppender.appendln(") {");
            int i = 0;
            for (TestValue value : inputValues) {
              codeAppender.append("    case ");
              codeAppender.append(Integer.toString(i));
              codeAppender.append(": ");
              codeAppender.appendAssignment(RETVAL_NAME, value);
              codeAppender.appendln(" break;");
              ++i;
            }
            codeAppender.appendln("  }");
            codeAppender.append("  ++");
            codeAppender.append(inputFunctionVectorIndexName);
            codeAppender.appendln(";");
          }
          codeAppender.append("  return ");
          codeAppender.append(RETVAL_NAME);
          codeAppender.appendln(";");
        }
        codeAppender.appendln("}");
      }
    } else {
      logger.log(
          Level.INFO, "Could not export a test harness, some test-vector values are missing.");
    }
  }

  private void copyTypeDeclarations(CodeAppender pTarget) throws IOException {
    List<ADeclaration> declarations = new ArrayList<>();
    CFATraversal.dfs()
        .traverseOnce(
            cfa.getMainFunction(),
            new CFAVisitor() {

              @Override
              public TraversalProcess visitNode(CFANode pNode) {
                return TraversalProcess.CONTINUE;
              }

              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {
                if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
                  ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
                  ADeclaration declaration = declarationEdge.getDeclaration();
                  if (declaration instanceof CTypeDeclaration
                      && !isPredefinedType((CTypeDeclaration) declaration)) {
                    declarations.add(declaration);
                  }
                } else if (pEdge.getEdgeType() == CFAEdgeType.BlankEdge
                    && !pEdge.getPredecessor().equals(cfa.getMainFunction())) {
                  return TraversalProcess.ABORT;
                }
                return TraversalProcess.CONTINUE;
              }
            });
    for (ADeclaration declaration : declarations) {
      pTarget.appendln(declaration.toASTString());
    }
  }

  private TestVector completeExternalFunctions(TestVector pVector) {
    Set<AFunctionDeclaration> externalFunctions = new HashSet<>();
    CFAVisitor externalFunctionCollector =
        new CFAVisitor() {

          @Override
          public TraversalProcess visitNode(CFANode pNode) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitEdge(CFAEdge pEdge) {
            if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
              ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
              ADeclaration declaration = declarationEdge.getDeclaration();
              if (declaration instanceof AFunctionDeclaration) {
                AFunctionDeclaration functionDeclaration = (AFunctionDeclaration) declaration;
                if (!cfa.getAllFunctionNames().contains(functionDeclaration.getName())
                    && !isPredefinedFunction(functionDeclaration)) {
                  externalFunctions.add(functionDeclaration);
                }
              }
            }
            return TraversalProcess.CONTINUE;
          }
        };
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), externalFunctionCollector);
    TestVector result = pVector;
    for (AFunctionDeclaration functionDeclaration :
        Sets.difference(
            externalFunctions, FluentIterable.from(pVector.getInputFunctions()).toSet())) {
      result = addDummyValue(result, functionDeclaration);
    }
    return result;
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
        AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

        if (!isPredefinedFunction(functionDeclaration)
            && !(functionCallExpression.getExpressionType() instanceof CVoidType)
            && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {

          AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
          if (nameExpression instanceof AIdExpression) {

            AIdExpression idExpression = (AIdExpression) nameExpression;
            ASimpleDeclaration declaration = idExpression.getDeclaration();
            if (declaration != null) {
              String name = declaration.getQualifiedName();
              if (cfa.getFunctionHead(name) == null) {
                final Optional<State> nextState;
                if (functionCall instanceof AFunctionCallStatement) {
                  return handlePlainFunctionCall(pPrevious, pChild, functionCallExpression);
                }
                AFunctionCallAssignmentStatement assignment =
                    (AFunctionCallAssignmentStatement) functionCall;
                nextState =
                    handleFunctionCallAssignment(
                        pEdge, pPrevious, pChild, functionCallExpression, assignment, pValueMap);
                if (nextState.isPresent()) {
                  return nextState;
                }
                if (!isSupported(functionDeclaration)) {
                  if (returnsPointer(functionDeclaration)) {
                    return handlePointerCall(pPrevious, pChild, functionCallExpression);
                  }
                  if (returnsComposite(functionDeclaration)) {
                    return handleCompositeCall(pPrevious, pChild, functionCallExpression);
                  }
                  return handlePlainFunctionCall(pPrevious, pChild, functionCallExpression);
                }
                return Optional.empty();
              }
            }
          }
        }
      }
    }
    return Optional.of(State.of(pChild, pPrevious.testVector));
  }

  private static boolean isSupported(@Nullable AFunctionDeclaration pDeclaration) {
    if (pDeclaration == null) {
      return false;
    }
    return !returnsPointer(pDeclaration) && !returnsComposite(pDeclaration);
  }

  private static boolean returnsPointer(AFunctionDeclaration pDeclaration) {
    Type type = getCanonicalType(pDeclaration.getType().getReturnType());
    return type instanceof CPointerType;
  }

  private static boolean returnsComposite(AFunctionDeclaration pDeclaration) {
    Type type = getCanonicalType(pDeclaration.getType().getReturnType());
    return type instanceof CCompositeType;
  }

  private Optional<State> handleFunctionCallAssignment(
      CFAEdge pEdge,
      State pPrevious,
      ARGState pChild,
      AFunctionCallExpression functionCallExpression,
      AFunctionCallAssignmentStatement assignment,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    ALeftHandSide leftHandSide = assignment.getLeftHandSide();
    ARGState argState = pChild;
    while (argState != null) {
      Iterable<AutomatonState> automatonStates =
          AbstractStates.asIterable(argState).filter(AutomatonState.class);
      for (AutomatonState automatonState : automatonStates) {
        for (AExpression assumption : automatonState.getAssumptions()) {
          Optional<ALiteralExpression> value = getOther(assumption, leftHandSide);
          if (value.isPresent()) {
            return extendTestVector(pPrevious, argState, functionCallExpression, value.get());
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
            return extendTestVector(pPrevious, argState, functionCallExpression, value.get());
          }
        }
      }
      Collection<ARGState> nextCandidates = argState.getChildren();
      if (nextCandidates.size() == 1) {
        ARGState candidate = nextCandidates.iterator().next();
        if (argState
            .getEdgesToChild(candidate)
            .stream()
            .allMatch(AutomatonGraphmlCommon::handleAsEpsilonEdge)) {
          argState = candidate;
          continue;
        }
      }
      argState = null;
    }
    return Optional.empty();
  }

  private static Optional<State> extendTestVector(
      State pPrevious,
      ARGState pChild,
      AFunctionCallExpression functionCallExpression,
      ALiteralExpression value) {
    TestVector newTestVector =
        addValue(pPrevious.testVector, functionCallExpression.getDeclaration(), value);
    return Optional.of(State.of(pChild, newTestVector));
  }

  private static Optional<State> handlePlainFunctionCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression functionCallExpression) {
    TestVector newTestVector =
        addDummyValue(pPrevious.testVector, functionCallExpression.getDeclaration());
    return Optional.of(State.of(pChild, newTestVector));
  }

  private Optional<State> handlePointerCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression pFunctionCallExpression) {
    AFunctionDeclaration declaration = pFunctionCallExpression.getDeclaration();
    Preconditions.checkArgument(returnsPointer(declaration));
    CPointerType expectedPointerType =
        (CPointerType) getCanonicalType(pFunctionCallExpression.getExpressionType());
    CType expectedTargetType = expectedPointerType.getType();

    TestValue pointerValue = assignMallocToTmpVariable(expectedTargetType);
    AExpression value = castIfNecessary(declaration, pointerValue.getValue());

    TestVector newTestVector =
        pPrevious.testVector.addInputValue(
            declaration, TestValue.of(pointerValue.getAuxiliaryStatements(), value));
    return Optional.of(State.of(pChild, newTestVector));
  }

  private Optional<State> handleCompositeCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression pFunctionCallExpression) {
    AFunctionDeclaration declaration = pFunctionCallExpression.getDeclaration();
    Preconditions.checkArgument(returnsComposite(declaration));
    CType expectedTargetType = (CType) pFunctionCallExpression.getExpressionType();

    TestValue pointerValue = assignMallocToTmpVariable(expectedTargetType);

    AExpression value =
        new CPointerExpression(
            FileLocation.DUMMY,
            CPointerType.POINTER_TO_VOID,
            (CExpression) pointerValue.getValue());
    value = castIfNecessary(declaration, value);

    TestVector newTestVector =
        pPrevious.testVector.addInputValue(
            declaration, TestValue.of(pointerValue.getAuxiliaryStatements(), value));
    return Optional.of(State.of(pChild, newTestVector));
  }

  private static TestValue assignMallocToTmpVariable(CType pExpectedTargetType) {
    CFunctionCallExpression pointerToValue = callMalloc(pExpectedTargetType);
    CSimpleDeclaration tmpVarDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            CPointerType.POINTER_TO_VOID,
            "__tmp_malloc_result",
            "__tmp_malloc_result",
            "__tmp_malloc_result",
            null);
    CLeftHandSide variable = new CIdExpression(FileLocation.DUMMY, tmpVarDeclaration);
    CAssignment assignment =
        new CFunctionCallAssignmentStatement(FileLocation.DUMMY, variable, pointerToValue);
    return TestValue.of(ImmutableList.of(tmpVarDeclaration, assignment), variable);
  }

  private static CFunctionCallExpression callMalloc(CType expectedTargetType) {
    CFunctionType type =
        new CFunctionType(
            false,
            false,
            CPointerType.POINTER_TO_VOID,
            Collections.singletonList(CNumericTypes.INT),
            false);
    CFunctionDeclaration functionDeclaration =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            type,
            "malloc",
            Collections.singletonList(
                new CParameterDeclaration(
                    FileLocation.DUMMY, CPointerType.POINTER_TO_VOID, "size")));
    final CExpression size;
    if (expectedTargetType.equals(CVoidType.VOID)) {
      size =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.valueOf(4096));
    } else {
      size =
          new CTypeIdExpression(
              FileLocation.DUMMY,
              CNumericTypes.UNSIGNED_INT,
              CTypeIdExpression.TypeIdOperator.SIZEOF,
              expectedTargetType);
    }
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        CPointerType.POINTER_TO_VOID,
        new CIdExpression(FileLocation.DUMMY, functionDeclaration),
        Collections.<CExpression>singletonList(size),
        functionDeclaration);
  }

  private static TestVector addDummyValue(
      TestVector pTestVector, AFunctionDeclaration pFunctionDeclaration) {
    AExpression value =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.ZERO);
    return addValue(pTestVector, pFunctionDeclaration, value);
  }

  private static TestVector addValue(
      TestVector pTestVector, AFunctionDeclaration pFunctionDeclaration, AExpression pValue) {
    AExpression value = castIfNecessary(pFunctionDeclaration, pValue);
    return pTestVector.addInputValue(pFunctionDeclaration, value);
  }

  private static AExpression castIfNecessary(
      AFunctionDeclaration pFunctionDeclaration, AExpression pValue) {
    AExpression value = pValue;
    Type expectedReturnType = getCanonicalType(pFunctionDeclaration.getType().getReturnType());
    Type actualType = getCanonicalType(value.getExpressionType());
    if (!actualType.equals(expectedReturnType)) {
      if (value instanceof CExpression && expectedReturnType instanceof CType) {
        if ((expectedReturnType instanceof CPointerType
                && !expectedReturnType.equals(CPointerType.POINTER_TO_VOID))
            || (actualType instanceof CPointerType
                && !actualType.equals(CPointerType.POINTER_TO_VOID))) {
          value =
              new CCastExpression(
                  pValue.getFileLocation(), CPointerType.POINTER_TO_VOID, (CExpression) value);
        }
        value =
            new CCastExpression(
                pValue.getFileLocation(),
                (CType) pFunctionDeclaration.getType().getReturnType(),
                (CExpression) value);
      } else if (value instanceof JExpression && expectedReturnType instanceof JType) {
        value =
            new JCastExpression(
                pValue.getFileLocation(), (JType) expectedReturnType, (JExpression) value);
      }
    }
    return value;
  }

  private static Optional<ALiteralExpression> getOther(
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
}
