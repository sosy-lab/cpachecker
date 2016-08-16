/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Variable;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Implements a converter for C code into SMT formulae.
 *
 * This supports pointer aliasing and models the heap with SMT arrays.
 */
public class CToFormulaConverterWithHeapArray extends CtoFormulaConverter {

  // Overrides just for visibility in other classes of this package

  @SuppressWarnings("hiding")
  final LogManagerWithoutDuplicates logger = super.logger;
  @SuppressWarnings("hiding")
  final FormulaManagerView formulaManager = super.fmgr;
  @SuppressWarnings("hiding")
  final BooleanFormulaManagerView bfmgr = super.bfmgr;

  final ArrayFormulaManagerView afmgr;
  private final QuantifiedFormulaManagerView qfmgr;
  @SuppressWarnings("hiding")
  final MachineModel machineModel = super.machineModel;
  @SuppressWarnings("hiding")
  private final ShutdownNotifier shutdownNotifier = super.shutdownNotifier;
  @SuppressWarnings("hiding") // same instance with narrower type
  final FormulaEncodingWithPointerAliasingOptions options;

  private final Optional<VariableClassification> variableClassification;
  private static final String POINTER_NAME_PREFIX = "*";
  static final String FIELD_NAME_SEPARATOR = "$";
  private static final Map<CType, String> arrayNameCache = new IdentityHashMap<>();

  private final TypeHandlerWithPointerAliasing typeHandler;
  final PointerTargetSetManagerHeapArray ptsMgr;

  final FormulaType<?> voidPointerFormulaType;
  final Formula nullPointer;

  /**
   * Creates a C to SMT formula converter with support for pointer aliasing. The heap is modelled
   * with SMT arrays.
   *
   * @param pOptions                Additional configuration options.
   * @param pFormulaManager         The formula manger for SMT formulae.
   * @param machineModel            The machine model for the evaluation run.
   * @param pVariableClassification An optional classification of variables.
   * @param pLogManager             The main CPAchecker logger.
   * @param pShutdownNotifier       A notifier for user shutdowns to stop long running algorithms.
   * @param pTypeHandler            A handler for C types.
   * @param pDirection              The direction of the analysis.
   */
  public CToFormulaConverterWithHeapArray(
      final FormulaEncodingWithPointerAliasingOptions pOptions,
      final FormulaManagerView pFormulaManager,
      final MachineModel machineModel,
      final Optional<VariableClassification> pVariableClassification,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final AnalysisDirection pDirection) {

    super(pOptions, pFormulaManager, machineModel, pVariableClassification,
        pLogManager, pShutdownNotifier, pTypeHandler, pDirection);

    variableClassification = pVariableClassification;
    options = pOptions;
    typeHandler = pTypeHandler;
    ptsMgr = new PointerTargetSetManagerHeapArray(options, formulaManager, typeHandler,
        shutdownNotifier);

    afmgr = pFormulaManager.getArrayFormulaManager();
    qfmgr = null;

    voidPointerFormulaType = typeHandler.getFormulaTypeFromCType(CPointerType.POINTER_TO_VOID);
    nullPointer = formulaManager.makeNumber(voidPointerFormulaType, 0);
  }

  /**
   * Creates a C to SMT formula converter with support for pointer aliasing. The heap is modelled
   * with SMT arrays.
   *
   * @param pOptions                      Additional configuration options.
   * @param pFormulaManager               The formula manager for SMT formulae.
   * @param pMachineModel                 The machine model for the evaluation run.
   * @param pVariableClassification       An optional classification of variables.
   * @param pLogManager                   The main CPAchecker logger.
   * @param pShutdownNotifier             A notifier for user shutdowns to stop long running
   *                                      algorithms.
   * @param pTypeHandler                  A handler for C types.
   * @param pDirection                    The direction of the analysis.
   * @param pQuantifiedFormulaManagerView A formula manager supporting quantifiers.
   */
  public CToFormulaConverterWithHeapArray(
      final FormulaEncodingWithPointerAliasingOptions pOptions,
      final FormulaManagerView pFormulaManager,
      final MachineModel pMachineModel,
      final Optional<VariableClassification> pVariableClassification,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final AnalysisDirection pDirection,
      final QuantifiedFormulaManagerView pQuantifiedFormulaManagerView) {

    super(pOptions, pFormulaManager, pMachineModel, pVariableClassification, pLogManager,
        pShutdownNotifier, pTypeHandler, pDirection);

    variableClassification = pVariableClassification;
    options = pOptions;
    typeHandler = pTypeHandler;
    ptsMgr = new PointerTargetSetManagerHeapArray(options, formulaManager, typeHandler,
        shutdownNotifier);

    afmgr = pFormulaManager.getArrayFormulaManager();
    qfmgr = pQuantifiedFormulaManagerView;

    voidPointerFormulaType = typeHandler.getFormulaTypeFromCType(CPointerType.POINTER_TO_VOID);
    nullPointer = formulaManager.makeNumber(voidPointerFormulaType, 0);
  }

