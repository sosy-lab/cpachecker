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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SetOptionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.SMTLibCommand;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3CfaMetadata;
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

  private final ImmutableSetMultimap.Builder<CFANode, K3TagProperty> nodeToTagAnnotations =
      new ImmutableSetMultimap.Builder<>();
  private final ImmutableSetMultimap.Builder<String, K3TagProperty> tagReferencesToAnnotations =
      ImmutableSetMultimap.builder();
  private final ImmutableSetMultimap.Builder<CFANode, K3TagReference> nodesToTagReferences =
      ImmutableSetMultimap.builder();

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

  private Pair<ADeclaration, String> parseGlobalConstant(K3DeclareConstCommand pCommand) {
    return Pair.of(pCommand.getVariable(), pCommand.getVariable().toASTString());
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

  private Pair<FunctionEntryNode, Set<CFANode>> parseProcedureDefinition(
      K3ProcedureDefinitionCommand pCommand) throws K3ParserException {
    K3ProcedureDeclaration procedureDeclaration = pCommand.getProcedureDeclaration();

    ImmutableMap.Builder<CFANode, String> gotoNodesToLabels = ImmutableMap.builder();
    ImmutableMap.Builder<String, CFANode> labelsToNodes = ImmutableMap.builder();
    ImmutableSet.Builder<CFANode> allNodesCollector = ImmutableSet.builder();

    // Create the entry and exit nodes for the function
    Pair<FunctionEntryNode, FunctionExitNode> functionNodes =
        newFunctionNodesWithMetadataTracking(
            procedureDeclaration, x -> {}, node -> allNodesCollector.add(node));

    FunctionExitNode functionExitNode = functionNodes.getSecondNotNull();
    FunctionEntryNode functionEntryNode = functionNodes.getFirstNotNull();

    K3StatementToCfaVisitor statementVisitor =
        new K3StatementToCfaVisitor(
            functionEntryNode,
            procedureDeclaration,
            logger,
            functionExitNode,
            nodeToTagAnnotations,
            nodesToTagReferences,
            gotoNodesToLabels,
            labelsToNodes,
            allNodesCollector,
            tagReferencesToAnnotations.build());

    Optional<CFANode> optionalEndNode = pCommand.getBody().accept(statementVisitor);
    if (optionalEndNode.isPresent()) {
      // In this case we need to add a blank edge to the function exit node
      // The contrary can happen if there is a return statement at the end of the function body.
      CFACreationUtils.addEdgeToCFA(
          new BlankEdge(
              "", FileLocation.DUMMY, optionalEndNode.orElseThrow(), functionExitNode, ""),
          logger);
    }

    // Now generate the connections for the goto labels
    Map<String, CFANode> labelsToNodesBuilt = labelsToNodes.buildOrThrow();
    Map<CFANode, String> gotoNodesToLabelBuilt = gotoNodesToLabels.buildOrThrow();
    for (Map.Entry<CFANode, String> gotoNodeToLabel : gotoNodesToLabelBuilt.entrySet()) {
      String label = gotoNodeToLabel.getValue();
      CFANode gotoNode = gotoNodeToLabel.getKey();

      CFANode labelNode = labelsToNodesBuilt.get(label);
      if (labelNode == null) {
        throw new K3ParserException("Could not find '" + label + "' to jump with a goto");
      }

      // Add a blank edge from the goto node to the label node
      CFACreationUtils.addEdgeToCFA(
          new BlankEdge(
              "Goto to label " + label,
              FileLocation.DUMMY,
              gotoNode,
              labelNode,
              "Goto to label " + label),
          logger);
    }

    return Pair.of(functionEntryNode, allNodesCollector.build());
  }

  public ParseResult buildCfaFromScript(K3Script script) throws K3ParserException {
    NavigableMap<String, FunctionEntryNode> functions = new TreeMap<>();
    TreeMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

    // We cannot use a more specialized data structure here, since we need to
    // pass it to the ParseResult constructor.
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
            "End of Global Declarations",
            FileLocation.DUMMY,
            mainFunctionNodes.getFirstNotNull(),
            currentMainFunctionNode,
            "End of Global Declarations"),
        logger);

    // Keep track of the metadata for the CFA, like the specification, and the SMT-LIB commands.
    ImmutableList.Builder<SMTLibCommand> smtLibCommandsBuilder = new ImmutableList.Builder<>();

    // Go through all the commands in the script and parse them.
    List<K3Command> commands = script.getCommands();
    int indexOfFirstVerifyCall = -1;

    // In order to determine whether correctness or violation witness production
    // has been enabled, we need to keep track of this information while parsing.
    boolean correctnessWitnessProductionEnabled = false;
    boolean violationWitnessProductionEnabled = false;

    for (int i = 0; i < commands.size() && indexOfFirstVerifyCall < 0; i++) {

      K3Command command = commands.get(i);

      switch (command) {
        case K3VariableDeclarationCommand variableDeclarationCommand ->
            globalDeclarations.add(parseGlobalVariable(variableDeclarationCommand));
        case K3ProcedureDefinitionCommand procedureDefinitionCommand -> {
          K3ProcedureDeclaration procedureDeclaration =
              procedureDefinitionCommand.getProcedureDeclaration();

          Pair<FunctionEntryNode, Set<CFANode>> functionDefinitionParseResult =
              parseProcedureDefinition(procedureDefinitionCommand);

          String functionName = procedureDeclaration.getOrigName();
          functions.put(functionName, functionDefinitionParseResult.getFirstNotNull());
          cfaNodes.putAll(functionName, functionDefinitionParseResult.getSecondNotNull());
        }
        case K3VerifyCallCommand pVerifyCallCommand -> {
          // In theory the idea behind K3 is to have an interactive shell with the ability to talk
          // between the verifier and the user. In this case we do a simplification to match
          // CPAchecker's architecture better by only accepting finished scripts and not enabling an
          // incremental/dialgue mode.
          // The simplification makes it such that we will create an artificial main function which
          // calls all the functions to be verified with the corresponding parameters.
          K3ProcedureCallStatement procedureCallStatement =
              new K3ProcedureCallStatement(
                  FileLocation.DUMMY,
                  ImmutableList.of(),
                  ImmutableList.of(),
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
          tagReferencesToAnnotations.putAll(
              pK3AnnotateTagCommand.getTagName(), pK3AnnotateTagCommand.getTags());
        }
        case K3GetWitnessCommand pK3GetWitnessCommand -> {
          logger.log(
              Level.WARNING,
              "Ignoring get-proof command, since there was no verify call command before.");
        }
        case K3SetLogicCommand pK3SetLogicCommand -> {
          // We add all set logic commands to the CFA metadata,
          // since we could need it later when creating the SMT-Solver instance.
          //
          // Currently due to how JavaSMT handles stuff we do not really need it,
          // but in the future this could change. In particular once more logics
          // of SMT-LIB are supported by K3.
          smtLibCommandsBuilder.add(pK3SetLogicCommand);
        }
        case K3DeclareConstCommand pK3DeclareConstCommand -> {
          globalDeclarations.add(parseGlobalConstant(pK3DeclareConstCommand));
        }
        case K3AssertCommand pK3AssertCommand -> {
          // Technically these are global assumptions, but we
          // keep them inside of the main function for simplicity.
          K3Term term = pK3AssertCommand.getTerm();

          CFANode successorNode =
              newNodeAddedToBuilder(
                  mainFunctionDeclaration, node -> cfaNodes.put(mainFunctionName, node));

          // We simply assume the term to be true here.
          CFAEdge trueEdge =
              new K3AssumeEdge(
                  term.toASTString(),
                  term.getFileLocation(),
                  currentMainFunctionNode,
                  successorNode,
                  term,
                  true,
                  false,
                  false);

          CFACreationUtils.addEdgeToCFA(trueEdge, logger);

          // Update the nodes for the next command
          currentMainFunctionNode = successorNode;
        }
        case K3DeclareFunCommand pK3DeclareFunCommand ->
            smtLibCommandsBuilder.add(pK3DeclareFunCommand);
        case K3DeclareSortCommand pK3DeclareSortCommand ->
            smtLibCommandsBuilder.add(pK3DeclareSortCommand);
        case K3SetOptionCommand pK3SetOptionCommand -> {
          if (pK3SetOptionCommand
              .getOption()
              .equals(K3SetOptionCommand.OPTION_PRODUCE_CORRECTNESS)) {
            Optional<Boolean> booleanValue = pK3SetOptionCommand.getBooleanValue();
            if (booleanValue.isEmpty()) {
              throw new K3ParserException(
                  "The value for the option "
                      + K3SetOptionCommand.OPTION_PRODUCE_CORRECTNESS
                      + " must be either 'true' or 'false'.");
            }
            correctnessWitnessProductionEnabled = booleanValue.orElseThrow();
          } else if (pK3SetOptionCommand
              .getOption()
              .equals(K3SetOptionCommand.OPTION_PRODUCE_VIOLATION)) {
            Optional<Boolean> booleanValue = pK3SetOptionCommand.getBooleanValue();
            if (booleanValue.isEmpty()) {
              throw new K3ParserException(
                  "The value for the option "
                      + K3SetOptionCommand.OPTION_PRODUCE_VIOLATION
                      + " must be either 'true' or 'false'.");
            }
            violationWitnessProductionEnabled = booleanValue.orElseThrow();
          } else {
            // For all other options we simply add them to the SMT-LIB commands.
            smtLibCommandsBuilder.add(pK3SetOptionCommand);
          }
        }
        case K3SelectTraceCommand pK3SelectTraceCommand -> {
          throw new K3ParserException(
              "Select trace commands are not yet supported in CFA parsing.");
        }
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
        case K3GetWitnessCommand pK3GetWitnessCommand -> {
          exportWitness = true;
        }
        default ->
            logger.log(
                Level.WARNING,
                "The command after the verify call command is neither a get-proof nor a"
                    + " get-counterexample command. It will be ignored.");
      }
    }

    if (indexOfFirstVerifyCall + 2 < commands.size()) {
      logger.log(
          Level.WARNING,
          "There are commands after the verify call and the possible get-proof/get-counterexample"
              + " command. These will be ignored.");
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
        functions,
        cfaNodes,
        globalDeclarations,
        fileNames,
        new K3CfaMetadata(
            smtLibCommandsBuilder.build(),
            nodeToTagAnnotations.build(),
            nodesToTagReferences.build(),
            exportWitness && correctnessWitnessProductionEnabled,
            exportWitness && violationWitnessProductionEnabled));
  }
}
