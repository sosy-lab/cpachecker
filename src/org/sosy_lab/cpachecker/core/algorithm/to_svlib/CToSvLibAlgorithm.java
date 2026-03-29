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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProceduresRecDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class CToSvLibAlgorithm implements Algorithm, StatisticsProvider, AutoCloseable {

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final SvLibCurrentScope scope;
  private final FormulaToSvLibTransformationVisitor formulaToSvLibVisitor;

  /**
   * Transforms the CFA of a C program to a SvLibScript. At the moment in development and works
   * currently only for a limited subset of the C language.
   */
  public CToSvLibAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;

    config = pConfiguration;

    // TODO Ideally use try-with-ressources to close solver when no longer needed!
    solver = Solver.create(config, logger, shutdownNotifier);
    formulaManager = solver.getFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            config,
            logger,
            shutdownNotifier,
            cfa,
            AnalysisDirection.FORWARD);

    scope = new SvLibCurrentScope();
    formulaToSvLibVisitor =
        new FormulaToSvLibTransformationVisitor(solver.getFormulaManager(), scope);
  }

  /**
   * Transforms the input {@link CFA} of a C program to a {@link SvLibScript}.
   *
   * @return The SvLibScript generated from the CFA
   * @throws InvalidConfigurationException If the program to be transformed is not a C program
   */
  public SvLibScript transformCfaToSvLib() throws InvalidConfigurationException {
    // TODO check here or already in constructor?
    if (cfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "Currently only C programs can be transformed to SV-LIB");
    }

    // TODO check config whether cpa.predicate.ignoreIrrelevantVariables is set to false, otherwise
    //  the transformation won't be valid

    ImmutableList.Builder<SvLibCommand> commandsCollector = ImmutableList.builder();
    commandsCollector.add(
        new SvLibSetLogicCommand(SmtLibLogic.QF_NIA, FileLocation.DUMMY),
        new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY));

    // 1. Step: Initialize CurrentScope with declarations of procedures and global variables,
    // global variables are added to scope +  declaration commands are added to commandsCollector
    initializeScope(commandsCollector);

    ImmutableList.Builder<SvLibProcedureDeclaration> procedureDeclarationCollector =
        ImmutableList.builder();
    ImmutableList.Builder<SvLibStatement> procedureBodiesCollector = ImmutableList.builder();

    // 2. Step: transform each function to a procedure body
    for (FunctionEntryNode functionEntryNode : cfa.entryNodes()) {
      SvLibStatement procedureBody = transformFunction((CFunctionEntryNode) functionEntryNode);

      procedureDeclarationCollector.add(
          scope.getProcedureDeclaration(functionEntryNode.getFunctionName()));
      procedureBodiesCollector.add(procedureBody);
    }

    SvLibProceduresRecDefinitionCommand proceduresRecDefinitionCommand =
        new SvLibProceduresRecDefinitionCommand(
            FileLocation.DUMMY,
            procedureDeclarationCollector.build(),
            procedureBodiesCollector.build());
    commandsCollector.add(proceduresRecDefinitionCommand);

    // TODO should work for most cases
    commandsCollector.add(
        new SvLibVerifyCallCommand(
            scope.getProcedureDeclaration(cfa.getMainFunction().getFunctionName()),
            ImmutableList.of(),
            FileLocation.DUMMY));

    return new SvLibScript(commandsCollector.build(), FileLocation.DUMMY);
  }

  private SvLibStatement transformFunction(CFunctionEntryNode pEntryNode) {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pEntryNode.getFunctionName());
    String procedureName = procedureDeclaration.getProcedureName();
    ListMultimap<CFANode, SvLibStatement> createdStatements = LinkedListMultimap.create();

    scope.enterProcedure(
        FluentIterable.from(procedureDeclaration.getParameters())
            .append(procedureDeclaration.getLocalVariables())
            .append(procedureDeclaration.getReturnValues())
            .toList());

    if (pEntryNode.getFunctionName().contains("main")
        && !procedureDeclaration.getReturnValues().isEmpty()) {
      // in the procedure created for the main-function: initialize _retval_ with 0
      createdStatements.put(
          pEntryNode,
          new SvLibAssignmentStatement(
              ImmutableMap.of(
                  procedureDeclaration.getReturnValues().getFirst(),
                  new SvLibIntegerConstantTerm(BigInteger.ZERO, FileLocation.DUMMY)),
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of()));
    }

    if (pEntryNode.getFunctionName().contains("reach_error")) {
      // FIXME not correct, error should depend on the property checked
      SvLibSequenceStatement errorStatement =
          new SvLibSequenceStatement(
              ImmutableList.of(),
              FileLocation.DUMMY,
              ImmutableList.of(
                  new SvLibCheckTrueTag(
                      new SvLibBooleanConstantTerm(false, FileLocation.DUMMY), FileLocation.DUMMY)),
              ImmutableList.of());
      createdStatements.put(pEntryNode, errorStatement);
    }

    if (!procedureDeclaration.getParameters().isEmpty()) {
      // assign the input parameters to assignable dummy variables
      for (SvLibParsingParameterDeclaration inputParameter : procedureDeclaration.getParameters()) {
        SvLibSimpleParsingDeclaration assignableInputDummyVariable =
            scope.getVariable(inputParameter.getName().replaceAll("(_0)$", ""));

        SvLibAssignmentStatement assignInput =
            new SvLibAssignmentStatement(
                ImmutableMap.of(
                    assignableInputDummyVariable,
                    new SvLibIdTerm(inputParameter.toSimpleDeclaration(), FileLocation.DUMMY)),
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of());
        createdStatements.put(pEntryNode, assignInput);
      }
    }

    ImmutableList<CFAEdge> relevantEdges = getAllRelevantEdges(pEntryNode);
    addLabelStatement(pEntryNode, createdStatements);

    // transform each edge to SV-LIB statement(s)
    for (CFAEdge currentEdge : relevantEdges) {
      handleEdge(currentEdge, createdStatements);
      addLabelStatement(currentEdge.getSuccessor(), createdStatements);
    }

    SvLibStatement procedureBodySequence =
        createSequenceStatement(createdStatements, procedureName);

    scope.leaveProcedure();
    return procedureBodySequence;
  }

  private void initializeScope(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
      CFunctionEntryNode cEntryNode = (CFunctionEntryNode) entryNode;
      String procedureName = entryNode.getFunctionName();

      ImmutableList<SvLibParsingParameterDeclaration> inputParameters =
          collectInputParameters(cEntryNode.getFunctionParameters(), procedureName);
      ImmutableList<SvLibParsingParameterDeclaration> returnParameter =
          collectReturnParameter(cEntryNode.getReturnVariable(), procedureName);
      ImmutableList.Builder<SvLibParsingParameterDeclaration> localParametersCollector =
          ImmutableList.builder();

      // add dummy variable for inputParameters
      for (SvLibParsingParameterDeclaration inputParameter : inputParameters) {
        SvLibParsingParameterDeclaration dummyForInput =
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                inputParameter.getType(),
                inputParameter.getName().replaceAll("(_0)$", ""),
                inputParameter.getProcedureName());
        localParametersCollector.add(dummyForInput);
      }

      ImmutableList.Builder<CDeclaration> declarationsCollector = ImmutableList.builder();
      for (CFAEdge edge : getAllRelevantEdges(entryNode)) {
        if (edge instanceof CDeclarationEdge declarationEdge) {
          declarationsCollector.add(declarationEdge.getDeclaration());
        }
      }
      ImmutableList<CDeclaration> declarations = declarationsCollector.build();

      // collect declarations of local parameters and global variables
      for (CDeclaration declaration : declarations) {
        if (declaration instanceof CVariableDeclaration variableDeclaration) {
          // TODO handle other types and improve default case?
          SvLibType type =
              switch (variableDeclaration.getType()) {
                // TODO also true for bool and char, since these are integer types
                case CSimpleType simpleType when simpleType.getType().isIntegerType() ->
                    SvLibSmtLibPredefinedType.INT;
                case CSimpleType simpleType when simpleType.getType().isFloatingPointType() ->
                    SvLibSmtLibPredefinedType.REAL;
                case null, default -> new SvLibAnyType();
              };

          if (variableDeclaration.isGlobal()) {
            SvLibParsingVariableDeclaration globalVariable =
                new SvLibParsingVariableDeclaration(
                    FileLocation.DUMMY,
                    variableDeclaration.isGlobal(),
                    variableDeclaration.getType().isConst(),
                    type,
                    variableDeclaration.getName(),
                    variableDeclaration.getOrigName(),
                    null);
            scope.addVariable(globalVariable);
            pCommandsCollector.add(
                new SvLibVariableDeclarationCommand(globalVariable, FileLocation.DUMMY));
          } else {
            SvLibParsingParameterDeclaration parameter =
                new SvLibParsingParameterDeclaration(
                    FileLocation.DUMMY, type, declaration.getName(), procedureName);
            localParametersCollector.add(parameter);
          }
        }
      }
      SvLibProcedureDeclaration procedureDeclaration =
          new SvLibProcedureDeclaration(
              FileLocation.DUMMY,
              procedureName,
              inputParameters,
              returnParameter,
              localParametersCollector.build());
      scope.addProcedureDeclaration(procedureDeclaration);
    }
  }

  private void handleEdge(CFAEdge pEdge, ListMultimap<CFANode, SvLibStatement> pCreatedStatements) {
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
          handleExternalFunctionCall(statementEdge, pCreatedStatements);
        } else {
          SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);
          handleAssignment(pEdge, transformedTerm, pCreatedStatements);
        }
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case DeclarationEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);

        // currently ignores function declarations, e.g. {int main();} and type declarations
        handleAssignment(pEdge, transformedTerm, pCreatedStatements);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case ReturnStatementEdge -> {
        SvLibTerm transformedTerm = transformToSvLibTerm(pEdge);
        handleAssignment(pEdge, transformedTerm, pCreatedStatements);
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
        handleFunctionCall(callEdge, pCreatedStatements);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
    }
  }

  private @NonNull SvLibTerm transformToSvLibTerm(CFAEdge pEdge) {
    PathFormula edgeFormula = pathFormulaManager.makeEmptyPathFormula();
    try {
      edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge);
    } catch (CPATransferException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    return formulaManager.visit(edgeFormula.getFormula(), formulaToSvLibVisitor);
  }

  private void handleExternalFunctionCall(
      CStatementEdge pStatementEdge, ListMultimap<CFANode, SvLibStatement> pCreatedStatements) {
    // TODO handle calls to other external functions
    //  i.e. every function call which does not have corresponding a functionEntryNode
    if (pStatementEdge.getStatement()
        instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {

      /*      if (functionCallAssignmentStatement
          .getRightHandSide()
          .getFunctionNameExpression()
          .toASTString()
          .contains("VERIFIER_nondet_memory")) {
        // TODO implement this when a  memory model is ready
      } else*/
      if (functionCallAssignmentStatement
              .getRightHandSide()
              .getFunctionNameExpression()
              .toASTString()
              .contains("VERIFIER_nondet")
          && functionCallAssignmentStatement.getLeftHandSide()
              instanceof CIdExpression lhsIdExpression) {
        // TODO also consider the other possibilities  for lhs (CArraySubscriptExpression,
        //        CComplexCastExpression, CFieldReference, CPointerExpression)
        SvLibSimpleParsingDeclaration assignedTo =
            scope.getVariableForQualifiedName(lhsIdExpression.getDeclaration().getQualifiedName());
        SvLibHavocStatement havocStatement =
            new SvLibHavocStatement(
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableList.of(assignedTo));
        pCreatedStatements.put(pStatementEdge.getPredecessor(), havocStatement);
      }
    } else if (pStatementEdge.getStatement()
        instanceof CFunctionCallStatement functionCallStatement) {
      if (functionCallStatement
          .getFunctionCallExpression()
          .getFunctionNameExpression()
          .toASTString()
          .contains("abort")) {
        SvLibAssumeStatement assumeFalseStatement =
            new SvLibAssumeStatement(
                FileLocation.DUMMY,
                new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
                ImmutableList.of(),
                ImmutableList.of(new SvLibTagReference("abort", FileLocation.DUMMY)));
        pCreatedStatements.put(pStatementEdge.getPredecessor(), assumeFalseStatement);
      }
    }
  }

  private void handleFunctionCall(
      CFunctionSummaryEdge pCallEdge, ListMultimap<CFANode, SvLibStatement> pCreatedStatements) {
    CFunctionCall functionCall = pCallEdge.getExpression();

    if (functionCall instanceof CFunctionCallAssignmentStatement assignment) {
      CIdExpression lhs = (CIdExpression) assignment.getLeftHandSide();
      SvLibProcedureCallStatement procedureCall =
          new SvLibProcedureCallStatement(
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of(),
              scope.getProcedureDeclaration(
                  assignment.getRightHandSide().getFunctionNameExpression().toASTString()),
              transformInputParameters(
                  assignment.getRightHandSide().getParameterExpressions(), pCallEdge),
              ImmutableList.of(
                  scope.getVariableForQualifiedName(lhs.getDeclaration().getQualifiedName())));
      pCreatedStatements.put(pCallEdge.getPredecessor(), procedureCall);

    } else if (functionCall instanceof CFunctionCallStatement callStatement) {
      SvLibProcedureCallStatement procedureCall =
          new SvLibProcedureCallStatement(
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of(),
              scope.getProcedureDeclaration(pCallEdge.getFunctionEntry().getFunctionName()),
              transformInputParameters(
                  callStatement.getFunctionCallExpression().getParameterExpressions(), pCallEdge),
              ImmutableList.of());

      pCreatedStatements.put(pCallEdge.getPredecessor(), procedureCall);
    }
  }

  private ImmutableList<SvLibTerm> transformInputParameters(
      ImmutableList<CExpression> pCParameters, CFunctionSummaryEdge pCallEdge) {
    ImmutableList.Builder<SvLibTerm> callInputParameterCollector = ImmutableList.builder();
    for (CExpression inputParameter : pCParameters) {
      // TODO too restrictive! adapt type of term
      if (inputParameter instanceof CIntegerLiteralExpression integerLiteral) {
        SvLibIntegerConstantTerm integerConstantTerm =
            new SvLibIntegerConstantTerm(integerLiteral.getValue(), FileLocation.DUMMY);
        callInputParameterCollector.add(integerConstantTerm);
      } else if (inputParameter instanceof CIdExpression idExpression) {
        SvLibSimpleParsingDeclaration inputVariable =
            scope.getVariableForQualifiedName(idExpression.getDeclaration().getQualifiedName());
        callInputParameterCollector.add(
            new SvLibIdTerm(inputVariable.toSimpleDeclaration(), FileLocation.DUMMY));
      } else {
        // else if (inputParameter instanceof CBinaryExpression binaryExpression)
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
    }
    return callInputParameterCollector.build();
  }

  private void handleAssignment(
      CFAEdge pEdge,
      SvLibTerm pTransformedTerm,
      ListMultimap<CFANode, SvLibStatement> pCreatedStatements) {

    if (pTransformedTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
        && symbolApplicationTerm.getSymbol().getName().equals("=")
        && symbolApplicationTerm.getTerms().size() == 2) {

      ImmutableList<SvLibTerm> termsList = ImmutableList.copyOf(symbolApplicationTerm.getTerms());
      SvLibTerm assignedTo = termsList.getFirst();
      SvLibTerm value = termsList.get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {
        SvLibSimpleParsingDeclaration assignedToAsDeclaration =
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                idTerm.getDeclaration().getType(),
                idTerm.getDeclaration().getName(),
                pEdge.getPredecessor().getFunctionName());
        SvLibAssignmentStatement assignmentStatement =
            new SvLibAssignmentStatement(
                ImmutableMap.of(assignedToAsDeclaration, value),
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of());
        pCreatedStatements.put(pEdge.getPredecessor(), assignmentStatement);
      }
    }
  }

  private ImmutableList<SvLibParsingParameterDeclaration> collectReturnParameter(
      Optional<CVariableDeclaration> pReturnVariable, String pProcedureName) {
    if (pReturnVariable.isEmpty()) {
      return ImmutableList.of();
    }
    if (pReturnVariable.orElseThrow().getType() instanceof CSimpleType asSimpleType) {
      return ImmutableList.of(
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              transformToSvLibType(asSimpleType),
              pReturnVariable.orElseThrow().getName(),
              pProcedureName));
    }
    return ImmutableList.of();
  }

  private ImmutableList<SvLibParsingParameterDeclaration> collectInputParameters(
      ImmutableList<CParameterDeclaration> pParameterDeclarations, String pProcedureName) {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> parameterCollector =
        ImmutableList.builder();

    for (CParameterDeclaration parameter : pParameterDeclarations) {
      if (parameter.asVariableDeclaration().getType() instanceof CSimpleType asSimpleType) {
        parameterCollector.add(
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                transformToSvLibType(asSimpleType),
                parameter.getName() + "_0",
                pProcedureName));
      }
    }
    return parameterCollector.build();
  }

  private SvLibType transformToSvLibType(CSimpleType pCSimpleType) {
    if (pCSimpleType.getType().isIntegerType()) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (pCSimpleType.getType().isFloatingPointType()) {
      return SvLibSmtLibPredefinedType.REAL;
    } else {
      // TODO improve default case or throw?
      return new SvLibAnyType();
    }
  }

  private ImmutableList<CFAEdge> getAllRelevantEdges(FunctionEntryNode pEntryNode) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pEntryNode, edgeCollector);
    return ImmutableList.copyOf(edgeCollector.getVisitedEdges());
  }

  private SvLibGotoStatement createGotoStatement(CFANode pGotoTarget) {
    return new SvLibGotoStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(), pGotoTarget.toString());
  }

  private void addLabelStatement(
      CFANode pNode, ListMultimap<CFANode, SvLibStatement> pCreatedStatements) {
    if (!pCreatedStatements.containsKey(pNode)) {
      String label = pNode.toString();
      pCreatedStatements.put(pNode, createLabelStatement(label));
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

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {}

  @Override
  public void close() throws Exception {
    // TODO is this acutally what we want here, or do we just delegate the problem?
    solver.close();
  }
}
