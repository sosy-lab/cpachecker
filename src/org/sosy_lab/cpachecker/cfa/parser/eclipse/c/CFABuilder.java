// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Verify;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroExpansion;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.util.SyntacticBlock;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;

/**
 * Builder to traverse AST.
 *
 * <p>After instantiating this class, call {@link #analyzeTranslationUnit(IASTTranslationUnit,
 * String, Scope)} once for each translation unit that should be used and finally call {@link
 * #createCFA()}.
 */
class CFABuilder extends ASTVisitor {

  // Data structures for handling function declarations
  private record FunctionsOfTranslationUnit(
      List<IASTFunctionDefinition> getFirst, String fileName, GlobalScope scope) {}

  private final List<FunctionsOfTranslationUnit> functionDeclarations = new ArrayList<>();
  private final NavigableMap<String, FunctionEntryNode> cfas = new TreeMap<>();
  private final TreeMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private final List<String> eliminateableDuplicates = new ArrayList<>();

  // Data structure for storing global declarations
  private record GlobalDeclaration(
      ADeclaration declaration, String rawSignature, GlobalScope scope) {}

  private final List<GlobalDeclaration> globalDeclarations = new ArrayList<>();
  private final List<Pair<ADeclaration, String>> globalDecls = new ArrayList<>();

  // Data structure for checking amount of initializations per global variable
  private final Set<String> globalInitializedVariables = new HashSet<>();

  // Here we keep track of all occurrences of _Atomic that we found but did not handle yet.
  // The set is filled by this class and then passed to ASTConverter, which removes handled cases.
  // The reason for this is that we want to detect cases like "int * _Atomic a;" where we do not see
  // the _Atomic in the AST nodes when converting them.
  private final Set<FileLocation> unhandledAtomicOccurrences = new HashSet<>();

  private final List<AcslComment> acslComments = new ArrayList<>();
  private final List<SyntacticBlock> blocks = new ArrayList<>();

  private final List<Path> parsedFiles = new ArrayList<>();

  private GlobalScope fileScope = new GlobalScope();
  private Scope artificialScope;
  private ProgramDeclarations programDeclarations = new ProgramDeclarations();
  private ASTConverter astCreator;
  private final ParseContext parseContext;

  private final EclipseCParserOptions options;
  private final MachineModel machine;
  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CheckBindingVisitor checkBinding;

  private boolean encounteredAsm = false;
  private Sideassignments sideAssignmentStack = null;

  public CFABuilder(
      EclipseCParserOptions pOptions,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ParseContext pParseContext,
      MachineModel pMachine) {
    options = pOptions;
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
    parseContext = pParseContext;
    machine = pMachine;

    checkBinding = new CheckBindingVisitor(pLogger);

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitProblems = true;
    shouldVisitTranslationUnit = true;
  }

  public void analyzeTranslationUnit(
      IASTTranslationUnit ast, String staticVariablePrefix, Scope pFallbackScope)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();

    if (!isNullOrEmpty(ast.getFilePath())) {
      parsedFiles.add(Path.of(ast.getFilePath()));
    }
    sideAssignmentStack = new Sideassignments();
    artificialScope = pFallbackScope;
    fileScope =
        new GlobalScope(
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            parseContext,
            programDeclarations,
            staticVariablePrefix,
            artificialScope);
    astCreator =
        new ASTConverter(
            options,
            fileScope,
            logger,
            parseContext,
            machine,
            staticVariablePrefix,
            sideAssignmentStack,
            unhandledAtomicOccurrences);
    functionDeclarations.add(
        new FunctionsOfTranslationUnit(new ArrayList<>(), staticVariablePrefix, fileScope));

    // Fill unhandledAtomicOccurrences. EclipseCdtWrapper makes sure that all "_Atomic" are replaced
    // by "__attribute__((__CPAchecker_Atomic__))", via a macro. So we can find all places where
    // this macro was applied.
    for (IASTPreprocessorMacroExpansion exp : ast.getMacroExpansions()) {
      if (exp.getMacroReference().toString().equals("_Atomic")) {
        unhandledAtomicOccurrences.add(astCreator.getLocation(exp));
      }
    }

