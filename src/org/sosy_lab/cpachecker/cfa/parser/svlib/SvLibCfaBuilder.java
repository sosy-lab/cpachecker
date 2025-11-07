// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

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
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibScript;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureEntryNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.Pair;

class SvLibCfaBuilder {

  private final LogManager logger;

  @SuppressWarnings("unused")
  private final Configuration config;

  @SuppressWarnings("unused")
  private final MachineModel machineModel;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final ImmutableSetMultimap.Builder<CFANode, SvLibTagProperty> nodeToTagAnnotations =
      new ImmutableSetMultimap.Builder<>();
  private final ImmutableSetMultimap.Builder<String, SvLibTagProperty> tagReferencesToAnnotations =
      ImmutableSetMultimap.builder();
  private final ImmutableSetMultimap.Builder<CFANode, SvLibTagReference> nodesToTagReferences =
      ImmutableSetMultimap.builder();

  public SvLibCfaBuilder(
      LogManager pLogger,
      Configuration pConfig,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    config = pConfig;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;
  }

  private Pair<ADeclaration, String> parseGlobalVariable(SvLibVariableDeclarationCommand pCommand) {
    return Pair.of(
        pCommand.getVariableDeclaration(), pCommand.getVariableDeclaration().toASTString());
  }

  private Pair<ADeclaration, String> parseGlobalConstant(SvLibDeclareConstCommand pCommand) {
    return Pair.of(pCommand.getVariable(), pCommand.getVariable().toASTString());
  }

  private CFANode newNodeAddedToBuilder(
      SvLibProcedureDeclaration pProcedure, Consumer<CFANode> pMetadataFunctionAllNodes) {
    CFANode newNode = new CFANode(pProcedure);
    pMetadataFunctionAllNodes.accept(newNode);
    return newNode;
  }

