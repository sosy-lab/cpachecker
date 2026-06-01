// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;

class CToSvLibTransformation {
  // private final CFA cfa;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final SvLibCurrentScope scope;

  private final String INPUT_DUMMY_VAR_PREFIX;
  private final ImmutableSet<String> NAMES_OF_ASSERT_FUNCTIONS;

  CToSvLibTransformation(
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      FormulaToSvLibVisitor pFormulaToSvLibVisitor,
      SvLibCurrentScope pCurrentScope,
      String pINPUT_DUMMY_VAR_PREFIX,
      ImmutableSet<String> pNAMES_OF_ASSERT_FUNCTIONS) {
    // cfa = pCFA;
    formulaManager = pFormulaManager;
    pathFormulaManager = pPathFormulaManager;
    formulaToSvLibVisitor = pFormulaToSvLibVisitor;
    scope = pCurrentScope;
    INPUT_DUMMY_VAR_PREFIX = pINPUT_DUMMY_VAR_PREFIX;
    NAMES_OF_ASSERT_FUNCTIONS = pNAMES_OF_ASSERT_FUNCTIONS;
  }

  SvLibStatement transformFunction(CFunctionEntryNode pEntryNode)
      throws CPATransferException, InterruptedException {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pEntryNode.getFunctionName());
    String procedureName = procedureDeclaration.getProcedureName();
    ImmutableListMultimap.Builder<CFANode, SvLibStatement> statementCollector =
        ImmutableListMultimap.builder();
    ImmutableMap.Builder<CFAEdge, PointerTargetSet> edgeToPointerTargetSet = ImmutableMap.builder();

    scope.enterProcedure(
        FluentIterable.from(procedureDeclaration.getParameters())
            .append(procedureDeclaration.getLocalVariables())
            .append(procedureDeclaration.getReturnValues())
            .toList());

    // Only in the procedure created for the main() function, and not entry functions in general:
    // Initialize _retval_ to 0 to account for the possibility of an implicit return 0; statement
    // in the main() function if no return value has been explicitly defined.
    if (pEntryNode.getFunctionName().contains("main")
        && !procedureDeclaration.getReturnValues().isEmpty()) {
      statementCollector.put(
          pEntryNode,
          createDefaultReturnForMain(pEntryNode, procedureDeclaration, edgeToPointerTargetSet));
    }

    // assign the dummy variables created for the input parameters to assignable variables that
    // have the original variable names
    if (!procedureDeclaration.getParameters().isEmpty()) {
      ImmutableMap.Builder<SvLibSimpleParsingDeclaration, SvLibTerm> inputAssignmentsCollector =
          ImmutableMap.builder();

      for (SvLibParsingParameterDeclaration inputParameter : procedureDeclaration.getParameters()) {
        SvLibSimpleParsingDeclaration assignableInputDummyVariable =
            scope.getVariable(getOriginalNameOfInputParameterDummy(inputParameter.getName()));

        inputAssignmentsCollector.put(
            assignableInputDummyVariable,
            new SvLibIdTerm(inputParameter.toSimpleDeclaration(), FileLocation.DUMMY));
      }

      SvLibAssignmentStatement assignDummyInput =
          new SvLibAssignmentStatement(
              inputAssignmentsCollector.buildOrThrow(),
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of());
      statementCollector.put(pEntryNode, assignDummyInput);
    }

    ImmutableList<CFAEdge> relevantEdges = getAllRelevantEdges(pEntryNode);

    createAllLabels(pEntryNode, relevantEdges, statementCollector);

    // transform each edge to SV-LIB statement(s)
    for (CFAEdge currentEdge : relevantEdges) {
      handleEdge(currentEdge, statementCollector, edgeToPointerTargetSet);

      if (!(currentEdge instanceof CReturnStatementEdge)
          && currentEdge.getSuccessor() instanceof FunctionExitNode functionExitNode) {
        statementCollector.put(
            functionExitNode,
            new SvLibReturnStatement(FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of()));
      }
      if (currentEdge.getSuccessor() instanceof CFATerminationNode terminationNode) {
        statementCollector.put(terminationNode, encodeTerminationNode(terminationNode));
      }
    }

    SvLibStatement procedureBodySequence =
        createSequenceStatement(statementCollector.build(), procedureName);