    ast.accept(this);

    if (options.shouldCollectACSLAnnotations()) {
      for (IASTComment comment : ast.getComments()) {
        String commentString = String.valueOf(comment.getComment());
        if (commentString.startsWith("/*@") || commentString.startsWith("//@")) {
          acslComments.add(
              new AcslComment(
                  astCreator.getLocation(comment), AcslParser.stripCommentMarker(commentString)));
        }
      }
    }

    shutdownNotifier.shutdownIfNecessary();
  }

  @Override
  public int visit(IASTDeclaration declaration) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    sideAssignmentStack.enterBlock();

    return switch (declaration) {
      case IASTSimpleDeclaration iASTSimpleDeclaration ->
          handleSimpleDeclaration(iASTSimpleDeclaration);

      case IASTFunctionDefinition fd -> {
        functionDeclarations.getLast().getFirst().add(fd);
        // add forward declaration to list of global declarations
        CFunctionDeclaration functionDefinition = astCreator.convert(fd);
        if (sideAssignmentStack.hasPreSideAssignments()
            || sideAssignmentStack.hasPostSideAssignments()) {
          throw parseContext.parseError("Function definition has side effect", fd);
        }
        fileScope.registerFunctionDeclaration(functionDefinition);
        if (!eliminateableDuplicates.contains(functionDefinition.toASTString())) {
          globalDeclarations.add(
              new GlobalDeclaration(
                  functionDefinition,
                  fd.getDeclSpecifier().getRawSignature()
                      + " "
                      + fd.getDeclarator().getRawSignature(),
                  fileScope));
          globalDecls.add(
              Pair.of(
                  functionDefinition,
                  fd.getDeclSpecifier().getRawSignature()
                      + " "
                      + fd.getDeclarator().getRawSignature()));
          eliminateableDuplicates.add(functionDefinition.toASTString());
        }
        sideAssignmentStack.leaveBlock();
        yield PROCESS_SKIP;
      }
      case IASTProblemDeclaration iASTProblemDeclaration -> {
        visit(iASTProblemDeclaration.getProblem());
        sideAssignmentStack.leaveBlock();
        yield PROCESS_SKIP;
      }
      case IASTASMDeclaration asm -> {
        // TODO Assembler code is ignored here
        encounteredAsm = true;
        @Nullable IASTFileLocation fileloc = declaration.getFileLocation();
        if (fileloc != null) {
          logger.log(
              Level.FINER,
              "Ignoring inline assembler code at line",
              fileloc.getStartingLineNumber());
        } else {
          logger.log(Level.FINER, "Ignoring inline assembler code at unknown line.");
        }
        sideAssignmentStack.leaveBlock();
        yield PROCESS_SKIP;
      }
      default ->
          throw parseContext.parseError(
              "Unknown declaration type " + declaration.getClass().getSimpleName(), declaration);
    };
  }

  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd) {

    // these are unneccesary semicolons which would cause an abort of CPAchecker
    if (sd.getDeclarators().length == 0
        && sd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier) {
      sideAssignmentStack.leaveBlock();
      return PROCESS_SKIP;
    }

    final List<CDeclaration> newDs = astCreator.convert(sd);
    assert !newDs.isEmpty();

    if (sideAssignmentStack.hasConditionalExpression()
        || sideAssignmentStack.hasPostSideAssignments()) {
      throw parseContext.parseError("Initializer of global variable has side effect", sd);
    }

    String rawSignature = sd.getRawSignature();

    for (CAstNode astNode : sideAssignmentStack.getAndResetPreSideAssignments()) {
      if (astNode instanceof CComplexTypeDeclaration) {
        // already registered
        globalDeclarations.add(
            new GlobalDeclaration((ADeclaration) astNode, rawSignature, fileScope));
        globalDecls.add(Pair.of((ADeclaration) astNode, rawSignature));
      } else if (astNode instanceof CVariableDeclaration cVariableDeclaration) {
        // If the initializer of a global struct contains a type-id expression,
        // a temporary variable is created, and we need to support this.
        // We detect this case if the initializer of the temp variable is an initializer list.
        CInitializer initializer = cVariableDeclaration.getInitializer();
        if (initializer instanceof CInitializerList) {
          globalDeclarations.add(
              new GlobalDeclaration((ADeclaration) astNode, rawSignature, fileScope));
          globalDecls.add(Pair.of((ADeclaration) astNode, rawSignature));
        } else {
          throw parseContext.parseError("Initializer of global variable has side effect", sd);
        }

      } else {
        throw parseContext.parseError("Initializer of global variable has side effect", sd);
      }
    }

    for (CDeclaration newD : newDs) {
      boolean used = true;

      switch (newD) {
        case CVariableDeclaration cVariableDeclaration -> {
          CInitializer init = cVariableDeclaration.getInitializer();
          if (init != null) {
            init.accept(checkBinding);

            // save global initialized variable in map to check duplicates
            if (!globalInitializedVariables.add(newD.getName())) {
              throw parseContext.parseError(
                  "Variable " + newD.getName() + " initialized for the second time", newD);
            }
          }
          fileScope.registerDeclaration(newD);
        }
        case CFunctionDeclaration cFunctionDeclaration ->
            fileScope.registerFunctionDeclaration(cFunctionDeclaration);
        case CComplexTypeDeclaration cComplexTypeDeclaration ->
            used = fileScope.registerTypeDeclaration(cComplexTypeDeclaration);
        case CTypeDefDeclaration cTypeDefDeclaration ->
            used = fileScope.registerTypeDeclaration(cTypeDefDeclaration);
        default -> throw new AssertionError();
      }

      if (used && !eliminateableDuplicates.contains(newD.toASTString())) {
        globalDeclarations.add(new GlobalDeclaration(newD, rawSignature, fileScope));
        globalDecls.add(Pair.of(newD, rawSignature));
        eliminateableDuplicates.add(newD.toASTString());
      }
    }

    sideAssignmentStack.leaveBlock();
    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  // Method to handle visiting a parsing problem.  Hopefully none exist
  @Override
  public int visit(IASTProblem problem) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    throw parseContext.parseError(problem);
  }

  public ParseResult createCFA() throws CParserException, InterruptedException {
    // in case we
    if (functionDeclarations.size() > 1) {
      programDeclarations.completeUncompletedElaboratedTypes();
    }

    for (GlobalDeclaration decl : globalDeclarations) {
      FillInAllBindingsVisitor fillInAllBindingsVisitor =
          new FillInAllBindingsVisitor(decl.scope(), programDeclarations);
      ((CDeclaration) decl.declaration()).getType().accept(fillInAllBindingsVisitor);
    }

    ImmutableMap.Builder<CFANode, Set<AVariableDeclaration>> cfaNodeToAstLocalVariablesInScope =
        ImmutableMap.builder();
    ImmutableMap.Builder<CFANode, Set<AParameterDeclaration>> cfaNodeToAstParametersInScope =
        ImmutableMap.builder();
    for (FunctionsOfTranslationUnit functionDeclaration : functionDeclarations) {
      GlobalScope actScope = functionDeclaration.scope();

      // giving these variables as parameters to the handleFunctionDefinition method
      // increases performance drastically, as there is no need to create the Immutable
      // Map each time
      ImmutableMap<String, CFunctionDeclaration> actFunctions = actScope.getFunctions();
      ImmutableMap<String, CComplexTypeDeclaration> actTypes = actScope.getTypes();
      ImmutableMap<String, CTypeDefDeclaration> actTypeDefs = actScope.getTypeDefs();
      ImmutableMap<String, CSimpleDeclaration> actVars = actScope.getGlobalVars();
      for (IASTFunctionDefinition declaration : functionDeclaration.getFirst()) {
        handleFunctionDefinition(
            actScope,
            functionDeclaration.fileName(),
            declaration,
            actFunctions,
            actTypes,
            actTypeDefs,
            actVars,
            cfaNodeToAstLocalVariablesInScope,
            cfaNodeToAstParametersInScope);
      }
    }

    if (encounteredAsm) {
      logger.log(Level.WARNING, "Inline assembler ignored, analysis is probably unsound!");
    }

    if (checkBinding.foundUndefinedIdentifiers()) {
      throw new CParserException(
          "Invalid C code because of undefined identifiers mentioned above.");
    }

    if (!unhandledAtomicOccurrences.isEmpty()) {
      FileLocation firstUnhandledAtomic = Collections.min(unhandledAtomicOccurrences);
      throw new CParserException(
          "Found %d cases of _Atomic in unsupported locations, first one is in %s."
              .formatted(unhandledAtomicOccurrences.size(), firstUnhandledAtomic));
    }

    ParseResult result;

    result = new ParseResult(cfas, cfaNodes, globalDecls, parsedFiles);

    result =
        result.withInScopeInformation(
            // We want to explicitly throw an error if a
            // key was added more than once, since this would be a bug
            cfaNodeToAstLocalVariablesInScope.buildOrThrow(),
            cfaNodeToAstParametersInScope.buildOrThrow());
    result = result.withAcslComments(acslComments, blocks);

    return result;
  }

  private void handleFunctionDefinition(
      final GlobalScope actScope,
      String fileName,
      IASTFunctionDefinition declaration,
      ImmutableMap<String, CFunctionDeclaration> functions,
      ImmutableMap<String, CComplexTypeDeclaration> types,
      ImmutableMap<String, CTypeDefDeclaration> typedefs,
      ImmutableMap<String, CSimpleDeclaration> globalVars,
      ImmutableMap.Builder<CFANode, Set<AVariableDeclaration>> cfaNodeToAstLocalVariablesInScope,
      ImmutableMap.Builder<CFANode, Set<AParameterDeclaration>> cfaNodeToAstParametersInScope)
      throws InterruptedException {

    FunctionScope localScope =
        new FunctionScope(functions, types, typedefs, globalVars, fileName, artificialScope);
    CFAFunctionBuilder functionBuilder =
        new CFAFunctionBuilder(
            options,
            logger,
            shutdownNotifier,
            localScope,
            parseContext,
            machine,
            fileName,
            sideAssignmentStack,
            checkBinding,
            cfaNodeToAstLocalVariablesInScope,
            cfaNodeToAstParametersInScope,
            unhandledAtomicOccurrences);

    declaration.accept(functionBuilder);

    // check whether an interrupt happened while parsing
    shutdownNotifier.shutdownIfNecessary();

    FunctionEntryNode startNode = functionBuilder.getStartNode();
    String functionName = startNode.getFunctionName();

    if (cfas.containsKey(functionName)) {
      throw new CFAGenerationRuntimeException(
          "Duplicate function "
              + functionName
              + " in "
              + startNode.getFileLocation()
              + " and "
              + cfas.get(functionName).getFileLocation());
    }
    cfas.put(functionName, startNode);
    cfaNodes.putAll(functionName, functionBuilder.getCfaNodes());
    globalDeclarations.addAll(
        Collections2.transform(
            functionBuilder.getGlobalDeclarations(),
            pInput -> new GlobalDeclaration(pInput.getFirst(), pInput.getSecond(), actScope)));
    globalDecls.addAll(functionBuilder.getGlobalDeclarations());

    encounteredAsm |= functionBuilder.didEncounterAsm();
    blocks.addAll(functionBuilder.getBlocks());
    functionBuilder.finish();
  }

  public ParseResult matchAcslCommentsToNodes(ParseResult pResult, AstCfaRelation pAstCfaRelation) {

    /*
    Find the CfaNode for each Acsl Comment

    Step 1: For regular statement annotations (e.g. assertions, loop invariants)
    we can get the Cfa Node from the tightest statement for the comment location.
    If "getTightestStatementForStarting()" fails, the comment is not a regular statement annotation.
     */
    ImmutableSet.Builder<AcslComment> notARegularAnnotationBuilder = ImmutableSet.builder();

    for (AcslComment comment : pResult.acslComments().orElseThrow()) {
      Optional<CFANode> nodeForComment = nodeForRegularAnnotation(comment, pAstCfaRelation);
      if (nodeForComment.isPresent()) {
        comment.updateCfaNode(nodeForComment.orElseThrow());
      } else {
        notARegularAnnotationBuilder.add(comment);
      }
    }
    ImmutableSet<AcslComment> notRegularAnnotations = notARegularAnnotationBuilder.build();

    /*
    Step 2: Search the reamining Acsl Commnets, that are not statement annotations for function contracts
    A function contract annotation comes immediately before the function declaration
     */
    ImmutableSet.Builder<AcslComment> notAFunctionContractBuilder = ImmutableSet.builder();

    for (AcslComment comment : notRegularAnnotations) {
      Optional<FunctionEntryNode> functionEntryNode =
          nodeForFunctionContract(comment, pAstCfaRelation, pResult.acslComments().orElseThrow());
      if (functionEntryNode.isPresent()) {
        comment.updateCfaNode(functionEntryNode.orElseThrow());
      } else {
        notAFunctionContractBuilder.add(comment);
      }
    }
    ImmutableSet<AcslComment> notFunctionContracts = notAFunctionContractBuilder.build();
    // ToDo: Handle special cases
    Verify.verify(notFunctionContracts.isEmpty());

    return pResult.withAcslComments(acslComments, blocks);
  }

  private Optional<CFANode> nodeForRegularAnnotation(
      AcslComment pComment, AstCfaRelation pAstCfaRelation) {
    FileLocation commentLocation = pComment.getFileLocation();
    String commentString = pComment.getComment();

    Optional<ASTElement> tightestStatement =
        pAstCfaRelation.getElemForStarting(
            commentLocation.getStartingLineNumber(),
            OptionalInt.of(commentLocation.getStartColumnInLine()));
    if (tightestStatement.isPresent() && !tightestStatement.orElseThrow().edges().isEmpty()) {
      FluentIterable<CFANode> predecessors =
          FluentIterable.from(tightestStatement.orElseThrow().edges())
              .transform(e -> e.getPredecessor());
      FluentIterable<CFANode> successors =
          FluentIterable.from(tightestStatement.orElseThrow().edges())
              .transform(e -> e.getSuccessor());
      List<CFANode> nodesForComment =
          successors
              .filter(n -> !predecessors.contains(n) && !(n instanceof FunctionExitNode))
              .toList();

      // An AcslComment should belong to exactly one CfaNode
      if (!nodesForComment.isEmpty()) {
        Optional<CFANode> node = Optional.of(nodesForComment.getFirst());

        if (commentString.startsWith("loop invariant")) {
          // Get the next loop head if we are dealing with a loop invariat
          Optional<IterationElement> it =
              pAstCfaRelation.getTightestIterationStructureForNode(node.orElseThrow());
          if (it.isPresent()) {
            node = it.orElseThrow().getLoopHead();
          }
        }
        return node;
      }
    }
    return Optional.empty();
  }

  private Optional<FunctionEntryNode> nodeForFunctionContract(
      AcslComment pComment, AstCfaRelation pAstCfaRelation, List<AcslComment> pAllComments) {
    FileLocation nextLocation =
        pAstCfaRelation.nextStartStatementLocation(pComment.getFileLocation().getNodeOffset());
    if (nextLocation.isRealLocation() && pComment.noCommentInBetween(nextLocation, pAllComments)) {
      Optional<CFANode> nextNode =
          pAstCfaRelation.getNodeForStatementLocation(
              nextLocation.getStartingLineNumber(), nextLocation.getStartColumnInLine());
      if (nextNode.isPresent()) {
        ImmutableList<CFAEdge> edges =
            nextNode
                .orElseThrow()
                .getEnteringEdges()
                .filter(e -> e.getPredecessor() instanceof FunctionEntryNode)
                .toList();
        if (edges.size() == 1 && edges.getFirst().getPredecessor() instanceof FunctionEntryNode f) {
          return Optional.of(f);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public int leave(IASTTranslationUnit ast) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    return PROCESS_CONTINUE;
  }
}
