// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
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
import java.util.Objects;
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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermReplacer;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureEntryNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunRecCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunsRecCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.exceptions.SvLibParserException;
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

  // Required to reconstruct violation witnesses properly
  private final ImmutableMap.Builder<CFANode, SvLibProcedureDeclaration>
      nodesToActualProcedureDefinitionEnd = ImmutableMap.builder();
  private final ImmutableMap.Builder<CFANode, SvLibHavocStatement> nodesToActualHavocStatementEnd =
      ImmutableMap.builder();

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
        pCommand.getVariableDeclaration().toSimpleDeclaration(),
        pCommand.getVariableDeclaration().toASTString());
  }

  private Pair<ADeclaration, String> parseGlobalConstant(SvLibDeclareConstCommand pCommand) {
    return Pair.of(
        pCommand.getVariable().toSimpleDeclaration(), pCommand.getVariable().toASTString());
  }

  private CFANode newNodeAddedToBuilder(
      SvLibFunctionDeclaration pProcedure, Consumer<CFANode> pMetadataFunctionAllNodes) {
    CFANode newNode = new CFANode(pProcedure);
    pMetadataFunctionAllNodes.accept(newNode);
    return newNode;
  }

  private Pair<FunctionEntryNode, FunctionExitNode> newFunctionNodesWithMetadataTracking(
      SvLibFunctionDeclaration pFunctionDeclaration,
      SvLibVariableDeclarationTuple pReturnValues,
      Consumer<FunctionEntryNode> pMetadataFunctionEntryNodes,
      Consumer<CFANode> pMetadataFunctionAllNodes) {
    FunctionExitNode functionExitNode = new FunctionExitNode(pFunctionDeclaration);
    FunctionEntryNode functionEntryNode =
        new SvLibProcedureEntryNode(
            pFunctionDeclaration.getFileLocation(),
            functionExitNode,
            pFunctionDeclaration,
            pReturnValues);

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
    SvLibFunctionDeclaration functionDeclaration = procedureDeclaration.toSimpleDeclaration();
    Pair<FunctionEntryNode, FunctionExitNode> functionNodes =
        newFunctionNodesWithMetadataTracking(
            functionDeclaration,
            new SvLibVariableDeclarationTuple(
                FileLocation.DUMMY,
                transformedImmutableListCopy(
                    procedureDeclaration.getReturnValues(),
                    SvLibParsingParameterDeclaration::toVariableDeclaration)),
            x -> {},
            node -> allNodesCollector.add(node));

    FunctionExitNode functionExitNode = functionNodes.getSecondNotNull();
    FunctionEntryNode functionEntryNode = functionNodes.getFirstNotNull();

    // Add this in order to be able to export the statement contracts later on, since the
    // abstractions happen at the function entry nodes and not for the first node in a statement.
    nodesToTagReferences.putAll(functionEntryNode, pCommand.getBody().getTagReferences());

    // Declare the local and output variables
    CFANode currentStartingNode = functionEntryNode;
    CFANode newNode;
    for (SvLibParsingParameterDeclaration localVar :
        FluentIterable.concat(
            procedureDeclaration.getLocalVariables(), procedureDeclaration.getReturnValues())) {
      newNode = newNodeAddedToBuilder(functionDeclaration, node -> allNodesCollector.add(node));
      CFAEdge declEdge =
          new SvLibDeclarationEdge(
              localVar.toASTString(),
              FileLocation.DUMMY,
              currentStartingNode,
              newNode,
              localVar.toVariableDeclaration());
      CFACreationUtils.addEdgeToCFA(declEdge, logger);
      currentStartingNode = newNode;
    }

    nodesToActualProcedureDefinitionEnd.put(currentStartingNode, procedureDeclaration);

    SvLibStatementToCfaVisitor statementVisitor =
        new SvLibStatementToCfaVisitor(
            currentStartingNode,
            procedureDeclaration,
            logger,
            functionExitNode,
            nodeToTagAnnotations,
            nodesToTagReferences,
            gotoNodesToLabels,
            labelsToNodes,
            allNodesCollector,
            tagReferencesToAnnotations.build(),
            nodesToActualHavocStatementEnd);

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

  private static SvLibTagProperty instantiateTagProperty(
      SvLibScope pScope, SvLibTagProperty pTagProperty) {
    SvLibIdTermReplacer variableInstantiation =
        new SvLibIdTermReplacer() {

          @Override
          public SvLibRelationalTerm replace(SvLibIdTerm pIdTerm) {
            if (pIdTerm.getDeclaration().getType().equals(new SvLibAnyType())) {
              return new SvLibIdTerm(
                  pScope.getVariable(pIdTerm.getDeclaration().getName()).toSimpleDeclaration(),
                  FileLocation.DUMMY);
            } else {
              return pIdTerm;
            }
          }
        };

    return switch (pTagProperty) {
      case SvLibCheckTrueTag pCheckTrueTag ->
          new SvLibCheckTrueTag(
              pCheckTrueTag.getTerm().accept(variableInstantiation),
              pCheckTrueTag.getFileLocation());
      case SvLibRequiresTag pRequiresTag ->
          new SvLibRequiresTag(
              (SvLibTerm) pRequiresTag.getTerm().accept(variableInstantiation),
              pRequiresTag.getFileLocation());
      case SvLibEnsuresTag pEnsuresTag ->
          new SvLibEnsuresTag(
              pEnsuresTag.getTerm().accept(variableInstantiation), pEnsuresTag.getFileLocation());
      case SvLibInvariantTag pInvariantTag ->
          new SvLibInvariantTag(
              pInvariantTag.getTerm().accept(variableInstantiation),
              pInvariantTag.getFileLocation());
    };
  }

  public ParseResult buildCfaFromScript(SvLibParsingResult pParsingResult)
      throws SvLibParserException {
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
    SvLibFunctionDeclaration mainFunctionDeclaration =
        SvLibFunctionDeclaration.mainFunctionDeclaration();
    String mainFunctionName = mainFunctionDeclaration.getName();
    Pair<FunctionEntryNode, FunctionExitNode> mainFunctionNodes =
        newFunctionNodesWithMetadataTracking(
            mainFunctionDeclaration,
            new SvLibVariableDeclarationTuple(FileLocation.DUMMY, ImmutableList.of()),
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

    // Get the map from tags to their scopes
    ImmutableMap<SvLibTagReference, SvLibScope> tagReferenceToScope =
        pParsingResult.tagReferenceScopes();

    ImmutableMap.Builder<SvLibFunctionDeclaration, SvLibProcedureDeclaration>
        functionToProcedureDeclaration = ImmutableMap.builder();

    // Go through all the commands in the script and parse them.
    List<SvLibCommand> commands = pParsingResult.script().getCommands();
    int indexOfFirstVerifyCall = -1;

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

          String functionName = procedureDeclaration.getName();
          functions.put(functionName, functionDefinitionParseResult.getFirstNotNull());
          cfaNodes.putAll(functionName, functionDefinitionParseResult.getSecondNotNull());

          functionToProcedureDeclaration.put(
              procedureDeclaration.toSimpleDeclaration(), procedureDeclaration);
        }
        case SvLibVerifyCallCommand pVerifyCallCommand -> {
          // In theory the idea behind SV-LIB is to have an interactive shell with the ability to
          // talk
          // between the verifier and the user. In this case we do a simplification to match
          // CPAchecker's architecture better by only accepting finished scripts and not enabling an
          // incremental/dialgue mode.
          // The simplification makes it such that we will create an artificial main function which
          // calls all the functions to be verified with the corresponding parameters.
          SvLibProcedureDeclaration procedureDeclaration =
              pVerifyCallCommand.getProcedureDeclaration();
          SvLibFunctionDeclaration functionDeclaration = procedureDeclaration.toSimpleDeclaration();
          SvLibFunctionCallAssignmentStatement functionCallStatement =
              new SvLibFunctionCallAssignmentStatement(
                  FileLocation.DUMMY,
                  new SvLibIdTermTuple(
                      FileLocation.DUMMY,
                      ImmutableList.of()), // No lvalues, since we ignore return values here
                  new SvLibFunctionCallExpression(
                      FileLocation.DUMMY,
                      functionDeclaration.getType(),
                      new SvLibIdTerm(functionDeclaration, FileLocation.DUMMY),
                      pVerifyCallCommand.getTerms(),
                      functionDeclaration));

          CFANode successorNode =
              newNodeAddedToBuilder(
                  mainFunctionDeclaration, node -> cfaNodes.put(mainFunctionName, node));

          CFAEdge procedureCallEdge =
              new SvLibStatementEdge(
                  functionCallStatement.toASTString(),
                  functionCallStatement,
                  FileLocation.DUMMY,
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
          //        A bi directional map would be much better here.
          for (Entry<CFANode, SvLibTagReference> entry : nodesToTagReferences.build().entries()) {
            CFANode node = entry.getKey();
            SvLibTagReference tagReference = entry.getValue();
            if (tagReference.getTagName().equals(tagName)) {
              for (SvLibTagProperty tagProperty : tagProperties) {
                nodeToTagAnnotations.put(
                    node,
                    instantiateTagProperty(
                        Objects.requireNonNull(tagReferenceToScope.get(tagReference)),
                        tagProperty));
              }
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
          // For all options we simply add them to the SMT-LIB commands.
          smtLibCommandsBuilder.add(pSvLibSetOptionCommand);
        }
        case SvLibSelectTraceCommand pSvLibSelectTraceCommand -> {
          throw new SvLibParserException(
              "Select trace commands are not yet supported in CFA parsing.");
        }
        case SvLibSetInfoCommand pSvLibSetInfoCommand -> {
          // we ignore set-info commands for now
        }
        case SmtLibDefineFunCommand pSmtLibDefineFunCommand -> {
          smtLibCommandsBuilder.add(pSmtLibDefineFunCommand);
        }
        case SmtLibDefineFunRecCommand pSmtLibDefineFunRecCommand -> {
          smtLibCommandsBuilder.add(pSmtLibDefineFunRecCommand);
        }
        case SmtLibDefineFunsRecCommand pSmtLibDefineFunsRecCommand -> {
          smtLibCommandsBuilder.add(pSmtLibDefineFunsRecCommand);
        }
      }
    }

    Verify.verify(indexOfFirstVerifyCall >= 0, "There must be at least one verify call command");

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
            tagReferenceToScope,
            functionToProcedureDeclaration.buildOrThrow(),
            nodesToActualProcedureDefinitionEnd.buildOrThrow(),
            nodesToActualHavocStatementEnd.buildOrThrow(),
            nodeToTagAnnotations.build(),
            nodesToTagReferences.build()));
  }
}
