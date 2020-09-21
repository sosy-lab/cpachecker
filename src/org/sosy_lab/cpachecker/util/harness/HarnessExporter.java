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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

@Options(prefix = "testHarnessExport")
public class HarnessExporter {

  private final CFA cfa;
  private final LogManager logger;

  @Option(secure = true, description = "Use the counterexample model to provide test-vector values")
  private boolean useModel = true;

  public HarnessExporter(Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    cfa = pCFA;
    logger = pLogger;
    pConfig.inject(this);
  }

  public void writeHarness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      CounterexampleInfo pCounterexampleInfo)
      throws IOException {

    TestVectorExtractor testVectorExtractor = new TestVectorExtractor(cfa, logger);

    // Find a path with sufficient test vector info
    Optional<TargetTestVector> optionalTestVector =
        testVectorExtractor.extractTestVector(
            pRootState, pIsRelevantState, pIsRelevantEdge, getValueMap(pCounterexampleInfo));
    if (optionalTestVector.isPresent()) {
      TargetTestVector testVector = optionalTestVector.get();
      Set<AFunctionDeclaration> externalFunctions = getExternalFunctions();

      CodeAppender codeAppender = new CodeAppender(pTarget, cfa);
      codeAppender.appendIncludes();
      codeAppender.appendGenericBoilerplate();

      Optional<AFunctionDeclaration> errorFunction =
          getErrorFunction(externalFunctions, testVector);

      Set<AFunctionDeclaration> externalFunctionsWithoutErrorFunction =
          getFunctionDeclarationSetWithoutElement(externalFunctions, errorFunction);

      codeAppender = appendErrorFunctionIfNeeded(errorFunction, codeAppender);
      codeAppender = writeVerifierAssumeIfNeeded(externalFunctions, codeAppender);

      TestVector vector =
          testVectorExtractor.completeExternalFunctions(
              testVector.testVector, externalFunctionsWithoutErrorFunction);
      codeAppender.append(vector);

    } else {
      logger.log(
          Level.WARNING, "Could not export a test harness, some test-vector values are missing.");
    }
  }

  private Set<AFunctionDeclaration> getFunctionDeclarationSetWithoutElement(
      Set<AFunctionDeclaration> pBaseSet, Optional<AFunctionDeclaration> pElementToRemove) {
    Set<AFunctionDeclaration> result = pBaseSet;
    if (pElementToRemove.isPresent()) {
      result =
          pBaseSet
              .stream()
              .filter(Predicates.not(Predicates.equalTo(pElementToRemove.get())))
              .collect(Collectors.toSet());
    }
    return result;
  }

  private CodeAppender appendErrorFunctionIfNeeded(
      Optional<AFunctionDeclaration> pErrorFunction, CodeAppender pCodeAppender)
      throws IOException {
    if (pErrorFunction.isPresent()) {
      pCodeAppender.appendErrorFunctionImplementation(pErrorFunction.get());
    } else {
      logger.log(Level.WARNING, "Could not find a call to an error function.");
    }
    return pCodeAppender;
  }

  private CodeAppender writeVerifierAssumeIfNeeded(
      Set<AFunctionDeclaration> pExternalFunctions, CodeAppender pCodeAppender) throws IOException {
    if (pExternalFunctions.stream().anyMatch(PredefinedTypes::isVerifierAssume)) {
      // implement __VERIFIER_assume with exit (EXIT_SUCCESS)
      pCodeAppender.appendln("void __VERIFIER_assume(int cond) { if (!(cond)) { exit(0); }}");
    }
    return pCodeAppender;
  }

  private Optional<AFunctionDeclaration> getErrorFunction(
      Set<AFunctionDeclaration> pExternalFunctions, TargetTestVector pTestVector) {
    CFAEdge edgeToTarget = pTestVector.edgeToTarget;
    if (edgeToTarget instanceof AStatementEdge) {
      AStatementEdge statementEdge = (AStatementEdge) edgeToTarget;
      AStatement statement = statementEdge.getStatement();
      if (statement instanceof AFunctionCall) {
        AFunctionCall functionCallStatement = (AFunctionCall) statement;
        AFunctionCallExpression functionCallExpression =
            functionCallStatement.getFunctionCallExpression();
        AFunctionDeclaration declaration = functionCallExpression.getDeclaration();
        if (declaration != null && pExternalFunctions.contains(declaration)) {
          return Optional.of(functionCallExpression.getDeclaration());
        }
      }
    }
    return Optional.empty();
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
              if (declaration instanceof AFunctionDeclaration) {
                AFunctionDeclaration functionDeclaration = (AFunctionDeclaration) declaration;
                if (!cfa.getAllFunctionNames().contains(functionDeclaration.getName())) {
                  externalFunctions.add(functionDeclaration);
                }
              }
            }
            return TraversalProcess.CONTINUE;
          }
        };
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), externalFunctionCollector);
    return externalFunctions;
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions> getValueMap(
      CounterexampleInfo pCounterexampleInfo) {
    if (useModel && pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    }
    return ImmutableMultimap.of();
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

  /**
   * Create a test vector that contains dummy values for the given external functions that are not
   * yet part of the provided test vector.
   *
   * @param pVector the current test vector
   * @param pExternalFunctions the external functions to check
   * @return a test vector that contains the values of the given vector and the newly created dummy
   *     values.
   */
  public static class TargetTestVector {

    private final CFAEdge edgeToTarget;

    private final TestVector testVector;

    public TestVector getTestVector() {
      return testVector;
    }

    public TargetTestVector(CFAEdge pEdgeToTarget, TestVector pTestVector) {
      edgeToTarget = Objects.requireNonNull(pEdgeToTarget);
      testVector = Objects.requireNonNull(pTestVector);
    }

    @Override
    public String toString() {
      return testVector.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(edgeToTarget, testVector);
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      }
      if (pObj instanceof TargetTestVector) {
        TargetTestVector other = (TargetTestVector) pObj;
        return edgeToTarget.equals(other.edgeToTarget) && testVector.equals(other.testVector);
      }
      return false;
    }

  }
}
