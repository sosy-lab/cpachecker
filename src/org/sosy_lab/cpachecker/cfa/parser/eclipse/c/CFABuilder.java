/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Builder to traverse AST.
 *
 * After instantiating this class,
 * call {@link #analyzeTranslationUnit(IASTTranslationUnit, String)}
 * once for each translation unit that should be used
 * and finally call {@link #createCFA()}.
 */
class CFABuilder extends ASTVisitor {

  // Data structures for handling function declarations
  private final List<Pair<List<IASTFunctionDefinition>, String>> functionDeclarations = new ArrayList<>();
  private final Map<String, FunctionEntryNode> cfas = new HashMap<>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private final List<String> eliminateableDuplicates = new ArrayList<>();

  // Data structure for storing global declarations
  private final List<Pair<org.sosy_lab.cpachecker.cfa.ast.IADeclaration, String>> globalDeclarations = Lists.newArrayList();

  // Data structure for checking amount of initializations per global variable
  private final Set<String> globalInitializedVariables = Sets.newHashSet();


  private GlobalScope fileScope = new GlobalScope();
  private GlobalScope globalScope = new GlobalScope();
  private ASTConverter astCreator;

  private final MachineModel machine;
  private final LogManager logger;
  private final CheckBindingVisitor checkBinding;

  private final Configuration config;

  private boolean encounteredAsm = false;

  public CFABuilder(Configuration config, LogManager pLogger, MachineModel pMachine) throws InvalidConfigurationException {
    logger = pLogger;
    machine = pMachine;
    this.config = config;

    checkBinding = new CheckBindingVisitor(pLogger);

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitProblems = true;
    shouldVisitTranslationUnit = true;
  }

  public void analyzeTranslationUnit(IASTTranslationUnit ast, String staticVariablePrefix) throws InvalidConfigurationException {
    fileScope = new GlobalScope();
    astCreator = new ASTConverter(config, fileScope, logger, machine, staticVariablePrefix, true);
    functionDeclarations.add(Pair.of((List<IASTFunctionDefinition>)new ArrayList<IASTFunctionDefinition>(), staticVariablePrefix));

    ast.accept(this);
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public int visit(IASTDeclaration declaration) {
    IASTFileLocation fileloc = declaration.getFileLocation();

    if (declaration instanceof IASTSimpleDeclaration) {
      return handleSimpleDeclaration((IASTSimpleDeclaration)declaration, fileloc);

    } else if (declaration instanceof IASTFunctionDefinition) {
      IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
      functionDeclarations.get(functionDeclarations.size() -1).getFirst().add(fd);

      // add forward declaration to list of global declarations
      CFunctionDeclaration functionDefinition = astCreator.convert(fd);
      if (!astCreator.getAndResetPreSideAssignments().isEmpty()
          || !astCreator.getAndResetPostSideAssignments().isEmpty()) {
        throw new CFAGenerationRuntimeException("Function definition has side effect", fd);
      }

      fileScope.registerFunctionDeclaration(functionDefinition);
      if(!eliminateableDuplicates.contains(functionDefinition.toASTString())) {
        globalDeclarations.add(Pair.of((IADeclaration)functionDefinition, fd.getDeclSpecifier().getRawSignature() + " " + fd.getDeclarator().getRawSignature()));
        eliminateableDuplicates.add(functionDefinition.toASTString());
      }

      return PROCESS_SKIP;

    } else if (declaration instanceof IASTProblemDeclaration) {
      // CDT parser struggles on GCC's __attribute__((something)) constructs
      // because we use C99 as default.
      // Either insert the following macro before compiling with CIL:
      // #define  __attribute__(x)  /*NOTHING*/
      // or insert "parser.dialect = GNUC" into properties file
      visit(((IASTProblemDeclaration)declaration).getProblem());
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTASMDeclaration) {
      // TODO Assembler code is ignored here
      encounteredAsm = true;
      logger.log(Level.FINER, "Ignoring inline assembler code at line", fileloc.getStartingLineNumber());
      return PROCESS_SKIP;

    } else {
      throw new CFAGenerationRuntimeException("Unknown declaration type "
          + declaration.getClass().getSimpleName(), declaration);
    }
  }

  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd,
      final IASTFileLocation fileloc) {

    //these are unneccesary semicolons which would cause an abort of CPAchecker
    if (sd.getDeclarators().length == 0  && sd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier) {
      return PROCESS_SKIP;
    }

    final List<CDeclaration> newDs = astCreator.convert(sd);
    assert !newDs.isEmpty();

    if (!astCreator.getAndResetConditionalExpressions().isEmpty()
        || !astCreator.getAndResetPostSideAssignments().isEmpty()) {
      throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", sd);
    }

    String rawSignature = sd.getRawSignature();

    for (CAstNode astNode : astCreator.getAndResetPreSideAssignments()) {
      if (astNode instanceof CComplexTypeDeclaration) {
        // already registered
        globalDeclarations.add(Pair.of((IADeclaration)astNode, rawSignature));
      } else {
        throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", sd);
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
            throw new CFAGenerationRuntimeException("Variable " + newD.getName()
                + " initialized for the second time", newD);
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

      if (used) {
        if (!eliminateableDuplicates.contains(newD.toASTString())) {
          globalDeclarations.add(Pair.of((IADeclaration)newD, rawSignature));
          eliminateableDuplicates.add(newD.toASTString());
        }
      }
    }

    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public int visit(IASTProblem problem) {
    throw new CFAGenerationRuntimeException(problem);
  }

  public ParseResult createCFA() {
    ImmutableMap<String, CFunctionDeclaration> functions = globalScope.getFunctions();
    ImmutableMap<String, CComplexTypeDeclaration> types = globalScope.getTypes();
    ImmutableMap<String, CSimpleDeclaration> globalVars = globalScope.getGlobalVars();

    FillInAllBindingsVisitor fillInAllBindingsVisitor = new FillInAllBindingsVisitor(globalScope);
    for (IADeclaration decl : from(globalDeclarations).transform(Pair.<IADeclaration>getProjectionToFirst())) {
      ((CDeclaration)decl).getType().accept(fillInAllBindingsVisitor);
    }

    for (Pair<List<IASTFunctionDefinition>, String> pair : functionDeclarations) {
      for (IASTFunctionDefinition declaration : pair.getFirst()) {
        FunctionScope localScope = new FunctionScope(functions, types, globalVars);
        CFAFunctionBuilder functionBuilder;

        try {
          functionBuilder = new CFAFunctionBuilder(config, logger, localScope, machine, pair.getSecond());
        } catch (InvalidConfigurationException e) {
          throw new CFAGenerationRuntimeException("Invalid configuration");
        }

        declaration.accept(functionBuilder);

        FunctionEntryNode startNode = functionBuilder.getStartNode();
        String functionName = startNode.getFunctionName();

        if (cfas.containsKey(functionName)) {
          throw new CFAGenerationRuntimeException("Duplicate function " + functionName);
        }
        cfas.put(functionName, startNode);
        cfaNodes.putAll(functionName, functionBuilder.getCfaNodes());

        encounteredAsm |= functionBuilder.didEncounterAsm();
        functionBuilder.finish();
      }
    }

    if (encounteredAsm) {
      logger.log(Level.WARNING, "Inline assembler ignored, analysis is probably unsound!");
    }

    return new ParseResult(cfas, cfaNodes, globalDeclarations, Language.C);
  }

  @Override
  public int leave(IASTTranslationUnit ast) {
    Map<String, CSimpleDeclaration> globalVars = new HashMap<>();
    Map<String, CFunctionDeclaration> functions = new HashMap<>();
    Map<String, CComplexTypeDeclaration> types = new HashMap<>();
    Map<String, CTypeDefDeclaration> typedefs = new HashMap<>();

    globalVars.putAll(globalScope.getGlobalVars());
    functions.putAll(globalScope.getFunctions());
    types.putAll(globalScope.getTypes());
    typedefs.putAll(globalScope.getTypeDefs());

    globalVars.putAll(fileScope.getGlobalVars());
    functions.putAll(fileScope.getFunctions());
    typedefs.putAll(fileScope.getTypeDefs());

    //only add those composite types from the filescope to the globalscope,
    // where no type, or only an elaborated type without realtype was registered before
    for (String key : fileScope.getTypes().keySet().asList()) {
      CComplexTypeDeclaration type = types.get(key);

      if (type != null) {
        if (type.getType() instanceof CElaboratedType && ((CElaboratedType) type.getType()).getRealType() == null) {
          types.put(key, fileScope.getTypes().get(key));
        }
      } else {
        types.put(key, fileScope.getTypes().get(key));
      }
    }

    globalScope= new GlobalScope(globalVars, functions, types, typedefs);
    return PROCESS_CONTINUE;
  }
}