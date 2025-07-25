// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.harness;

import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.getCanonicalType;
import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.isPredefinedFunction;
import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.isPredefinedFunctionWithoutVerifierError;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
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
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.testcase.ExpressionTestValue;
import org.sosy_lab.cpachecker.util.testcase.InitializerTestValue;
import org.sosy_lab.cpachecker.util.testcase.TestValue;
import org.sosy_lab.cpachecker.util.testcase.TestValue.AuxiliaryCode;
import org.sosy_lab.cpachecker.util.testcase.TestVector;
import org.sosy_lab.cpachecker.util.testcase.TestVector.TargetTestVector;

@Options(prefix = "testHarnessExport")
public class HarnessExporter {

  private record State(ARGState argState, TestVector testVector) {}

  private static final String TMP_VAR = "__tmp_var";

  private static final String ERR_MSG = "CPAchecker test harness: property violation reached";

  private final CFA cfa;
  private final MachineModel machineModel;

  private final LogManager logger;

  private final CBinaryExpressionBuilder binExpBuilder;

  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  @Option(secure = true, description = "Use the counterexample model to provide test-vector values")
  private boolean useModel = true;

  @Option(secure = true, description = "Only genenerate for __VERIFIER_nondet calls")
  private boolean onlyVerifierNondet = false;

  @Option(
      secure = true,
      description =
          "Provide dummy values for external variable declarations."
              + " This is useful when definitions are not implemented yet or missing."
              + " But it may introduce conflicts with values from standard libraries.")
  private boolean provideDummyValues = false;

  public HarnessExporter(Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    cfa = pCFA;
    machineModel = cfa.getMachineModel();
    logger = pLogger;
    binExpBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    pConfig.inject(this);
  }

  /**
   * Write a new test harness from the given counterexample info to the target appendable.
   *
   * @param pRootState root state of the ARG.
   * @param pIsRelevantState predicate that filters relevant states. Should be a predicate that only
   *     includes states on a single target path.
   * @param pIsRelevantEdge predicate that filters relevant edges. Should be a predicate that only
   *     includes edges on a single target path.
   * @param pCounterexampleInfo the counterexample to extract test vectors from
   * @return the harness code. Returns <code>empty</code> if no harness could be generated.
   */
  public Optional<String> writeHarness(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      CounterexampleInfo pCounterexampleInfo) {

    // Find a path with sufficient test vector info
    Optional<TargetTestVector> testVector =
        extractTestVector(
            pRootState, pIsRelevantState, pIsRelevantEdge, getValueMap(pCounterexampleInfo));
    if (testVector.isPresent()) {

      Set<AFunctionDeclaration> externalFunctions = getExternalFunctions();

      CFAEdge edgeToTarget = testVector.orElseThrow().getEdgeToTarget();
      Optional<AFunctionDeclaration> errorFunction = getErrorFunction(edgeToTarget);
      TestVector vector =
          completeExternalFunctions(
              testVector.orElseThrow().getVector(),
              errorFunction.isPresent()
                  ? FluentIterable.from(externalFunctions)
                      .filter(Predicates.not(Predicates.equalTo(errorFunction.orElseThrow())))
                  : externalFunctions);

      // write harness content
      StringBuilder delegateAppender = new StringBuilder();
      CodeAppender codeAppender = new CodeAppender(delegateAppender);
      try {
        codeAppender.appendln("struct _IO_FILE;");
        codeAppender.appendln("typedef struct _IO_FILE FILE;");
        codeAppender.appendln("extern struct _IO_FILE *stderr;");
        codeAppender.appendln(
            "extern int fprintf(FILE *__restrict __stream, const char *__restrict __format, ...);");
        codeAppender.appendln("extern void exit(int __status) __attribute__ ((__noreturn__));");

        // implement error-function
        if (errorFunction.isPresent()) {
          codeAppender.append(errorFunction.orElseThrow());
          codeAppender.appendln(" {");
          codeAppender.appendln("  fprintf(stderr, \"" + ERR_MSG + "\\n\");");
          codeAppender.appendln("  exit(107);");
          codeAppender.appendln("}");
        } else {
          codeAppender.appendln("// Could not find a call to an error function.");
          codeAppender.appendln(
              "// CPAchecker can not guarantee that this harness exposes the found property"
                  + " violation.");
        }

        if (externalFunctions.stream().anyMatch(PredefinedTypes::isVerifierAssume)) {
          // implement __VERIFIER_assume with exit (EXIT_SUCCESS)
          codeAppender.appendln("void __VERIFIER_assume(int cond) { if (!(cond)) { exit(0); }}");
        }
        codeAppender.append(vector);
        return Optional.of(codeAppender.toString());
      } catch (IOException e) {
        // Only StringBuilder is used in background, so
        // IOException from codeAppender should not be possible.
        throw new IllegalStateException("Exception should not be possible", e);
      }
    } else {
      logger.log(
          Level.FINE, "Could not export a test harness, some test-vector values are missing.");
      return Optional.empty();
    }
  }

