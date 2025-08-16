// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.k3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.VerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3ProcedureEntryNode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3StatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.Pair;

class K3CfaBuilder {

  private final LogManager logger;
  private final Configuration config;
  private final MachineModel machineModel;
  private final ShutdownNotifier shutdownNotifier;

  public K3CfaBuilder(
      LogManager pLogger,
      Configuration pConfig,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    config = pConfig;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;
  }

  private Pair<ADeclaration, String> parseGlobalVariable(K3VariableDeclarationCommand pCommand) {
    return Pair.of(
        pCommand.getVariableDeclaration(), pCommand.getVariableDeclaration().toASTString());
  }

  private CFANode newNodeAddedToBuilder(
      K3ProcedureDeclaration pProcedure, Consumer<CFANode> pMetadataFunctionAllNodes) {
    CFANode newNode = new CFANode(pProcedure);
    pMetadataFunctionAllNodes.accept(newNode);
    return newNode;
  }

  private Pair<FunctionEntryNode, FunctionExitNode> newFunctionNodesWithMetadataTracking(
      K3ProcedureDeclaration pProcedure,
      Consumer<FunctionEntryNode> pMetadataFunctionEntryNodes,
      Consumer<CFANode> pMetadataFunctionAllNodes) {
    FunctionExitNode functionExitNode = new FunctionExitNode(pProcedure);
    FunctionEntryNode functionEntryNode =
        new K3ProcedureEntryNode(pProcedure.getFileLocation(), functionExitNode, pProcedure);

    // TODO: I'm unsure why this is handled like this and not directly in the constructor of
    //  FunctionEntryNode.
    functionExitNode.setEntryNode(functionEntryNode);

    // Update the metadata for the nodes
    pMetadataFunctionEntryNodes.accept(functionEntryNode);
    pMetadataFunctionAllNodes.accept(functionEntryNode);
    pMetadataFunctionAllNodes.accept(functionExitNode);
    return Pair.of(functionEntryNode, functionExitNode);
  }

  private Pair<CFANode, List<CFANode>> buildCfaForStatement(
      K3Statement pStatement, CFANode pCurrentNode, K3ProcedureDeclaration pProcedure)
      throws K3ParserException, InterruptedException {
    ImmutableList.Builder<CFANode> builder = ImmutableList.builder();
    CFANode predecessorNode = pCurrentNode;
    CFANode currentSuccessorNode = newNodeAddedToBuilder(pProcedure, node -> builder.add(node));

    switch (pStatement) {
      case K3AssignmentStatement assignmentStatement -> {
        // Handle assignment statements.
        CFAEdge edge =
            new K3StatementEdge(
                assignmentStatement.toASTString(),
                assignmentStatement,
                assignmentStatement.getFileLocation(),
                predecessorNode,
                currentSuccessorNode);
        CFACreationUtils.addEdgeToCFA(edge, logger);
      }
      case K3AssumeStatement assumeStatement -> {
        // We do not need to split the assumption into multiple edges, since there is no
        // short-circuiting
        CFAEdge edge =
            new K3AssumeEdge(
                assumeStatement.toASTString(),
                assumeStatement.getFileLocation(),
                predecessorNode,
                currentSuccessorNode,
                assumeStatement.getTerm(),
                true,
                false,
                false);
        CFACreationUtils.addEdgeToCFA(edge, logger);
      }
      case K3SequenceStatement sequenceStatement -> {
        // Handle sequence statements.
        for (K3Statement subStatement : sequenceStatement.getStatements()) {
          Pair<CFANode, List<CFANode>> lastNodeWithAllNodes =
              buildCfaForStatement(subStatement, predecessorNode, pProcedure);
          builder.addAll(lastNodeWithAllNodes.getSecondNotNull());
          predecessorNode = lastNodeWithAllNodes.getFirstNotNull();
        }
        currentSuccessorNode = predecessorNode;
      }
      case K3ProcedureCallStatement pK3ProcedureCallStatement -> {
        // We do not need to split the assumption into multiple edges, since there is no
        // short-circuiting
        CFAEdge edge =
            new K3StatementEdge(
                pK3ProcedureCallStatement.toASTString(),
                pK3ProcedureCallStatement,
                pK3ProcedureCallStatement.getFileLocation(),
                predecessorNode,
                currentSuccessorNode);
        CFACreationUtils.addEdgeToCFA(edge, logger);
      }
    }

    return Pair.of(currentSuccessorNode, builder.build());
  }

