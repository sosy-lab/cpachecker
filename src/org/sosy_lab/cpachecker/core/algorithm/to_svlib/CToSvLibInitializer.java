// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibUninterpretedScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

class CToSvLibInitializer {

  /*TODO change this to an option, add prefix @Options(prefix = "analysis.algorithm.toSvLib")
  @Option(secure = true,
      description = "Use SV-COMP conform semantics for transformation of external functions.")*/
  private ExternalFunctionsEncodingMode encodingModeForExternalFunctions =
      CToSvLibInitializer.ExternalFunctionsEncodingMode.SV_COMP;

  private enum ExternalFunctionsEncodingMode {
    SV_COMP,
    HAVOC
  }

  private final LogManager logger;
  private final CFA cfa;
  private final SvLibCurrentScope scope;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final CtoFormulaConverter converter;

  /** The names of the global variables of the program, which a local variable must not have. */
  private final ImmutableSet<String> namesOfGlobalVariables;

  CToSvLibInitializer(
      LogManager pLogger,
      CFA pCFA,
      SvLibCurrentScope pCurrentScope,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      CtoFormulaConverter pConverter) {
    logger = pLogger;
    cfa = pCFA;
    scope = pCurrentScope;
    formulaManager = pFormulaManager;
    pathFormulaManager = pPathFormulaManager;
    converter = pConverter;
    namesOfGlobalVariables = collectNamesOfGlobalVariables();
  }

