/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

/**
 * Builder to traverse AST.
 *
 * After instantiating this class,
 * call {@link #analyzeTranslationUnit(IASTTranslationUnit, String, Scope)}
 * once for each translation unit that should be used
 * and finally call {@link #createCFA()}.
 */
class CFABuilder extends ASTVisitor {

  // Data structures for handling function declarations
  private final List<Triple<List<IASTFunctionDefinition>, String, GlobalScope>> functionDeclarations = new ArrayList<>();
  private final SortedMap<String, FunctionEntryNode> cfas = new TreeMap<>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private final List<String> eliminateableDuplicates = new ArrayList<>();

  // Data structure for storing global declarations
  private final List<Triple<ADeclaration, String, GlobalScope>> globalDeclarations = Lists.newArrayList();
  private final List<Pair<ADeclaration, String>> globalDecls = Lists.newArrayList();

  // Data structure for checking amount of initializations per global variable
  private final Set<String> globalInitializedVariables = Sets.newHashSet();


  private GlobalScope fileScope = new GlobalScope();
  private Scope artificialScope;
  private ProgramDeclarations programDeclarations = new ProgramDeclarations();
  private ASTConverter astCreator;
  private final ParseContext parseContext;

  private final EclipseCParserOptions options;
  private final MachineModel machine;
  private final LogManagerWithoutDuplicates logger;
  private final CheckBindingVisitor checkBinding;

  private boolean encounteredAsm = false;
  private Sideassignments sideAssignmentStack = null;

  public CFABuilder(
      EclipseCParserOptions pOptions,
      LogManager pLogger,
      ParseContext pParseContext,
      MachineModel pMachine) {
    options = pOptions;
    logger = new LogManagerWithoutDuplicates(pLogger);
    parseContext = pParseContext;
    machine = pMachine;

    checkBinding = new CheckBindingVisitor(pLogger);

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitProblems = true;
    shouldVisitTranslationUnit = true;
  }

  public void analyzeTranslationUnit(
      IASTTranslationUnit ast, String staticVariablePrefix, Scope pFallbackScope) {
    sideAssignmentStack = new Sideassignments();
    artificialScope = pFallbackScope;
    fileScope =
        new GlobalScope(
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
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
            sideAssignmentStack);
    functionDeclarations.add(Triple.of((List<IASTFunctionDefinition>)new ArrayList<IASTFunctionDefinition>(), staticVariablePrefix, fileScope));

    ast.accept(this);
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public int visit(IASTDeclaration declaration) {
    sideAssignmentStack.enterBlock();

    if (declaration instanceof IASTSimpleDeclaration) {
      return handleSimpleDeclaration((IASTSimpleDeclaration)declaration);

    } else if (declaration instanceof IASTFunctionDefinition) {
      IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
      functionDeclarations.get(functionDeclarations.size() -1).getFirst().add(fd);

      // add forward declaration to list of global declarations
      CFunctionDeclaration functionDefinition = astCreator.convert(fd);
      if (sideAssignmentStack.hasPreSideAssignments()
          || sideAssignmentStack.hasPostSideAssignments()) {
        throw parseContext.parseError("Function definition has side effect", fd);
      }

      fileScope.registerFunctionDeclaration(functionDefinition);
      if (!eliminateableDuplicates.contains(functionDefinition.toASTString())) {
        globalDeclarations.add(Triple.of((ADeclaration)functionDefinition,
                                         fd.getDeclSpecifier().getRawSignature() + " " + fd.getDeclarator().getRawSignature(),
                                         fileScope));
        globalDecls.add(Pair.of((ADeclaration)functionDefinition,
                                         fd.getDeclSpecifier().getRawSignature() + " " + fd.getDeclarator().getRawSignature()));
        eliminateableDuplicates.add(functionDefinition.toASTString());
      }

      sideAssignmentStack.leaveBlock();
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTProblemDeclaration) {
      // CDT parser struggles on GCC's __attribute__((something)) constructs
      // because we use C99 as default.
      // Either insert the following macro before compiling with CIL:
      // #define  __attribute__(x)  /*NOTHING*/
      // or insert "parser.dialect = GNUC" into properties file
      visit(((IASTProblemDeclaration)declaration).getProblem());
      sideAssignmentStack.leaveBlock();
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTASMDeclaration) {
      // TODO Assembler code is ignored here
      encounteredAsm = true;
      @Nullable IASTFileLocation fileloc = declaration.getFileLocation();
      if (fileloc != null) {
        logger.log(Level.FINER, "Ignoring inline assembler code at line", fileloc.getStartingLineNumber());
      } else {
        logger.log(Level.FINER, "Ignoring inline assembler code at unknown line.");
      }
      sideAssignmentStack.leaveBlock();
      return PROCESS_SKIP;

    } else {
      throw parseContext.parseError(
          "Unknown declaration type " + declaration.getClass().getSimpleName(), declaration);
    }
  }

  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd) {

    //these are unneccesary semicolons which would cause an abort of CPAchecker
    if (sd.getDeclarators().length == 0  && sd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier) {
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
        globalDeclarations.add(Triple.of((ADeclaration)astNode, rawSignature, fileScope));
        globalDecls.add(Pair.of((ADeclaration)astNode, rawSignature));
      } else if (astNode instanceof CVariableDeclaration) {
        // If the initializer of a global struct contains a type-id expression,
        // a temporary variable is created and we need to support this.
        // We detect this case if the initializer of the temp variable is an initializer list.
        CInitializer initializer = ((CVariableDeclaration)astNode).getInitializer();
        if (initializer instanceof CInitializerList) {
          globalDeclarations.add(Triple.of((ADeclaration)astNode, rawSignature, fileScope));
          globalDecls.add(Pair.of((ADeclaration)astNode, rawSignature));
        } else {
          throw parseContext.parseError("Initializer of global variable has side effect", sd);
        }

      } else {
        throw parseContext.parseError("Initializer of global variable has side effect", sd);
      }
    }

