// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.k3;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BreakStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ContinueStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GetCounterexampleCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GetProofCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GotoStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3HavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IfStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3LabelStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3WhileStatement;
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

  @SuppressWarnings("unused")
  private final Configuration config;

  @SuppressWarnings("unused")
  private final MachineModel machineModel;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final Map<String, CFANode> labelToNode = new HashMap<>();

  private Optional<CFANode> outermostLoopHead = Optional.empty();

  private Map<CFANode, String> gotoNodesToLabel = new HashMap<>();

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
      K3Statement pStatement,
      CFANode pCurrentNode,
      K3ProcedureDeclaration pProcedure,
      FunctionExitNode pFunctionExitNode)
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
              buildCfaForStatement(subStatement, predecessorNode, pProcedure, pFunctionExitNode);
          builder.addAll(lastNodeWithAllNodes.getSecondNotNull());
          predecessorNode = lastNodeWithAllNodes.getFirstNotNull();
        }
        CFAEdge edge =
            new BlankEdge(
                "end of sequence statement",
                FileLocation.DUMMY,
                predecessorNode,
                currentSuccessorNode,
                "end of sequence statement");
        CFACreationUtils.addEdgeToCFA(edge, logger);
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
      case K3BreakStatement pK3BreakStatement -> {
        Verify.verify(outermostLoopHead.isPresent());
        CFAEdge edge =
            new BlankEdge(
                "break",
                pK3BreakStatement.getFileLocation(),
                predecessorNode,
                outermostLoopHead.get(),
                "break");
        CFACreationUtils.addEdgeToCFA(edge, logger);
      }
      case K3ContinueStatement pK3ContinueStatement -> {
        Verify.verify(outermostLoopHead.isPresent());
        CFAEdge edge =
            new BlankEdge(
                "continue",
                pK3ContinueStatement.getFileLocation(),
                predecessorNode,
                outermostLoopHead.get(),
                "continue");
        CFACreationUtils.addEdgeToCFA(edge, logger);
      }

      case K3GotoStatement pK3GotoStatement -> {
        // We may not have all the labels yet, so we just store the information for later.
        String label = pK3GotoStatement.getLabel();
        gotoNodesToLabel.put(predecessorNode, label);
      }

      case K3HavocStatement pK3HavocStatement -> {
        CFAEdge egde =
            new K3StatementEdge(
                pK3HavocStatement.toASTString(),
                pK3HavocStatement,
                pK3HavocStatement.getFileLocation(),
                predecessorNode,
                currentSuccessorNode);
        CFACreationUtils.addEdgeToCFA(egde, logger);
      }
      case K3IfStatement pK3IfStatement -> {
        // Handle conditions
        CFANode trueConditionNode = newNodeAddedToBuilder(pProcedure, node -> builder.add(node));
        CFANode falseConditionNode = newNodeAddedToBuilder(pProcedure, node -> builder.add(node));

        CFAEdge trueEdge =
            new K3AssumeEdge(
                pK3IfStatement.getCondition().toASTString(),
                pK3IfStatement.getFileLocation(),
                predecessorNode,
                trueConditionNode,
                pK3IfStatement.getCondition(),
                true,
                false,
                false);
        CFAEdge falseEdge =
            new K3AssumeEdge(
                "!(" + pK3IfStatement.getCondition().toASTString() + ")",
                pK3IfStatement.getFileLocation(),
                predecessorNode,
                falseConditionNode,
                pK3IfStatement.getCondition(),
                false,
                false,
                false);
        CFACreationUtils.addEdgeToCFA(trueEdge, logger);
        CFACreationUtils.addEdgeToCFA(falseEdge, logger);

        // Handle the then branch
        Pair<CFANode, List<CFANode>> thenBranchNodes =
            buildCfaForStatement(
                pK3IfStatement.getThenBranch(), trueConditionNode, pProcedure, pFunctionExitNode);
        builder.addAll(thenBranchNodes.getSecondNotNull());
        final CFANode thenBranchEndNode = thenBranchNodes.getFirstNotNull();

        // Handle the else branch if it exists
        final CFANode elseBranchEndNode;
        if (pK3IfStatement.getElseBranch().isPresent()) {
          Pair<CFANode, List<CFANode>> elseBranchNodes =
              buildCfaForStatement(
                  pK3IfStatement.getElseBranch().orElseThrow(),
                  falseConditionNode,
                  pProcedure,
                  pFunctionExitNode);
          builder.addAll(elseBranchNodes.getSecondNotNull());
          elseBranchEndNode = elseBranchNodes.getFirstNotNull();
        } else {
          elseBranchEndNode = falseConditionNode;
        }

        // Connect the end nodes with the node after the if statement
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                thenBranchEndNode,
                currentSuccessorNode,
                "End of if-then(-else)"),
            logger);
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                elseBranchEndNode,
                currentSuccessorNode,
                "End of if-then(-else)"),
            logger);
      }
      case K3LabelStatement pK3LabelStatement -> {
        String label = pK3LabelStatement.getLabel();
        labelToNode.put(label, predecessorNode);
      }
      case K3ReturnStatement pK3ReturnStatement -> {
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge(
                "return",
                pK3ReturnStatement.getFileLocation(),
                predecessorNode,
                pFunctionExitNode,
                "return"),
            logger);
      }
      case K3WhileStatement pK3WhileStatement -> {
        // Create the loop head and update the outermost loop head
        CFANode loopHeadNode = newNodeAddedToBuilder(pProcedure, node -> builder.add(node));
        Optional<CFANode> previousOutermostLoopHead = outermostLoopHead;

        outermostLoopHead = Optional.of(loopHeadNode);

        // Connect the predecessor to the loop head
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge(
                "",
                pK3WhileStatement.getFileLocation(),
                predecessorNode,
                loopHeadNode,
                "entering loop"),
            logger);

        // Create the edges for the condition
        CFANode trueConditionNode = newNodeAddedToBuilder(pProcedure, node -> builder.add(node));
        CFANode falseConditionNode = newNodeAddedToBuilder(pProcedure, node -> builder.add(node));

        CFAEdge trueEdge =
            new K3AssumeEdge(
                pK3WhileStatement.getCondition().toASTString(),
                pK3WhileStatement.getFileLocation(),
                loopHeadNode,
                trueConditionNode,
                pK3WhileStatement.getCondition(),
                true,
                false,
                false);
        CFAEdge falseEdge =
            new K3AssumeEdge(
                "!(" + pK3WhileStatement.getCondition().toASTString() + ")",
                pK3WhileStatement.getFileLocation(),
                loopHeadNode,
                falseConditionNode,
                pK3WhileStatement.getCondition(),
                false,
                false,
                false);
        CFACreationUtils.addEdgeToCFA(trueEdge, logger);
        CFACreationUtils.addEdgeToCFA(falseEdge, logger);

        // Handle the body of the while loop
        Pair<CFANode, List<CFANode>> bodyNodes =
            buildCfaForStatement(
                pK3WhileStatement.getBody(), trueConditionNode, pProcedure, pFunctionExitNode);
        builder.addAll(bodyNodes.getSecondNotNull());
        CFANode bodyEndNode = bodyNodes.getFirstNotNull();

        // Connect the end of the body back to the loop head
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge("", FileLocation.DUMMY, bodyEndNode, loopHeadNode, "Return to loop head"),
            logger);

        // Reset the outermost loop head
        outermostLoopHead = previousOutermostLoopHead;
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
        buildCfaForStatement(
            pCommand.getBody(), functionEntryNode, procedureDeclaration, functionExitNode);
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
    CFANode currentMainFunctionNode =
        newNodeAddedToBuilder(
            mainFunctionDeclaration, node -> cfaNodes.put(mainFunctionName, node));

    // Add blank edge for all global declarations
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge(
            "Start of Global Declarations",
            FileLocation.DUMMY,
            mainFunctionNodes.getFirstNotNull(),
            currentMainFunctionNode,
            "Start of Global Declarations"),
        logger);

    // Go through all the commands in the script and parse them.
    List<K3Command> commands = script.getCommands();
    int indexOfFirstVerifyCall = -1;
    ImmutableSetMultimap.Builder<String, K3TagAttribute> tagAnnotations =
        ImmutableSetMultimap.builder();

    for (int i = 0; i < commands.size() && indexOfFirstVerifyCall < 0; i++) {

      K3Command command = commands.get(i);

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

          indexOfFirstVerifyCall = i;
        }
        case K3AnnotateTagCommand pK3AnnotateTagCommand -> {
          tagAnnotations.putAll(
              pK3AnnotateTagCommand.getTagName(), pK3AnnotateTagCommand.getTags());
        }
        case K3GetCounterexampleCommand pK3GetCounterexampleCommand -> {
          logger.log(
              Level.WARNING,
              "Ignoring get-counterexample command, since there was no verify call command before.");
        }
        case K3GetProofCommand pK3GetProofCommand -> {
          logger.log(
              Level.WARNING,
              "Ignoring get-proof command, since there was no verify call command before.");
        }
        // TODO: handle these commands properly
        case K3SetLogicCommand pK3SetLogicCommand -> {}
        case K3AssertCommand pK3AssertCommand -> {}
        case K3DeclareConstCommand pK3DeclareConstCommand -> {}
      }
    }

    Verify.verify(indexOfFirstVerifyCall >= 0, "There must be at least one verify call command");

    // We now see if we should export a witness after the verify call.
    // After the verify call command we stop parsing the script, since we need to execute
    // this command first before continuing. Therefore, we need to stop here.
    // We will print a warning if there are any commands after this one.
    boolean exportWitness = false;
    if (indexOfFirstVerifyCall + 1 < commands.size()) {
      switch (commands.get(indexOfFirstVerifyCall + 1)) {
        case K3GetCounterexampleCommand pK3GetCounterexampleCommand -> {
          exportWitness = true;
        }
        case K3GetProofCommand pK3GetProofCommand -> {
          exportWitness = true;
        }
        default ->
            logger.log(
                Level.WARNING,
                "The command after the verify call command is neither a get-proof nor a get-counterexample command. It will be ignored.");
      }
    }

    if (indexOfFirstVerifyCall + 2 < commands.size()) {
      logger.log(
          Level.WARNING,
          "There are commands after the verify call and the possible get-proof/get-counterexample command. These will be ignored.");
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

    return new ParseResult(
        functions, cfaNodes, globalDeclarations, fileNames, tagAnnotations.build(), exportWitness);
  }
}