  private Optional<AFunctionDeclaration> getErrorFunction(CFAEdge pEdgeToTarget) {
    AFunctionCall callStatement = null;
    if (pEdgeToTarget instanceof AStatementEdge statementEdge) {
      AStatement statement = statementEdge.getStatement();
      if (statement instanceof AFunctionCall aFunctionCall) {
        callStatement = aFunctionCall;
      }
    } else if (pEdgeToTarget instanceof FunctionCallEdge functionCallEdge) {
      callStatement = functionCallEdge.getFunctionCall();
    }

    if (callStatement != null) {
      return getFunctionDeclaration(callStatement);
    } else {
      return Optional.empty();
    }
  }

  private Optional<AFunctionDeclaration> getFunctionDeclaration(final AFunctionCall pFunctionCall) {
    final AFunctionCallExpression functionCallExpression =
        pFunctionCall.getFunctionCallExpression();
    final AFunctionDeclaration declaration = functionCallExpression.getDeclaration();
    return Optional.ofNullable(declaration);
  }

  private Set<AFunctionDeclaration> getExternalFunctions() {
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
              if ((declaration instanceof AFunctionDeclaration functionDeclaration)
                  && !cfa.getAllFunctionNames().contains(functionDeclaration.getName())) {
                externalFunctions.add(functionDeclaration);
              }
            }
            return TraversalProcess.CONTINUE;
          }
        };
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), externalFunctionCollector);
    return externalFunctions;
  }

  /**
   * Create a test vector that contains dummy values for the given external functions that are not
   * yet part of the provided test vector.
   *
   * @param pVector the current test vector
   * @param pExternalFunctions the external functions to check
   * @return a test vector that contains the values of the given vector and the newly created dummy
   *     values.
   */
  private TestVector completeExternalFunctions(
      TestVector pVector, Iterable<AFunctionDeclaration> pExternalFunctions) {
    TestVector result = pVector;
    for (AFunctionDeclaration functionDeclaration : pExternalFunctions) {
      if (!isPredefinedFunctionWithoutVerifierError(functionDeclaration)
          && !pVector.contains(functionDeclaration)) {
        result = addDummyValue(result, functionDeclaration);
      }
    }
    return result;
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions> getValueMap(
      CounterexampleInfo pCounterexampleInfo) {
    if (useModel && pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    }
    return ImmutableListMultimap.of();
  }

  public Optional<TargetTestVector> extractTestVector(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    Deque<State> stack = new ArrayDeque<>();
    Deque<CFAEdge> lastEdgeStack = new ArrayDeque<>();
    stack.push(new State(pRootState, TestVector.newTestVector()));
    Set<State> visited = new HashSet<>(stack);
    while (!stack.isEmpty()) {
      State previous = stack.pop();
      CFAEdge lastEdge = null;
      if (!lastEdgeStack.isEmpty()) {
        lastEdge = lastEdgeStack.pop();
      }
      if (AbstractStates.isTargetState(previous.argState())) {
        assert lastEdge != null
            : "Expected target state to be different from root state, but was not";
        return Optional.of(new TargetTestVector(lastEdge, previous.testVector()));
      }
      ARGState parent = previous.argState();
      for (ARGState child : parent.getChildren()) {
        if (pIsRelevantState.apply(child) && pIsRelevantEdge.test(parent, child)) {
          // if parent and child are relevant,
          // try to match the assumptions in pValueMap and any control automaton
          // with nondet function calls on the CFA edges between parent and child.
          // Any match is a new element for the test vector.
          List<CFAEdge> edges = parent.getEdgesToChild(child);
          State lastSuccInSequence = previous;
          boolean sequenceHandledSuccessfully = true;
          for (CFAEdge edge : edges) {
            // 'computeNextState' currently always returns a state with the child ARG state,
            // so we have to build our own state with the parent ARG state (for matching
            // assumptions)
            // but the current test vector (to not lose information)
            Optional<State> nextState =
                computeNextState(
                    new State(parent, lastSuccInSequence.testVector()), child, edge, pValueMap);
            if (nextState.isEmpty()) {
              sequenceHandledSuccessfully = false;
              break;
            }
            lastSuccInSequence = nextState.orElseThrow();
          }
          if (sequenceHandledSuccessfully && visited.add(lastSuccInSequence)) {
            stack.push(lastSuccInSequence);
            CFAEdge lastEdgeInSequence = edges.get(edges.size() - 1);
            lastEdgeStack.push(lastEdgeInSequence);
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
    if (pEdge instanceof AStatementEdge statementEdge) {
      return handleStatementEdge(pPrevious, pChild, statementEdge, pValueMap);
    } else if (pEdge instanceof ADeclarationEdge declarationEdge) {
      return handleDeclarationEdge(pPrevious, pChild, declarationEdge, pValueMap);
    }
    return Optional.of(new State(pChild, pPrevious.testVector));
  }

  private Optional<State> handleStatementEdge(
      State pPrevious,
      ARGState pChild,
      AStatementEdge pStatementEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    AStatement statement = pStatementEdge.getStatement();
    if (statement instanceof AFunctionCall functionCall) {
      AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
      AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

      if (!isPredefinedFunction(functionDeclaration)
          && !(functionCallExpression.getExpressionType() instanceof CVoidType)
          && (functionCallExpression.getExpressionType() != JSimpleType.VOID)) {

        AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
        if (nameExpression instanceof AIdExpression idExpression) {

          ASimpleDeclaration declaration = idExpression.getDeclaration();
          if (declaration != null) {
            String name = declaration.getQualifiedName();
            if (cfa.getFunctionHead(name) == null
                && (!onlyVerifierNondet || name.startsWith("__VERIFIER_nondet"))) {
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
              } else if (onlyVerifierNondet && name.startsWith("__VERIFIER_nondet")) {
                Optional<ExpressionTestValue> defaultValue =
                    getDefaultValue(machineModel, functionDeclaration.getType().getReturnType());
                if (defaultValue.isPresent()) {
                  return Optional.of(
                      new State(
                          pChild,
                          pPrevious.testVector.addInputValue(
                              functionDeclaration, defaultValue.orElseThrow())));
                }
              }
              return Optional.empty();
            }
          }
        }
      }
    }
    return Optional.of(new State(pChild, pPrevious.testVector()));
  }

  private Optional<State> handleDeclarationEdge(
      State pPrevious,
      ARGState pChild,
      ADeclarationEdge pDeclarationEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    if (!provideDummyValues) {
      return Optional.of(new State(pChild, pPrevious.testVector()));
    }

    ADeclaration declaration = pDeclarationEdge.getDeclaration();
    if ((declaration instanceof CVariableDeclaration variableDeclaration)
        && (variableDeclaration.getCStorageClass() == CStorageClass.EXTERN)) {
      final Optional<State> nextState =
          handleVariableDeclaration(
              pDeclarationEdge, pPrevious, pChild, variableDeclaration, pValueMap);
      if (nextState.isPresent()) {
        return nextState;
      }
      Type type = variableDeclaration.getType();
      Type canonicalType = getCanonicalType(type);
      if (canonicalType instanceof CPointerType) {
        return Optional.of(
            new State(
                pChild, handlePointerDeclaration(pPrevious.testVector(), variableDeclaration)));
      }
      if (canonicalType instanceof CCompositeType) {
        return Optional.of(
            new State(
                pChild, handleCompositeDeclaration(pPrevious.testVector(), variableDeclaration)));
      }
      if (canonicalType instanceof CArrayType) {
        return Optional.of(
            new State(pChild, handleArrayDeclaration(pPrevious.testVector(), variableDeclaration)));
      }
      return Optional.of(
          new State(
              pChild,
              pPrevious
                  .testVector()
                  .addInputValue(
                      variableDeclaration,
                      getDummyInitializer(machineModel, variableDeclaration.getType()))));
    }
    return Optional.of(new State(pChild, pPrevious.testVector()));
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
        value -> vector -> vector.addInputValue(pFunctionCallExpression.getDeclaration(), value),
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
    } else if (pVariableDeclaration instanceof JVariableDeclaration variableDeclaration) {
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
        value -> vector -> vector.addInputValue(pVariableDeclaration, toInitializer(value)),
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
          Optional<AExpression> value = getOther(assumption, pLeftHandSide);
          if (value.isPresent()) {
            AExpression v = castIfNecessary(pLeftHandSide.getExpressionType(), value.orElseThrow());
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
          Optional<AExpression> value = getOther(assumption, pLeftHandSide);
          if (value.isPresent()) {
            AExpression v = castIfNecessary(pLeftHandSide.getExpressionType(), value.orElseThrow());
            return Optional.of(new State(pChild, pUpdate.apply(v).apply(pPrevious.testVector())));
          }
        }
      }
      Collection<ARGState> nextCandidates = argState.getChildren();
      if (nextCandidates.size() == 1) {
        ARGState candidate = nextCandidates.iterator().next();
        if (argState.getEdgesToChild(candidate).stream()
            .allMatch(
                e -> e instanceof AssumeEdge || AutomatonGraphmlCommon.handleAsEpsilonEdge(e))) {
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
        addDummyValue(pPrevious.testVector(), functionCallExpression.getDeclaration());
    return Optional.of(new State(pChild, newTestVector));
  }

  private Optional<State> handlePointerCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression pFunctionCallExpression) {
    TestVector newTestVector =
        handlePointerCall(pPrevious.testVector(), pFunctionCallExpression.getDeclaration());
    return Optional.of(new State(pChild, newTestVector));
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

  private TestVector handlePointerDeclaration(
      TestVector pTestVector, AVariableDeclaration pVariableDeclaration) {
    Type declarationType = pVariableDeclaration.getType();
    Preconditions.checkArgument(declarationType instanceof CPointerType);
    ExpressionTestValue pointerValue = handlePointer((CPointerType) declarationType, true);
    AExpression value = pointerValue.getValue();
    final AInitializer initializer = toInitializer(value);
    InitializerTestValue initializerTestValue =
        InitializerTestValue.of(pointerValue.getAuxiliaryCode(), initializer);
    return pTestVector.addInputValue(pVariableDeclaration, initializerTestValue);
  }

  private static AInitializer toInitializer(AExpression pValue) {
    if (pValue instanceof CExpression cExpression) {
      return new CInitializerExpression(FileLocation.DUMMY, cExpression);
    }
    if (pValue instanceof JExpression jExpression) {
      return new JInitializerExpression(FileLocation.DUMMY, jExpression);
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
      if (pType.getType() instanceof CFunctionType innerType) {
        List<CType> parameterTypes = innerType.getParameters();
        List<CParameterDeclaration> parameters = new ArrayList<>(parameterTypes.size());
        for (int i = 0; i < parameterTypes.size(); i++) {
          // If the innerType is a CFunctionTypeWithNames,
          // the parameters names we provide here may not be used.
          // Instead, the original parameter names of innerType will be used.
          // We ignore this here, because (a) we do not care about concrete names,
          // and (b) because we should not use CFunctionTypeWithNames explicitly,
          // outside the cfa package.
          CType parameterType = parameterTypes.get(i);
          String parameterName = "p" + i;
          CParameterDeclaration parameterDeclaration =
              new CParameterDeclaration(FileLocation.DUMMY, parameterType, parameterName);
          parameters.add(parameterDeclaration);
        }
        CFunctionDeclaration tmpDeclaration =
            new CFunctionDeclaration(
                FileLocation.DUMMY, innerType, varName, parameters, ImmutableSet.of());
        CIdExpression var = new CIdExpression(FileLocation.DUMMY, tmpDeclaration);
        String functionDefinition = getDummyFunctionDefinition(tmpDeclaration);
        return ExpressionTestValue.of(
            new AuxiliaryCode(functionDefinition),
            new CUnaryExpression(FileLocation.DUMMY, pType, var, UnaryOperator.AMPER));
      } else {
        CVariableDeclaration tmpDeclaration =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                true,
                CStorageClass.AUTO,
                pType.getType(),
                varName,
                varName,
                varName,
                (CInitializer) getDummyInitializer(machineModel, pType.getType()));
        CIdExpression var = new CIdExpression(FileLocation.DUMMY, tmpDeclaration);
        return ExpressionTestValue.of(
            new AuxiliaryCode(tmpDeclaration.toASTString()),
            new CUnaryExpression(FileLocation.DUMMY, pType, var, UnaryOperator.AMPER));
      }
    }
    ExpressionTestValue pointerValue =
        assignMallocToTmpVariable(pTargetSize, pType.getType(), false);
    return ExpressionTestValue.of(pointerValue.getAuxiliaryCode(), pointerValue.getValue());
  }

  private String getDummyFunctionDefinition(CFunctionDeclaration pDeclaration) {
    String functionSignature = getFunctionSignature(pDeclaration);
    return functionSignature
        + " { "
        + getReturnForDummyFunction(pDeclaration.getType().getReturnType())
        + " }";
  }

  private String getFunctionSignature(CFunctionDeclaration pDeclaration) {
    // To not have to build the function signature ourselves, we take a shortcut here:
    // Generate the function declaration, and then strip the trailing ';' from the string.
    String functionDeclaration = pDeclaration.toASTString();
    assert functionDeclaration.endsWith(";");
    return functionDeclaration.substring(0, functionDeclaration.length() - 1);
  }

  private String getReturnForDummyFunction(CType returnType) {
    if (returnType.equals(CVoidType.VOID)) {
      return "return;";
    } else {
      return "return " + getDummyValue(machineModel, returnType).toASTString() + ";";
    }
  }

  private Optional<State> handleCompositeCall(
      State pPrevious, ARGState pChild, AFunctionCallExpression pFunctionCallExpression) {
    AFunctionDeclaration declaration = pFunctionCallExpression.getDeclaration();
    TestVector newTestVector = handleCompositeCall(pPrevious.testVector(), declaration);
    return Optional.of(new State(pChild, newTestVector));
  }

  private TestVector handleCompositeCall(
      TestVector pTestVector, AFunctionDeclaration pDeclaration) {
    Preconditions.checkArgument(returnsComposite(pDeclaration));
    CType expectedTargetType = (CType) pDeclaration.getType().getReturnType();

    return pTestVector.addInputValue(
        pDeclaration, handleComposite(expectedTargetType, getSizeOf(expectedTargetType), false));
  }

  private TestVector handleCompositeDeclaration(
      TestVector pTestVector, AVariableDeclaration pVariableDeclaration) {
    Type expectedTargetType = pVariableDeclaration.getType();
    Preconditions.checkArgument(getCanonicalType(expectedTargetType) instanceof CCompositeType);

    return pTestVector.addInputValue(
        pVariableDeclaration, getDummyInitializer(machineModel, pVariableDeclaration.getType()));
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

    return ExpressionTestValue.of(pointerValue.getAuxiliaryCode(), value);
  }

  private TestVector handleArrayDeclaration(
      TestVector pTestVector, AVariableDeclaration pVariableDeclaration) {
    return pTestVector.addInputValue(
        pVariableDeclaration, getDummyInitializer(machineModel, pVariableDeclaration.getType()));
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
    return ExpressionTestValue.of(
        ImmutableList.of(
            new AuxiliaryCode(tmpVarDeclaration.toASTString()),
            new AuxiliaryCode(assignment.toASTString())),
        variable);
  }

  private CExpression getSizeOf(CType pExpectedTargetType) {
    final CExpression size;
    CType canonicalType = pExpectedTargetType.getCanonicalType();
    CExpression dummyLength =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.valueOf(4096));
    if (canonicalType.equals(CVoidType.VOID)) {
      size = dummyLength;
    } else if (canonicalType instanceof CArrayType arrayType) {
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
            CPointerType.POINTER_TO_VOID, Collections.singletonList(CNumericTypes.INT), false);
    CFunctionDeclaration functionDeclaration =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            type,
            "malloc",
            ImmutableList.of(
                new CParameterDeclaration(
                    FileLocation.DUMMY, CPointerType.POINTER_TO_VOID, "size")),
            ImmutableSet.of());
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        CPointerType.POINTER_TO_VOID,
        new CIdExpression(FileLocation.DUMMY, functionDeclaration),
        Collections.singletonList(pSize),
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
    AExpression value = getDummyValue(machineModel, pFunctionDeclaration.getType().getReturnType());
    return addValue(pTestVector, pFunctionDeclaration, value);
  }

  private static AExpression getDummyValue(MachineModel pMachineModel, Type pType) {
    if (pType instanceof CType cType) {
      if (canInitialize(pType)) {
        CInitializer initializer = CDefaults.forType(pMachineModel, cType, FileLocation.DUMMY);
        if (initializer instanceof CInitializerExpression cInitializerExpression) {
          return cInitializerExpression.getExpression();
        }
      }
      return new CIntegerLiteralExpression(
          FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.ZERO);
    }
    return new JIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ZERO);
  }

  private static AInitializer getDummyInitializer(MachineModel pMachineModel, Type pType) {
    if (pType instanceof CType cType) {
      if (canInitialize(pType)) {
        return CDefaults.forType(pMachineModel, cType, FileLocation.DUMMY);
      }
      return new CInitializerExpression(
          FileLocation.DUMMY,
          new CIntegerLiteralExpression(
              FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, BigInteger.ZERO));
    }
    return new JInitializerExpression(
        FileLocation.DUMMY, new JIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ZERO));
  }

  private static Optional<ExpressionTestValue> getDefaultValue(
      MachineModel pMachineModel, final Type pReturnType) {
    if (pReturnType instanceof CType cType) {

      CType returnType = cType.getCanonicalType();

      if (returnType instanceof CSimpleType cSimpleType
          && cSimpleType.getType() == CBasicType.CHAR) {
        return Optional.of(
            ExpressionTestValue.of(
                new CCharLiteralExpression(FileLocation.DUMMY, returnType, ' ')));
      }

      if (!(returnType instanceof CCompositeType
          || returnType instanceof CArrayType
          || returnType instanceof CBitFieldType
          || (returnType instanceof CElaboratedType cElaboratedType
              && cElaboratedType.getKind() != ComplexTypeKind.ENUM))) {

        return Optional.of(
            ExpressionTestValue.of(
                ((CInitializerExpression)
                        CDefaults.forType(pMachineModel, returnType, FileLocation.DUMMY))
                    .getExpression()));
      }
    }
    return Optional.empty();
  }

  static boolean canInitialize(Type pType) {
    Type canonicalType = getCanonicalType(pType);
    if (canonicalType.equals(CVoidType.VOID)) {
      return false;
    }
    if (canonicalType instanceof CCompositeType cCompositeType) {
      return !cCompositeType.isIncomplete();
    }
    if (canonicalType instanceof CElaboratedType cElaboratedType) {
      return cElaboratedType.getKind() == ComplexTypeKind.ENUM;
    }
    if (canonicalType instanceof CFunctionType) {
      return false;
    }
    return true;
  }

  private static TestVector addValue(
      TestVector pTestVector, AFunctionDeclaration pFunctionDeclaration, AExpression pValue) {
    AExpression value = castIfNecessary(pFunctionDeclaration.getType().getReturnType(), pValue);
    return pTestVector.addInputValue(pFunctionDeclaration, value);
  }

  private static AExpression castIfNecessary(Type pExpectedReturnType, AExpression pValue) {
    AExpression value = pValue;
    Type expectedReturnType = getCanonicalType(pExpectedReturnType);
    Type actualType = getCanonicalType(value.getExpressionType());
    if (!areTypesCompatible(pValue, expectedReturnType)) {
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
                pValue.getFileLocation(), (CType) pExpectedReturnType, (CExpression) value);
      } else if (value instanceof JExpression && expectedReturnType instanceof JType jType) {
        value = new JCastExpression(pValue.getFileLocation(), jType, (JExpression) value);
      }
    }
    return value;
  }

  private static boolean areTypesCompatible(AExpression pValue, Type pExpectedType) {
    Type actualType = getCanonicalType(pValue.getExpressionType());
    if (actualType.equals(pExpectedType)) {
      return true;
    }
    if ((actualType instanceof CSimpleType simpleActualType
            && pExpectedType instanceof CSimpleType simpleExpectedType)
        && (simpleActualType.hasUnsignedSpecifier() && simpleExpectedType.hasUnsignedSpecifier())) {
      return true;
    }
    return false;
  }

  private static Optional<AExpression> getOther(
      AExpression pAssumption, ALeftHandSide pLeftHandSide) {
    if (!(pAssumption instanceof ABinaryExpression binOp)) {
      return Optional.empty();
    }

    if (binOp.getOperator() != BinaryOperator.EQUALS
        && binOp.getOperator()
            != org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.EQUALS) {
      return Optional.empty();
    }
    if (binOp.getOperand1().equals(pLeftHandSide)) {
      return Optional.of(binOp.getOperand2());
    }
    if (binOp.getOperand2().equals(pLeftHandSide)) {
      return Optional.of(binOp.getOperand1());
    }
    return Optional.empty();
  }
}
