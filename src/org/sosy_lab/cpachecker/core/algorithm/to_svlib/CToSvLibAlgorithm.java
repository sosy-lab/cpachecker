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
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CFormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

public class CToSvLibAlgorithm implements Algorithm, StatisticsProvider, AutoCloseable {

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final CtoFormulaConverter converter;

  private final SvLibCurrentScope scope;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final TransformationStatistics transformationStatistics;

  private final String INPUT_DUMMY_VAR_PREFIX = "__originalInput_@";

  /**
   * Transforms the CFA of a C program to a SvLibScript. At the moment in development and works
   * currently only for a limited subset of the C language.
   *
   * @throws InvalidConfigurationException If the program to be transformed is not a C program
   */
  public CToSvLibAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    config = pConfiguration;

    cfa = pCfa;
    if (cfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "Currently only C programs can be transformed to SV-LIB");
    }

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
    converter =
        new CtoFormulaConverter(
            new CFormulaEncodingOptions(config),
            solver.getFormulaManager(),
            cfa.getMachineModel(),
            cfa.getVarClassification(),
            logger,
            shutdownNotifier,
            new CtoFormulaTypeHandler(logger, cfa.getMachineModel()),
            AnalysisDirection.FORWARD);

    scope = new SvLibCurrentScope();
    formulaToSvLibVisitor = new FormulaToSvLibVisitor(solver.getFormulaManager(), scope);
    transformationStatistics = new TransformationStatistics();
  }

  /**
   * Transforms the input {@link CFA} of a C program to a {@link SvLibScript}.
   *
   * @return The SvLibScript generated from the CFA
   */
  public SvLibScript transformToSvLib()
      throws UnsupportedOperationException, CPATransferException, InterruptedException {
    SvLibScript outputScript;

    logger.log(Level.INFO, "Starting transformation of the input C program to SV-LIB.");
    transformationStatistics.totalTransformationTime.start();
    try {
      outputScript = transformCfaToSvLibScript();
    } finally {
      transformationStatistics.totalTransformationTime.stop();
    }
    logger.log(Level.INFO, "Finished transformation of the input C program to SV-LIB.");

    return outputScript;
  }

  private SvLibScript transformCfaToSvLibScript()
      throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SvLibCommand> commandsCollector = ImmutableList.builder();
    commandsCollector.add(
        new SvLibSetLogicCommand(SmtLibLogic.ALL, FileLocation.DUMMY),
        new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY));

    // 1. Step: Initialize CurrentScope with declarations of procedures and global variables,
    // global variables are added to scope +  declaration commands are added to commandsCollector

    transformationStatistics.initializationTime.start();
    try {
      initializeScope(commandsCollector);
    } finally {
      transformationStatistics.initializationTime.stop();
    }

    // 2. Step: transform each function to a procedure body
    transformationStatistics.transformationTime.start();
    ImmutableList.Builder<SvLibProcedureDeclaration> procedureDeclarationCollector =
        ImmutableList.builder();
    ImmutableList.Builder<SvLibStatement> procedureBodiesCollector = ImmutableList.builder();

    try {
      for (FunctionEntryNode functionEntryNode : cfa.entryNodes()) {
        SvLibStatement procedureBody = transformFunction((CFunctionEntryNode) functionEntryNode);

        procedureDeclarationCollector.add(
            scope.getProcedureDeclaration(functionEntryNode.getFunctionName()));
        procedureBodiesCollector.add(procedureBody);
      }
    } finally {
      transformationStatistics.transformationTime.stop();
    }

    SvLibProceduresRecDefinitionCommand proceduresRecDefinitionCommand =
        new SvLibProceduresRecDefinitionCommand(
            FileLocation.DUMMY,
            procedureDeclarationCollector.build(),
            procedureBodiesCollector.build());
    commandsCollector.add(proceduresRecDefinitionCommand);

    commandsCollector.add(
        new SvLibVerifyCallCommand(
            scope.getProcedureDeclaration(cfa.getMainFunction().getFunctionName()),
            ImmutableList.of(),
            FileLocation.DUMMY));

    ImmutableList<SvLibCommand> commandsCollectorBuilt = commandsCollector.build();

    transformationStatistics.numberOfCommands = commandsCollectorBuilt.size();
    return new SvLibScript(commandsCollectorBuilt, FileLocation.DUMMY);
  }

  private SvLibStatement transformFunction(CFunctionEntryNode pEntryNode)
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
      statementCollector.put(pEntryNode, errorStatement);
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

      SvLibAssignmentStatement assginDummyInput =
          new SvLibAssignmentStatement(
              inputAssignmentsCollector.buildOrThrow(),
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of());
      statementCollector.put(pEntryNode, assginDummyInput);
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

  private void initializeScope(ImmutableList.Builder<SvLibCommand> pCommandsCollector)
      throws UnsupportedOperationException {
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
                getOriginalNameOfInputParameterDummy(inputParameter.getName()),
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
          SvLibType type = convertToSvLibType(variableDeclaration.getType());

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

  private SvLibStatement handleExternFunctionCall(CStatementEdge pStatementEdge) {
    // TODO handle calls to other extern functions
    //  i.e. every function call which does not have corresponding a functionEntryNode
    if (pStatementEdge.getStatement()
        instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {

      /*if (functionCallAssignmentStatement
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
        return new SvLibHavocStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of(assignedTo));
      }
    } else if (pStatementEdge.getStatement()
        instanceof CFunctionCallStatement functionCallStatement) {
      if (functionCallStatement
          .getFunctionCallExpression()
          .getFunctionNameExpression()
          .toASTString()
          .contains("__VERIFIER_nondet")) {
        // use 'skip' for __VERIFIER_nondet without assignment
        return new SvLibAssumeStatement(
            FileLocation.DUMMY,
            new SvLibBooleanConstantTerm(true, FileLocation.DUMMY),
            ImmutableList.of(),
            ImmutableList.of());
      } else if (functionCallStatement
          .getFunctionCallExpression()
          .getFunctionNameExpression()
          .toASTString()
          .contains("abort")) {
        return new SvLibAssumeStatement(
            FileLocation.DUMMY,
            new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
            ImmutableList.of(),
            ImmutableList.of(new SvLibTagReference("abort", FileLocation.DUMMY)));
      }
    }
    // FIXME throw instead of returning SKIP
    return new SvLibAssumeStatement(
        FileLocation.DUMMY,
        new SvLibBooleanConstantTerm(true, FileLocation.DUMMY),
        ImmutableList.of(),
        ImmutableList.of());
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
      ImmutableList<CExpression> pCParameters, CFunctionSummaryEdge pCallEdge)
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

  private ImmutableList<SvLibParsingParameterDeclaration> collectReturnParameter(
      Optional<CVariableDeclaration> pReturnVariable, String pProcedureName)
      throws UnsupportedOperationException {
    if (pReturnVariable.isEmpty()) {
      return ImmutableList.of();
    }
    if (pReturnVariable.orElseThrow().getType() instanceof CSimpleType asSimpleType) {
      return ImmutableList.of(
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibType(asSimpleType),
              pReturnVariable.orElseThrow().getName(),
              pProcedureName));
    }
    return ImmutableList.of();
  }

  private ImmutableList<SvLibParsingParameterDeclaration> collectInputParameters(
      ImmutableList<CParameterDeclaration> pParameterDeclarations, String pProcedureName)
      throws UnsupportedOperationException {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> parameterCollector =
        ImmutableList.builder();

    for (CParameterDeclaration parameter : pParameterDeclarations) {
      if (parameter.asVariableDeclaration().getType() instanceof CSimpleType asSimpleType) {
        parameterCollector.add(
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                convertToSvLibType(asSimpleType),
                getNameForInputParameterDummy(parameter.getName()),
                pProcedureName));
      }
    }
    return parameterCollector.build();
  }

  private SvLibType convertToSvLibType(CType pCType) {
    FormulaType<?> formulaType = converter.getFormulaTypeFromType(pCType);
    FormulaType<Formula> encodedFormulaType = formulaManager.getEncodedFormulaType(formulaType);

    if (encodedFormulaType.isBooleanType()) {
      return SvLibSmtLibPredefinedType.BOOL;
    } else if (encodedFormulaType.isIntegerType()) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (encodedFormulaType.isStringType()) {
      return SvLibSmtLibPredefinedType.STRING;
    } else if (encodedFormulaType.isFloatingPointType() || encodedFormulaType.isRationalType()) {
      return SvLibSmtLibPredefinedType.REAL;
    } else if (encodedFormulaType.isBitvectorType()) {
      BitvectorType bitvectorType = (BitvectorType) formulaType;
      return new SvLibSmtLibBitVectorType(bitvectorType.getSize());
    }

    throw new UnsupportedOperationException(
        "Transformation to a SvLibType failed for CType " + pCType);
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
      CFANode pNode,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pStatementsCollector,
      Set<CFANode> pLabelCreated) {
    if (!pLabelCreated.contains(pNode)) {
      String labelNodeNumber = pNode.toString();
      pStatementsCollector.put(pNode, createLabelStatement(labelNodeNumber));
      pLabelCreated.add(pNode);
      transformationStatistics.numberOfLabelsCreated++;

      if (pNode instanceof CFALabelNode labelNode) {
        String originalLabel = labelNode.getLabel() + "__" + labelNodeNumber;
        pStatementsCollector.put(pNode, createLabelStatement(originalLabel));
        transformationStatistics.numberOfLabelsCreated++;
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

  private String getNameForInputParameterDummy(String pOriginalName) {
    return INPUT_DUMMY_VAR_PREFIX + pOriginalName;
  }

  private String getOriginalNameOfInputParameterDummy(String pDummyName) {
    if (pDummyName.startsWith(INPUT_DUMMY_VAR_PREFIX)) {
      // return the name without the prefix
      return pDummyName.substring(INPUT_DUMMY_VAR_PREFIX.length());
    }
    // FIXME This case should never occur, so throw instead?
    return pDummyName;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {}

  @Override
  public void close() {
    solver.close();
  }

  private static class TransformationStatistics implements Statistics {

    private final StatTimer totalTransformationTime = new StatTimer("Total Transformation Time");
    private final StatTimer initializationTime = new StatTimer("Time to initialize scope");
    private final StatTimer transformationTime = new StatTimer("Transformation Time");
    private int numberOfCommands;
    private int numberOfLabelsCreated;

    private TransformationStatistics() {
      numberOfCommands = 0;
      numberOfLabelsCreated = 0;
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Total time for transformation:     " + totalTransformationTime);
      out.println("Time to initialize scope:          " + initializationTime);
      out.println("Time for transformation:           " + transformationTime);
      out.println("Number of commands:                " + numberOfCommands);
      out.println("Number of labels created:          " + numberOfLabelsCreated);
    }

    @Override
    public String getName() {
      return "C to SV-LIB Transformation Statistics";
    }
  }
}
