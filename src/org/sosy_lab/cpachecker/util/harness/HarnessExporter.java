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
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
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
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
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

  private static final String TMP_VAR = "__tmp_var";

  private static final int ERR_REACHED_CODE = 107;

  private final CFA cfa;

  private final LogManager logger;

  private final CBinaryExpressionBuilder binExpBuilder;

  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

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
    binExpBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
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
      codeAppender.append(vector);
    } else {
      logger.log(
          Level.INFO, "Could not export a test harness, some test-vector values are missing.");
    }
  }

  private void copyTypeDeclarations(CodeAppender pTarget) throws IOException {
    Set<ADeclaration> declarations = new LinkedHashSet<>();
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
                  if (declaration instanceof CTypeDeclaration) {
                    CTypeDeclaration typeDeclaration = (CTypeDeclaration) declaration;
                    if (!isPredefinedType((CTypeDeclaration) declaration)) {
                      CType declaredType = typeDeclaration.getType().getCanonicalType();
                      if (declaredType instanceof CElaboratedType && declaredType.isIncomplete()) {
                        CElaboratedType elaboratedType = (CElaboratedType) declaredType;
                        final CComplexType dummyType;
                        switch (elaboratedType.getKind()) {
                          case ENUM:
                            dummyType = new CEnumType(elaboratedType.isConst(), elaboratedType.isVolatile(), Collections.emptyList(), elaboratedType.getName(), elaboratedType.getOrigName());
                            break;
                          case STRUCT:
                          case UNION:
                            dummyType = new CCompositeType(elaboratedType.isConst(), elaboratedType.isVolatile(), elaboratedType.getKind(), Collections.emptyList(), elaboratedType.getName(), elaboratedType.getOrigName());
                            break;
                          default:
                            throw new AssertionError("Unsupported kind of elaborated type: " + elaboratedType.getKind());
                        }
                        declarations.add(new CComplexTypeDeclaration(FileLocation.DUMMY, typeDeclaration.isGlobal(), dummyType));
                      } else {
                        declarations.add(declaration);
                      }
                    }
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
                    && !isPredefinedFunction(functionDeclaration)
                    && !pVector.contains(functionDeclaration)) {
                  externalFunctions.add(functionDeclaration);
                }
              }
            }
            return TraversalProcess.CONTINUE;
          }
        };
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), externalFunctionCollector);
    TestVector result = pVector;
    for (AFunctionDeclaration functionDeclaration : externalFunctions) {
      result = addDummyValue(result, functionDeclaration);
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
      AStatementEdge statementEdge = (AStatementEdge) pEdge;
      return handleStatementEdge(pPrevious, pChild, statementEdge, pValueMap);
    } else if (pEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
      return handleDeclarationEdge(pPrevious, pChild, declarationEdge, pValueMap);
    }
    return Optional.of(State.of(pChild, pPrevious.testVector));
  }

  private Optional<State> handleStatementEdge(
      State pPrevious,
      ARGState pChild,
      AStatementEdge pStatementEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    AStatement statement = pStatementEdge.getStatement();
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
              if (functionCall instanceof AFunctionCallStatement) {
                return handlePlainFunctionCall(pPrevious, pChild, functionCallExpression);
              }
              AFunctionCallAssignmentStatement assignment =
                  (AFunctionCallAssignmentStatement) functionCall;
              final Optional<State> nextState =
                  handleFunctionCallAssignment(
                      pStatementEdge,
                      pPrevious,
                      pChild,
                      functionCallExpression,
                      assignment,
                      pValueMap);
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
    return Optional.of(State.of(pChild, pPrevious.testVector));
  }

  private Optional<State> handleDeclarationEdge(
      State pPrevious,
      ARGState pChild,
      ADeclarationEdge pDeclarationEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    ADeclaration declaration = pDeclarationEdge.getDeclaration();
    if (declaration instanceof CVariableDeclaration) {
      CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
      if (variableDeclaration.getCStorageClass() == CStorageClass.EXTERN) {
        final Optional<State> nextState =
            handleVariableDeclaration(
                pDeclarationEdge, pPrevious, pChild, variableDeclaration, pValueMap);
        if (nextState.isPresent()) {
          return nextState;
        }
        Type type = variableDeclaration.getType();
        Type canonicalType = getCanonicalType(type);
        if (canonicalType instanceof CPointerType) {
          return handlePointerDeclaration(pPrevious, pChild, variableDeclaration);
        }
        if (canonicalType instanceof CCompositeType) {
          return handleCompositeDeclaration(pPrevious, pChild, variableDeclaration);
        }
        if (canonicalType instanceof CArrayType) {
          return handleArrayDeclaration(pPrevious, pChild, variableDeclaration);
        }
        return Optional.of(
            State.of(
                pChild,
                pPrevious.testVector.addInputValue(
                    variableDeclaration, getDummyInitializer(variableDeclaration.getType()))));
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
      AFunctionCallExpression pFunctionCallExpression,
      AFunctionCallAssignmentStatement assignment,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    ALeftHandSide leftHandSide = assignment.getLeftHandSide();
    return handleEdge(
        pEdge,
        pPrevious,
        pChild,
        leftHandSide,
        value -> (vector -> vector.addInputValue(pFunctionCallExpression.getDeclaration(), value)),
        pValueMap);
  }

  private Optional<State> handleVariableDeclaration(
      CFAEdge pEdge,
      State pPrevious,
      ARGState pChild,
      AVariableDeclaration pVariableDeclaration,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    final ALeftHandSide leftHandSide;
    if (pVariableDeclaration instanceof CVariableDeclaration) {
      leftHandSide =
          new CIdExpression(
              pVariableDeclaration.getFileLocation(), (CSimpleDeclaration) pVariableDeclaration);
    } else if (pVariableDeclaration instanceof JVariableDeclaration) {
      JVariableDeclaration variableDeclaration = (JVariableDeclaration) pVariableDeclaration;
      leftHandSide =
          new JIdExpression(
              pVariableDeclaration.getFileLocation(),
              variableDeclaration.getType(),
              variableDeclaration.getName(),
              variableDeclaration);
    } else {
      throw new AssertionError("Unsupported declaration type: " + pVariableDeclaration);
    }
    return handleEdge(
        pEdge,
        pPrevious,
        pChild,
        leftHandSide,
        value -> (vector -> vector.addInputValue(pVariableDeclaration, toInitializer(value))),
        pValueMap);
  }

  private Optional<State> handleEdge(
      CFAEdge pEdge,
      State pPrevious,
      ARGState pChild,
      ALeftHandSide pLeftHandSide,
      Function<AExpression, Function<TestVector, TestVector>> pUpdate,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    ARGState argState = pChild;
    while (argState != null) {
      Iterable<AutomatonState> automatonStates =
          AbstractStates.asIterable(argState).filter(AutomatonState.class);
      for (AutomatonState automatonState : automatonStates) {
        for (AExpression assumption : automatonState.getAssumptions()) {
          Optional<ALiteralExpression> value = getOther(assumption, pLeftHandSide);
          if (value.isPresent()) {
            AExpression v = castIfNecessary(pLeftHandSide.getExpressionType(), value.get());
            return Optional.of(new State(pChild, pUpdate.apply(v).apply(pPrevious.testVector)));
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
          Optional<ALiteralExpression> value = getOther(assumption, pLeftHandSide);
          if (value.isPresent()) {
            AExpression v = castIfNecessary(pLeftHandSide.getExpressionType(), value.get());
            return Optional.of(new State(pChild, pUpdate.apply(v).apply(pPrevious.testVector)));
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

  private Optional<State> handlePlainFunctionCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression functionCallExpression) {
    TestVector newTestVector =
        addDummyValue(pPrevious.testVector, functionCallExpression.getDeclaration());
    return Optional.of(State.of(pChild, newTestVector));
  }

  private Optional<State> handlePointerCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression pFunctionCallExpression) {
    TestVector newTestVector = handlePointerCall(pPrevious.testVector, pFunctionCallExpression.getDeclaration());
    return Optional.of(State.of(pChild, newTestVector));
  }

  private TestVector handlePointerCall(TestVector pTestVector, AFunctionDeclaration pDeclaration) {
    Type declarationType = pDeclaration.getType().getReturnType();
    Preconditions.checkArgument(getCanonicalType(declarationType) instanceof CPointerType);
    if (!(declarationType instanceof CPointerType)) {
      declarationType = getCanonicalType(declarationType);
    }
    ExpressionTestValue pointerValue = handlePointer((CPointerType) declarationType, false);
    return pTestVector.addInputValue(pDeclaration, pointerValue);
  }

  private Optional<State> handlePointerDeclaration(
      State pPrevious, ARGState pChild, AVariableDeclaration pVariableDeclaration) {
    Type declarationType = pVariableDeclaration.getType();
    Preconditions.checkArgument(declarationType instanceof CPointerType);
    ExpressionTestValue pointerValue = handlePointer((CPointerType) declarationType, true);
    AExpression value = pointerValue.getValue();
    final AInitializer initializer = toInitializer(value);
    InitializerTestValue initializerTestValue =
        InitializerTestValue.of(pointerValue.getAuxiliaryStatements(), initializer);
    TestVector newTestVector =
        pPrevious.testVector.addInputValue(pVariableDeclaration, initializerTestValue);
    return Optional.of(State.of(pChild, newTestVector));
  }

  private static AInitializer toInitializer(AExpression pValue) {
    if (pValue instanceof CExpression) {
      return new CInitializerExpression(FileLocation.DUMMY, (CExpression) pValue);
    }
    if (pValue instanceof JExpression) {
      return new JInitializerExpression(FileLocation.DUMMY, (JExpression) pValue);
    }
    throw new AssertionError("Unsupported expression type: " + pValue);
  }

  private ExpressionTestValue handlePointer(CPointerType pType, boolean pIsGlobal) {
    return handlePointer(pType, getSizeOf(pType.getType()), pIsGlobal);
  }

  private ExpressionTestValue handlePointer(
      CPointerType pType, CExpression pTargetSize, boolean pIsGlobal) {
    if (pIsGlobal) {
      String varName = TMP_VAR + "_" + idGenerator.getFreshId();
      CVariableDeclaration tmpDeclaration =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              pType.getType(),
              varName,
              varName,
              varName,
              (CInitializer) getDummyInitializer(pType.getType()));
      CIdExpression var = new CIdExpression(FileLocation.DUMMY, tmpDeclaration);
      return ExpressionTestValue.of(
          Collections.singletonList(tmpDeclaration),
          new CUnaryExpression(FileLocation.DUMMY, pType, var, UnaryOperator.AMPER));
    }
    assert !pIsGlobal;
    ExpressionTestValue pointerValue =
        assignMallocToTmpVariable(pTargetSize, pType.getType(), false);
    return ExpressionTestValue.of(pointerValue.getAuxiliaryStatements(), pointerValue.getValue());
  }

  private Optional<State> handleCompositeCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression pFunctionCallExpression) {
    AFunctionDeclaration declaration = pFunctionCallExpression.getDeclaration();
    TestVector newTestVector = handleCompositeCall(pPrevious.testVector, declaration);
    return Optional.of(State.of(pChild, newTestVector));
  }

  private TestVector handleCompositeCall(
      TestVector pTestVector, AFunctionDeclaration pDeclaration) {
    Preconditions.checkArgument(returnsComposite(pDeclaration));
    CType expectedTargetType = (CType) pDeclaration.getType().getReturnType();

    return pTestVector.addInputValue(
            pDeclaration,
            handleComposite(expectedTargetType, getSizeOf(expectedTargetType), false));
  }

  private Optional<State> handleCompositeDeclaration(
      State pPrevious, ARGState pChild, AVariableDeclaration pVariableDeclaration) {
    Type expectedTargetType = pVariableDeclaration.getType();
    Preconditions.checkArgument(getCanonicalType(expectedTargetType) instanceof CCompositeType);

    TestVector newTestVector =
        pPrevious.testVector.addInputValue(
            pVariableDeclaration, getDummyInitializer(pVariableDeclaration.getType()));
    return Optional.of(State.of(pChild, newTestVector));
  }

  private ExpressionTestValue handleComposite(CType pType, CExpression pSize, boolean pIsGlobal) {
    Preconditions.checkArgument(getCanonicalType(pType) instanceof CCompositeType);
    CPointerType pointerType = new CPointerType(false, false, pType);

    TestValue pointerValue = handlePointer(pointerType, pSize, pIsGlobal);
    CExpression pointerExpression = (CExpression) pointerValue.getValue();

    AExpression value =
        new CPointerExpression(
            FileLocation.DUMMY,
            CPointerType.POINTER_TO_VOID,
            (CExpression) castIfNecessary(pointerType, pointerExpression));
    value = castIfNecessary(pType, value);

    return ExpressionTestValue.of(pointerValue.getAuxiliaryStatements(), value);
  }

  private Optional<State> handleArrayDeclaration(
      State pPrevious, ARGState pChild, AVariableDeclaration pVariableDeclaration) {
    TestVector newTestVector =
        pPrevious.testVector.addInputValue(
            pVariableDeclaration, getDummyInitializer(pVariableDeclaration.getType()));
    return Optional.of(State.of(pChild, newTestVector));
  }

  private ExpressionTestValue assignMallocToTmpVariable(
      CExpression pSize, CType pTargetType, boolean pIsGlobal) {
    CFunctionCallExpression pointerToValue = callMalloc(pSize);
    String variableName = TMP_VAR;
    if (pIsGlobal) {
      variableName = variableName + "_" + idGenerator.getFreshId();
    }
    CSimpleDeclaration tmpVarDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            new CPointerType(false, false, pTargetType),
            variableName,
            variableName,
            variableName,
            null);
    CLeftHandSide variable = new CIdExpression(FileLocation.DUMMY, tmpVarDeclaration);
    CAssignment assignment =
        new CFunctionCallAssignmentStatement(FileLocation.DUMMY, variable, pointerToValue);
    return ExpressionTestValue.of(ImmutableList.of(tmpVarDeclaration, assignment), variable);
  }

  private CExpression getSizeOf(CType pExpectedTargetType) {
    final CExpression size;
    CType canonicalType = pExpectedTargetType.getCanonicalType();
    CExpression dummyLength =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.valueOf(4096));
    if (canonicalType.equals(CVoidType.VOID)) {
      size = dummyLength;
    } else if (canonicalType instanceof CArrayType) {
      CArrayType arrayType = (CArrayType) canonicalType;
      CExpression length = arrayType.getLength();
      if (length == null) {
        length = dummyLength;
      }
      size =
          binExpBuilder.buildBinaryExpressionUnchecked(
              getSizeOf(arrayType.getType()), length, BinaryOperator.MULTIPLY);
    } else {
      size =
          new CTypeIdExpression(
              FileLocation.DUMMY,
              CNumericTypes.UNSIGNED_INT,
              CTypeIdExpression.TypeIdOperator.SIZEOF,
              pExpectedTargetType);
    }
    return size;
  }

  private static CFunctionCallExpression callMalloc(CExpression pSize) {
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
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        CPointerType.POINTER_TO_VOID,
        new CIdExpression(FileLocation.DUMMY, functionDeclaration),
        Collections.<CExpression>singletonList(pSize),
        functionDeclaration);
  }

  private TestVector addDummyValue(
      TestVector pTestVector, AFunctionDeclaration pFunctionDeclaration) {
    Type returnType = pFunctionDeclaration.getType().getReturnType();
    Type canonicalReturnType = getCanonicalType(returnType);
    if (canonicalReturnType instanceof CPointerType) {
      return handlePointerCall(pTestVector, pFunctionDeclaration);
    }
    if (canonicalReturnType instanceof CCompositeType) {
      return handleCompositeCall(pTestVector, pFunctionDeclaration);
    }
    AExpression value = getDummyValue(pFunctionDeclaration.getType().getReturnType());
    return addValue(pTestVector, pFunctionDeclaration, value);
  }

  private static final AExpression getDummyValue(Type pType) {
    if (pType instanceof CType) {
      if (canInitialize(pType)) {
        CInitializer initializer = CDefaults.forType((CType) pType, FileLocation.DUMMY);
        if (initializer instanceof CInitializerExpression) {
          return ((CInitializerExpression) initializer).getExpression();
        }
      }
      return new CIntegerLiteralExpression(
          FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.ZERO);
    }
    return new JIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ZERO);
  }

  private static final AInitializer getDummyInitializer(Type pType) {
    if (pType instanceof CType) {
      if (canInitialize(pType)) {
        return CDefaults.forType((CType) pType, FileLocation.DUMMY);
      }
      return new CInitializerExpression(
          FileLocation.DUMMY,
          new CIntegerLiteralExpression(
              FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.ZERO));
    }
    return new JInitializerExpression(
        FileLocation.DUMMY, new JIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ZERO));
  }

  static boolean canInitialize(Type pType) {
    Type canonicalType = getCanonicalType(pType);
    if (canonicalType.equals(CVoidType.VOID)) {
      return false;
    }
    if (canonicalType instanceof CCompositeType) {
      return !((CCompositeType) canonicalType).isIncomplete();
    }
    if (canonicalType instanceof CElaboratedType) {
      return ((CElaboratedType) canonicalType).getKind() == ComplexTypeKind.ENUM;
    }
    return true;
  }

  private static TestVector addValue(
      TestVector pTestVector, AFunctionDeclaration pFunctionDeclaration, AExpression pValue) {
    AExpression value = castIfNecessary(pFunctionDeclaration.getType().getReturnType(), pValue);
    return pTestVector.addInputValue(pFunctionDeclaration, value);
  }

  private static AExpression castIfNecessary(
      Type pExpectedReturnType, AExpression pValue) {
    AExpression value = pValue;
    Type expectedReturnType = getCanonicalType(pExpectedReturnType);
    Type actualType = getCanonicalType(value.getExpressionType());
    if (!actualType.equals(expectedReturnType)) {
      if (value instanceof CExpression && expectedReturnType instanceof CType) {
        if (expectedReturnType instanceof CPointerType
                && !expectedReturnType.equals(CPointerType.POINTER_TO_VOID)
                && actualType instanceof CPointerType
                && !actualType.equals(CPointerType.POINTER_TO_VOID)) {
          value =
              new CCastExpression(
                  pValue.getFileLocation(), CPointerType.POINTER_TO_VOID, (CExpression) value);
        }
        value =
            new CCastExpression(
                pValue.getFileLocation(),
                (CType) pExpectedReturnType,
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
