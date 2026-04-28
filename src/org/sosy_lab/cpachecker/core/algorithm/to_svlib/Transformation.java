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
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;

public class Transformation {
  // private final CFA cfa;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final SvLibCurrentScope scope;
  private final String INPUT_DUMMY_VAR_PREFIX;

  public Transformation(
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      FormulaToSvLibVisitor pFormulaToSvLibVisitor,
      SvLibCurrentScope pCurrentScope,
      String pINPUT_DUMMY_VAR_PREFIX) {
    // cfa = pCFA;
    formulaManager = pFormulaManager;
    pathFormulaManager = pPathFormulaManager;
    formulaToSvLibVisitor = pFormulaToSvLibVisitor;
    scope = pCurrentScope;
    INPUT_DUMMY_VAR_PREFIX = pINPUT_DUMMY_VAR_PREFIX;
  }

  SvLibStatement transformFunction(CFunctionEntryNode pEntryNode)
      throws CPATransferException, InterruptedException {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pEntryNode.getFunctionName());
    String procedureName = procedureDeclaration.getProcedureName();
    ImmutableListMultimap.Builder<CFANode, SvLibStatement> statementCollector =
        ImmutableListMultimap.builder();
    Set<CFANode> labelsCreated = new HashSet<>();

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
      // TODO: Refactor into own function or class
      CStatementEdge statementEdge =
          new CStatementEdge(
              "return = 0;",
              new CExpressionAssignmentStatement(
                  FileLocation.DUMMY,
                  new CIdExpression(
                      FileLocation.DUMMY, pEntryNode.getReturnVariable().orElseThrow()),
                  new CIntegerLiteralExpression(
                      FileLocation.DUMMY,
                      pEntryNode.getReturnVariable().orElseThrow().getType(),
                      BigInteger.ZERO)),
              FileLocation.DUMMY,
              pEntryNode,
              pEntryNode);
      SvLibTerm assignmentTerm = transformToSvLibTerm(statementEdge);
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

      statementCollector.put(
          pEntryNode,
          new SvLibAssignmentStatement(
              ImmutableMap.of(procedureDeclaration.getReturnValues().getFirst(), returnValue),
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of()));
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
    addLabelStatement(pEntryNode, statementCollector, labelsCreated);

