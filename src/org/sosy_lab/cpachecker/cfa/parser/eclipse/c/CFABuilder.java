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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.cdt.core.dom.ast.IType;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import com.google.common.collect.ImmutableList;
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
  private final List<Pair<List<IASTFunctionDefinition>, Pair<String, GlobalScope>>> functionDeclarations = new ArrayList<>();
  private final Map<String, Set<String>> renamedTypes = new HashMap<>();
  private final Map<String, FunctionEntryNode> cfas = new HashMap<>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private final List<String> eliminateableDuplicates = new ArrayList<>();
  private final List<CElaboratedType> typesWithoutCompleteDeclaration = new ArrayList<>();

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
  private String staticVariablePrefix;
  private CatchAllGlobalTypesVisitor preBuildTypeChecker = null;
  private IASTTranslationUnit ast = null;
  private Sideassignments sideAssignmentStack = null;

  public CFABuilder(Configuration pConfig, LogManager pLogger,
      MachineModel pMachine) throws InvalidConfigurationException {

    logger = pLogger;
    machine = pMachine;
    config = pConfig;

    checkBinding = new CheckBindingVisitor(pLogger);

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitProblems = true;
    shouldVisitTranslationUnit = true;
  }

  public void analyzeTranslationUnit(IASTTranslationUnit ast, String staticVariablePrefix) throws InvalidConfigurationException {
    this.staticVariablePrefix = staticVariablePrefix;
    sideAssignmentStack = new Sideassignments();
    fileScope = new GlobalScope(new HashMap<String, CSimpleDeclaration>(),
                                new HashMap<String, CFunctionDeclaration>(),
                                new HashMap<String, CComplexTypeDeclaration>(),
                                new HashMap<String, CTypeDefDeclaration>(),
                                globalScope.getTypes().keySet());
    astCreator = new ASTConverter(config, fileScope, logger, machine, staticVariablePrefix, true, sideAssignmentStack);
    functionDeclarations.add(Pair.of((List<IASTFunctionDefinition>)new ArrayList<IASTFunctionDefinition>(), Pair.of(staticVariablePrefix, fileScope)));

    preBuildTypeChecker = null;
    this.ast = ast;
    ast.accept(this);
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public int visit(IASTDeclaration declaration) {
    sideAssignmentStack.enterBlock();
    IASTFileLocation fileloc = declaration.getFileLocation();

    if (declaration instanceof IASTSimpleDeclaration) {
      return handleSimpleDeclaration((IASTSimpleDeclaration)declaration, fileloc);

    } else if (declaration instanceof IASTFunctionDefinition) {
      IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
      functionDeclarations.get(functionDeclarations.size() -1).getFirst().add(fd);

      // add forward declaration to list of global declarations
      CFunctionDeclaration functionDefinition = astCreator.convert(fd);
      if (sideAssignmentStack.hasPreSideAssignments()
          || sideAssignmentStack.hasPostSideAssignments()) {
        throw new CFAGenerationRuntimeException("Function definition has side effect", fd);
      }

      fileScope.registerFunctionDeclaration(functionDefinition);
      if(!eliminateableDuplicates.contains(functionDefinition.toASTString())) {
        globalDeclarations.add(Pair.of((IADeclaration)functionDefinition, fd.getDeclSpecifier().getRawSignature() + " " + fd.getDeclarator().getRawSignature()));
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
      logger.log(Level.FINER, "Ignoring inline assembler code at line", fileloc.getStartingLineNumber());
      sideAssignmentStack.leaveBlock();
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
      sideAssignmentStack.leaveBlock();
      return PROCESS_SKIP;
    }

    final List<CDeclaration> newDs = astCreator.convert(sd);
    assert !newDs.isEmpty();

    if (sideAssignmentStack.hasConditionalExpression()
        || sideAssignmentStack.hasPostSideAssignments()) {
      throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", sd);
    }

    String rawSignature = sd.getRawSignature();

    for (CAstNode astNode : sideAssignmentStack.getAndResetPreSideAssignments()) {
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
        if (globalScope.getTypes().containsKey(((CComplexType)newD.getType()).getQualifiedName())) {
          used = false;
          handleEqualNamedTypes((CComplexTypeDeclaration) newD, rawSignature);
        } else {
          used = fileScope.registerTypeDeclaration((CComplexTypeDeclaration)newD);
        }
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

    sideAssignmentStack.leaveBlock();
    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  /**
   * This method gets called when there are two types equally named througout
   * different files. If the types are equal, no new type declaration is added
   * if the types are different, the type which should be added is renamed.
   */
  private void handleEqualNamedTypes(CComplexTypeDeclaration newD, String rawSignature) {
    boolean used = true;
    CComplexType newType = newD.getType();
    CComplexType oldType = globalScope.lookupType(newType.getQualifiedName());
    CComplexType forwardType;

    // if there is only an elaborated type we need to evaluate the complete type before
    // we can say something about type equality
    if (newType instanceof CElaboratedType && ((CElaboratedType) newType).getRealType() == null && staticVariablePrefix.equals("")) {

      // only instantiate the typechecker if it was not already instantiated
      if(preBuildTypeChecker == null) {
        try {
          preBuildTypeChecker = new CatchAllGlobalTypesVisitor(config, logger, machine, staticVariablePrefix, sideAssignmentStack);
          ast.accept(preBuildTypeChecker);
        } catch (InvalidConfigurationException e) {
          throw new CFAGenerationRuntimeException("Invalid configuration");
        }
      }
      forwardType = preBuildTypeChecker.lookupType(newType.getQualifiedName());
    } else {
      forwardType = newType;
    }


    boolean areEqual = true;
    // start counter by -1 so the first index will be 0
    int counter = -1;

    // test equality of types for all possible old types (those could also be renamed)
    while (oldType != null) {
      counter++;
      areEqual = areEqualTypes(oldType, forwardType);

      if (areEqual) {
        if (counter == 0) {
          newD = globalScope.getTypes().get(newType.getQualifiedName());
          CComplexTypeDeclaration decl;
          if (( decl = fileScope.getTypes().get(newType.getQualifiedName())) != null) {
            if (areEqualTypes(decl.getType(), newD.getType())) {
              break;
            }
          }
          used = fileScope.registerTypeDeclaration(newD);
          break;

        } else {
          newD = globalScope.getTypes().get(newType.getQualifiedName() + "__" + (counter - 1));
          used = fileScope.registerTypeDeclaration(newD);

          ASTTypeConverter conv = new ASTTypeConverter(fileScope, astCreator, staticVariablePrefix);
          IType key = conv.getTypeFromTypeConversion(newType);

          if (newType instanceof CElaboratedType && ((CElaboratedType) newType).getRealType() == null) {
            conv.overwriteType(key, new CElaboratedType(newType.isConst(), newType.isVolatile(), newType.getKind(),
                                                           newType.getName() + "__" + (counter -1 ), null));
          } else {
            conv.overwriteType(key, newD.getType());
          }
          break;
        }
      } else {
        oldType = globalScope.lookupType(newType.getQualifiedName() + "__" + counter);
      }
    }

    if (!areEqual) {
      newD = handleUnequalTypes(newD, "__" + counter);
      used = fileScope.registerTypeDeclaration(newD);
    }

    if (used) {
      if (!eliminateableDuplicates.contains(newD.toASTString())) {
        globalDeclarations.add(Pair.of((IADeclaration)newD, rawSignature));
        eliminateableDuplicates.add(newD.toASTString());
      }
    }
  }

  /**
   * This method creates a new CComplexTypeDeclaration with an unoccupied name for
   * unequal types with the same name.
   */
  private CComplexTypeDeclaration handleUnequalTypes(CComplexTypeDeclaration newD, String suffix) {
    CComplexType oldType = newD.getType();
    String newName = oldType.getName() + suffix;

    if (oldType instanceof CCompositeType) {
      CCompositeType ct = new CCompositeType(oldType.isConst(), oldType.isVolatile(), oldType.getKind(),
                                          ImmutableList.<CCompositeTypeMemberDeclaration>of(), newName);

      ASTTypeConverter conv = new ASTTypeConverter(fileScope, astCreator, staticVariablePrefix);
      IType key = conv.getTypeFromTypeConversion(oldType);
      conv.overwriteType(key, new CElaboratedType(ct.isConst(), ct.isVolatile(), ct.getKind(), ct.getName(), ct));

      List<CCompositeTypeMemberDeclaration> newMembers = new ArrayList<>(((CCompositeType)oldType).getMembers().size());
      for(CCompositeTypeMemberDeclaration decl : ((CCompositeType) oldType).getMembers()) {
        if (!(decl.getType() instanceof CPointerType)) {
          newMembers.add(new CCompositeTypeMemberDeclaration(decl.getType(), decl.getName()));
        } else {
          newMembers.add(new CCompositeTypeMemberDeclaration(createPointerField((CPointerType) decl.getType(), oldType, ct), decl.getName()));
        }
      }
      ct.setMembers(newMembers);
      newD = new CComplexTypeDeclaration(newD.getFileLocation(), newD.isGlobal(), ct);

    } else if (oldType instanceof CEnumType) {
      List<CEnumerator> list = new ArrayList<>(((CEnumType) oldType).getEnumerators().size());

      for (CEnumerator c : ((CEnumType) oldType).getEnumerators()) {
        CEnumerator newC = new CEnumerator(c.getFileLocation(), c.getName(), c.getQualifiedName(), c.hasValue() ? c.getValue() : null);
        list.add(newC);
      }

      CEnumType et = new CEnumType(oldType.isConst(), oldType.isVolatile(), list, newName);
      for (CEnumerator enumValue : et.getEnumerators()) {
        enumValue.setEnum(et);
      }
      newD = new CComplexTypeDeclaration(newD.getFileLocation(), newD.isGlobal(), et);

    } else if (oldType instanceof CElaboratedType) {
      CElaboratedType et = new CElaboratedType(oldType.isConst(), oldType.isVolatile(),
                       oldType.getKind(), newName, null);
      newD = new CComplexTypeDeclaration(newD.getFileLocation(), true, et);
    }
    return newD;
  }

  /**
   * This method checks CComplexTypes on equality. As members are usually not
   * checked by our equality methods these are here checked additionally, but
   * only by name.
   */
  private boolean areEqualTypes(CComplexType oldType, CComplexType forwardType) {
    boolean areEqual = false;
    oldType = (CComplexType) oldType.getCanonicalType();
    forwardType = (CComplexType) forwardType.getCanonicalType();
    if (forwardType.equals(oldType)) {

      if (forwardType instanceof CCompositeType) {
        List<CCompositeTypeMemberDeclaration> members = ((CCompositeType) forwardType).getMembers();
        List<CCompositeTypeMemberDeclaration> oldMembers = ((CCompositeType) oldType).getMembers();

        if (members.size() == oldMembers.size()) {
          areEqual = true;
          for (int i = 0; i < members.size() && areEqual; i++) {
            if (members.get(i).getName() == null) {
              areEqual = false;
            } else {
              areEqual = members.get(i).getName().equals(oldMembers.get(i).getName());
            }
          }
        }
      } else if (forwardType instanceof CEnumType) {
        List<CEnumerator> members = ((CEnumType) forwardType).getEnumerators();
        List<CEnumerator> oldMembers = ((CEnumType) oldType).getEnumerators();

        if (members.size() == oldMembers.size()) {
          areEqual = true;
          for (int i = 0; i < members.size() && areEqual; i++) {
            areEqual = members.get(i).getName().equals(oldMembers.get(i).getName());
          }
        }
      }
    } else {

    // in files where only a forwards declaration can be found but no complete
    // type we assume that this type is equal to the before found type with the
    // same name this also works when the elaborated type is the old type, the
    // first type found which has the same name and a complete type will now be
    // the realType of the oldType
    areEqual = ((forwardType instanceof CElaboratedType && forwardType.getName().equals(oldType.getName()))
               || (oldType instanceof CElaboratedType && oldType.getName().equals(forwardType.getName())));
    }

    return areEqual;
  }

  /**
   * This method creates the CType for a referenced field of a CCompositeType.
   */
  private CType createPointerField(CPointerType oldType, CType eqType, CType newType) {
    if (oldType.getType() instanceof CPointerType) {
      return new CPointerType(oldType.isConst(), oldType.isVolatile(), createPointerField((CPointerType) oldType.getType(), eqType, newType));
    } else {
      if (oldType.getType().equals(eqType)) {
        return new CPointerType(oldType.isConst(), oldType.isVolatile(), newType);
      } else {
        return new CPointerType(oldType.isConst(), oldType.isVolatile(), oldType.getType());
      }
    }
  }

  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public int visit(IASTProblem problem) {
    throw new CFAGenerationRuntimeException(problem);
  }

  private boolean isSingleFileEvaluation() {
    return functionDeclarations.size() == 1;
  }

  public ParseResult createCFA() throws CParserException {
    ParseResult result;
    if (isSingleFileEvaluation()) {
      result = createSingleFileCFA();
    } else {
      result = createMultipleFileCFA();
    }

    if (encounteredAsm) {
      logger.log(Level.WARNING, "Inline assembler ignored, analysis is probably unsound!");
    }

    if (checkBinding.foundUndefinedIdentifiers()) {
      throw new CParserException("Invalid C code because of undefined identifiers mentioned above.");
    }

    return result;
  }

  private ParseResult createSingleFileCFA() {
    ImmutableMap<String, CFunctionDeclaration> functions = globalScope.getFunctions();
    ImmutableMap<String, CComplexTypeDeclaration> types = globalScope.getTypes();
    ImmutableMap<String, CTypeDefDeclaration> typedefs = globalScope.getTypeDefs();

    FillInAllBindingsVisitor fillInAllBindingsVisitor = new FillInAllBindingsVisitor(globalScope);
    for (IADeclaration decl : from(globalDeclarations).transform(Pair.<IADeclaration>getProjectionToFirst())) {
      ((CDeclaration)decl).getType().accept(fillInAllBindingsVisitor);
    }

    for (Pair<List<IASTFunctionDefinition>, Pair<String, GlobalScope>> pair : functionDeclarations) {
      for (IASTFunctionDefinition declaration : pair.getFirst()) {
        handleFunctionDefinition(functions, types, typedefs, pair, declaration);
      }
    }

    return new ParseResult(cfas, cfaNodes, globalDeclarations, Language.C);
  }

  private void handleFunctionDefinition(ImmutableMap<String, CFunctionDeclaration> functions,
      ImmutableMap<String, CComplexTypeDeclaration> types, ImmutableMap<String, CTypeDefDeclaration> typedefs,
      Pair<List<IASTFunctionDefinition>, Pair<String, GlobalScope>> pair,
      IASTFunctionDefinition declaration) {

    FunctionScope localScope = new FunctionScope(functions,
                                                 types,
                                                 typedefs,
                                                 pair.getSecond().getSecond().getGlobalVars(),
                                                 renamedTypes.get(pair.getSecond().getFirst()));
    CFAFunctionBuilder functionBuilder;

    try {
      functionBuilder = new CFAFunctionBuilder(config, logger, localScope, machine,
          pair.getSecond().getFirst(), sideAssignmentStack, checkBinding);
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
    globalDeclarations.addAll(functionBuilder.getGlobalDeclarations());

    encounteredAsm |= functionBuilder.didEncounterAsm();
    functionBuilder.finish();
  }

  private ParseResult createMultipleFileCFA() {
    for(CElaboratedType decl : typesWithoutCompleteDeclaration) {
      String typeName = decl.getQualifiedName().split("__\\d")[0];

      CComplexType type = globalScope.lookupType(typeName);
      if (type instanceof CElaboratedType && ((CElaboratedType) type).getRealType() == null) {

        int counter = 0;
        while (type instanceof CElaboratedType && ((CElaboratedType) type).getRealType() == null) {
          type = globalScope.lookupType(typeName + "__" + counter);
          counter++;
        }
      }

      if (type != null) {
        if (type instanceof CCompositeType) {
          decl.setRealType(new CCompositeType(type.isConst(), type.isVolatile(), type.getKind(), ((CCompositeType) type).getMembers(), decl.getName()));
        } else if (type instanceof CEnumType) {
          decl.setRealType(new CEnumType(type.isConst(), type.isVolatile(), ((CEnumType) type).getEnumerators(), decl.getName()));
        }
      }
    }

    ImmutableMap<String, CFunctionDeclaration> functions = globalScope.getFunctions();
    ImmutableMap<String, CComplexTypeDeclaration> types = globalScope.getTypes();
    ImmutableMap<String, CTypeDefDeclaration> typedefs = globalScope.getTypeDefs();

    FillInAllBindingsVisitor fillInAllBindingsVisitor = new FillInAllBindingsVisitor(globalScope);
    for (IADeclaration decl : from(globalDeclarations).transform(Pair.<IADeclaration>getProjectionToFirst())) {
      ((CDeclaration)decl).getType().accept(fillInAllBindingsVisitor);
    }

    for (Pair<List<IASTFunctionDefinition>, Pair<String, GlobalScope>> pair : functionDeclarations) {
      for (IASTFunctionDefinition declaration : pair.getFirst()) {
        Map<String, CComplexTypeDeclaration> localTypes = new HashMap<>();
        localTypes.putAll(types);

        Set<String> localTypeKeys = pair.getSecond().getSecond().getTypes().keySet();
        for (String str : localTypeKeys) {
          if (str.matches(".*__\\d")) {
            String base = str.split("__\\d")[0];
            localTypes.remove(base);
            int counter = 0;
            while (localTypes.remove(base + "__" + counter) != null) {
              counter++;
            }
          }
        }
        localTypes.putAll(pair.getSecond().getSecond().getTypes());

        handleFunctionDefinition(functions, ImmutableMap.copyOf(localTypes), typedefs, pair, declaration);
      }
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

   //globalVars.putAll(fileScope.getGlobalVars());
    functions.putAll(fileScope.getFunctions());
    typedefs.putAll(fileScope.getTypeDefs());

    renamedTypes.put(staticVariablePrefix, fileScope.getRenamedTypes());

    // only add those composite types from the filescope to the globalscope,
    // where no type, or only an elaborated type without realtype was registered before
    for (String key : fileScope.getTypes().keySet().asList()) {
      CComplexTypeDeclaration type = types.get(key);
      CComplexTypeDeclaration newType = fileScope.getTypes().get(key);

      boolean isAdded = false;
      if (type != null) {
        if (type.getType() instanceof CElaboratedType && ((CElaboratedType) type.getType()).getRealType() == null) {
          types.put(key, newType);
          isAdded = true;
        }
      } else {
        types.put(key, newType);
        isAdded = true;
      }

      if (isAdded && newType.getType() instanceof CElaboratedType && ((CElaboratedType) newType.getType()).getRealType() == null) {
        typesWithoutCompleteDeclaration.add((CElaboratedType) newType.getType());
      }
    }

    globalScope= new GlobalScope(globalVars, functions, types, typedefs, new HashSet<String>());
    return PROCESS_CONTINUE;
  }
}