  private Pair<FunctionEntryNode, List<CFANode>> parseProcedureDefinition(
      K3ProcedureDefinitionCommand pCommand) throws K3ParserException, InterruptedException {
    K3ProcedureDeclaration procedureDeclaration = pCommand.getProcedureDeclaration();
    ImmutableList.Builder<CFANode> nodesBuilder = ImmutableList.builder();

    // Create the entry and exit nodes for the function
    Pair<FunctionEntryNode, FunctionExitNode> functionNodes =
        newFunctionNodesWithMetadataTracking(
            procedureDeclaration, x -> {}, node -> nodesBuilder.add(node));

    FunctionExitNode functionExitNode = functionNodes.getSecondNotNull();
    FunctionEntryNode functionEntryNode = functionNodes.getFirstNotNull();

    Pair<CFANode, List<CFANode>> bodyNodes =
        buildCfaForStatement(pCommand.getBody(), functionEntryNode, procedureDeclaration);
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge("", FileLocation.DUMMY, bodyNodes.getFirstNotNull(), functionExitNode, ""),
        logger);
    nodesBuilder.addAll(bodyNodes.getSecondNotNull());

    return Pair.of(functionEntryNode, nodesBuilder.build());
  }

  public ParseResult buildCfaFromScript(K3Script script)
      throws K3ParserException, InterruptedException {
    NavigableMap<String, FunctionEntryNode> functions = new TreeMap<>();
    TreeMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

    // TODO: For some reason I cannot use the more specialized type `K3VariableDeclaration` here
    List<Pair<ADeclaration, String>> globalDeclarations = new ArrayList<>();
    // Temporary list to keep separation of concerns.
    // This class only parses the script and builds the CFA.
    // The actual parsing of the string, and therefore the information about the file names
    // is done previously.
    List<Path> fileNames = ImmutableList.of();

    // Keep track of the invented main function
    K3ProcedureDeclaration mainFunctionDeclaration =
        K3ProcedureDeclaration.mainFunctionDeclaration();
    String mainFunctionName = mainFunctionDeclaration.getOrigName();
    Pair<FunctionEntryNode, FunctionExitNode> mainFunctionNodes =
        newFunctionNodesWithMetadataTracking(
            mainFunctionDeclaration,
            entryNode -> functions.put(mainFunctionName, entryNode),
            node -> cfaNodes.put(mainFunctionName, node));

    FunctionExitNode mainFunctionExitNode = mainFunctionNodes.getSecondNotNull();
    CFANode currentMainFunctionNode = mainFunctionNodes.getFirstNotNull();

    // Go through all the commands in the script and parse them.
    for (K3Command command : script.getCommands()) {
      switch (command) {
        case K3VariableDeclarationCommand variableDeclarationCommand ->
            globalDeclarations.add(parseGlobalVariable(variableDeclarationCommand));
        case K3ProcedureDefinitionCommand procedureDefinitionCommand -> {
          K3ProcedureDeclaration procedureDeclaration =
              procedureDefinitionCommand.getProcedureDeclaration();

          Pair<FunctionEntryNode, List<CFANode>> functionParseResult =
              parseProcedureDefinition(procedureDefinitionCommand);

          String functionName = procedureDeclaration.getOrigName();
          functions.put(functionName, functionParseResult.getFirstNotNull());
          cfaNodes.putAll(functionName, functionParseResult.getSecondNotNull());
        }
        case VerifyCallCommand pVerifyCallCommand -> {
          // In theory the idea behind K3 is to have an interactive shell with the ability to talk
          // between the verifier and the user. In this case we do a simplification to match
          // CPAchecker's architecture better by only accepting finished scripts and not enabling an
          // incremental/dialgue mode.
          // The simplification makes it such that we will create an artificial main function which
          // calls all the functions to be verified with the corresponding parameters.
          K3ProcedureCallStatement procedureCallStatement =
              new K3ProcedureCallStatement(
                  FileLocation.DUMMY,
                  List.of(),
                  List.of(),
                  pVerifyCallCommand.getProcedureDeclaration(),
                  pVerifyCallCommand.getTerms(),
                  ImmutableList.of());

          CFANode successorNode =
              newNodeAddedToBuilder(
                  mainFunctionDeclaration, node -> cfaNodes.put(mainFunctionName, node));

          CFAEdge procedureCallEdge =
              new K3StatementEdge(
                  procedureCallStatement.toASTString(),
                  procedureCallStatement,
                  procedureCallStatement.getFileLocation(),
                  currentMainFunctionNode,
                  successorNode);
          CFACreationUtils.addEdgeToCFA(procedureCallEdge, logger);

          // Update the nodes for the next command
          currentMainFunctionNode = successorNode;
        }
      }
    }

    // Finish the main function
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge(
            "End of main function",
            FileLocation.DUMMY,
            currentMainFunctionNode,
            mainFunctionExitNode,
            ""),
        logger);

    return new ParseResult(functions, cfaNodes, globalDeclarations, fileNames);
  }
}