  /**
   * Returns the SMT formula array name for a C type.
   *
   * @param pType The type to get an array name for.
   * @return The array name for the type.
   */
  public static String getArrayName(final CType pType) {
    String result = arrayNameCache.get(pType);
    if (result != null) {
      return result;
    } else {
      result = POINTER_NAME_PREFIX + CTypeUtils.typeToString(pType).replace(' ', '_');
      arrayNameCache.put(pType, result);
      return result;
    }
  }

  /**
   * Checks, whether a symbol is an SMT array or not.
   *
   * @param pSymbol The name of the symbol.
   * @return Whether the symbol is an array or not.
   */
  public static boolean isSMTArray(final String pSymbol) {
    return pSymbol.startsWith(POINTER_NAME_PREFIX);
  }

  /**
   * Creates a formula for the base address of a term.
   *
   * @param pAddress The formula to create a base address for.
   * @return The base address for the formula.
   */
  Formula makeBaseAddressOfTerm(final Formula pAddress) {
    final ArrayFormula<?, ?> arrayFormula = afmgr.makeArray("__BASE_ADDRESS_OF_",
        FormulaType.IntegerType, voidPointerFormulaType);
    return afmgr.select(arrayFormula, pAddress);
  }

  /**
   * Eliminates the arrow operator for field references.
   *
   * @param pField The field reference with arrow operator.
   * @param pEdge  The current edge in the CFA (or logging purposes).
   * @return The field reference without the arrow operator.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  static CFieldReference eliminateArrow(
      final CFieldReference pField,
      final CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    if (pField.isPointerDereference()) {
      final CType fieldOwnerType = CTypeUtils.simplifyType(
          pField.getFieldOwner().getExpressionType());

      if (fieldOwnerType instanceof CPointerType) {
        return new CFieldReference(pField.getFileLocation(),
            pField.getExpressionType(), pField.getFieldName(),
            new CPointerExpression(pField.getFieldOwner().getFileLocation(),
                ((CPointerType) fieldOwnerType).getType(),
                pField.getFieldOwner()), false);
      } else {
        throw new UnrecognizedCCodeException("Can't dereference a non-pointer "
            + "in the field reference", pEdge, pField);
      }

    } else {
      return pField;
    }
  }

  /**
   * Checks if a variable is only found with a single type.
   *
   * @param pName         The name of the variable.
   * @param pType         The type of the variable.
   * @param pSsaSavedType The type of the variable as saved in the SSA map.
   */
  @Override
  protected void checkSsaSavedType(
      final String pName,
      final CType pType,
      CType pSsaSavedType) {
    if (pSsaSavedType != null) {
      pSsaSavedType = CTypeUtils.simplifyType(pSsaSavedType);
    }
    if (pSsaSavedType != null
        && !pSsaSavedType.equals(CTypeUtils.simplifyType(pType))) {
      logger.logf(Level.FINEST, "Variable %s was found with multiple types! (Type1: %s, Type2: %s)",
          pName, pSsaSavedType, pType);
    }
  }

  /**
   * Checks, whether a given variable has an SSA index or not.
   *
   * @param pName          The name of the variable.
   * @param pType          The  type of the variable.
   * @param pSSAMapBuilder The SSA map.
   * @return Whether a given variable has an SSA index or not.
   */
  boolean hasIndex(
      final String pName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder) {
    checkSsaSavedType(pName, pType, pSSAMapBuilder.getType(pName));
    return pSSAMapBuilder.getIndex(pName) > 0;
  }

  /**
   * Returns a formula for a dereference.
   *
   * @param pType            The type of the variable.
   * @param pAddress         The address formula of the variable that will be dereferenced.
   * @param pSSAMapBuilder   The SSA map.
   * @param pErrorConditions Additional error conditions.
   * @return A formula for the dereference of the variable.
   */
  Formula makeDereference(
      CType pType,
      final Formula pAddress,
      final SSAMapBuilder pSSAMapBuilder,
      final ErrorConditions pErrorConditions) {

    if (pErrorConditions.isEnabled()) {
      pErrorConditions.addInvalidDerefCondition(
          formulaManager.makeEqual(pAddress, nullPointer));
      pErrorConditions.addInvalidDerefCondition(formulaManager.makeLessThan(
          pAddress, makeBaseAddressOfTerm(pAddress), false));
    }

    return makeSafeDereference(pType, pAddress, pSSAMapBuilder);
  }