  /**
   * The names of the global variables of the program.
   *
   * <p>The generated program has no scopes, so a local variable of a procedure and a global
   * variable cannot have the same name in it, and a local variable of the input program that
   * shadows a global one has to be renamed.
   */
  private ImmutableSet<String> collectNamesOfGlobalVariables() {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    for (CFAEdge edge : cfa.edges()) {
      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration
          && variableDeclaration.isGlobal()) {
        names.add(variableDeclaration.getName());
      }
    }
    return names.build();
  }

  /**
   * Create the declaration of a local variable of a procedure, whose name in the generated program
   * is the qualified name of the variable if a global variable has the same name.
   */
  private SvLibParsingParameterDeclaration createLocalVariable(
      SvLibType pType, String pName, String pProcedureName) {
    String qualifiedName = pProcedureName + "::" + pName;
    String name = namesOfGlobalVariables.contains(pName) ? qualifiedName : pName;
    return new SvLibParsingParameterDeclaration(
        FileLocation.DUMMY,
        pType,
        CToSvLibTransformationConstants.asSymbol(name),
        pProcedureName,
        qualifiedName);
  }

  void initialize(ImmutableList.Builder<SvLibCommand> pCommandsCollector)
      throws UnsupportedOperationException, CPATransferException, InterruptedException {
    ImmutableSet.Builder<CType> typesOfHeapArraysToBuild = ImmutableSet.builder();

    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
      CFunctionEntryNode cEntryNode = (CFunctionEntryNode) entryNode;
      String procedureName = entryNode.getFunctionName();

      ImmutableList<SvLibParsingParameterDeclaration> inputParameters =
          collectInputParameters(cEntryNode.getFunctionParameters(), procedureName);
      ImmutableList<SvLibParsingParameterDeclaration> returnParameter =
          collectReturnParameter(cEntryNode.getReturnVariable(), procedureName);
      ImmutableSet.Builder<SvLibParsingParameterDeclaration> localVariablesCollector =
          ImmutableSet.builder();

      // create (assignable) dummy parameters for (non-assignable) inputParameters
      for (SvLibParsingParameterDeclaration inputParameter : inputParameters) {
        localVariablesCollector.add(createDummyForInputParameter(inputParameter));
      }

      ImmutableSet.Builder<CFunctionCallExpression> undeclaredFunctionsCollector =
          ImmutableSet.builder();
      for (CFAEdge edge : getAllRelevantEdges(entryNode)) {
        if (edge instanceof CDeclarationEdge declarationEdge) {
          CDeclaration declaration = declarationEdge.getDeclaration();

          if (declaration instanceof CVariableDeclaration variableDeclaration) {
            SvLibSimpleParsingDeclaration parsingDeclaration =
                initializeVariableDeclaration(
                    edge,
                    variableDeclaration,
                    procedureName,
                    typesOfHeapArraysToBuild,
                    pCommandsCollector);

            if (parsingDeclaration
                instanceof SvLibParsingVariableDeclaration globalVariableDeclaration) {
              scope.addVariable(globalVariableDeclaration);
              pCommandsCollector.add(
                  new SvLibVariableDeclarationCommand(
                      globalVariableDeclaration, FileLocation.DUMMY));
            } else if (parsingDeclaration
                instanceof SvLibParsingParameterDeclaration localVariableDeclaration) {
              localVariablesCollector.add(localVariableDeclaration);
            }

          } else if (declaration instanceof CFunctionDeclaration functionDeclaration) {
            // handle external functions
            boolean isExtern = !cfa.getAllFunctionNames().contains(functionDeclaration.getName());
            if (isExtern) {
              SvLibProcedureDefinitionCommand externProcedureDefinition =
                  createExternProcedureDefinition(functionDeclaration);
              scope.addProcedureDeclaration(externProcedureDefinition.getProcedureDeclaration());
              pCommandsCollector.add(externProcedureDefinition);
            }
          }

        } else if (edge instanceof CStatementEdge cStatementEdge
            && cStatementEdge.getStatement()
                instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement
            && cFunctionCallAssignmentStatement.getRightHandSide().getDeclaration() == null) {
          undeclaredFunctionsCollector.add(
              cFunctionCallAssignmentStatement.getFunctionCallExpression());

        } else if (edge instanceof CFunctionSummaryEdge pCFunctionSummaryEdge
            && pCFunctionSummaryEdge.getExpression() instanceof CFunctionCallStatement functionCall
            && !(functionCall.getFunctionCallExpression().getExpressionType()
                instanceof CVoidType)) {
          // create dummy return parameters for procedure calls for function calls without
          // assignment and non-void return type
          SvLibParsingParameterDeclaration dummyReturnParameter =
              createDummyReturnParameter(functionCall, procedureName);
          localVariablesCollector.add(dummyReturnParameter);

        } else if (edge instanceof CStatementEdge cStatementEdge
            && cStatementEdge.getStatement() instanceof CFunctionCallStatement functionCall
            && !(functionCall.getFunctionCallExpression().getExpressionType()
                instanceof CVoidType)) {
          SvLibParsingParameterDeclaration dummyReturnParameter =
              createDummyReturnParameter(functionCall, procedureName);
          localVariablesCollector.add(dummyReturnParameter);

        } else if (edge instanceof CFunctionSummaryEdge pCFunctionSummaryEdge
            && pCFunctionSummaryEdge.getExpression()
                instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {

          SvLibType returnValueType;
          if (functionCallAssignmentStatement.getLeftHandSide()
              instanceof CArraySubscriptExpression arraySubscript) {
            returnValueType = convertToSvLibSmtLibType(arraySubscript.getExpressionType());
          } else {
            returnValueType =
                convertToSvLibSmtLibType(
                    functionCallAssignmentStatement.getLeftHandSide().getExpressionType());
          }
          SvLibParsingParameterDeclaration tmpHeapAssignVariable =
              new SvLibParsingParameterDeclaration(
                  FileLocation.DUMMY,
                  returnValueType,
                  CToSvLibTransformationConstants.TMP_VAR_ASSIGNMENT + returnValueType,
                  procedureName);
          localVariablesCollector.add(tmpHeapAssignVariable);

        } else if (edge instanceof CStatementEdge cStatementEdge
            && cStatementEdge.getStatement()
                instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement
            && functionCallAssignmentStatement.getRightHandSide().getDeclaration() != null) {

          SvLibType returnValueType;
          if (functionCallAssignmentStatement.getLeftHandSide()
              instanceof CArraySubscriptExpression arraySubscript) {
            returnValueType = convertToSvLibSmtLibType(arraySubscript.getExpressionType());
          } else {
            returnValueType =
                convertToSvLibSmtLibType(
                    functionCallAssignmentStatement.getLeftHandSide().getExpressionType());
          }
          SvLibParsingParameterDeclaration tmpHeapAssignVariable =
              new SvLibParsingParameterDeclaration(
                  FileLocation.DUMMY,
                  returnValueType,
                  CToSvLibTransformationConstants.TMP_VAR_ASSIGNMENT + returnValueType,
                  procedureName);
          localVariablesCollector.add(tmpHeapAssignVariable);
        }
      }

      ImmutableSet<CFunctionCallExpression> undeclaredFunctions =
          undeclaredFunctionsCollector.build();
      if (!undeclaredFunctions.isEmpty()) {
        initializeUndeclaredFunctions(undeclaredFunctions, pCommandsCollector);
      }

      SvLibProcedureDeclaration procedureDeclaration =
          new SvLibProcedureDeclaration(
              FileLocation.DUMMY,
              procedureName,
              inputParameters,
              returnParameter,
              localVariablesCollector.build().asList());
      scope.addProcedureDeclaration(procedureDeclaration);
    }

    for (CType type : typesOfHeapArraysToBuild.build()) {
      // for arrays with dimensionality = 2
      if (type instanceof CArrayType arrayType) {
        typesOfHeapArraysToBuild.add(arrayType.getCanonicalType().getType());
      }
    }

    for (CType heapArrayType : typesOfHeapArraysToBuild.build()) {
      if (!(heapArrayType instanceof CArrayType)) {
        SvLibParsingVariableDeclaration heapArrayParsingVariableDeclaration =
            createArrayDeclarationForHeap(heapArrayType);
        SvLibVariableDeclarationCommand heapArrayVariableDeclarationCommand =
            new SvLibVariableDeclarationCommand(
                heapArrayParsingVariableDeclaration, FileLocation.DUMMY);
        pCommandsCollector.add(heapArrayVariableDeclarationCommand);
        scope.addVariable(heapArrayParsingVariableDeclaration);
      }
    }
  }

  private SvLibSimpleParsingDeclaration initializeVariableDeclaration(
      CFAEdge pEdge,
      CVariableDeclaration pVariableDeclaration,
      String pProcedureName,
      ImmutableSet.Builder<CType> pTypesOfHeapArraysToCreate,
      ImmutableList.Builder<SvLibCommand> pCommandsCollector)
      throws CPATransferException, InterruptedException {

    PointerTargetSet pointerTargetSetForEdge =
        pathFormulaManager
            .makeAnd(pathFormulaManager.makeEmptyPathFormula(), pEdge)
            .getPointerTargetSet();
    // One edge can make more than one variable part of the heap representation, for example the
    // declaration "int *p = &a;", so the base of the declared variable is searched for instead of
    // assuming that it is the only one.
    for (Entry<PointerBase, CType> baseEntry : pointerTargetSetForEdge.getBases().entrySet()) {
      if (baseEntry.getKey().name().equals(pVariableDeclaration.getQualifiedName())) {
        addTypesOfHeapArraysFor(baseEntry.getValue(), pTypesOfHeapArraysToCreate);
        if (pVariableDeclaration.isGlobal()) {
          // A global variable is initialized before its address can be taken, so the formulas
          // refer to the variable itself as well as to the memory that it is part of.
          declareGlobalVariable(
              pVariableDeclaration.getName(),
              convertToSvLibSmtLibType(pVariableDeclaration.getType()),
              pCommandsCollector);
        }
        return createAddressOfVariable(baseEntry.getKey());
      }
    }

    return createVariable(pVariableDeclaration, pProcedureName);
  }

  /** Declare a global variable of the generated program, unless it is already declared. */
  private void declareGlobalVariable(
      String pName, SvLibSmtLibType pType, ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    if (scope.hasVariableForQualifiedName(pName)) {
      return;
    }
    String name = CToSvLibTransformationConstants.asSymbol(pName);
    SvLibParsingVariableDeclaration declaration =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, pType, name, name, null);
    scope.addVariable(declaration);
    pCommandsCollector.add(new SvLibVariableDeclarationCommand(declaration, FileLocation.DUMMY));
  }

  private SvLibSimpleParsingDeclaration createAddressOfVariable(PointerBase pPointerBase) {
    String addressNameEscaped = "|" + pPointerBase.formulaEncoding() + "|";
    SvLibSmtLibType addressType = getAddressType();

    // The address of an object is the same wherever the program refers to it, and the formulas of
    // every procedure are built with all objects of the program in their context, so the address of
    // a local variable is a variable of the generated program as well: a procedure that only reads
    // the memory of such an object needs it, too.
    return new SvLibParsingVariableDeclaration(
        FileLocation.DUMMY, true, false, addressType, addressNameEscaped, addressNameEscaped, null);
  }

  private SvLibSimpleParsingDeclaration createVariable(
      CVariableDeclaration pVariableDeclaration, String pProcedureName) {
    SvLibType type = convertToSvLibSmtLibType(pVariableDeclaration.getType());
    if (pVariableDeclaration.isGlobal()) {
      // global variable declaration
      return createGlobalVariableDeclaration(pVariableDeclaration, type);
    } else {
      // local variable declaration
      return createLocalVariable(type, pVariableDeclaration.getName(), pProcedureName);
    }
  }

  private SvLibParsingVariableDeclaration createArrayDeclarationForHeap(CType pElementType) {
    SvLibSmtLibType indexType = getAddressType();
    SvLibSmtLibArrayType arrayType =
        new SvLibSmtLibArrayType(indexType, convertToSvLibSmtLibType(pElementType));

    String heapTypeName = getHeapArrayName(pElementType);
    return new SvLibParsingVariableDeclaration(
        FileLocation.DUMMY, true, false, arrayType, heapTypeName, heapTypeName, null);
  }

  /**
   * The name of the array that models the memory holding values of the given type.
   *
   * <p>The name has to be the one that the formulas use for accesses to that memory, because the
   * transformation looks the array up by the name of the free variable of such an access.
   */
  private @NonNull String getHeapArrayName(CType pElementType) {
    String name = pathFormulaManager.getPointerAccessName(pElementType);
    // The name of the memory of an array type contains the size of the array, as in "*(int)[2]",
    // which is not a simple symbol in SMT-LIB and therefore has to be quoted.
    return name.matches("[*]?[A-Za-z0-9_]+") ? name : "|" + name + "|";
  }

  /**
   * Does an object of the given type have values, i.e. is its size known and greater than zero?
   *
   * <p>A variable of a type without values, such as a structure without members or one that is only
   * declared, is not declared in the generated program, because the formulas cannot contain a value
   * of that type either. C forbids reading or writing such a variable, only its address can be
   * taken.
   */
  private boolean hasValues(CType pType) {
    return pType.hasKnownConstantSize() && cfa.getMachineModel().getSizeof(pType).signum() > 0;
  }

  /**
   * The type of an address, which is the one that the formulas use for the value of a pointer and
   * for an index into an array that models the memory of the heap.
   */
  private SvLibSmtLibType getAddressType() {
    return convertToSvLibSmtLibType(CPointerType.POINTER_TO_VOID);
  }

  private SvLibParsingParameterDeclaration createDummyReturnParameter(
      CFunctionCallStatement pFunctionCall, String pProcedureName) {
    CType functionReturnType = pFunctionCall.getFunctionCallExpression().getExpressionType();
    SvLibSmtLibType returnType = convertToSvLibSmtLibType(functionReturnType);
    String returnDummyName =
        (returnType instanceof SvLibSmtLibBitVectorType bitVectorType)
            ? CToSvLibTransformationConstants.RETURN_VAR_DUMMY_PREFIX
                + "bv"
                + bitVectorType.getSize()
            : CToSvLibTransformationConstants.RETURN_VAR_DUMMY_PREFIX + returnType;
    return new SvLibParsingParameterDeclaration(
        FileLocation.DUMMY, returnType, returnDummyName, pProcedureName);
  }

  private SvLibSmtLibType convertToSvLibSmtLibType(CType pCType) {
    if (pCType instanceof CArrayType arrayType && !arrayType.hasKnownConstantSize()) {
      // TODO probably better recursively but works for 2d
      if (arrayType.getCanonicalType().getType() instanceof CArrayType nestedArrayType) {
        CType innerType = nestedArrayType.getCanonicalType().getType();
        pCType = innerType;
      } else {
        pCType = arrayType.getCanonicalType().getType();
      }
    }

    FormulaType<?> formulaType = converter.getFormulaTypeFromType(pCType);
    FormulaType<Formula> encodedFormulaType = formulaManager.getEncodedFormulaType(formulaType);

    if (encodedFormulaType.isBooleanType()) {
      return SvLibSmtLibPredefinedType.BOOL;
    } else if (encodedFormulaType.isIntegerType()) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (encodedFormulaType.isStringType()) {
      return SvLibSmtLibPredefinedType.STRING;
    } else if (encodedFormulaType.isRationalType()) {
      return SvLibSmtLibPredefinedType.REAL;
    } else if (encodedFormulaType.isBitvectorType()) {
      BitvectorType bitvectorType = (BitvectorType) formulaType;
      return new SvLibSmtLibBitVectorType(bitvectorType.getSize());
    } else if (encodedFormulaType.isFloatingPointType()) {
      FloatingPointType floatingPointType = (FloatingPointType) formulaType;
      return new SvLibSmtLibFloatingPointType(
          floatingPointType.getExponentSize(), floatingPointType.getMantissaSizeWithHiddenBit());
    }

    throw new UnsupportedOperationException(
        "Transformation to a SvLibType failed for CType " + pCType);
  }

  /**
   * Add the types of the arrays that model the memory of an object of the given type.
   *
   * <p>The elements of an array and the members of a structure are accessed under the name of their
   * own type, and the object as a whole, for example when its address is taken or when it is
   * assigned at once, under the name of its type.
   */
  private void addTypesOfHeapArraysFor(
      CType pType, ImmutableSet.Builder<CType> pTypesOfHeapArraysToCreate) {
    CType type = pType.getCanonicalType();
    if (!hasValues(type)) {
      // No value of that type can be read from or written to the memory, so no array is needed.
      return;
    }
    if (type instanceof CArrayType arrayType) {
      pTypesOfHeapArraysToCreate.add(arrayType);
      addTypesOfHeapArraysFor(arrayType.getType(), pTypesOfHeapArraysToCreate);
    } else if (type instanceof CCompositeType compositeType) {
      pTypesOfHeapArraysToCreate.add(compositeType);
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        addTypesOfHeapArraysFor(memberDeclaration.getType(), pTypesOfHeapArraysToCreate);
      }
    } else if (!(type instanceof CVoidType)) {
      pTypesOfHeapArraysToCreate.add(pType);
    }
  }

  /**
   * The type that a parameter or a returned value of the given C type has in the generated program.
   *
   * <p>The value of a pointer is its address, which is as wide as a pointer of the machine model
   * and independent of the type it points to, so the type of the pointer itself is used. C passes
   * an array as a pointer to its first element, so an array parameter is one as well.
   */
  private CType getTypeOfParameter(CType pType) {
    CType type = pType.getCanonicalType();
    if (type instanceof CArrayType arrayType) {
      return new CPointerType(CTypeQualifiers.NONE, arrayType.getType());
    }
    return type;
  }

  private ImmutableList<SvLibParsingParameterDeclaration> collectInputParameters(
      ImmutableList<CParameterDeclaration> pParameterDeclarations, String pProcedureName)
      throws UnsupportedOperationException {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> parameterCollector =
        ImmutableList.builder();

    // TODO string "main" or cfa.getMainFunction()
    if (pProcedureName.equals(cfa.getMainFunction().getFunctionName())) {
      // Ignore input parameters of the main function
      return parameterCollector.build();
    }

    for (CParameterDeclaration parameter : pParameterDeclarations) {
      if (parameter.asVariableDeclaration().getType() instanceof CSimpleType asSimpleType) {
        parameterCollector.add(
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                convertToSvLibSmtLibType(asSimpleType),
                getNameForInputParameterDummy(parameter.getName()),
                pProcedureName));
      } else if (parameter.asVariableDeclaration().getType()
          instanceof CPointerType asPointerType) {
        parameterCollector.add(
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                convertToSvLibSmtLibType(asPointerType.getType()),
                getNameForInputParameterDummy(parameter.getName()),
                pProcedureName));
      } else if (parameter.asVariableDeclaration().getType() instanceof CArrayType) {
        throw new UnsupportedOperationException(
            "Transformation of function "
                + pProcedureName
                + " with an array as input is currently not supported.");
      }
    }
    return parameterCollector.build();
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
              convertToSvLibSmtLibType(asSimpleType),
              pReturnVariable.orElseThrow().getName(),
              pProcedureName));
    }
    return ImmutableList.of();
  }

  private SvLibParsingParameterDeclaration createDummyForInputParameter(
      SvLibParsingParameterDeclaration pInputParameter) {
    return new SvLibParsingParameterDeclaration(
        FileLocation.DUMMY,
        pInputParameter.getType(),
        getOriginalNameOfInputParameterDummy(pInputParameter.getName()),
        pInputParameter.getProcedureName());
  }

  private String getNameForInputParameterDummy(String pOriginalName) {
    return CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX + pOriginalName;
  }

  private String getOriginalNameOfInputParameterDummy(String pDummyName) {
    if (pDummyName.startsWith(CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX)) {
      // return the name without the prefix
      return pDummyName.substring(CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX.length());
    }
    throw new IllegalArgumentException(
        "Cannot remove prefix "
            + CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX
            + " from name "
            + pDummyName);
  }

  private ImmutableList<CFAEdge> getAllRelevantEdges(FunctionEntryNode pEntryNode) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pEntryNode, edgeCollector);
    return ImmutableList.copyOf(edgeCollector.getVisitedEdges());
  }

  private SvLibParsingVariableDeclaration createGlobalVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, SvLibType pType) {
    // The declaration is printed with the original name, so that name has to be quoted as well.
    String name = CToSvLibTransformationConstants.asSymbol(pVariableDeclaration.getName());
    return new SvLibParsingVariableDeclaration(
        FileLocation.DUMMY,
        pVariableDeclaration.isGlobal(),
        pVariableDeclaration.getType().isConst(),
        pType,
        name,
        name,
        null);
  }

  private SvLibProcedureDefinitionCommand createExternProcedureDefinition(
      CFunctionDeclaration pFunctionDeclaration) throws CPATransferException, InterruptedException {
    if (CToSvLibTransformationConstants.NAMES_OF_ASSERT_FUNCTIONS.contains(
        pFunctionDeclaration.getName())) {
      // Special handling of a set of external __assert functions that have char* input parameters
      // in the C program, since Transformation via the FormulaToSvlibVisitor cannot yet handle
      // pathFormulas with strings.
      // Therefore, dummy procedures with no parameters and (assert fail) as body are created for
      // these functions.
      return escapeExternalAssertWithString(pFunctionDeclaration);
    } else if (CToSvLibTransformationConstants.NAMES_OF_UNSUPPORTED_STDLIB_EXTERNAL_FUNCTIONS
        .contains(pFunctionDeclaration.getName())) {
      return escapeExternalAssertWithString(pFunctionDeclaration);
    } else if (CToSvLibTransformationConstants.NAMES_OF_UNSUPPORTED_NONDET_FUNCTIONS.contains(
        pFunctionDeclaration.getName())) {
      return escapeExternalAssertWithString(pFunctionDeclaration);
    } else {
      SvLibProcedureDeclaration externProcedureDeclaration =
          createProcedureDeclarationForExternFunction(pFunctionDeclaration);
      CType cReturnType = pFunctionDeclaration.getType().getReturnType();
      SvLibStatement externProcedureBody =
          createBodyForExternProcedure(externProcedureDeclaration, cReturnType);
      return new SvLibProcedureDefinitionCommand(
          FileLocation.DUMMY, externProcedureDeclaration, externProcedureBody);
    }
  }

  private SvLibProcedureDefinitionCommand escapeExternalAssertWithString(
      CFunctionDeclaration pFunctionDeclaration) {
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            pFunctionDeclaration.getName(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());

    SvLibStatement procedureBody =
        new SvLibAssumeStatement(
            FileLocation.DUMMY,
            new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
            ImmutableList.of(),
            ImmutableList.of(
                new SvLibTagReference(pFunctionDeclaration.getName(), FileLocation.DUMMY)));

    return new SvLibProcedureDefinitionCommand(
        FileLocation.DUMMY, procedureDeclaration, procedureBody);
  }

  private void initializeUndeclaredFunctions(
      ImmutableSet<CFunctionCallExpression> pUndeclaredFunctions,
      ImmutableList.Builder<SvLibCommand> pCommandsCollector)
      throws CPATransferException, InterruptedException {
    for (CFunctionCallExpression functionCallExpression : pUndeclaredFunctions) {
      String functionName = functionCallExpression.getFunctionNameExpression().toASTString();
      CType expressionType = functionCallExpression.getExpressionType();
      SvLibProcedureDefinitionCommand procedureDefinition =
          createProcedureDefinitionForUndeclaredFunction(functionName, expressionType);
      pCommandsCollector.add(procedureDefinition);
      scope.addProcedureDeclaration(procedureDefinition.getProcedureDeclaration());
    }
  }

  private SvLibProcedureDefinitionCommand createProcedureDefinitionForUndeclaredFunction(
      String pFunctionName, CType pReturnType) throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> returnParameterCollector =
        ImmutableList.builder();
    if (!(pReturnType instanceof CVoidType)) {
      returnParameterCollector.add(
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibSmtLibType(pReturnType),
              "__retval__",
              pFunctionName));
    }
    SvLibProcedureDeclaration procedureDeclarationForUndeclaredFunction =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            pFunctionName,
            ImmutableList.of(),
            returnParameterCollector.build(),
            ImmutableList.of());
    SvLibStatement body =
        createBodyForExternProcedure(procedureDeclarationForUndeclaredFunction, pReturnType);
    return new SvLibProcedureDefinitionCommand(
        FileLocation.DUMMY, procedureDeclarationForUndeclaredFunction, body);
  }

  private SvLibProcedureDeclaration createProcedureDeclarationForExternFunction(
      CFunctionDeclaration pCFunctionDeclaration) {
    String functionName = pCFunctionDeclaration.getName();

    ImmutableList.Builder<SvLibParsingParameterDeclaration> returnParameterCollector =
        ImmutableList.builder();
    CType originalCReturnType = pCFunctionDeclaration.getType().getReturnType();
    if (!(originalCReturnType instanceof CVoidType)) {
      SvLibType convertedReturnType = convertToSvLibSmtLibType(originalCReturnType);
      SvLibParsingParameterDeclaration returnParameterDeclaration =
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY, convertedReturnType, "_retval_", functionName);
      returnParameterCollector.add(returnParameterDeclaration);
    }

    ImmutableList<CParameterDeclaration> inputParameters = pCFunctionDeclaration.getParameters();
    ImmutableList.Builder<SvLibParsingParameterDeclaration> convertedInputParametersCollector =
        ImmutableList.builder();
    for (int i = 0; i < inputParameters.size(); i++) {
      CParameterDeclaration inputParameter = inputParameters.get(i);
      String inputParameterName =
          !inputParameter.getName().isEmpty()
              ? inputParameter.getName()
              : "inputDummy_" + i + "_extern";
      SvLibParsingParameterDeclaration convertedInputParameter =
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibSmtLibType(inputParameter.getType()),
              inputParameterName,
              functionName);
      convertedInputParametersCollector.add(convertedInputParameter);
    }

    return new SvLibProcedureDeclaration(
        FileLocation.DUMMY,
        functionName,
        convertedInputParametersCollector.build(),
        returnParameterCollector.build(),
        ImmutableList.of());
  }

  private SvLibStatement createBodyForExternProcedure(
      SvLibProcedureDeclaration pProcedureDeclaration, CType pCReturnType)
      throws CPATransferException, InterruptedException {
    String procedureName = pProcedureDeclaration.getProcedureName();

    if (procedureName.equals("abort") || procedureName.equals("exit")) {
      return new SvLibAssumeStatement(
          FileLocation.DUMMY,
          new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
          ImmutableList.of(),
          ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)));
    }

    if (encodingModeForExternalFunctions.equals(ExternalFunctionsEncodingMode.SV_COMP)) {
      if (procedureName.startsWith("__VERIFIER_nondet_memory")) {
        // implement when a memory model exists
        throw new UnsupportedOperationException(
            "Transformation of programs that include extern function __VERIFIER_nondet_memory() is"
                + " not supported.");
      }
    }

    if (!pProcedureDeclaration.getReturnValues().isEmpty()) {
      return createHavocWithBounds(pProcedureDeclaration, pCReturnType);
    }
    // empty sequence statement for external, void functions
    return new SvLibSequenceStatement(
        ImmutableList.of(),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)));
  }

  private SvLibSequenceStatement createHavocWithBounds(
      SvLibProcedureDeclaration pProcedureDeclaration, CType pCReturnType)
      throws CPATransferException, InterruptedException {
    SvLibHavocStatement havocStatement =
        new SvLibHavocStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            castListToSimpleParsingDeclaration(pProcedureDeclaration.getReturnValues()));

    SvLibAssumeStatement assumeStatement;
    if (pCReturnType instanceof CSimpleType simpleReturnType
        && simpleReturnType.getType().isIntegerType()) {

      assumeStatement =
          createAssumeBounds(
              pProcedureDeclaration,
              pCReturnType,
              cfa.getMachineModel().getMinimalIntegerValue(simpleReturnType),
              cfa.getMachineModel().getMaximalIntegerValue(simpleReturnType));
      return new SvLibSequenceStatement(
          ImmutableList.of(havocStatement, assumeStatement),
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(
              new SvLibTagReference(pProcedureDeclaration.getName(), FileLocation.DUMMY)));
    } else {
      // TODO log that no assume statement for bounds was created?
      return new SvLibSequenceStatement(
          ImmutableList.of(havocStatement),
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(
              new SvLibTagReference(pProcedureDeclaration.getName(), FileLocation.DUMMY)));
    }
  }

  private SvLibAssumeStatement createAssumeBounds(
      SvLibProcedureDeclaration pProcedureDeclaration,
      CType pCReturnType,
      BigInteger pMinValue,
      BigInteger pMaxValue)
      throws CPATransferException, InterruptedException {

    String variableName = pProcedureDeclaration.getReturnValues().getFirst().getName();
    CIdExpression variableExpression =
        new CIdExpression(
            FileLocation.DUMMY,
            pCReturnType,
            variableName,
            getDummyVariableDeclaration(variableName, pCReturnType));
    CAssumeEdge minValueDummyAssumeEdge =
        createAssumeEdgeForBound(variableExpression, pCReturnType, pMinValue, true);
    CAssumeEdge maxValueDummyAssumeEdge =
        createAssumeEdgeForBound(variableExpression, pCReturnType, pMaxValue, false);

    SvLibTerm combinedBoundsTerm =
        transformToSvLibTerm(minValueDummyAssumeEdge, maxValueDummyAssumeEdge);
    return new SvLibAssumeStatement(
        FileLocation.DUMMY, combinedBoundsTerm, ImmutableList.of(), ImmutableList.of());
  }

  private CAssumeEdge createAssumeEdgeForBound(
      CIdExpression pVariableExpression,
      CType pCReturnType,
      BigInteger pBoundValue,
      Boolean isLowerLimit)
      throws UnrecognizedCodeException {
    CIntegerLiteralExpression limitLiteral =
        new CIntegerLiteralExpression(FileLocation.DUMMY, pCReturnType, pBoundValue);
    BinaryOperator operator =
        isLowerLimit ? BinaryOperator.GREATER_EQUAL : BinaryOperator.LESS_EQUAL;
    CBinaryExpressionBuilder expressionBuilder =
        new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    CBinaryExpression limitAssumption =
        expressionBuilder.buildBinaryExpression(pVariableExpression, limitLiteral, operator);
    return new CAssumeEdge(
        limitAssumption.toASTString(),
        FileLocation.DUMMY,
        CFANode.newDummyCFANode(),
        CFANode.newDummyCFANode(),
        limitAssumption,
        true);
  }

  private @NonNull SvLibTerm transformToSvLibTerm(CFAEdge pEdge1, CFAEdge pEdge2)
      throws CPATransferException, InterruptedException {
    PathFormula edgeFormula = pathFormulaManager.makeEmptyPathFormula();
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge1);
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge2);
    return formulaManager.visit(
        edgeFormula.getFormula(),
        new FormulaToSvLibVisitor(formulaManager, new SvLibUninterpretedScope()));
  }

  private CVariableDeclaration getDummyVariableDeclaration(String pVariableName, CType pCType) {
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        false,
        CStorageClass.AUTO,
        pCType,
        pVariableName,
        pVariableName,
        pVariableName,
        null);
  }

  private ImmutableList<SvLibSimpleParsingDeclaration> castListToSimpleParsingDeclaration(
      ImmutableList<SvLibParsingParameterDeclaration> pParameters) {

    ImmutableList.Builder<SvLibSimpleParsingDeclaration> simpleDeclarations =
        ImmutableList.builder();
    if (!pParameters.isEmpty()) {
      for (SvLibParsingParameterDeclaration parameterDeclaration : pParameters) {
        simpleDeclarations.add(parameterDeclaration);
      }
    }
    return simpleDeclarations.build();
  }
}
