// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing.c;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.slicing.Slice;

public abstract class CSlice implements Slice {

  private static final String SOME_VALUE_PREFIX = "__SOME_VALUE_";

  private final CFA originalCfa;
  private final ImmutableCollection<CFAEdge> criteriaEdges;
  private final ImmutableSet<CFAEdge> relevantEdges;

  private final ImmutableMap<AFunctionDeclaration, FunctionEntryNode> entryNodes;

  protected CSlice(
      CFA pOriginalCfa,
      ImmutableCollection<CFAEdge> pCriteriaEdges,
      ImmutableSet<CFAEdge> pRelevantEdges) {

    originalCfa = pOriginalCfa;
    criteriaEdges = pCriteriaEdges;
    relevantEdges = pRelevantEdges;

    entryNodes =
        originalCfa.getAllFunctionHeads().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    entryNode -> entryNode.getFunction(), entryNode -> entryNode));
  }

  protected abstract boolean isInitializerRelevant(
      CFAEdge pEdge, CVariableDeclaration pVariableInitializer);

  protected abstract boolean isParameterRelevant(
      CFAEdge pEdge, CParameterDeclaration pCParameterDeclaration);

  protected abstract boolean isReturnVariableRelevant(
      CFAEdge pEdge, CVariableDeclaration pVariableDeclaration);

  protected abstract boolean isArgumentRelevant(
      CFAEdge pEdge, CParameterDeclaration pCParameterDeclaration);

  protected abstract boolean isReturnValueRelevant(
      CFAEdge pEdge, CVariableDeclaration pCVariableDeclaration);

  @Override
  public CFA getOriginalCfa() {
    return originalCfa;
  }

  @Override
  public ImmutableCollection<CFAEdge> getUsedCriteria() {
    return criteriaEdges;
  }

  @Override
  public ImmutableSet<CFAEdge> getRelevantEdges() {
    return relevantEdges;
  }

  @Override
  public Optional<AAstNode> getRelevantAstNode(CFAEdge pEdge) {

    if (pEdge instanceof CDeclarationEdge) {
      CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        return Optional.of(
            getRelevantVariableDeclaration(pEdge, (CVariableDeclaration) declaration));
      } else if (declaration instanceof CFunctionDeclaration) {
        return Optional.of(
            getRelevantFunctionDeclaration(pEdge, (CFunctionDeclaration) declaration));
      }
    } else if (pEdge instanceof CFunctionCallEdge) {
      return Optional.of(
          getRelevantFunctionCall(pEdge, ((CFunctionCallEdge) pEdge).getFunctionCall()));
    } else if (pEdge instanceof CFunctionSummaryEdge) {
      return Optional.of(
          getRelevantFunctionCall(pEdge, ((CFunctionSummaryEdge) pEdge).getExpression()));
    }

    return pEdge.getRawAST();
  }

  private CVariableDeclaration getRelevantVariableDeclaration(
      CFAEdge pEdge, CVariableDeclaration pVariableDeclaration) {

    if (isInitializerRelevant(pEdge, pVariableDeclaration)) {
      return pVariableDeclaration;
    } else {
      var variableDeclarationWithoutInitializer =
          new CVariableDeclaration(
              pVariableDeclaration.getFileLocation(),
              pVariableDeclaration.isGlobal(),
              pVariableDeclaration.getCStorageClass(),
              pVariableDeclaration.getType(),
              pVariableDeclaration.getName(),
              pVariableDeclaration.getOrigName(),
              pVariableDeclaration.getQualifiedName(),
              null);
      return variableDeclarationWithoutInitializer;
    }
  }

  private Optional<CVariableDeclaration> getFunctionReturnVariableDeclaration(
      AFunctionDeclaration pFunctionDeclaration) {
    return entryNodes
        .get(pFunctionDeclaration)
        .getReturnVariable()
        .map(declaration -> (CVariableDeclaration) declaration);
  }

  private CFunctionDeclaration getRelevantFunctionDeclaration(
      CFAEdge pEdge, CFunctionDeclaration pFunctionDeclaration) {

    List<CParameterDeclaration> parameterDeclarations = pFunctionDeclaration.getParameters();

    ImmutableList<CParameterDeclaration> relevantParameterDeclarations =
        parameterDeclarations.stream()
            .filter(declaration -> isParameterRelevant(pEdge, declaration))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<CType> relevantParameterTypes =
        relevantParameterDeclarations.stream()
            .map(CParameterDeclaration::getType)
            .collect(ImmutableList.toImmutableList());

    CFunctionType functionType = pFunctionDeclaration.getType();
    Optional<CVariableDeclaration> optReturnVariableDeclaration =
        getFunctionReturnVariableDeclaration(pFunctionDeclaration);
    CType relevantReturnType =
        optReturnVariableDeclaration.isPresent()
                && isReturnVariableRelevant(pEdge, optReturnVariableDeclaration.orElseThrow())
            ? functionType.getReturnType()
            : CVoidType.VOID;
    boolean relevantTakesVarargs =
        functionType.takesVarArgs()
            && !parameterDeclarations.isEmpty()
            && isParameterRelevant(
                pEdge, parameterDeclarations.get(parameterDeclarations.size() - 1));
    CFunctionType relevantFunctionType =
        new CFunctionType(relevantReturnType, relevantParameterTypes, relevantTakesVarargs);

    return new CFunctionDeclaration(
        pFunctionDeclaration.getFileLocation(),
        relevantFunctionType,
        pFunctionDeclaration.getName(),
        pFunctionDeclaration.getOrigName(),
        relevantParameterDeclarations);
  }

  private CFunctionCallExpression getRelevantFunctionCallExpression(
      CFAEdge pEdge, CFunctionCallExpression pFunctionCallExpression) {

    CFunctionDeclaration functionDeclaration = pFunctionCallExpression.getDeclaration();
    CFunctionDeclaration relevantFunctionDeclaration =
        getRelevantFunctionDeclaration(pEdge, functionDeclaration);

    List<CParameterDeclaration> parameterDeclarations = functionDeclaration.getParameters();
    List<CExpression> parameterExpressions = pFunctionCallExpression.getParameterExpressions();
    ImmutableList.Builder<CExpression> relevantParameterDeclarationsBuilder =
        ImmutableList.builder();
    for (int index = 0; index < parameterExpressions.size(); index++) {

      CExpression argumentExpression = parameterExpressions.get(index);

      if (index >= parameterDeclarations.size()
          || isArgumentRelevant(pEdge, parameterDeclarations.get(index))) {
        relevantParameterDeclarationsBuilder.add(argumentExpression);
        continue;
      }

      // function parameter is relevant but actual argument is not
      if (isParameterRelevant(pEdge, parameterDeclarations.get(index))) {

        String someValueVariableName =
            SOME_VALUE_PREFIX
                + pEdge.getPredecessor().getNodeNumber()
                + "_"
                + pEdge.getSuccessor().getNodeNumber()
                + "_"
                + index;
        var someValueVariableDeclaration =
            new CVariableDeclaration(
                argumentExpression.getFileLocation(),
                false,
                CStorageClass.AUTO,
                argumentExpression.getExpressionType(),
                someValueVariableName,
                someValueVariableName,
                someValueVariableName,
                null);
        var someValueIdExpression =
            new CIdExpression(argumentExpression.getFileLocation(), someValueVariableDeclaration);
        relevantParameterDeclarationsBuilder.add(someValueIdExpression);
      }
    }

    return new CFunctionCallExpression(
        pFunctionCallExpression.getFileLocation(),
        relevantFunctionDeclaration.getType().getReturnType(),
        pFunctionCallExpression.getFunctionNameExpression(),
        relevantParameterDeclarationsBuilder.build(),
        relevantFunctionDeclaration);
  }

  private CFunctionCall getRelevantFunctionCall(
      CFAEdge pEdge, CFunctionCallStatement pFunctionCallStatement) {

    CFunctionCallExpression relevantFunctionCallExpression =
        getRelevantFunctionCallExpression(
            pEdge, pFunctionCallStatement.getFunctionCallExpression());

    return new CFunctionCallStatement(
        pFunctionCallStatement.getFileLocation(), relevantFunctionCallExpression);
  }

  private CFunctionCall getRelevantFunctionCall(
      CFAEdge pEdge, CFunctionCallAssignmentStatement pFunctionCallAssignmentStatement) {

    CFunctionCallExpression functionCallExpression =
        pFunctionCallAssignmentStatement.getFunctionCallExpression();
    CFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

    Optional<CVariableDeclaration> optReturnVariableDeclaration =
        getFunctionReturnVariableDeclaration(functionDeclaration);

    CFunctionCallExpression relevantFunctionCallExpression =
        getRelevantFunctionCallExpression(
            pEdge, pFunctionCallAssignmentStatement.getFunctionCallExpression());

    if (optReturnVariableDeclaration.isEmpty()
        || !isReturnValueRelevant(pEdge, optReturnVariableDeclaration.orElseThrow())) {
      return new CFunctionCallStatement(
          pFunctionCallAssignmentStatement.getFileLocation(), relevantFunctionCallExpression);
    }

    return new CFunctionCallAssignmentStatement(
        pFunctionCallAssignmentStatement.getFileLocation(),
        pFunctionCallAssignmentStatement.getLeftHandSide(),
        relevantFunctionCallExpression);
  }

  private CFunctionCall getRelevantFunctionCall(CFAEdge pEdge, CFunctionCall pFunctionCall) {
    if (pFunctionCall instanceof CFunctionCallStatement) {
      return getRelevantFunctionCall(pEdge, (CFunctionCallStatement) pFunctionCall);
    } else if (pFunctionCall instanceof CFunctionCallAssignmentStatement) {
      return getRelevantFunctionCall(pEdge, (CFunctionCallAssignmentStatement) pFunctionCall);
    } else {
      throw new AssertionError("Unknown function call type: " + pFunctionCall.getClass());
    }
  }
}