    scope.leaveProcedure();
    return procedureBodySequence;
  }

  private ImmutableList<CFAEdge> getAllRelevantEdges(FunctionEntryNode pEntryNode) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pEntryNode, edgeCollector);
    return ImmutableList.copyOf(edgeCollector.getVisitedEdges());
  }

  private void createAllLabels(
      FunctionEntryNode pFunctionEntryNode,
      ImmutableList<CFAEdge> pRelevantEdges,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pStatementCollector) {
    ImmutableSet.Builder<CFANode> relevantNodesCollector = ImmutableSet.builder();
    relevantNodesCollector.add(pFunctionEntryNode);
    for (CFAEdge currentEdge : pRelevantEdges) {
      relevantNodesCollector.add(currentEdge.getSuccessor());
    }

    ImmutableSet<CFANode> relevantNodes = relevantNodesCollector.build();
    for (CFANode node : relevantNodes) {
      String nodeNumber = node.toString();
      pStatementCollector.put(node, createLabelStatement(nodeNumber));

      if (node instanceof CFALabelNode labelNode) {
        String labelNodeName = labelNode.getLabel() + "__" + nodeNumber;
        pStatementCollector.put(node, createLabelStatement(labelNodeName));
      }
    }
  }

  private SvLibLabelStatement createLabelStatement(String pLabelName) {
    SvLibTagReference tagReference = new SvLibTagReference(pLabelName, FileLocation.DUMMY);
    return new SvLibLabelStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(tagReference), pLabelName);
  }

  private SvLibAssignmentStatement createDefaultReturnForMain(
      CFunctionEntryNode pEntryNode,
      SvLibProcedureDeclaration pProcedureDeclaration,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    CStatementEdge statementEdge =
        new CStatementEdge(
            "retval = 0;",
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY,
                new CIdExpression(FileLocation.DUMMY, pEntryNode.getReturnVariable().orElseThrow()),
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY,
                    pEntryNode.getReturnVariable().orElseThrow().getType(),
                    BigInteger.ZERO)),
            FileLocation.DUMMY,
            pEntryNode,
            pEntryNode);
    SvLibTerm assignmentTerm = transformToSvLibTerm(statementEdge, pEdgeToPointerTargetSet);
    // Obtain the only constant term inside the previous term
    // could be done with a visitor but is easier like this
    // and works for most cases
    SvLibConstantTerm returnValue;
    if (assignmentTerm instanceof SvLibConstantTerm pConstantTerm) {
      returnValue = pConstantTerm;
    } else if (assignmentTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm) {
      List<SvLibConstantTerm> constantTerms =
          FluentIterable.from(symbolApplicationTerm.getTerms())
              .filter(SvLibConstantTerm.class)
              .toList();
      returnValue = Iterables.getOnlyElement(constantTerms);
    } else {
      throw new UnsupportedOperationException(
          "Unexpected term generated for return value initialization in main function: "
              + assignmentTerm);
    }

    return new SvLibAssignmentStatement(
        ImmutableMap.of(pProcedureDeclaration.getReturnValues().getFirst(), returnValue),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  private void handleEdge(
      CFAEdge pEdge,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pCreatedStatements,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    switch (pEdge.getEdgeType()) {
      case BlankEdge -> {
        transformToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case AssumeEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
        SvLibGotoStatement gotoStatement = createGotoStatement(pEdge.getSuccessor());
        SvLibIfStatement ifStatement =
            new SvLibIfStatement(
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of(),
                transformedTerm,
                gotoStatement);
        pCreatedStatements.put(pEdge.getPredecessor(), ifStatement);
      }
      case StatementEdge -> {
        CStatementEdge statementEdge = (CStatementEdge) pEdge;
        if (statementEdge.getStatement() instanceof CFunctionCall) {
          SvLibStatement externCallStatement =
              handleExternFunctionCall(statementEdge, pEdgeToPointerTargetSet);
          pCreatedStatements.put(pEdge.getPredecessor(), externCallStatement);
        } else {
          SvLibTerm transformedTerm = transformToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
          SvLibStatement assignmentStatement = handleAssignment(pEdge, transformedTerm);
          pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement);
        }
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case DeclarationEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
        SvLibStatement assignmentStatement = handleAssignment(pEdge, transformedTerm);
        pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case ReturnStatementEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
        SvLibStatement assignmentStatement = handleAssignment(pEdge, transformedTerm);
        pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement);
        SvLibReturnStatement returnStatement =
            new SvLibReturnStatement(FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());
        pCreatedStatements.put(pEdge.getPredecessor(), returnStatement);
      }
      case FunctionCallEdge, FunctionReturnEdge -> {
        // function calls are handled in case for CallToReturnEdge
      }
      case CallToReturnEdge -> {
        // CFunctionSummaryEdge for function calls
        CFunctionSummaryEdge callEdge = (CFunctionSummaryEdge) pEdge;
        SvLibProcedureCallStatement callStatement =
            handleFunctionCall(callEdge, pEdgeToPointerTargetSet);
        pCreatedStatements.put(pEdge.getPredecessor(), callStatement);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
    }
  }

  private @NonNull SvLibTerm transformToSvLibTerm(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    return transformToSvLibTerm(pEdge, pEdgeToPointerTargetSet, false);
  }

  private @NonNull SvLibTerm transformToSvLibTerm(
      CFAEdge pEdge,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet,
      boolean isGhostEdge)
      throws CPATransferException, InterruptedException {
    PointerTargetSet pointerTargetSet = getPtsForEdge(pEdge, pEdgeToPointerTargetSet);
    PathFormula edgeFormula =
        pathFormulaManager.makeEmptyPathFormulaWithContext(SSAMap.emptySSAMap(), pointerTargetSet);
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge);
    if (!isGhostEdge) {
      pEdgeToPointerTargetSet.put(pEdge, edgeFormula.getPointerTargetSet());
    }
    return formulaManager.visit(edgeFormula.getFormula(), formulaToSvLibVisitor);
  }

  private PointerTargetSet getPtsForEdge(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws InterruptedException {
    PointerTargetSet pointerTargetSet = PointerTargetSet.emptyPointerTargetSet();
    if (pEdge.getPredecessor().getNumEnteringEdges() >= 1) {
      ImmutableMap<CFAEdge, PointerTargetSet> edgeToPtsBuilt =
          pEdgeToPointerTargetSet.buildOrThrow();
      SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
      FluentIterable<CFAEdge> enteringEdges = pEdge.getPredecessor().getEnteringEdges();
      for (CFAEdge enteringEdge : enteringEdges) {
        PointerTargetSet predEdgePts = edgeToPtsBuilt.get(enteringEdge);
        if (predEdgePts != null) {
          pointerTargetSet =
              pathFormulaManager.mergePts(pointerTargetSet, predEdgePts, ssaMapBuilder);
        }
      }
      if (pEdge.getPredecessor().getEnteringSummaryEdge() != null) {
        PointerTargetSet predEdgePts =
            edgeToPtsBuilt.get(pEdge.getPredecessor().getEnteringSummaryEdge());
        if (predEdgePts != null) {
          pointerTargetSet =
              pathFormulaManager.mergePts(pointerTargetSet, predEdgePts, ssaMapBuilder);
        }
      }
    }
    return pointerTargetSet;
  }

  private void storePtsForFunctionCall(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws InterruptedException, CPATransferException {
    PointerTargetSet pointerTargetSet = getPtsForEdge(pEdge, pEdgeToPointerTargetSet);

    PathFormula edgeFormula =
        pathFormulaManager.makeEmptyPathFormulaWithContext(SSAMap.emptySSAMap(), pointerTargetSet);
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge);
    pEdgeToPointerTargetSet.put(pEdge, edgeFormula.getPointerTargetSet());
  }

  private SvLibStatement handleExternFunctionCall(
      CStatementEdge pStatementEdge,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    if (pStatementEdge.getStatement()
        instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {
      storePtsForFunctionCall(pStatementEdge, pEdgeToPointerTargetSet);
      CIdExpression leftHandSide =
          (CIdExpression) functionCallAssignmentStatement.getLeftHandSide();
      SvLibProcedureDeclaration calledProcedure =
          scope.getProcedureDeclaration(
              functionCallAssignmentStatement
                  .getRightHandSide()
                  .getFunctionNameExpression()
                  .toASTString());

      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          calledProcedure,
          transformInputParameters(
              functionCallAssignmentStatement.getRightHandSide().getParameterExpressions(),
              pStatementEdge,
              calledProcedure,
              pEdgeToPointerTargetSet),
          ImmutableList.of(
              scope.getVariableForQualifiedName(leftHandSide.getDeclaration().getQualifiedName())));
    } else if (pStatementEdge.getStatement()
        instanceof CFunctionCallStatement functionCallStatement) {
      storePtsForFunctionCall(pStatementEdge, pEdgeToPointerTargetSet);

      SvLibProcedureDeclaration calledProcedure =
          scope.getProcedureDeclaration(
              functionCallStatement
                  .getFunctionCallExpression()
                  .getFunctionNameExpression()
                  .toASTString());

      // Handle calls to a set of external __assert functions that have a char* input parameter
      if (NAMES_OF_ASSERT_FUNCTIONS.contains(calledProcedure.getName())) {
        return new SvLibProcedureCallStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            calledProcedure,
            ImmutableList.of(),
            ImmutableList.of());
      }

      ImmutableList.Builder<SvLibSimpleParsingDeclaration> returnVariableDummies =
          ImmutableList.builder();
      for (SvLibParsingParameterDeclaration parsingParameterDeclaration :
          calledProcedure.getReturnValues()) {
        SvLibType returnType = parsingParameterDeclaration.getType();
        SvLibSimpleParsingDeclaration returnDummyVariable =
            scope.getVariable("transformationDummyReturn_" + returnType);
        returnVariableDummies.add(returnDummyVariable);
      }

      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          calledProcedure,
          transformInputParameters(
              functionCallStatement.getFunctionCallExpression().getParameterExpressions(),
              pStatementEdge,
              calledProcedure,
              pEdgeToPointerTargetSet),
          returnVariableDummies.build());
    }
    throw new UnsupportedOperationException(
        "Failed to transform call to extern C function to SvLib based on Edge " + pStatementEdge);
  }

  private SvLibProcedureCallStatement handleFunctionCall(
      CFunctionSummaryEdge pCallEdge,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    CFunctionCall functionCall = pCallEdge.getExpression();

    if (functionCall instanceof CFunctionCallAssignmentStatement assignment) {
      storePtsForFunctionCall(pCallEdge, pEdgeToPointerTargetSet);
      CIdExpression lhs = (CIdExpression) assignment.getLeftHandSide();
      SvLibProcedureDeclaration calledProcedure =
          scope.getProcedureDeclaration(
              assignment.getRightHandSide().getFunctionNameExpression().toASTString());

      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          calledProcedure,
          transformInputParameters(
              assignment.getRightHandSide().getParameterExpressions(),
              pCallEdge,
              calledProcedure,
              pEdgeToPointerTargetSet),
          ImmutableList.of(
              scope.getVariableForQualifiedName(lhs.getDeclaration().getQualifiedName())));

    } else if (functionCall instanceof CFunctionCallStatement callStatement) {
      storePtsForFunctionCall(pCallEdge, pEdgeToPointerTargetSet);

      SvLibProcedureDeclaration calledProcedure =
          scope.getProcedureDeclaration(pCallEdge.getFunctionEntry().getFunctionName());

      ImmutableList.Builder<SvLibSimpleParsingDeclaration> returnVariableDummies =
          ImmutableList.builder();
      for (SvLibParsingParameterDeclaration parsingParameterDeclaration :
          calledProcedure.getReturnValues()) {
        SvLibType returnType = parsingParameterDeclaration.getType();
        SvLibSimpleParsingDeclaration returnDummyVariable =
            scope.getVariable("transformationDummyReturn_" + returnType);
        returnVariableDummies.add(returnDummyVariable);
      }

      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          calledProcedure,
          transformInputParameters(
              callStatement.getFunctionCallExpression().getParameterExpressions(),
              pCallEdge,
              calledProcedure,
              pEdgeToPointerTargetSet),
          returnVariableDummies.build());

    } else {
      throw new UnsupportedOperationException(
          "Failed to convert CFunctionCall to SvLibProcedureCallStatement based on"
              + " CFunctionSummaryEdge "
              + pCallEdge);
    }
  }

  private ImmutableList<SvLibTerm> transformInputParameters(
      ImmutableList<CExpression> pCParameters,
      CFAEdge pCallEdge,
      SvLibProcedureDeclaration pProcedureDeclaration,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SvLibTerm> callInputParameterCollector = ImmutableList.builder();
    for (int i = 0; i < pCParameters.size(); i++) {
      CExpression inputParameter = pCParameters.get(i);
      CAssumeEdge ghostEdge =
          new CAssumeEdge(
              inputParameter.toASTString(),
              FileLocation.DUMMY,
              pCallEdge.getPredecessor(),
              pCallEdge.getSuccessor(),
              inputParameter,
              true);
      SvLibTerm term = transformToSvLibTerm(ghostEdge, pEdgeToPointerTargetSet, true);

      if (inputParameter instanceof CIdExpression
          && term instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
          && symbolApplicationTerm.getSymbol().getName().equals("not")
          && symbolApplicationTerm.getTerms().size() == 1
          && symbolApplicationTerm.getTerms().getFirst()
              instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("=")) {

        term = innerTerm.getTerms().getFirst();
      } else if (inputParameter instanceof CLiteralExpression
          && term instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
          && symbolApplicationTerm.getSymbol().getName().equals("not")
          && symbolApplicationTerm.getTerms().size() == 1
          && symbolApplicationTerm.getTerms().getFirst()
              instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("=")) {

        term = innerTerm.getTerms().getFirst();
      } else {
        SvLibType argumentType = term.getExpressionType();
        SvLibType parameterType = pProcedureDeclaration.getParameters().get(i).getType();
        if (!argumentType.equals(parameterType)) {
          if (argumentType.equals(SvLibSmtLibPredefinedType.BOOL)
              && parameterType.equals(SvLibSmtLibPredefinedType.INT)) {

            SvLibSymbolApplicationTerm ghostTerm =
                createIntegerTermsViaGhostEdge(
                    pCallEdge, inputParameter.getExpressionType(), pEdgeToPointerTargetSet);
            SvLibTerm oneTerm = ghostTerm.getTerms().getFirst();
            SvLibTerm zeroTerm = ghostTerm.getTerms().get(1);

            term =
                new SvLibSymbolApplicationTerm(
                    new SvLibIdTerm(
                        SmtLibTheoryDeclarations.ite(parameterType), FileLocation.DUMMY),
                    ImmutableList.of(term, oneTerm, zeroTerm),
                    FileLocation.DUMMY);
          } else {
            throw new IllegalArgumentException(
                "Cannot convert mismatched types! Type of argument "
                    + argumentType
                    + " does not match type "
                    + parameterType
                    + " expected by the declaration of the procedure "
                    + pProcedureDeclaration.getProcedureName());
          }
        }
      }
      callInputParameterCollector.add(term);
    }
    return callInputParameterCollector.build();
  }

  private SvLibSymbolApplicationTerm createIntegerTermsViaGhostEdge(
      CFAEdge pEdge,
      CType pCType,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    CAssumeEdge ghostEdge =
        new CAssumeEdge(
            "1",
            FileLocation.DUMMY,
            pEdge.getPredecessor(),
            pEdge.getSuccessor(),
            new CIntegerLiteralExpression(FileLocation.DUMMY, pCType, BigInteger.ONE),
            false);
    SvLibTerm ghostTerm = transformToSvLibTerm(ghostEdge, pEdgeToPointerTargetSet, true);
    if (ghostTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
        && symbolApplicationTerm.getTerms().size() == 2) {
      return symbolApplicationTerm;
    }
    throw new UnsupportedOperationException(
        "Failed to generate integer constant terms via ghost edge.");
  }

  private SvLibStatement handleAssignment(CFAEdge pEdge, SvLibTerm pTransformedTerm) {
    // For some edges without assignment, such as a declarationEdge for int x;, the
    // pTransformedTerm is a SvLibBooleanConstantTerm with the value true, and no
    // SvLibAssignmentStatement should be returned.
    if (pTransformedTerm instanceof SvLibBooleanConstantTerm booleanConstant
        && booleanConstant.getValue()) {
      return new SvLibSequenceStatement(
          ImmutableList.of(), FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());

    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
        && symbolApplicationTerm.getSymbol().getName().equals("=")
        && symbolApplicationTerm.getTerms().size() == 2) {

      ImmutableList<SvLibTerm> termsList = ImmutableList.copyOf(symbolApplicationTerm.getTerms());
      SvLibTerm assignedTo = termsList.getFirst();
      SvLibTerm termToAssign = termsList.get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {
        return createAssignmentStatement(
            idTerm, termToAssign, pEdge.getPredecessor().getFunctionName());
      }
    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm outerTerm
        && outerTerm.getSymbol().getName().equals("and")
        && outerTerm.getTerms().size() == 2
        && outerTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
        && innerTerm.getSymbol().getName().equals("=")
        && innerTerm.getTerms().size() == 2) {

      SvLibTerm assignedTo = innerTerm.getTerms().getFirst();
      SvLibTerm assignedTerm = innerTerm.getTerms().get(1);
      SvLibTerm assumeTerm = outerTerm.getTerms().get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {

        SvLibAssignmentStatement assignmentStatement =
            createAssignmentStatement(
                idTerm, assignedTerm, pEdge.getPredecessor().getFunctionName());
        SvLibAssumeStatement assumeStatement =
            new SvLibAssumeStatement(
                FileLocation.DUMMY, assumeTerm, ImmutableList.of(), ImmutableList.of());

        return new SvLibSequenceStatement(
            ImmutableList.of(assignmentStatement, assumeStatement),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of());
      }
    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm outerTerm
        && outerTerm.getSymbol().getName().equals("and")
        && outerTerm.getTerms().size() == 2
        && outerTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
        && innerTerm.getSymbol().getName().equals("and")
        && innerTerm.getTerms().size() == 2) {

      ImmutableList<SvLibStatement> statements =
          handleAssignmentForNestedTerm(
              outerTerm, innerTerm, pEdge.getPredecessor().getFunctionName());
      return new SvLibSequenceStatement(
          statements, FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());

    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm term
        && term.getSymbol().getName().equals("and")
        && term.getTerms().size() == 3) {
      return new SvLibAssumeStatement(
          FileLocation.DUMMY, term, ImmutableList.of(), ImmutableList.of());
    }
    throw new UnsupportedOperationException(
        "Failed to handle assignment for edge "
            + pEdge
            + " and transformed term "
            + pTransformedTerm.toASTString());
  }

  private ImmutableList<SvLibStatement> handleAssignmentForNestedTerm(
      SvLibSymbolApplicationTerm pOuterTerm,
      SvLibSymbolApplicationTerm pInnerTerm,
      String pFunctionName) {
    // extract all equality terms from the innerSymbolApplicationTerm
    ImmutableList.Builder<SvLibSymbolApplicationTerm> assignmentTermsCollector =
        ImmutableList.builder();
    pInnerTerm.accept(new CToSvLibTransformationTermVisitor(assignmentTermsCollector));

    // create assignment statements for each collected term
    ImmutableList<SvLibSymbolApplicationTerm> assignmentTerms = assignmentTermsCollector.build();
    ImmutableList.Builder<SvLibStatement> statementsCollector = ImmutableList.builder();
    for (SvLibSymbolApplicationTerm symbolApplicationTerm : assignmentTerms) {
      if (symbolApplicationTerm.getTerms().getFirst() instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {
        statementsCollector.add(
            createAssignmentStatement(
                idTerm, symbolApplicationTerm.getTerms().get(1), pFunctionName));
      }
    }
    // create an assumeStatement for the conditions in the outerTerm
    statementsCollector.add(
        new SvLibAssumeStatement(
            FileLocation.DUMMY,
            pOuterTerm.getTerms().get(1),
            ImmutableList.of(),
            ImmutableList.of()));

    return statementsCollector.build();
  }

  private SvLibAssignmentStatement createAssignmentStatement(
      SvLibIdTerm pIdTerm, SvLibTerm pAssignedTerm, String pFunctionName) {
    SvLibSimpleParsingDeclaration assignedToAsDeclaration =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY,
            pIdTerm.getDeclaration().getType(),
            pIdTerm.getDeclaration().getName(),
            pFunctionName);

    return new SvLibAssignmentStatement(
        ImmutableMap.of(assignedToAsDeclaration, pAssignedTerm),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  private SvLibAssumeStatement encodeTerminationNode(CFATerminationNode pTerminationNode) {
    return new SvLibAssumeStatement(
        FileLocation.DUMMY,
        new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
        ImmutableList.of(),
        ImmutableList.of(
            new SvLibTagReference(
                "CFATerminationNode_N" + pTerminationNode.getNodeNumber(), FileLocation.DUMMY)));
  }

  private SvLibGotoStatement createGotoStatement(CFANode pGotoTarget) {
    return new SvLibGotoStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(), pGotoTarget.toString());
  }

  private SvLibSequenceStatement createSequenceStatement(
      ImmutableListMultimap<CFANode, SvLibStatement> pSequenceBody, String pProcedureName) {
    ImmutableList.Builder<SvLibStatement> statementList = ImmutableList.builder();
    for (CFANode key : pSequenceBody.keySet()) {
      pSequenceBody.get(key).forEach(statementList::add);
    }
    return new SvLibSequenceStatement(
        statementList.build(),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of(new SvLibTagReference(pProcedureName, FileLocation.DUMMY)));
  }

  private String getOriginalNameOfInputParameterDummy(String pDummyName) {
    if (pDummyName.startsWith(INPUT_DUMMY_VAR_PREFIX)) {
      // return the name without the prefix
      return pDummyName.substring(INPUT_DUMMY_VAR_PREFIX.length());
    }
    throw new IllegalArgumentException(
        "Cannot remove prefix " + INPUT_DUMMY_VAR_PREFIX + " from name " + pDummyName);
  }
}