    // transform each edge to SV-LIB statement(s)
    for (CFAEdge currentEdge : relevantEdges) {
      handleEdge(currentEdge, statementCollector);
      addLabelStatement(currentEdge.getSuccessor(), statementCollector, labelsCreated);
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

  private void handleEdge(
      CFAEdge pEdge, ImmutableListMultimap.Builder<CFANode, SvLibStatement> pCreatedStatements)
      throws CPATransferException, InterruptedException {
    switch (pEdge.getEdgeType()) {
      case BlankEdge ->
          pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      case AssumeEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);
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
          SvLibStatement externCallStatement = handleExternFunctionCall(statementEdge);
          pCreatedStatements.put(pEdge.getPredecessor(), externCallStatement);
        } else {
          SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);
          Optional<SvLibStatement> assignmentStatement = handleAssignment(pEdge, transformedTerm);
          if (assignmentStatement.isPresent()) {
            pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement.orElseThrow());
          }
        }
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case DeclarationEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);
        Optional<SvLibStatement> assignmentStatement = handleAssignment(pEdge, transformedTerm);
        if (assignmentStatement.isPresent()) {
          pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement.orElseThrow());
        }
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case ReturnStatementEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);
        Optional<SvLibStatement> assignmentStatement = handleAssignment(pEdge, transformedTerm);
        if (assignmentStatement.isPresent()) {
          pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement.orElseThrow());
        }
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
        SvLibProcedureCallStatement callStatement = handleFunctionCall(callEdge);
        pCreatedStatements.put(pEdge.getPredecessor(), callStatement);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
    }
  }

  private @NonNull SvLibTerm transformToSvLibTerm(CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    PathFormula edgeFormula = pathFormulaManager.makeEmptyPathFormula();
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge);

    return formulaManager.visit(edgeFormula.getFormula(), formulaToSvLibVisitor);
  }

  private SvLibStatement handleExternFunctionCall(CStatementEdge pStatementEdge)
      throws CPATransferException, InterruptedException {
    // handle calls to other extern functions,
    //  i.e. every function call which does not have corresponding a functionEntryNode
    if (pStatementEdge.getStatement()
        instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {
      CIdExpression leftHandSide =
          (CIdExpression) functionCallAssignmentStatement.getLeftHandSide();
      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          scope.getProcedureDeclaration(
              functionCallAssignmentStatement
                  .getRightHandSide()
                  .getFunctionNameExpression()
                  .toASTString()),
          transformInputParameters(
              functionCallAssignmentStatement.getRightHandSide().getParameterExpressions(),
              pStatementEdge),
          ImmutableList.of(
              scope.getVariableForQualifiedName(leftHandSide.getDeclaration().getQualifiedName())));
    } else if (pStatementEdge.getStatement()
        instanceof CFunctionCallStatement functionCallStatement) {
      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          scope.getProcedureDeclaration(
              functionCallStatement
                  .getFunctionCallExpression()
                  .getFunctionNameExpression()
                  .toASTString()),
          transformInputParameters(
              functionCallStatement.getFunctionCallExpression().getParameterExpressions(),
              pStatementEdge),
          ImmutableList.of());
    }
    throw new UnsupportedOperationException(
        "Failed to transform call to extern C function to SvLib based on Edge " + pStatementEdge);
  }

  private SvLibProcedureCallStatement handleFunctionCall(CFunctionSummaryEdge pCallEdge)
      throws CPATransferException, InterruptedException {
    CFunctionCall functionCall = pCallEdge.getExpression();

    if (functionCall instanceof CFunctionCallAssignmentStatement assignment) {
      CIdExpression lhs = (CIdExpression) assignment.getLeftHandSide();
      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          scope.getProcedureDeclaration(
              assignment.getRightHandSide().getFunctionNameExpression().toASTString()),
          transformInputParameters(
              assignment.getRightHandSide().getParameterExpressions(), pCallEdge),
          ImmutableList.of(
              scope.getVariableForQualifiedName(lhs.getDeclaration().getQualifiedName())));

    } else if (functionCall instanceof CFunctionCallStatement callStatement) {
      return new SvLibProcedureCallStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(),
          scope.getProcedureDeclaration(pCallEdge.getFunctionEntry().getFunctionName()),
          transformInputParameters(
              callStatement.getFunctionCallExpression().getParameterExpressions(), pCallEdge),
          ImmutableList.of());

    } else {
      throw new UnsupportedOperationException(
          "Failed to convert CFunctionCall to SvLibProcedureCallStatement based on"
              + " CFunctionSummaryEdge "
              + pCallEdge);
    }
  }

  private ImmutableList<SvLibTerm> transformInputParameters(
      ImmutableList<CExpression> pCParameters, CFAEdge pCallEdge)
      throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SvLibTerm> callInputParameterCollector = ImmutableList.builder();
    for (CExpression inputParameter : pCParameters) {
      CAssumeEdge ghostEdge =
          new CAssumeEdge(
              inputParameter.toASTString(),
              FileLocation.DUMMY,
              pCallEdge.getPredecessor(),
              pCallEdge.getSuccessor(),
              inputParameter,
              false);
      SvLibTerm transformedDummy = transformToSvLibTerm(ghostEdge);
      SvLibSymbolApplicationTerm outerTerm = (SvLibSymbolApplicationTerm) transformedDummy;
      SvLibTerm innerTerm = outerTerm.getTerms().getFirst();

      callInputParameterCollector.add(innerTerm);
    }
    return callInputParameterCollector.build();
  }

  private Optional<SvLibStatement> handleAssignment(CFAEdge pEdge, SvLibTerm pTransformedTerm) {
    // For some edges without assignment, such as a declarationEdge for int x;, the
    // pTransformedTerm is a SvLibBooleanConstantTerm with the value true, and no
    // SvLibAssignmentStatement should be returned.
    if (pTransformedTerm instanceof SvLibBooleanConstantTerm booleanConstant
        && booleanConstant.getValue()) {
      return Optional.empty();
    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
        && symbolApplicationTerm.getSymbol().getName().equals("=")
        && symbolApplicationTerm.getTerms().size() == 2) {

      ImmutableList<SvLibTerm> termsList = ImmutableList.copyOf(symbolApplicationTerm.getTerms());
      SvLibTerm assignedTo = termsList.getFirst();
      SvLibTerm termToAssign = termsList.get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {

        SvLibAssignmentStatement assignmentStatement =
            transformTermToAssignmentStatement(
                idTerm, termToAssign, pEdge.getPredecessor().getFunctionName());

        return Optional.of(assignmentStatement);
      }
    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm outerSymbolApplicationTerm
        && outerSymbolApplicationTerm.getSymbol().getName().equals("and")
        && outerSymbolApplicationTerm.getTerms().size() == 2
        && outerSymbolApplicationTerm.getTerms().getFirst()
            instanceof SvLibSymbolApplicationTerm innerSymbolApplicationTerm
        && innerSymbolApplicationTerm.getSymbol().getName().equals("=")
        && innerSymbolApplicationTerm.getTerms().size() == 2) {

      SvLibTerm assignedTo = innerSymbolApplicationTerm.getTerms().getFirst();
      SvLibTerm assignedTerm = innerSymbolApplicationTerm.getTerms().get(1);
      SvLibTerm assumeTerm = outerSymbolApplicationTerm.getTerms().get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {

        SvLibAssignmentStatement assignmentStatement =
            transformTermToAssignmentStatement(
                idTerm, assignedTerm, pEdge.getPredecessor().getFunctionName());
        SvLibAssumeStatement assumeStatement =
            new SvLibAssumeStatement(
                FileLocation.DUMMY, assumeTerm, ImmutableList.of(), ImmutableList.of());

        return Optional.of(
            new SvLibSequenceStatement(
                ImmutableList.of(assignmentStatement, assumeStatement),
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of()));
      }
    }
    throw new UnsupportedOperationException(
        "Failed to handle assignment for edge "
            + pEdge
            + " and transformed term "
            + pTransformedTerm.toASTString());
  }

  private SvLibAssignmentStatement transformTermToAssignmentStatement(
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

  private SvLibGotoStatement createGotoStatement(CFANode pGotoTarget) {
    return new SvLibGotoStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(), pGotoTarget.toString());
  }

  private void addLabelStatement(
      CFANode pNode,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pStatementsCollector,
      Set<CFANode> pLabelCreated) {
    if (!pLabelCreated.contains(pNode)) {
      String labelNodeNumber = pNode.toString();
      pStatementsCollector.put(pNode, createLabelStatement(labelNodeNumber));
      pLabelCreated.add(pNode);
      // transformationStatistics.numberOfLabelsCreated++;

      if (pNode instanceof CFALabelNode labelNode) {
        String originalLabel = labelNode.getLabel() + "__" + labelNodeNumber;
        pStatementsCollector.put(pNode, createLabelStatement(originalLabel));
        // transformationStatistics.numberOfLabelsCreated++;
      }
    }
  }

  private SvLibLabelStatement createLabelStatement(String pLabelName) {
    SvLibTagReference tagReference = new SvLibTagReference(pLabelName, FileLocation.DUMMY);
    return new SvLibLabelStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(tagReference), pLabelName);
  }

  private SvLibSequenceStatement createSequenceStatement(
      ListMultimap<CFANode, SvLibStatement> pSequenceBody, String pProcedureName) {
    ImmutableList.Builder<SvLibStatement> statementList = ImmutableList.builder();
    for (CFANode key : pSequenceBody.keySet()) {
      pSequenceBody.get(key).stream().filter(Objects::nonNull).forEach(statementList::add);
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
    // FIXME This case should never occur, so throw instead?
    return pDummyName;
  }
}