  /**
   * Returns a formula for a safe dereference.
   *
   * @param pType          The type of the variable.
   * @param pAddress       The address formula of the variable that will be dereferenced.
   * @param pSSAMapBuilder The SSA map.
   * @return A formula for a safe dereference of a variable.
   */
  Formula makeSafeDereference(
      CType pType,
      final Formula pAddress,
      final SSAMapBuilder pSSAMapBuilder) {

    pType = CTypeUtils.simplifyType(pType);
    final String ufName = getArrayName(pType);
    final int index = getIndex(ufName, pType, pSSAMapBuilder);
    final FormulaType<?> returnType = getFormulaTypeFromCType(pType);
    final ArrayFormula<?, ?> arrayFormula = afmgr.makeArray(ufName + "@" + index,
        FormulaType.IntegerType, returnType);
    return afmgr.select(arrayFormula, pAddress);
  }

  /**
   * Checks, whether a field is relevant in the composite type.
   *
   * @param pCompositeType The composite type to check.
   * @param pFieldName     The field to check its relevance.
   * @return Whether a field is relevant for the composite type.
   */
  @Override
  protected boolean isRelevantField(
      final CCompositeType pCompositeType,
      final String pFieldName) {
    return super.isRelevantField(pCompositeType, pFieldName)
        || getSizeof(pCompositeType) <= options.maxPreFilledAllocationSize();
  }

  /**
   * Checks, whether a variable declaration is addressed or not.
   *
   * @param pVar The variable declaration to check.
   * @return Whether the variable declaration is addressed or not.
   */
  private boolean isAddressedVariable(CDeclaration pVar) {
    return !variableClassification.isPresent()
        || variableClassification.get().getAddressedVariables().contains(pVar.getQualifiedName());
  }

