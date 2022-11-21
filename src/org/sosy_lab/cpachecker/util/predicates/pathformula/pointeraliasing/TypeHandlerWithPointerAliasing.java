// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;

public class TypeHandlerWithPointerAliasing extends CtoFormulaTypeHandler {

  private static final String POINTER_NAME_PREFIX = "*";
  private static final String BYTE_ARRAY_HEAP_ACCESS_NAME =
      POINTER_NAME_PREFIX + "SINGLE_BYTE_ARRAY";

  private final MachineModel model;
  private final FormulaEncodingWithPointerAliasingOptions options;
  private final CachingCanonizingCTypeVisitor canonizingVisitor =
      new CachingCanonizingCTypeVisitor(
          /*ignoreConst=*/ true, /*ignoreVolatile=*/ true, /*ignoreSignedness=*/ false);
  private final CachingCanonizingCTypeVisitor canonizingVisitorWithoutSignedness =
      new CachingCanonizingCTypeVisitor(
          /*ignoreConst=*/ true, /*ignoreVolatile=*/ true, /*ignoreSignedness=*/ true);

  private final IdentityHashMap<CType, String> pointerNameCache = new IdentityHashMap<>();

  private final Map<CCompositeType, Long> sizes = new HashMap<>();

  public TypeHandlerWithPointerAliasing(
      LogManager pLogger,
      MachineModel pMachineModel,
      FormulaEncodingWithPointerAliasingOptions pOptions) {
    super(pLogger, pMachineModel);

    model = pMachineModel;
    options = pOptions;
  }

  public static boolean isByteArrayAccessName(String pName) {
    return BYTE_ARRAY_HEAP_ACCESS_NAME.equals(pName);
  }

  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes of declared
   * composite types.
   *
   * @param cType the type of which the size should be retrieved
   * @return The size of a given type.
   */
  @Override
  public long getSizeof(CType cType) {
    // Callers from inside this package should have simplified the type,
    // but callers from ctoformula package might have not.
    cType = simplifyType(cType);
    if (cType instanceof CCompositeType) {
      return sizes.computeIfAbsent((CCompositeType) cType, this::getSizeofUncached);
    } else {
      return getSizeofUncached(cType);
    }
  }

  private long getSizeofUncached(CType cType) {
    if (cType instanceof CArrayType && !cType.hasKnownConstantSize()) {
      CArrayType t = (CArrayType) cType;
      int length = t.getLengthAsInt().orElse(options.defaultArrayLength());
      final long sizeOfType = getSizeofUncached(t.getType());
      return length * sizeOfType;
    } else {
      return model.getSizeof(cType).longValueExact();
    }
  }

  public int getAlignof(CType type) {
    return model.getAlignof(type);
  }

  /**
   * The method should be used everywhere the type of any expression is determined. This is because
   * the encoding uses types for naming of the UFs as well as for over-approximating points-to sets
   * (may-aliases). To make the encoding precise enough the types should correspond to actually
   * different types (requiring explicit casts to be converted to one another), so {@link
   * CCompositeType}s, corresponding {@link CElaboratedType}s and {@link CTypedefType}s shouldn't be
   * distinguished and are converted to the same canonical type by this method.
   *
   * <p>This method will also perform {@code const} and {@code volatile} modifiers elimination.
   *
   * <p>Note that all code in this package should only use simplified types, so calling this method
   * should be only necessary when retrieving types from AST nodes. Use {@link
   * CTypeUtils#checkIsSimplified(CType)} as a precondition in other places when you want to make
   * sure a type is simplified.
   *
   * <p>Also consider using one of the {@link #getSimplifiedType} overloads.
   *
   * @param type The type obtained from the CFA
   * @return The corresponding simplified canonical type
   */
  CType simplifyType(final CType type) {
    return type.accept(canonizingVisitor);
  }

  /** Get a simplified type as defined by {@link #simplifyType(CType)} from an AST node. */
  public CType getSimplifiedType(final CRightHandSide exp) {
    return simplifyType(exp.getExpressionType());
  }

  /** Get a simplified type as defined by {@link #simplifyType(CType)} from a declaration. */
  CType getSimplifiedType(final CSimpleDeclaration decl) {
    return simplifyType(decl.getType());
  }

  /** Get a simplified type as defined by {@link #simplifyType(CType)} from a field declaration. */
  CType getSimplifiedType(final CCompositeTypeMemberDeclaration field) {
    return simplifyType(field.getType());
  }

  /**
   * Get a simplified type that is suited for identifying a target region on the heap. This means
   * that two types which are compatible (i.e., where pointer aliasing may occur) need to have the
   * same type returned by this method.
   *
   * <p>This is different from {@link #simplifyType(CType)}, which just canonicalizes types.
   */
  CType simplifyTypeForPointerAccess(final CType type) {
    return type.accept(canonizingVisitorWithoutSignedness);
  }

  /**
   * Checks, whether a symbol is a pointer access encoded in SMT.
   *
   * @param symbol The name of the symbol.
   * @return Whether the symbol is a pointer access or not.
   */
  static boolean isPointerAccessSymbol(final String symbol) {
    return symbol.startsWith(POINTER_NAME_PREFIX);
  }

  /**
   * Returns the SMT symbol name for encoding a pointer access for a C type.
   *
   * @param type The type to get the symbol name for.
   * @return The symbol name for the type.
   */
  public String getPointerAccessNameForType(final CType type) {
    String result = pointerNameCache.get(type);
    if (result != null) {
      return result;
    } else {
      if (options.useByteArrayForHeap()) {
        result = BYTE_ARRAY_HEAP_ACCESS_NAME;
      } else {
        result =
            POINTER_NAME_PREFIX + simplifyTypeForPointerAccess(type).toString().replace(' ', '_');
      }
      pointerNameCache.put(type, result);
      return result;
    }
  }
}
