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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.RealPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class CToFormulaConverterWithPointerAliasing extends CtoFormulaConverter {

  /**
   * Prefix for marking symbols in the SSAMap that do not need update terms.
   */
  static final String SSAMAP_SYMBOL_WITHOUT_UPDATE_PREFIX = "#";

  // Overrides just for visibility in other classes of this package

  @SuppressWarnings("hiding")
  final LogManagerWithoutDuplicates logger = super.logger;
  @SuppressWarnings("hiding")
  final FormulaManagerView fmgr = super.fmgr;
  @SuppressWarnings("hiding")
  final BooleanFormulaManagerView bfmgr = super.bfmgr;
  @SuppressWarnings("hiding")
  final MachineModel machineModel = super.machineModel;
  @SuppressWarnings("hiding")
  final ShutdownNotifier shutdownNotifier = super.shutdownNotifier;

  private final @Nullable ArrayFormulaManagerView afmgr;
  final TypeHandlerWithPointerAliasing typeHandler;
  final PointerTargetSetManager ptsMgr;

  final FormulaType<?> voidPointerFormulaType;
  final Formula nullPointer;
  private MemoryRegionManager regionMgr;

  public CToFormulaConverterWithPointerAliasing(
      final FormulaEncodingWithPointerAliasingOptions pOptions,
      final FormulaManagerView formulaManagerView,
      final MachineModel pMachineModel,
      final Optional<VariableClassification> pVariableClassification,
      final LogManager logger,
      final ShutdownNotifier pShutdownNotifier,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final AnalysisDirection pDirection)
      throws InvalidConfigurationException {
    super(pOptions, formulaManagerView, pMachineModel, pVariableClassification, logger, pShutdownNotifier, pTypeHandler, pDirection);

    if (pDirection == AnalysisDirection.BACKWARD) {
      throw new InvalidConfigurationException(
          "Backward formula construction is not yet implemented for pointer aliasing.");
    }

    variableClassification = pVariableClassification;
    options = pOptions;
    typeHandler = pTypeHandler;

    if(options.useMemoryRegions()) {
      //create BnB regions here
      //get referenced fields and addressed fields from variable classification
      //use aliasingTypeHandler to simplify types
      regionMgr = buildBnBMemoryRegions();
    } else {
      //if !useMemoryRegions then create default regions - all in one for each type
      regionMgr = new DefaultRegionManager();
    }

    ptsMgr = new PointerTargetSetManager(options, fmgr, typeHandler, shutdownNotifier, regionMgr);
    afmgr = options.useArraysForHeap() ? fmgr.getArrayFormulaManager() : null;

    voidPointerFormulaType = typeHandler.getFormulaTypeFromCType(CPointerType.POINTER_TO_VOID);
    nullPointer = fmgr.makeNumber(voidPointerFormulaType, 0);
  }

  private MemoryRegionManager buildBnBMemoryRegions() {
    if(!variableClassification.isPresent()) {
      return new BnBRegionManager(variableClassification, ImmutableMultimap.<CType, String>of());
    }
    VariableClassification var = variableClassification.get();
    Multimap<CCompositeType, String> relevant = var.getRelevantFields();
    Multimap<CCompositeType, String> addressed = var.getAddressedFields();

    Multimap<CType, String> bnb = HashMultimap.create();
    for(Map.Entry<CCompositeType, String> p : relevant.entries()) {
      if(!addressed.containsEntry(p.getKey(), p.getValue())) {
        CType type = typeHandler.simplifyType(p.getKey());
        bnb.put(type, p.getValue());
      }
    }
    return new BnBRegionManager(variableClassification, ImmutableMultimap.<CType, String>copyOf(bnb));
  }

  /**
   * Returns the SMT symbol name for encoding a pointer access for a C type.
   *
   * @param type The type to get the symbol name for.
   * @return The symbol name for the type.
   */
  public static String getPointerAccessNameForType(final CType type) {
    String result = pointerNameCache.get(type);
    if (result != null) {
      return result;
    } else {
      result = POINTER_NAME_PREFIX + CTypeUtils.typeToString(type).replace(' ', '_');
      pointerNameCache.put(type, result);
      return result;
    }
  }

  /**
   * Checks, whether a symbol is a pointer access encoded in SMT.
   *
   * @param symbol The name of the symbol.
   * @return Whether the symbol is a pointer access or not.
   */
  private static boolean isPointerAccessSymbol(final String symbol) {
    return symbol.startsWith(POINTER_NAME_PREFIX);
  }

  /**
   * Creates a formula for the base address of a term.
   *
   * @param address The formula to create a base address for.
   * @return The base address for the formula.
   */
  Formula makeBaseAddressOfTerm(final Formula address) {
    return ptsMgr.makePointerDereference("__BASE_ADDRESS_OF__", voidPointerFormulaType, address);
  }

  /**
   * Checks if a variable is only found with a single type.
   *
   * @param name The name of the variable.
   * @param type The type of the variable.
   * @param ssaSavedType The type of the variable as saved in the SSA map.
   */
  @Override
  protected void checkSsaSavedType(
      final String name, final CType type, final @Nullable CType ssaSavedType) {
    if (ssaSavedType != null && !ssaSavedType.equals(type)) {
      checkIsSimplified(ssaSavedType);
      checkIsSimplified(type);
      logger.logf(Level.FINEST,
                  "Variable %s was found with multiple types! (Type1: %s, Type2: %s)",
                  name,
                  ssaSavedType,
                  type);
    }
  }

  @Override
  public BooleanFormula makeSsaUpdateTerm(
      final String symbolName,
      final CType symbolType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts)
      throws InterruptedException {
    checkArgument(oldIndex > 0 && newIndex > oldIndex);

    if (symbolName.startsWith(SSAMAP_SYMBOL_WITHOUT_UPDATE_PREFIX)) {
      return bfmgr.makeTrue();
    } else if (isPointerAccessSymbol(symbolName)) {
      if(!options.useMemoryRegions()) {
        assert symbolName.equals(getPointerAccessNameForType(symbolType));
      } else {
        //TODO: find a better assertion for the memory regions case
      }
      if (options.useArraysForHeap()) {
        return makeSsaArrayMerger(symbolName, symbolType, oldIndex, newIndex);
      } else {
        return makeSsaUFMerger(symbolName, symbolType, oldIndex, newIndex, pts);
      }
    }
    return super.makeSsaUpdateTerm(symbolName, symbolType, oldIndex, newIndex, pts);
  }

  private BooleanFormula makeSsaArrayMerger(
      final String pFunctionName,
      final CType pReturnType,
      final int pOldIndex,
      final int pNewIndex) {

    final FormulaType<?> returnFormulaType = getFormulaTypeFromCType(pReturnType);
    final ArrayFormula<?, ?> newArray =
        afmgr.makeArray(pFunctionName, pNewIndex, voidPointerFormulaType, returnFormulaType);
    final ArrayFormula<?, ?> oldArray =
        afmgr.makeArray(pFunctionName, pOldIndex, voidPointerFormulaType, returnFormulaType);
    return fmgr.makeEqual(newArray, oldArray);
  }

  private BooleanFormula makeSsaUFMerger(
      final String functionName,
      final CType returnType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts)
      throws InterruptedException {

    final FormulaType<?> returnFormulaType = getFormulaTypeFromCType(returnType);

    if (options.useQuantifiersOnArrays()) {
      Formula counter = fmgr.makeVariable(voidPointerFormulaType, functionName + "@counter");
      return fmgr.getQuantifiedFormulaManager()
          .forall(
              counter,
              makeRetentionConstraint(
                  functionName, oldIndex, newIndex, returnFormulaType, counter));
    }

    List<BooleanFormula> result = new ArrayList<>();
    for (final PointerTarget target : pts.getAllTargets(functionName)) {
      shutdownNotifier.shutdownIfNecessary();
      final Formula targetAddress = makeFormulaForTarget(target);
      result.add(
          makeRetentionConstraint(
              functionName, oldIndex, newIndex, returnFormulaType, targetAddress));
    }

    return bfmgr.and(result);
  }

  /**
   * Checks, whether a given variable has an SSA index or not.
   *
   * @param name The name of the variable.
   * @param type The  type of the variable.
   * @param ssa The SSA map.
   * @return Whether a given variable has an SSA index or not.
   */
  boolean hasIndex(final String name, final CType type, final SSAMapBuilder ssa) {
    checkSsaSavedType(name, type, ssa.getType(name));
    return ssa.getIndex(name) > 0;
  }

  /**
   * Returns a formula for a dereference.
   *
   * @param type The type of the variable.
   * @param address The address formula of the variable that will be dereferenced.
   * @param ssa The SSA map.
   * @param errorConditions Additional error conditions.
   * @return A formula for the dereference of the variable.
   */
  Formula makeDereference(CType type,
                         final Formula address,
                         final SSAMapBuilder ssa,
                         final ErrorConditions errorConditions,
                         final MemoryRegion region) {
    if (errorConditions.isEnabled()) {
      errorConditions.addInvalidDerefCondition(fmgr.makeEqual(address, nullPointer));
      errorConditions.addInvalidDerefCondition(fmgr.makeLessThan(address, makeBaseAddressOfTerm(address), false));
    }
    return makeSafeDereference(type, address, ssa, region);
  }

  /**
   * Returns a formula for a safe dereference.
   *
   * @param type The type of the variable.
   * @param address The address formula of the variable that will be dereferenced.
   * @param ssa The SSA map.
   * @return A formula for a safe dereference of a variable.
   */
  Formula makeSafeDereference(CType type,
                         final Formula address,
                         final SSAMapBuilder ssa,
                         final MemoryRegion region) {
    checkIsSimplified(type);
    final String ufName = regionMgr.getPointerAccessName(region);
    final int index = getIndex(ufName, type, ssa);
    final FormulaType<?> returnType = getFormulaTypeFromCType(type);
    return ptsMgr.makePointerDereference(ufName, returnType, index, address);
  }

  Formula makeFormulaForTarget(final PointerTarget target) {
    return fmgr.makePlus(
        fmgr.makeVariable(voidPointerFormulaType, target.getBaseName()),
        fmgr.makeNumber(voidPointerFormulaType, target.getOffset()));
  }

  BooleanFormula makeRetentionConstraint(
      final String targetName,
      final int oldIndex,
      final int newIndex,
      final FormulaType<?> type,
      final Formula targetAddress) {
    return fmgr.assignment(
        ptsMgr.makePointerDereference(targetName, type, newIndex, targetAddress),
        ptsMgr.makePointerDereference(targetName, type, oldIndex, targetAddress));
  }

  /**
   * Checks, whether a field is relevant in the composite type.
   *
   * @param compositeType The composite type to check.
   * @param fieldName The field to check its relevance.
   * @return Whether a field is relevant for the composite type.
   */
  @Override
  protected boolean isRelevantField(final CCompositeType compositeType,
                          final String fieldName) {
    return super.isRelevantField(compositeType, fieldName)
        || getSizeof(compositeType) <= options.maxPreFilledAllocationSize();
  }

  /**
   * Checks, whether a variable declaration is addressed or not.
   *
   * @param var The variable declaration to check.
   * @return Whether the variable declaration is addressed or not.
   */
  private boolean isAddressedVariable(CDeclaration var) {
    return !variableClassification.isPresent() ||
        variableClassification.get().getAddressedVariables().contains(var.getQualifiedName());
  }

  /**
   * Adds all fields of a C type to the pointer target set.
   *
   * @param type The type of the composite type.
   * @param pts The underlying pointer target set.
   */
  private void addAllFields(final CType type, final PointerTargetSetBuilder pts) {
    if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (isRelevantField(compositeType, memberDeclaration.getName())) {
          pts.addField(compositeType, memberDeclaration.getName());
          final CType memberType = typeHandler.getSimplifiedType(memberDeclaration);
          addAllFields(memberType, pts);
        }
      }
    } else if (type instanceof CArrayType) {
      final CType elementType = checkIsSimplified(((CArrayType) type).getType());
      addAllFields(elementType, pts);
    }
  }

  /**
   * Adds a pre filled base to the pointer target set.
   *
   * @param base The name of the base.
   * @param type The type of the base.
   * @param prepared A flag indicating whether the base is prepared or not.
   * @param forcePreFill A flag indicating whether we force the pre fill.
   * @param constraints Additional constraints
   * @param pts The underlying pointer target set.
   */
  void addPreFilledBase(final String base,
                        final CType type,
                        final boolean prepared,
                        final boolean forcePreFill,
                        final Constraints constraints,
                        final PointerTargetSetBuilder pts) {
    if (!prepared) {
      constraints.addConstraint(pts.addBase(base, type));
    } else {
      pts.shareBase(base, type);
    }
    if (forcePreFill ||
        (options.maxPreFilledAllocationSize() > 0 && getSizeof(type) <= options.maxPreFilledAllocationSize())) {
      addAllFields(type, pts);
    }
  }

  /**
   * Declares a shared base on a declaration.
   *
   * @param declaration The declaration.
   * @param originalDeclaration the declaration used to determine if the base corresponds to a function parameter
   *        (needed to distinguish real (non-moving) arrays from pointers)
   * @param shareImmediately A flag that indicates, if the base is shared immediately.
   * @param constraints Additional constraints.
   * @param pts The underlying pointer target set.
   */
  private void declareSharedBase(
      final CDeclaration declaration,
      final CSimpleDeclaration originalDeclaration,
      final boolean shareImmediately,
      final Constraints constraints,
      final PointerTargetSetBuilder pts) {
    CType type = typeHandler.getSimplifiedType(declaration);
    if (shareImmediately) {
      addPreFilledBase(declaration.getQualifiedName(), type, false, false, constraints, pts);
    } else if (isAddressedVariable(declaration)
        || CTypeUtils.containsArray(type, originalDeclaration)) {
      constraints.addConstraint(pts.prepareBase(declaration.getQualifiedName(), type));
    }
  }

  /**
   * Adds constraints for the value import.
   *
   * @param cfaEdge The current CFA edge.
   * @param address A formula for the current address.
   * @param base The base of  the variable.
   * @param fields A list of fields of the composite type.
   * @param ssa The SSA map.
   * @param constraints Additional constraints.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  void addValueImportConstraints(final CFAEdge cfaEdge,
                                 final Formula address,
                                 final Variable base,
                                 final List<Pair<CCompositeType, String>> fields,
                                 final SSAMapBuilder ssa,
                                 final Constraints constraints,
                                 @Nullable final MemoryRegion region) throws
                                                                  UnrecognizedCCodeException {
    final CType baseType = base.getType();
    if (baseType instanceof CArrayType) {
      throw new UnrecognizedCCodeException("Array access can't be encoded as a variable", cfaEdge);
    } else if (baseType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) baseType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final int offset = typeHandler.getBitOffset(compositeType, memberName);
        final CType memberType = typeHandler.getSimplifiedType(memberDeclaration);
        final Variable newBase = Variable.create(base.getName() + FIELD_NAME_SEPARATOR + memberName,
                                                 memberType);
        if (hasIndex(newBase.getName(), newBase.getType(), ssa) &&
            isRelevantField(compositeType, memberName)) {
          fields.add(Pair.of(compositeType, memberName));
          MemoryRegion newRegion = regionMgr.makeMemoryRegion(compositeType, memberType, memberName);
          addValueImportConstraints(cfaEdge,
                                    fmgr.makePlus(address, fmgr.makeNumber(voidPointerFormulaType, offset)),
                                    newBase,
                                    fields,
                                    ssa,
                                    constraints,
                                    newRegion);
        }
      }
    } else if (!(baseType instanceof CFunctionType) && !baseType.isIncomplete()) {
      // This adds a constraint *a = a for the case where we previously tracked
      // a variable directly and now via its address (we do not want to loose
      // the value previously stored in the variable).
      // Make sure to not add invalid-deref constraints for this dereference
      MemoryRegion newRegion = region;
      if(newRegion==null) {
        newRegion = regionMgr.makeMemoryRegion(baseType);
      }
      constraints.addConstraint(fmgr.makeEqual(makeSafeDereference(baseType, address, ssa, newRegion),
                                               makeVariable(base.getName(), baseType, ssa)));
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
   * @param e The string that has to be expanded
   * @param type The type of the character array.
   * @return List of character-literal expressions
   */
  private static List<CCharLiteralExpression> expandStringLiteral(final CStringLiteralExpression e,
                                                                  final CArrayType type) {
    // The string is either NULL terminated, or not.
    // If the length is not provided explicitly, NULL termination is used
    final String s = e.getContentString();
    final int length = type.getLengthAsInt().orElse(s.length() + 1);
    assert length >= s.length();

    // create one CharLiteralExpression for each character of the string
    final List<CCharLiteralExpression> result = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      result.add(new CCharLiteralExpression(e.getFileLocation(), CNumericTypes.SIGNED_CHAR, s.charAt(i)));
    }


    // http://stackoverflow.com/questions/10828294/c-and-c-partial-initialization-of-automatic-structure
    // C99 Standard 6.7.8.21
    // If there are ... fewer characters in a string literal
    // used to initialize an array of known size than there are elements in the array,
    // the remainder of the aggregate shall be initialized implicitly ...
    for (int i = s.length(); i < length; i++) {
      result.add(new CCharLiteralExpression(e.getFileLocation(), CNumericTypes.SIGNED_CHAR, '\0'));
    }

    return result;
  }

  /**
   * Expands a string literal to an array of characters.
   *
   * @param assignments The list of assignments.
   * @return An assignment statement.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private static List<CExpressionAssignmentStatement> expandStringLiterals(
                                                 final List<CExpressionAssignmentStatement> assignments)
  throws UnrecognizedCCodeException {
    final List<CExpressionAssignmentStatement> result = new ArrayList<>();
    for (CExpressionAssignmentStatement assignment : assignments) {
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
          throw new UnrecognizedCCodeException("Assigning string literal to " + lhsType.toString(), assignment);
        }

        List<CCharLiteralExpression> chars = expandStringLiteral((CStringLiteralExpression) rhs, lhsArrayType);

        int offset = 0;
        for (CCharLiteralExpression e : chars) {
          result.add(new CExpressionAssignmentStatement(
                       assignment.getFileLocation(),
                       new CArraySubscriptExpression(lhs.getFileLocation(),
                                                     lhsArrayType.getType(),
                                                     lhs,
                                                     new CIntegerLiteralExpression(lhs.getFileLocation(),
                                                                                   CNumericTypes.INT,
                                                                                   BigInteger.valueOf(offset))),
                       e));
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
   * @param declaration The declaration of the variable.
   * @param explicitAssignments A list of explicit assignments to the variable.
   * @return A list of assignment statements.
   */
  private List<CExpressionAssignmentStatement> expandAssignmentList(
                                                final CVariableDeclaration declaration,
                                                final List<CExpressionAssignmentStatement> explicitAssignments) {
    final CType variableType = typeHandler.getSimplifiedType(declaration);
    final CLeftHandSide lhs = new CIdExpression(declaration.getFileLocation(),
                                                variableType,
                                                declaration.getName(),
                                                declaration);
    final Set<String> alreadyAssigned = new HashSet<>();
    for (CExpressionAssignmentStatement statement : explicitAssignments) {
      alreadyAssigned.add(statement.getLeftHandSide().toString());
    }
    final List<CExpressionAssignmentStatement> defaultAssignments = new ArrayList<>();
    expandAssignmentList(variableType, lhs, alreadyAssigned, defaultAssignments);
    defaultAssignments.addAll(explicitAssignments);
    return defaultAssignments;
  }

  /**
   * Expands an assignment list.
   *
   * @param type The type of the assignment.
   * @param lhs The left hand side of the assignment.
   * @param alreadyAssigned A set of already assigned values.
   * @param defaultAssignments A list of the default assignments.
   */
  private void expandAssignmentList(CType type,
                                           final CLeftHandSide lhs,
                                           final Set<String> alreadyAssigned,
                                           final List<CExpressionAssignmentStatement> defaultAssignments) {
    if (alreadyAssigned.contains(lhs.toString())) {
      return;
    }

    checkIsSimplified(type);
    if (type instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) type;
      final CType elementType = checkIsSimplified(arrayType.getType());
      final OptionalInt length = arrayType.getLengthAsInt();
      if (length.isPresent()) {
        final int l = Math.min(length.getAsInt(), options.maxArrayLength());
        for (int i = 0; i < l; i++) {
          final CLeftHandSide newLhs = new CArraySubscriptExpression(
                                             lhs.getFileLocation(),
                                             elementType,
                                             lhs,
                                             new CIntegerLiteralExpression(lhs.getFileLocation(),
                                                                           CNumericTypes.INT,
                                                                           BigInteger.valueOf(i)));
          expandAssignmentList(elementType, newLhs, alreadyAssigned, defaultAssignments);
        }
      }
    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
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
        final CLeftHandSide newLhs = new CFieldReference(lhs.getFileLocation(),
                                                         memberType,
                                                         memberDeclaration.getName(),
                                                         lhs, false);
        expandAssignmentList(memberType, newLhs, alreadyAssigned, defaultAssignments);
      }
    } else {
      assert isSimpleType(type);
      CExpression initExp = ((CInitializerExpression)CDefaults.forType(type, lhs.getFileLocation())).getExpression();
      defaultAssignments.add(new CExpressionAssignmentStatement(lhs.getFileLocation(), lhs, initExp));
    }
  }

  /**
   * Creates a builder for pointer target sets.
   *
   * @param pts The current pointer target set.
   * @return A builder for pointer target sets.
   */
  @Override
  protected PointerTargetSetBuilder createPointerTargetSetBuilder(PointerTargetSet pts) {
    return new RealPointerTargetSetBuilder(pts, fmgr, typeHandler, ptsMgr, options, regionMgr);
  }

  /**
   * Merges two sets of pointer targets.
   *
   * @param pPts1 The first set of pointer targets.
   * @param pPts2 The second set of pointer targets.
   * @param pResultSSA The SSA map.
   * @return A set of pointer targets merged from both.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(PointerTargetSet pPts1,
      PointerTargetSet pPts2, SSAMapBuilder pResultSSA) throws InterruptedException {
    return ptsMgr.mergePointerTargetSets(pPts1, pPts2, pResultSSA, this);
  }

  /**
   * Creates a visitor for right hand side expressions.
   *
   * @param pEdge The current edge of the CFA.
   * @param pFunction The name of the current function.
   * @param pSsa The SSA map.
   * @param pPts The underlying set of pointer targets.
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   * @return A visitor for right hand side expressions.
   */
  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, PointerTargetSetBuilder pPts,
      Constraints pConstraints, ErrorConditions pErrorConditions) {

    CExpressionVisitorWithPointerAliasing rhsVisitor = new CExpressionVisitorWithPointerAliasing(this, pEdge, pFunction, pSsa, pConstraints, pErrorConditions, pPts, regionMgr);
    return rhsVisitor.asFormulaVisitor();
  }

  /**
   * Creates a formula for a {@code return} statement in C code.
   *
   * @param assignment The (optional) assignment done at the return statement.
   * @param returnEdge The return edge of the CFA.
   * @param function The name of the current function.
   * @param ssa The SSA map.
   * @param pts The underlying pointer target set.
   * @param constraints Additional constraints.
   * @param errorConditions Additional error conditions.
   * @return A formula for a return statement.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeReturn(final Optional<CAssignment> assignment,
                                      final CReturnStatementEdge returnEdge,
                                      final String function,
                                      final SSAMapBuilder ssa,
                                      final PointerTargetSetBuilder pts,
                                      final Constraints constraints,
                                      final ErrorConditions errorConditions)
  throws UnrecognizedCCodeException, InterruptedException {
    BooleanFormula result = super.makeReturn(assignment, returnEdge, function, ssa, pts, constraints, errorConditions);

    if (assignment.isPresent()) {
      final CVariableDeclaration returnVariableDeclaraton =
          ((CFunctionEntryNode) returnEdge.getSuccessor().getEntryNode()).getReturnVariable().get();
      final boolean containsArray =
          CTypeUtils.containsArray(
              typeHandler.getSimplifiedType(returnVariableDeclaraton), returnVariableDeclaraton);

      declareSharedBase(
          returnVariableDeclaraton, returnVariableDeclaraton, containsArray, constraints, pts);
    }
    return result;
  }

  /**
   * Creates a formula for an assignment.
   *
   * @param lhs  the left-hand-side of the assignment
   * @param lhsForChecking a left-hand-side of the assignment
   *      (for most cases: {@code lhs == lhsForChecking}),
   *      that is used to check if the assignment is important.
   *      If the assignment is not important, we return TRUE.
   * @param rhs the right-hand-side of the assignment
   * @param edge The current edge in the CFA.
   * @param function The name of the current function.
   * @param ssa The SSA map.
   * @param pts The underlying pointer target set.
   * @param constraints Additional constraints.
   * @param errorConditions Additional error conditions.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeAssignment(
      final CLeftHandSide lhs, final CLeftHandSide lhsForChecking, CRightHandSide rhs,
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    // This corresponds to an argument passed as function parameter of array type
    // (this is the only case when arrays can be "assigned" explicitly, not as structure members)
    // In this case the parameter is treated as pointer
    // In other places this case should be distinguished by calling containsArray with the
    // second CDeclaration parameter, but makeAssignment (in AssignmentHandler) only considers
    // types (not declarations),  so we make an explicit conversion to
    // a pointer here to avoid complicating the (already non-trivial) logic in makeAssignment.
    // Note that makeCastFromArrayToPointerIfNecessary won't initially handle this because it only
    // converts the RHS to make it compatible with the LHS, but in this case *both* sides should
    // be converted to pointers
    CType lhsType = typeHandler.getSimplifiedType(lhs);
    if (lhs instanceof CIdExpression
        && ((CIdExpression) lhs).getDeclaration() instanceof CParameterDeclaration
        && lhsType instanceof CArrayType) {
      lhsType =
          new CPointerType(
              lhsType.isConst(), lhsType.isVolatile(), ((CArrayType) lhsType).getType());
    }

    if (rhs instanceof CExpression) {
      rhs = makeCastFromArrayToPointerIfNecessary((CExpression) rhs, lhsType);
    }

    AssignmentHandler assignmentHandler = new AssignmentHandler(this, edge, function, ssa, pts, constraints, errorConditions, regionMgr);
    return assignmentHandler.handleAssignment(lhs, lhsForChecking, lhsType, rhs, false, null);
  }

  /**
   * Creates a log message string from a given message and the current CFA edge.
   *
   * @param msg The message string.
   * @param edge The current CFA edge.
   * @return A log message string.
   */
  private static String getLogMessage(final String msg, final CFAEdge edge) {
    return edge.getFileLocation()
            + ": " + msg
            + ": " + edge.getDescription();
  }

  /**
   * Logs a message for debugging purpose.
   *
   * @param msg The message to be logged.
   * @param edge The current edge in the CFA.
   */
  private void logDebug(final String msg, final CFAEdge edge) {
    logger.log(Level.ALL, () -> getLogMessage(msg, edge));
  }

  /**
   * Creates a formula for declarations in the C code.
   *
   * @param declarationEdge The declaration edge in the CFA.
   * @param function The name of the current function.
   * @param ssa The SSA map.
   * @param pts The underlying pointer target set.
   * @param constraints Additional constraints.
   * @param errorConditions Additional error conditions.
   * @return A formula for a declaration in C code.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeDeclaration(
      final CDeclarationEdge declarationEdge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    // TODO merge with super-class method

    if (declarationEdge.getDeclaration() instanceof CTypeDeclaration) {
      final CType declarationType = typeHandler.getSimplifiedType(declarationEdge.getDeclaration());
      if (declarationType instanceof CCompositeType) {
        typeHandler.addCompositeTypeToCache((CCompositeType) declarationType);
      }
    }

    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // function declaration, typedef etc.
      logDebug("Ignoring declaration", declarationEdge);
      return bfmgr.makeTrue();
    }

    CVariableDeclaration declaration = (CVariableDeclaration) declarationEdge.getDeclaration();

    // makeFreshIndex(variableName, declaration.getType(), ssa); // TODO: Make sure about
                                                                 // correctness of SSA indices without this trick!

    CType declarationType = typeHandler.getSimplifiedType(declaration);

    if (!isRelevantVariable(declaration) &&
        !isAddressedVariable(declaration)) {
      // The variable is unused
      logDebug("Ignoring declaration of unused variable", declarationEdge);
      return bfmgr.makeTrue();
    }

    checkForLargeArray(declarationEdge, declarationType);

    if (errorConditions.isEnabled()) {
      final Formula address = makeConstant(PointerTargetSet.getBaseName(declaration.getQualifiedName()),
                                           CTypeUtils.getBaseType(declarationType));
      constraints.addConstraint(fmgr.makeEqual(makeBaseAddressOfTerm(address), address));
    }

    // if there is an initializer associated to this variable,
    // take it into account
    final CInitializer initializer = declaration.getInitializer();

    // Fixing unsized array declarations
    if (declarationType instanceof CArrayType && ((CArrayType) declarationType).getLength() == null) {
      final Integer actualLength;
      if (initializer instanceof  CInitializerList) {
        actualLength = ((CInitializerList) initializer).getInitializers().size();
      } else if (initializer instanceof CInitializerExpression &&
                 ((CInitializerExpression) initializer).getExpression() instanceof CStringLiteralExpression) {
        actualLength = ((CStringLiteralExpression) ((CInitializerExpression) initializer).getExpression())
                         .getContentString()
                         .length() + 1;
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
                                               declaration.isGlobal(),
                                               declaration.getCStorageClass(),
                                               declarationType,
                                               declaration.getName(),
                                               declaration.getOrigName(),
                                               declaration.getQualifiedName(),
                                               initializer);
      }
    }

    declareSharedBase(declaration, declaration, false, constraints, pts);
    if (CTypeUtils.containsArray(declarationType, declaration)) {
      addPreFilledBase(declaration.getQualifiedName(), declarationType, true, false, constraints, pts);
    }

    if (options.useParameterVariablesForGlobals() && declaration.isGlobal()) {
      globalDeclarations.add(declaration);
    }

    final CIdExpression lhs =
        new CIdExpression(declaration.getFileLocation(), declaration);
    final AssignmentHandler assignmentHandler = new AssignmentHandler(this, declarationEdge, function, ssa, pts, constraints, errorConditions, regionMgr);
    final BooleanFormula result;
    if (initializer instanceof CInitializerExpression || initializer == null) {

      if (initializer != null) {
        result = assignmentHandler.handleAssignment(lhs, lhs, ((CInitializerExpression) initializer).getExpression(), false, null);
      } else if (isRelevantVariable(declaration) && !declarationType.isIncomplete()) {
        result = assignmentHandler.handleAssignment(lhs, lhs, null, false, null);
      } else {
        result = bfmgr.makeTrue();
      }

    } else if (initializer instanceof CInitializerList) {

      List<CExpressionAssignmentStatement> assignments =
        CInitializers.convertToAssignments(declaration, declarationEdge);
      if (options.handleStringLiteralInitializers()) {
        // Special handling for string literal initializers -- convert them into character arrays
        assignments = expandStringLiterals(assignments);
      }
      if (options.handleImplicitInitialization()) {
        assignments = expandAssignmentList(declaration, assignments);
      }
      result = assignmentHandler.handleInitializationAssignments(lhs, declarationType, assignments);

    } else {
      throw new UnrecognizedCCodeException("Unrecognized initializer", declarationEdge, initializer);
    }

    return result;
  }

  /**
   * Check whether a large array is declared and abort analysis in this case.
   * This is a heuristic for SV-COMP to avoid
   * wasting a lot of time for programs we probably cannot handle anyway
   * or returning a wrong answer.
   */
  private void checkForLargeArray(final CDeclarationEdge declarationEdge, CType declarationType)
      throws UnsupportedCCodeException {
    if (options.useArraysForHeap() || options.useQuantifiersOnArrays()) {
      return; // unbounded heap encodings should be able to handle large arrays
    }

    if (!(declarationType instanceof CArrayType)) {
      return;
    }
    CArrayType arrayType = (CArrayType) declarationType;
    CType elementType = arrayType.getType();

    if (elementType instanceof CSimpleType
        && ((CSimpleType) elementType).getType().isFloatingPointType()) {
      if (arrayType.getLengthAsInt().orElse(0) > 100) {
        throw new UnsupportedCCodeException("large floating-point array", declarationEdge);
      }
    }

    if (elementType instanceof CSimpleType
        && ((CSimpleType) elementType).getType() == CBasicType.INT) {
      if (arrayType.getLengthAsInt().orElse(0) >= 10000) {
        throw new UnsupportedCCodeException("large integer array", declarationEdge);
      }
    }
  }

  /**
   * Creates a predicate formula for an expression and its truth assumption.
   *
   * @param e The expression.
   * @param truthAssumption The assumption for the truth of the expression.
   * @param edge The current edge in the CFA.
   * @param function The name of the current function.
   * @param ssa The SSA map.
   * @param pts The underlying pointer target set.
   * @param constraints Additional constraints.
   * @param errorConditions Additional error conditions.
   * @return A predicate formula for an expression and its truth assumption.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makePredicate(final CExpression e, final boolean truthAssumption,
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {
    final CType expressionType = typeHandler.getSimplifiedType(e);
    CExpressionVisitorWithPointerAliasing ev = new CExpressionVisitorWithPointerAliasing(this, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    BooleanFormula result = toBooleanFormula(ev.asValueFormula(e.accept(ev), expressionType));

    if (options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(this, edge, ssa, pts, constraints, errorConditions, regionMgr);
      memoryHandler.handleDeferredAllocationsInAssume(e, ev.getLearnedPointerTypes());
    }

    if (!truthAssumption) {
      result = bfmgr.not(result);
    }

    pts.addEssentialFields(ev.getInitializedFields());
    pts.addEssentialFields(ev.getUsedFields());
    return result;
  }

  /**
   * Creates a formula for a function call.
   *
   * @param edge The function call edge in the CFA.
   * @param callerFunction The name of the caller function.
   * @param ssa The SSA map.
   * @param pts The underlying pointer target set.
   * @param constraints Additional constraints.
   * @param errorConditions Additional error conditions.
   * @return A formula representing the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeFunctionCall(
      final CFunctionCallEdge edge, final String callerFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    final CFunctionEntryNode entryNode = edge.getSuccessor();
    BooleanFormula result = super.makeFunctionCall(edge, callerFunction, ssa, pts, constraints, errorConditions);

    for (CParameterDeclaration formalParameter : entryNode.getFunctionParameters()) {
      final CType parameterType = typeHandler.getSimplifiedType(formalParameter);
      final CVariableDeclaration formalDeclaration = formalParameter.asVariableDeclaration();
      final CVariableDeclaration declaration;
      if (options.useParameterVariables()) {
        CParameterDeclaration tmpParameter = new CParameterDeclaration(
                formalParameter.getFileLocation(), formalParameter.getType(), formalParameter.getName() + PARAM_VARIABLE_NAME);
        tmpParameter.setQualifiedName(formalParameter.getQualifiedName() + PARAM_VARIABLE_NAME);
        declaration = tmpParameter.asVariableDeclaration();
      } else {
        declaration = formalDeclaration;
      }
      declareSharedBase(
          declaration,
          formalParameter,
          CTypeUtils.containsArrayInFunctionParameter(parameterType),
          constraints,
          pts);
    }

    return result;
  }

  /**
   * Creates a formula for a function exit.
   *
   * @param summaryEdge The function's summary edge in the CFA.
   * @param calledFunction The name of the called function.
   * @param ssa The SSA map.
   * @param pts The underlying pointer target set
   * @param constraints Additional constraints.
   * @param errorConditions Additional error conditions.
   * @return A formula for the function exit.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  protected BooleanFormula makeExitFunction(
      final CFunctionSummaryEdge summaryEdge, final String calledFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    final BooleanFormula result = super.makeExitFunction(summaryEdge, calledFunction, ssa, pts, constraints, errorConditions);

    DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(this, summaryEdge, ssa, pts, constraints, errorConditions, regionMgr);
    memoryHandler.handleDeferredAllocationInFunctionExit(calledFunction);

    return result;
  }

  @SuppressWarnings("hiding") // same instance with narrower type
  final FormulaEncodingWithPointerAliasingOptions options;

  private final Optional<VariableClassification> variableClassification;

  private static final String POINTER_NAME_PREFIX = "*";

  static final String FIELD_NAME_SEPARATOR = "$";

  private static final Map<CType, String> pointerNameCache = new IdentityHashMap<>();


  // Overrides just for visibility in other classes of this package

  /**
   * Evaluates the return type of a function call.
   *
   * @param pFuncCallExp The function call expression.
   * @param pEdge The edge in the CFA.
   * @return The type of the function call.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  @Override
  protected CType getReturnType(CFunctionCallExpression pFuncCallExp, CFAEdge pEdge) throws UnrecognizedCCodeException {
    return typeHandler.simplifyType(super.getReturnType(pFuncCallExp, pEdge));
  }

  /**
   * Creates an expression for a cast from an array to a pointer type. This cast is only done, if
   * necessary.
   *
   * @param pExp The expression to get casted.
   * @param pTargetType The target type of the cast.
   * @return A pointer expression for the casted array expression.
   */
  @Override
  protected CExpression makeCastFromArrayToPointerIfNecessary(CExpression pExp, CType pTargetType) {
    return super.makeCastFromArrayToPointerIfNecessary(pExp, pTargetType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeCast(CType pFromType, CType pToType, Formula pFormula, Constraints constraints, CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    return super.makeCast(pFromType, pToType, pFormula, constraints, pEdge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeConstant(String pName, CType pType) {
    return super.makeConstant(pName, pType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeVariable(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.makeVariable(pName, pType, pSsa);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Formula makeFormulaForVariable(
      SSAMap pContextSSA,
      PointerTargetSet pContextPTS,
      String pVarName,
      CType pType,
      boolean forcePointerDereference) {
    Preconditions.checkArgument(!(pType instanceof CFunctionType));

    Expression e = makeFormulaForVariable(pVarName, pType, pContextPTS, forcePointerDereference);

    SSAMapBuilder ssa = pContextSSA.builder();
    Formula formula;

    if (e.isValue()) {
      formula = e.asValue().getValue();
    } else if (e.isAliasedLocation()) {
      MemoryRegion region = e.asAliasedLocation().getMemoryRegion();
      if(region==null) {
        region = regionMgr.makeMemoryRegion(pType);
      }
      formula = makeSafeDereference(pType, e.asAliasedLocation().getAddress(), ssa, region);
    } else {
      formula = makeVariable(e.asUnaliasedLocation().getVariableName(), pType, ssa);
    }

    if (!ssa.build().equals(pContextSSA)) {
      throw new IllegalArgumentException(
          "we cannot apply the SSAMap changes to the point where the"
              + " information would be needed. Possible problems: uninitialized variables could be"
              + " in more formulas which get conjuncted and then we get unsatisfiable formulas as a result.\n"
              + " difference in SSA variables: "
              + Sets.difference(ssa.allVariables(), pContextSSA.allVariables()));
    }

    return formula;
  }

  protected Expression makeFormulaForVariable(
      String pVarName, CType pType, PointerTargetSet pts, boolean forceDereference) {
    if (forceDereference) {
      final Formula address = makeConstant(pVarName, CTypeUtils.getBaseType(pType));
      return AliasedLocation.ofAddress(address);
    } else if (!pts.isActualBase(pVarName)
        && !CTypeUtils.containsArrayOutsideFunctionParameter(pType)) {
      Variable variable = Variable.create(pVarName, pType);

      final String variableName = variable.getName();
      return UnaliasedLocation.ofVariableName(variableName);
    } else {
      final Formula address =
          makeConstant(PointerTargetSet.getBaseName(pVarName), CTypeUtils.getBaseType(pType));
      return AliasedLocation.ofAddress(address);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Formula makeFreshVariable(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.makeFreshVariable(pName, typeHandler.simplifyType(pType), pSsa);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getIndex(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.getIndex(pName, pType, pSsa);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getFreshIndex(String pName, CType pType, SSAMapBuilder pSsa) {
    return super.getFreshIndex(pName, pType, pSsa);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int getSizeof(CType pType) {
    return super.getSizeof(pType);
  }

  /**
   * Checks, whether a left hand side is relevant for the analysis.
   *
   * @param pLhs The left hand side to check.
   * @return Whether a left hand side is relevant for the analysis.
   */
  @Override
  protected boolean isRelevantLeftHandSide(CLeftHandSide pLhs) {
    return super.isRelevantLeftHandSide(pLhs);
  }

  @Override
  public void printStatistics(PrintStream out) {
    regionMgr.printStatistics(out);
  }
}