    for (CDeclaration newD : newDs) {
      boolean used = true;

      if (newD instanceof CVariableDeclaration) {

        CInitializer init = ((CVariableDeclaration) newD).getInitializer();
        if (init != null) {
          init.accept(checkBinding);

          // save global initialized variable in map to check duplicates
          if (!globalInitializedVariables.add(newD.getName())) {
            throw parseContext.parseError(
                "Variable " + newD.getName() + " initialized for the second time", newD);
          }
        }

        fileScope.registerDeclaration(newD);
      } else if (newD instanceof CFunctionDeclaration) {
        fileScope.registerFunctionDeclaration((CFunctionDeclaration) newD);
      } else if (newD instanceof CComplexTypeDeclaration) {
        used = fileScope.registerTypeDeclaration((CComplexTypeDeclaration)newD);
      } else if (newD instanceof CTypeDefDeclaration) {
        used = fileScope.registerTypeDeclaration((CTypeDefDeclaration)newD);
      }

      if (used && !eliminateableDuplicates.contains(newD.toASTString())) {
        globalDeclarations.add(Triple.of((ADeclaration)newD, rawSignature, fileScope));
        globalDecls.add(Pair.of((ADeclaration)newD, rawSignature));
        eliminateableDuplicates.add(newD.toASTString());
      }
    }

    sideAssignmentStack.leaveBlock();
    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public int visit(IASTProblem problem) {
    throw parseContext.parseError(problem);
  }

  public ParseResult createCFA() throws CParserException {
    // in case we
    if (functionDeclarations.size() > 1) {
      programDeclarations.completeUncompletedElaboratedTypes();
    }

    for (Triple<ADeclaration, String, GlobalScope> decl : globalDeclarations) {
      FillInAllBindingsVisitor fillInAllBindingsVisitor = new FillInAllBindingsVisitor(decl.getThird(), programDeclarations);
      ((CDeclaration)decl.getFirst()).getType().accept(fillInAllBindingsVisitor);
    }

    for (Triple<List<IASTFunctionDefinition>, String, GlobalScope> triple : functionDeclarations) {
      GlobalScope actScope = triple.getThird();

      // giving these variables as parameters to the handleFunctionDefinition method
      // increases performance drastically, as there is no need to create the Immutable
      // Map each time
      ImmutableMap<String, CFunctionDeclaration> actFunctions = actScope.getFunctions();
      ImmutableMap<String, CComplexTypeDeclaration> actTypes = actScope.getTypes();
      ImmutableMap<String, CTypeDefDeclaration> actTypeDefs = actScope.getTypeDefs();
      ImmutableMap<String, CSimpleDeclaration> actVars = actScope.getGlobalVars();
      for (IASTFunctionDefinition declaration : triple.getFirst()) {
          handleFunctionDefinition(actScope,
                                   triple.getSecond(),
                                   declaration,
                                   actFunctions,
                                   actTypes,
                                   actTypeDefs,
                                   actVars);
      }
    }

    if (encounteredAsm) {
      logger.log(Level.WARNING, "Inline assembler ignored, analysis is probably unsound!");
    }

    if (checkBinding.foundUndefinedIdentifiers()) {
      throw new CParserException("Invalid C code because of undefined identifiers mentioned above.");
    }

    ParseResult result = new ParseResult(cfas,
                                         cfaNodes,
                                         globalDecls,
                                         Language.C);

    return result;
  }

  private void handleFunctionDefinition(final GlobalScope actScope,
                                        String fileName,
                                        IASTFunctionDefinition declaration,
                                        ImmutableMap<String, CFunctionDeclaration> functions,
                                        ImmutableMap<String, CComplexTypeDeclaration> types,
                                        ImmutableMap<String, CTypeDefDeclaration> typedefs,
                                        ImmutableMap<String, CSimpleDeclaration> globalVars) {

    FunctionScope localScope =
        new FunctionScope(functions, types, typedefs, globalVars, fileName, artificialScope);
    CFAFunctionBuilder functionBuilder =
        new CFAFunctionBuilder(
            options,
            logger,
            localScope,
            parseContext,
            machine,
            fileName,
            sideAssignmentStack,
            checkBinding);
    declaration.accept(functionBuilder);

    FunctionEntryNode startNode = functionBuilder.getStartNode();
    String functionName = startNode.getFunctionName();

    if (cfas.containsKey(functionName)) {
      throw new CFAGenerationRuntimeException("Duplicate function " + functionName
          + " in " + startNode.getFileLocation() + " and " + cfas.get(functionName).getFileLocation());
    }
    cfas.put(functionName, startNode);
    cfaNodes.putAll(functionName, functionBuilder.getCfaNodes());
    globalDeclarations.addAll(
        from(functionBuilder.getGlobalDeclarations())
            .transform(pInput -> Triple.of(pInput.getFirst(), pInput.getSecond(), actScope))
            .toList());
    globalDecls.addAll(functionBuilder.getGlobalDeclarations());

    encounteredAsm |= functionBuilder.didEncounterAsm();
    functionBuilder.finish();
  }

  @Override
  public int leave(IASTTranslationUnit ast) {
    return PROCESS_CONTINUE;
  }
}