  /**
   * Adds all fields of a C type to the pointer target set.
   *
   * @param pType The type of the composite type.
   * @param pPts  The underlying pointer target set.
   */
  private void addAllFields(
      final CType pType,
      final PointerTargetSetBuilder pPts) {

    if (pType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) pType;
      for (CCompositeTypeMemberDeclaration memberDeclaration
          : compositeType.getMembers()) {
        if (isRelevantField(compositeType, memberDeclaration.getName())) {
          pPts.addField(compositeType, memberDeclaration.getName());
          final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
          addAllFields(memberType, pPts);
        }
      }
    } else if (pType instanceof CArrayType) {
      final CType elementType = CTypeUtils.simplifyType(((CArrayType) pType).getType());
      addAllFields(elementType, pPts);
    }
  }

  /**
   * Adds a pre filled base to the pointer target set.
   *
   * @param pBase                    The name of the base.
   * @param pType                    The type of the base.
   * @param pPrepared                A flag indicating whether the base is prepared or not.
   * @param pForcePreFill            A flag indicating whether we force the pre fill.
   * @param pConstraints             Additional constraints
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   */
  void addPreFilledBase(
      final String pBase,
      final CType pType,
      final boolean pPrepared,
      final boolean pForcePreFill,
      final Constraints pConstraints,
      final PointerTargetSetBuilder pPointerTargetSetBuilder) {
    if (!pPrepared) {
      pConstraints.addConstraint(pPointerTargetSetBuilder.addBase(pBase, pType));
    } else {
      pPointerTargetSetBuilder.shareBase(pBase, pType);
    }

    if (pForcePreFill
        || (options.maxPreFilledAllocationSize() > 0
        && getSizeof(pType) <= options.maxPreFilledAllocationSize())) {
      addAllFields(pType, pPointerTargetSetBuilder);
    }
  }

  /**
   * Declares a shared base on a declaration.
   *
   * @param pDeclaration             The declaration.
   * @param pShareImmediately        A flag that indicates, if the base is shared immediately.
   * @param pConstraints             Additional constraints.
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   */
  private void declareSharedBase(
      final CDeclaration pDeclaration,
      final boolean pShareImmediately,
      final Constraints pConstraints,
      final PointerTargetSetBuilder pPointerTargetSetBuilder) {
    if (pShareImmediately) {
      addPreFilledBase(pDeclaration.getQualifiedName(), pDeclaration.getType(), false, false,
          pConstraints, pPointerTargetSetBuilder);
    } else if (isAddressedVariable(pDeclaration)
        || CTypeUtils.containsArray(pDeclaration.getType())) {
      pConstraints.addConstraint(pPointerTargetSetBuilder.prepareBase(
          pDeclaration.getQualifiedName(), CTypeUtils.simplifyType(pDeclaration.getType())));
    }
  }

  /**
   * Adds constraints for the value import.
   *
   * @param pCFAEdge       The current CFA edge.
   * @param pAddress       A formula for the current address.
   * @param pBase          The base of  the variable.
   * @param pFields        A list of fields of the composite type.
   * @param pSSAMapBuilder The SSA map.
   * @param pConstraints   Additional constraints.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  void addValueImportConstraints(
      final CFAEdge pCFAEdge,
      final Formula pAddress,
      final Variable pBase,
      final List<Pair<CCompositeType, String>> pFields,
      final SSAMapBuilder pSSAMapBuilder,
      final Constraints pConstraints)
      throws UnrecognizedCCodeException {

    final CType baseType = CTypeUtils.simplifyType(pBase.getType());

    if (baseType instanceof CArrayType) {
      throw new UnrecognizedCCodeException("Array access can't be encoded as a variable", pCFAEdge);

    } else if (baseType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) baseType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
          + compositeType;

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
        final Variable newBase = Variable.create(pBase.getName()
            + FIELD_NAME_SEPARATOR + memberName, memberType);

        if (hasIndex(newBase.getName(), newBase.getType(), pSSAMapBuilder)
            && isRelevantField(compositeType, memberName)) {
          pFields.add(Pair.of(compositeType, memberName));
          addValueImportConstraints(pCFAEdge, formulaManager.makePlus(
              pAddress, formulaManager.makeNumber(
                  voidPointerFormulaType, offset)), newBase,
              pFields, pSSAMapBuilder, pConstraints);
        }

        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += getSizeof(memberType);
        }
      }
    } else if (!(baseType instanceof CFunctionType) && !baseType.isIncomplete()) {
      // This adds a constraint *a = a for the case where we previously tracked
      // a variable directly and now via its address (we do not want to loose
      // the value previously stored in the variable).
      // Make sure to not add invalid-deref constraints for this dereference
      pConstraints.addConstraint(formulaManager.makeEqual(
          makeSafeDereference(baseType, pAddress, pSSAMapBuilder),
          makeVariable(pBase.getName(), baseType, pSSAMapBuilder)));
    }
  }

  /**
   * Expand a string literal to an array of characters.
   *
   * http://stackoverflow.com/a/6915917
   * As the C99 Draft Specification's 32nd Example in §6.7.8 (p. 130) states
   *    char s[] = "abc", t[3] = "abc";
   *  is identical to:
   *    char s[] = { 'a', 'b', 'c', '\0' }, t[] = { 'a', 'b', 'c' };
   *
   * @param pExpression The string that has to be expanded
   * @param pType       The type of the character array.
   * @return List of character-literal expressions
   */
  private static List<CCharLiteralExpression> expandStringLiteral(
      final CStringLiteralExpression pExpression,
      final CArrayType pType) {

    // The string is either NULL terminated, or not.
    // If the length is not provided explicitly, NULL termination is used
    Integer length = CTypeUtils.getArrayLength(pType);
    final String s = pExpression.getContentString();
    if (length == null) {
      length = s.length() + 1;
    }
    assert length >= s.length();

    // create one CharLiteralExpression for each character of the string
    final List<CCharLiteralExpression> result = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      result.add(new CCharLiteralExpression(pExpression.getFileLocation(),
          CNumericTypes.SIGNED_CHAR, s.charAt(i)));
    }


    // http://stackoverflow.com/questions/10828294/c-and-c-partial-initialization-of-automatic-structure
    // C99 Standard 6.7.8.21
    // If there are ... fewer characters in a string literal
    // used to initialize an array of known size than there are elements in the
    // array, the remainder of the aggregate shall be initialized implicitly ...
    for (int i = s.length(); i < length; i++) {
      result.add(new CCharLiteralExpression(pExpression.getFileLocation(),
          CNumericTypes.SIGNED_CHAR, '\0'));
    }

    return result;
  }

  /**
   * Expands a string literal to an array of characters.
   *
   * @param pAssignments The list of assignments.
   * @return An assignment statement.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private static List<CExpressionAssignmentStatement> expandStringLiterals(
      final List<CExpressionAssignmentStatement> pAssignments)
      throws UnrecognizedCCodeException {

    final List<CExpressionAssignmentStatement> result = new ArrayList<>();
    for (CExpressionAssignmentStatement assignment : pAssignments) {
      final CExpression rhs = assignment.getRightHandSide();
      if (rhs instanceof CStringLiteralExpression) {
        final CExpression lhs = assignment.getLeftHandSide();
        final CType lhsType = lhs.getExpressionType();
        final CArrayType lhsArrayType;
        if (lhsType instanceof CArrayType) {
          lhsArrayType = (CArrayType) lhsType;
        } else if (lhsType instanceof CPointerType) {
          lhsArrayType = new CArrayType(false, false, ((CPointerType) lhsType).getType(), null);
        } else {
          throw new UnrecognizedCCodeException("Assigning string literal to "
              + lhsType.toString(), assignment);
        }

        List<CCharLiteralExpression> chars = expandStringLiteral(
            (CStringLiteralExpression) rhs, lhsArrayType);

        int offset = 0;
        for (CCharLiteralExpression e : chars) {
          result.add(new CExpressionAssignmentStatement(
              assignment.getFileLocation(),
              new CArraySubscriptExpression(lhs.getFileLocation(),
                  lhsArrayType.getType(), lhs,
                  new CIntegerLiteralExpression(lhs.getFileLocation(),
                      CNumericTypes.INT, BigInteger.valueOf(offset))), e));
          offset++;
        }
      } else {
        result.add(assignment);
      }
    }
    return result;
  }

  /**
   * Expands an assignment list.
   *
   * @param pDeclaration         The declaration of the variable.
   * @param pExplicitAssignments A list of explicit assignments to the variable.
   * @return A list of assignment statements.
   */
  private List<CExpressionAssignmentStatement> expandAssignmentList(
      final CVariableDeclaration pDeclaration,
      final List<CExpressionAssignmentStatement> pExplicitAssignments) {
    final CType variableType = CTypeUtils.simplifyType(pDeclaration.getType());
    final CLeftHandSide lhs = new CIdExpression(pDeclaration.getFileLocation(),
        variableType, pDeclaration.getName(), pDeclaration);
    final Set<String> alreadyAssigned =
        pExplicitAssignments
            .stream()
            .map(statement -> statement.getLeftHandSide().toString())
            .collect(Collectors.toSet());

    final List<CExpressionAssignmentStatement> defaultAssignments = new ArrayList<>();
    expandAssignmentList(variableType, lhs, alreadyAssigned, defaultAssignments);
    defaultAssignments.addAll(pExplicitAssignments);
    return defaultAssignments;
  }

  /**
   * Expands an assignment list.
   *
   * @param pType               The type of the assignment.
   * @param pLhs                The left hand side of the assignment.
   * @param pAlreadyAssigned    A set of already assigned values.
   * @param pDefaultAssignments A list of the default assignments.
   */
  private void expandAssignmentList(
      CType pType,
      final CLeftHandSide pLhs,
      final Set<String> pAlreadyAssigned,
      final List<CExpressionAssignmentStatement> pDefaultAssignments) {
    if (pAlreadyAssigned.contains(pLhs.toString())) {
      return;
    }

    pType = CTypeUtils.simplifyType(pType);
    if (pType instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) pType;
      final CType elementType = CTypeUtils.simplifyType(arrayType.getType());
      final Integer length = CTypeUtils.getArrayLength(arrayType);

      if (length != null) {
        for (int i = 0; i < length; i++) {
          final CLeftHandSide newLhs = new CArraySubscriptExpression(
              pLhs.getFileLocation(), elementType, pLhs,
              new CIntegerLiteralExpression(pLhs.getFileLocation(),
                  CNumericTypes.INT, BigInteger.valueOf(i)));

          expandAssignmentList(elementType, newLhs, pAlreadyAssigned, pDefaultAssignments);
        }
      }

    } else if (pType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) pType;
      if (compositeType.getKind() == ComplexTypeKind.UNION) {
        // If it is a union, we must make sure that the first member is initialized,
        // but only if none of the members appear in alreadyAssigned.
        // The way it is currently implemented this is very difficult to check,
        // so for now we initialize none of the union members to be safe.
        // TODO: add implicit initializers for union members
        return;
      }
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final CType memberType = memberDeclaration.getType();
        final CLeftHandSide newLhs = new CFieldReference(pLhs.getFileLocation(),
            memberType, memberDeclaration.getName(), pLhs, false);
        expandAssignmentList(memberType, newLhs, pAlreadyAssigned, pDefaultAssignments);
      }
    } else {
      assert isSimpleType(pType);
      CExpression initExp = ((CInitializerExpression) CDefaults.forType(pType,
          pLhs.getFileLocation())).getExpression();
      pDefaultAssignments.add(new CExpressionAssignmentStatement(
          pLhs.getFileLocation(), pLhs, initExp));
    }
  }

  /**
   * Creates a builder for pointer target sets.
   *
   * @param pPointerTargetSet The current pointer target set.
   * @return A builder for pointer target sets.
   */
  @Override
  protected PointerTargetSetBuilder createPointerTargetSetBuilder(
      PointerTargetSet pPointerTargetSet) {
    return new RealPointerTargetSetBuilder(pPointerTargetSet, formulaManager, ptsMgr, options);
  }

  /**
   * Merges two sets of pointer targets.
   *
   * @param pPointerTargetSet1 The first set of pointer targets.
   * @param pPointerTargetSet2 The second set of pointer targets.
   * @param pSSAMapBuilder     The SSA map.
   * @return A set of pointer targets merged from both.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPointerTargetSet1,
      PointerTargetSet pPointerTargetSet2,
      SSAMapBuilder pSSAMapBuilder) throws InterruptedException {
    return ptsMgr.mergePointerTargetSets(pPointerTargetSet1, pPointerTargetSet2,
        pSSAMapBuilder, this);
  }

  /**
   * Creates a visitor for right hand side expressions.
   *
   * @param pCFAEdge                 The current edge of the CFA.
   * @param pFunction                The name of the current function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying set of pointer targets.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A visitor for right hand side expressions.
   */
  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCCodeException>
  createCRightHandSideVisitor(
      CFAEdge pCFAEdge,
      String pFunction,
      SSAMapBuilder pSSAMapBuilder,
      PointerTargetSetBuilder pPointerTargetSetBuilder,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {

    CExpressionVisitorWithHeapArray rhsVisitor =
        new CExpressionVisitorWithHeapArray(this, pCFAEdge, pFunction, pSSAMapBuilder,
            pConstraints, pErrorConditions, pPointerTargetSetBuilder);
    return rhsVisitor.asFormulaVisitor();
  }

  /**
   * Creates a formula for a {@code return} statement in C code.
   *
   * @param pAssignment              The (optional) assignment done at the return statement.
   * @param pReturnEdge              The return edge of the CFA.
   * @param pFunction                The name of the current function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A formula for a return statement.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeReturn(
      final Optional<CAssignment> pAssignment,
      final CReturnStatementEdge pReturnEdge,
      final String pFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {
    BooleanFormula result = super.makeReturn(pAssignment, pReturnEdge, pFunction, pSSAMapBuilder,
        pPointerTargetSetBuilder, pConstraints, pErrorConditions);

    if (pAssignment.isPresent()) {
      final CVariableDeclaration returnVariableDeclaraton =
          ((CFunctionEntryNode) pReturnEdge.getSuccessor().getEntryNode()).getReturnVariable().get();
      final boolean containsArray = CTypeUtils.containsArray(returnVariableDeclaraton.getType());

      declareSharedBase(returnVariableDeclaraton, containsArray, pConstraints,
          pPointerTargetSetBuilder);
    }

    return result;
  }

  /**
   * Creates a formula for an assignment.
   *
   * @param pLhs                     the left-hand-side of the assignment
   * @param pLhsForChecking          a left-hand-side of the assignment (for most cases: {@code lhs
   *                                 == lhsForChecking}), that is used to check, if the assignment
   *                                 is important. If the assignment is not important, we return
   *                                 TRUE.
   * @param pRhs                     the right-hand-side of the assignment
   * @param pCFAEdge                 The current edge in the CFA.
   * @param pFunction                The name of the current function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeAssignment(
      final CLeftHandSide pLhs,
      final CLeftHandSide pLhsForChecking,
      CRightHandSide pRhs,
      final CFAEdge pCFAEdge,
      final String pFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    if (pRhs instanceof CExpression) {
      pRhs = makeCastFromArrayToPointerIfNecessary((CExpression) pRhs, pLhs.getExpressionType());
    }

    AssignmentHandler assignmentHandler = new AssignmentHandler(this, pCFAEdge,pFunction,
        pSSAMapBuilder, pPointerTargetSetBuilder, pConstraints,pErrorConditions);
    return assignmentHandler.handleAssignment(pLhs, pLhsForChecking, pRhs, false, null);
  }

  /**
   * Creates a log message string from a given message and the current CFA edge.
   *
   * @param pMsg     The message string.
   * @param pCFAEdge The current CFA edge.
   * @return A log message string.
   */
  private static String getLogMessage(
      final String pMsg,
      final CFAEdge pCFAEdge) {
    return pCFAEdge.getFileLocation() + ": " + pMsg + ": " + pCFAEdge.getDescription();
  }

  /**
   * Logs a message for debugging purpose.
   *
   * @param pMsg     The message to be logged.
   * @param pCFAEdge The current edge in the CFA.
   */
  private void logDebug(final String pMsg, final CFAEdge pCFAEdge) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(pMsg, pCFAEdge));
    }
  }

  /**
   * Creates a formula for declarations in the C code.
   *
   * @param pDeclarationEdge         The declaration edge in the CFA.
   * @param pFunction                The name of the current function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A formula for a declaration in C code.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeDeclaration(
      final CDeclarationEdge pDeclarationEdge,
      final String pFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    // TODO merge with super-class method

    if (pDeclarationEdge.getDeclaration() instanceof CTypeDeclaration) {
      final CType declarationType = CTypeUtils.simplifyType(
          (pDeclarationEdge.getDeclaration()).getType());
      if (declarationType instanceof CCompositeType) {
        typeHandler.addCompositeTypeToCache((CCompositeType) declarationType);
      }
    }

    if (!(pDeclarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // function declaration, typedef etc.
      logDebug("Ignoring declaration", pDeclarationEdge);
      return bfmgr.makeBoolean(true);
    }

    CVariableDeclaration declaration = (CVariableDeclaration) pDeclarationEdge.getDeclaration();

    // makeFreshIndex(variableName, declaration.getType(), ssa);
    // TODO: Make sure about correctness of SSA indices without this trick!

    CType declarationType = CTypeUtils.simplifyType(declaration.getType());

    if (!isRelevantVariable(declaration) && !isAddressedVariable(declaration)) {
      // The variable is unused
      logDebug("Ignoring declaration of unused variable", pDeclarationEdge);
      return bfmgr.makeBoolean(true);
    }

    if (pErrorConditions.isEnabled()) {
      final Formula address = makeConstant(PointerTargetSet.getBaseName(
          declaration.getQualifiedName()), CTypeUtils.getBaseType(declarationType));
      pConstraints.addConstraint(formulaManager.makeEqual(makeBaseAddressOfTerm(address), address));
    }

    // if there is an initializer associated to this variable,
    // take it into account
    final CInitializer initializer = declaration.getInitializer();

    // Fixing unsized array declarations
    if (declarationType instanceof CArrayType
        && ((CArrayType) declarationType).getLength() == null) {
      final Integer actualLength;
      if (initializer instanceof CInitializerList) {
        actualLength = ((CInitializerList) initializer).getInitializers().size();
      } else if (initializer instanceof CInitializerExpression
          && ((CInitializerExpression) initializer).getExpression()
          instanceof CStringLiteralExpression) {
        actualLength = ((CStringLiteralExpression) ((CInitializerExpression)
            initializer).getExpression()).getContentString().length() + 1;
      } else {
        actualLength = null;
      }

      if (actualLength != null) {
        declarationType = new CArrayType(declarationType.isConst(),
            declarationType.isVolatile(),
            ((CArrayType) declarationType).getType(),
            new CIntegerLiteralExpression(declaration.getFileLocation(),
                machineModel.getPointerDiffType(),
                BigInteger.valueOf(actualLength)));

        declaration = new CVariableDeclaration(declaration.getFileLocation(),
            declaration.isGlobal(), declaration.getCStorageClass(),
            declarationType, declaration.getName(), declaration.getOrigName(),
            declaration.getQualifiedName(), initializer);
      }
    }

    declareSharedBase(declaration, false, pConstraints, pPointerTargetSetBuilder);
    if (CTypeUtils.containsArray(declarationType)) {
      addPreFilledBase(declaration.getQualifiedName(), declarationType, true, false, pConstraints,
          pPointerTargetSetBuilder);
    }

    if (options.useParameterVariablesForGlobals() && declaration.isGlobal()) {
      globalDeclarations.add(declaration);
    }

    final CIdExpression lhs = new CIdExpression(declaration.getFileLocation(), declaration);
    final AssignmentHandler assignmentHandler = new AssignmentHandler(this, pDeclarationEdge,
        pFunction, pSSAMapBuilder, pPointerTargetSetBuilder, pConstraints, pErrorConditions);
    final BooleanFormula result;
    if (initializer instanceof CInitializerExpression || initializer == null) {

      if (initializer != null) {
        result = assignmentHandler.handleAssignment(lhs, lhs,
            ((CInitializerExpression) initializer).getExpression(), false, null);
      } else if (isRelevantVariable(declaration) && !declarationType.isIncomplete()) {
        result = assignmentHandler.handleAssignment(lhs, lhs, null, false, null);
      } else {
        result = bfmgr.makeBoolean(true);
      }

    } else if (initializer instanceof CInitializerList) {

      List<CExpressionAssignmentStatement> assignments =
          CInitializers.convertToAssignments(declaration, pDeclarationEdge);
      if (options.handleStringLiteralInitializers()) {
        // Special handling for string literal initializers -- convert them
        // into character arrays
        assignments = expandStringLiterals(assignments);
      }
      if (options.handleImplicitInitialization()) {
        assignments = expandAssignmentList(declaration, assignments);
      }
      if (qfmgr == null || !(declarationType instanceof CArrayType)) {
        result = assignmentHandler.handleInitializationAssignments(lhs, assignments);
      } else {
        result = assignmentHandler.handleInitializationAssignmentsWithQuantifier(
            lhs, assignments, qfmgr, false);
      }

    } else {
      throw new UnrecognizedCCodeException("Unrecognized initializer",
          pDeclarationEdge, initializer);
    }

    return result;
  }

  /**
   * Creates a predicate formula for an expression and its truth assumption.
   *
   * @param pExpression              The expression.
   * @param pTruthAssumption         The assumption for the truth of the expression.
   * @param pCFAEdge                 The current edge in the CFA.
   * @param pFunction                The name of the current function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A predicate formula for an expression and its truth assumption.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makePredicate(
      final CExpression pExpression,
      final boolean pTruthAssumption,
      final CFAEdge pCFAEdge,
      final String pFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {
    final CType expressionType = CTypeUtils.simplifyType(pExpression.getExpressionType());
    CExpressionVisitorWithHeapArray ev =
        new CExpressionVisitorWithHeapArray(this, pCFAEdge, pFunction, pSSAMapBuilder,
            pConstraints, pErrorConditions, pPointerTargetSetBuilder);
    BooleanFormula result = toBooleanFormula(ev.asValueFormula(
        pExpression.accept(ev), expressionType));

    if (options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(this, pCFAEdge, pSSAMapBuilder,
          pPointerTargetSetBuilder, pConstraints, pErrorConditions);
      memoryHandler.handleDeferredAllocationsInAssume(pExpression,
          ev.getUsedDeferredAllocationPointers());
    }

    if (!pTruthAssumption) {
      result = bfmgr.not(result);
    }

    pPointerTargetSetBuilder.addEssentialFields(ev.getInitializedFields());
    pPointerTargetSetBuilder.addEssentialFields(ev.getUsedFields());
    return result;
  }

  /**
   * Creates a formula for a function call.
   *
   * @param pEdge                    The function call edge in the CFA.
   * @param pCallerFunction          The name of the caller function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A formula representing the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeFunctionCall(
      final CFunctionCallEdge pEdge,
      final String pCallerFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    final CFunctionEntryNode entryNode = pEdge.getSuccessor();
    BooleanFormula result = super.makeFunctionCall(pEdge, pCallerFunction, pSSAMapBuilder,
        pPointerTargetSetBuilder, pConstraints, pErrorConditions);

    for (CParameterDeclaration formalParameter : entryNode.getFunctionParameters()) {
      final CType parameterType = CTypeUtils.simplifyType(formalParameter.getType());
      final CVariableDeclaration formalDeclaration = formalParameter.asVariableDeclaration();
      final CVariableDeclaration declaration;

      if (options.useParameterVariables()) {
        CParameterDeclaration tmpParameter = new CParameterDeclaration(
            formalParameter.getFileLocation(), formalParameter.getType(),
            formalParameter.getName() + PARAM_VARIABLE_NAME);

        tmpParameter.setQualifiedName(formalParameter.getQualifiedName() + PARAM_VARIABLE_NAME);
        declaration = tmpParameter.asVariableDeclaration();
      } else {
        declaration = formalDeclaration;
      }
      declareSharedBase(declaration, CTypeUtils.containsArray(parameterType),
          pConstraints, pPointerTargetSetBuilder);
    }

    return result;
  }

  /**
   * Creates a formula for a function exit.
   *
   * @param pSummaryEdge             The function's summary edge in the CFA.
   * @param pCalledFunction          The name of the called function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying pointer target set
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   * @return A formula for the function exit.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeExitFunction(
      final CFunctionSummaryEdge pSummaryEdge,
      final String pCalledFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions)
      throws UnrecognizedCCodeException, InterruptedException {

    final BooleanFormula result = super.makeExitFunction(pSummaryEdge, pCalledFunction,
        pSSAMapBuilder, pPointerTargetSetBuilder, pConstraints, pErrorConditions);

    DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(this, pSummaryEdge,
        pSSAMapBuilder, pPointerTargetSetBuilder, pConstraints, pErrorConditions);
    memoryHandler.handleDeferredAllocationInFunctionExit(pCalledFunction);

    return result;
  }

  // Overrides just for visibility in other classes of this package

  /**
   * Evaluates the return type of a function call.
   *
   * @param pExpression The function call expression.
   * @param pCFAEdge    The edge in the CFA.
   * @return The type of the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  protected CType getReturnType(
      final CFunctionCallExpression pExpression,
      final CFAEdge pCFAEdge) throws UnrecognizedCCodeException {
    return super.getReturnType(pExpression, pCFAEdge);
  }

  /**
   * Creates an expression for a cast from an array to a pointer type. This cast is only done, if
   * necessary.
   *
   * @param pExpression The expression to get casted.
   * @param pTargetType The target type of the cast.
   * @return A pointer expression for the casted array expression.
   */
  @Override
  protected CExpression makeCastFromArrayToPointerIfNecessary(
      final CExpression pExpression,
      final CType pTargetType) {
    return super.makeCastFromArrayToPointerIfNecessary(pExpression, pTargetType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeCast(
      final CType pFromType,
      final CType pToType,
      final Formula pFormula,
      final Constraints pConstraints,
      final CFAEdge pCFAEdge)
      throws UnrecognizedCCodeException {
    return super.makeCast(pFromType, pToType, pFormula, pConstraints, pCFAEdge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeConstant(final String pName, final CType pType) {
    return super.makeConstant(pName, pType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeVariable(
      final String pName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder) {
    return super.makeVariable(pName, pType, pSSAMapBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeFreshVariable(
      final String pName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder) {
    return super.makeFreshVariable(pName, pType, pSSAMapBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int makeFreshIndex(
      final String pName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder) {
    return super.makeFreshIndex(pName, pType, pSSAMapBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getIndex(
      final String pName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder) {
    return super.getIndex(pName, pType, pSSAMapBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getFreshIndex(
      final String pName,
      final CType pType,
      final SSAMapBuilder pSSAMapBuilder) {
    return super.getFreshIndex(pName, pType, pSSAMapBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getSizeof(final CType pType) {
    return super.getSizeof(pType);
  }

  /**
   * Checks, whether a left hand side is relevant for the analysis.
   *
   * @param pLhs The left hand side to check.
   * @return Whether a left hand side is relevant for the analysis.
   */
  @Override
  protected boolean isRelevantLeftHandSide(final CLeftHandSide pLhs) {
    return super.isRelevantLeftHandSide(pLhs);
  }
}