  private Pair<FunctionEntryNode, FunctionExitNode> newFunctionNodesWithMetadataTracking(
      SvLibProcedureDeclaration pProcedure,
      Consumer<FunctionEntryNode> pMetadataFunctionEntryNodes,
      Consumer<CFANode> pMetadataFunctionAllNodes) {
    FunctionExitNode functionExitNode = new FunctionExitNode(pProcedure);
    FunctionEntryNode functionEntryNode =
        new SvLibProcedureEntryNode(pProcedure.getFileLocation(), functionExitNode, pProcedure);

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
      SvLibProcedureDefinitionCommand pCommand) throws SvLibParserException {
    SvLibProcedureDeclaration procedureDeclaration = pCommand.getProcedureDeclaration();

    ImmutableMap.Builder<CFANode, String> gotoNodesToLabels = ImmutableMap.builder();
    ImmutableMap.Builder<String, CFANode> labelsToNodes = ImmutableMap.builder();
    ImmutableSet.Builder<CFANode> allNodesCollector = ImmutableSet.builder();

    // Create the entry and exit nodes for the function
    Pair<FunctionEntryNode, FunctionExitNode> functionNodes =
        newFunctionNodesWithMetadataTracking(
            procedureDeclaration, x -> {}, node -> allNodesCollector.add(node));

    FunctionExitNode functionExitNode = functionNodes.getSecondNotNull();
    FunctionEntryNode functionEntryNode = functionNodes.getFirstNotNull();

    SvLibStatementToCfaVisitor statementVisitor =
        new SvLibStatementToCfaVisitor(
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
        throw new SvLibParserException("Could not find '" + label + "' to jump with a goto");
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

  public ParseResult buildCfaFromScript(SvLibScript script) throws SvLibParserException {
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
    SvLibProcedureDeclaration mainFunctionDeclaration =
        SvLibProcedureDeclaration.mainFunctionDeclaration();
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
    ImmutableList.Builder<SmtLibCommand> smtLibCommandsBuilder = new ImmutableList.Builder<>();

    // Go through all the commands in the script and parse them.
    List<SvLibCommand> commands = script.getCommands();
    int indexOfFirstVerifyCall = -1;

    // In order to determine whether correctness or violation witness production
    // has been enabled, we need to keep track of this information while parsing.
    boolean correctnessWitnessProductionEnabled = false;
    boolean violationWitnessProductionEnabled = false;

    for (int i = 0; i < commands.size() && indexOfFirstVerifyCall < 0; i++) {

      SvLibCommand command = commands.get(i);

      switch (command) {
        case SvLibVariableDeclarationCommand variableDeclarationCommand ->
            globalDeclarations.add(parseGlobalVariable(variableDeclarationCommand));
        case SvLibProcedureDefinitionCommand procedureDefinitionCommand -> {
          SvLibProcedureDeclaration procedureDeclaration =
              procedureDefinitionCommand.getProcedureDeclaration();

          Pair<FunctionEntryNode, Set<CFANode>> functionDefinitionParseResult =
              parseProcedureDefinition(procedureDefinitionCommand);

          String functionName = procedureDeclaration.getOrigName();
          functions.put(functionName, functionDefinitionParseResult.getFirstNotNull());
          cfaNodes.putAll(functionName, functionDefinitionParseResult.getSecondNotNull());
        }
        case SvLibVerifyCallCommand pVerifyCallCommand -> {
          // In theory the idea behind SV-LIB is to have an interactive shell with the ability to
          // talk
          // between the verifier and the user. In this case we do a simplification to match
          // CPAchecker's architecture better by only accepting finished scripts and not enabling an
          // incremental/dialgue mode.
          // The simplification makes it such that we will create an artificial main function which
          // calls all the functions to be verified with the corresponding parameters.
          SvLibProcedureCallStatement procedureCallStatement =
              new SvLibProcedureCallStatement(
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
              new SvLibStatementEdge(
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
        case SvLibAnnotateTagCommand pSvLibAnnotateTagCommand -> {
          String tagName = pSvLibAnnotateTagCommand.getTagName();
          List<SvLibTagProperty> tagProperties = pSvLibAnnotateTagCommand.getTags();

          // TODO: This is highly inefficient!!!
          for (Entry<CFANode, SvLibTagReference> entry : nodesToTagReferences.build().entries()) {
            CFANode node = entry.getKey();
            SvLibTagReference tagReference = entry.getValue();
            if (tagReference.getTagName().equals(tagName)) {
              nodeToTagAnnotations.putAll(node, tagProperties);
            }
          }
          tagReferencesToAnnotations.putAll(tagName, tagProperties);
        }
        case SvLibGetWitnessCommand pSvLibGetWitnessCommand -> {
          logger.log(
              Level.WARNING,
              "Ignoring get-proof command, since there was no verify call command before.");
        }
        case SvLibSetLogicCommand pSvLibSetLogicCommand -> {
          // We add all set logic commands to the CFA metadata,
          // since we could need it later when creating the SMT-Solver instance.
          //
          // Currently due to how JavaSMT handles stuff we do not really need it,
          // but in the future this could change. In particular once more logics
          // of SMT-LIB are supported by SV-LIB.
          smtLibCommandsBuilder.add(pSvLibSetLogicCommand);
        }
        case SvLibDeclareConstCommand pSvLibDeclareConstCommand -> {
          globalDeclarations.add(parseGlobalConstant(pSvLibDeclareConstCommand));
        }
        case SvLibAssertCommand pSvLibAssertCommand -> {
          // Technically these are global assumptions, but we
          // keep them inside of the main function for simplicity.
          SvLibTerm term = pSvLibAssertCommand.getTerm();

          CFANode successorNode =
              newNodeAddedToBuilder(
                  mainFunctionDeclaration, node -> cfaNodes.put(mainFunctionName, node));

          // We simply assume the term to be true here.
          CFAEdge trueEdge =
              new SvLibAssumeEdge(
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
        case SvLibDeclareFunCommand pSvLibDeclareFunCommand ->
            smtLibCommandsBuilder.add(pSvLibDeclareFunCommand);
        case SvLibDeclareSortCommand pSvLibDeclareSortCommand ->
            smtLibCommandsBuilder.add(pSvLibDeclareSortCommand);
        case SvLibSetOptionCommand pSvLibSetOptionCommand -> {
          if (pSvLibSetOptionCommand
              .getOption()
              .equals(SvLibSetOptionCommand.OPTION_PRODUCE_CORRECTNESS)) {
            Optional<Boolean> booleanValue = pSvLibSetOptionCommand.getBooleanValue();
            if (booleanValue.isEmpty()) {
              throw new SvLibParserException(
                  "The value for the option "
                      + SvLibSetOptionCommand.OPTION_PRODUCE_CORRECTNESS
                      + " must be either 'true' or 'false'.");
            }
            correctnessWitnessProductionEnabled = booleanValue.orElseThrow();
          } else if (pSvLibSetOptionCommand
              .getOption()
              .equals(SvLibSetOptionCommand.OPTION_PRODUCE_VIOLATION)) {
            Optional<Boolean> booleanValue = pSvLibSetOptionCommand.getBooleanValue();
            if (booleanValue.isEmpty()) {
              throw new SvLibParserException(
                  "The value for the option "
                      + SvLibSetOptionCommand.OPTION_PRODUCE_VIOLATION
                      + " must be either 'true' or 'false'.");
            }
            violationWitnessProductionEnabled = booleanValue.orElseThrow();
          } else {
            // For all other options we simply add them to the SMT-LIB commands.
            smtLibCommandsBuilder.add(pSvLibSetOptionCommand);
          }
        }
        case SvLibSelectTraceCommand pSvLibSelectTraceCommand -> {
          throw new SvLibParserException(
              "Select trace commands are not yet supported in CFA parsing.");
        }
        case SvLibSetInfoCommand pSvLibSetInfoCommand -> {
          // we ignore set-info commands for now
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
        case SvLibGetWitnessCommand pSvLibGetWitnessCommand -> {
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
        new SvLibCfaMetadata(
            smtLibCommandsBuilder.build(),
            nodeToTagAnnotations.build(),
            nodesToTagReferences.build(),
            exportWitness && correctnessWitnessProductionEnabled,
            exportWitness && violationWitnessProductionEnabled));
  }
}
