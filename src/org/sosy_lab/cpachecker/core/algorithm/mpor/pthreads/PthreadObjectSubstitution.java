// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationParseTest;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class PthreadObjectSubstitution {

  /**
   * Substitutes all pthread object types in {@code pType}.
   *
   * <p>Specific classes to substitute can be specified via {@code pClasses}. This can be useful
   * when substituting the types of {@link CVariableDeclaration}, where it should be desired to
   * substitute {@link CCompositeType} but only {@link CElaboratedType} so that the {@link
   * CCompositeType} is not redeclared.
   */
  public static CType substitutePthreadObjectTypes(
      CType pType, ImmutableSet<Class<? extends CType>> pClasses) {

    CType substituted = pType;
    // replace all pthread object types, which is necessary for structs that contain multiple
    // pthread object types
    for (PthreadObjectType pObjectType : PthreadObjectType.values()) {
      for (CType substituteType : pObjectType.substituteTypes) {
        if (pClasses.stream().anyMatch(c -> c.isInstance(substituteType))) {
          CTypeSubstitutionVisitor substitutionVisitor =
              new CTypeSubstitutionVisitor(
                  ImmutableSet.of(
                      pObjectType.name,
                      SequentializationParseTest.ANON_TYPE_KEYWORD + pObjectType.name),
                  substituteType);
          substituted = substitutionVisitor.visitDefault(substituted);
        }
      }
    }
    return substituted;
  }

  private static class CTypeSubstitutionVisitor extends DefaultCTypeVisitor<CType, NoException> {

    private final Set<CCompositeType> visitedCompositeTypes = new HashSet<>();

    /** The names of {@link CType} to substitute. */
    private final ImmutableSet<String> substitutedNames;

    private final CType substitution;

    private CTypeSubstitutionVisitor(ImmutableSet<String> pSubstitutedNames, CType pSubstitution) {
      substitutedNames = pSubstitutedNames;
      substitution = pSubstitution;
    }

    @Override
    public CType visitDefault(CType pType) {
      // using getCanonicalType results in CElaboratedType being replaced with CCompositeType which
      // leads to duplicate declarations of types and parse errors
      return pType.accept(this);
    }

    @Override
    public CType visit(CVoidType pVoidType) {
      // nothing to substitute in CVoidType
      return pVoidType;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) {
      // nothing to substitute in CVoidType
      return pSimpleType;
    }

    @Override
    public CType visit(CArrayType pArrayType) {
      // getCanonicalType returns the same CArrayType and results in infinite recursion
      return new CArrayType(
          pArrayType.getQualifiers(), pArrayType.getType().accept(this), pArrayType.getLength());
    }

    @Override
    public CType visit(CCompositeType pCompositeType) {
      if (substitution instanceof CCompositeType
          && substitutedNames.contains(pCompositeType.getName())) {
        return substitution;
      }
      // prevent circular searches
      if (!visitedCompositeTypes.add(pCompositeType)) {
        return pCompositeType;
      }
      ImmutableList.Builder<CCompositeTypeMemberDeclaration> memberDeclarations =
          ImmutableList.builder();
      for (CCompositeTypeMemberDeclaration memberDeclaration : pCompositeType.getMembers()) {
        CType memberTypeSubstitute = memberDeclaration.getType().accept(this);
        memberDeclarations.add(
            new CCompositeTypeMemberDeclaration(memberTypeSubstitute, memberDeclaration.getName()));
      }
      return new CCompositeType(
          pCompositeType.getQualifiers(),
          pCompositeType.getKind(),
          memberDeclarations.build(),
          pCompositeType.getName(),
          pCompositeType.getOrigName());
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) {
      if (substitution instanceof CElaboratedType
          && substitutedNames.contains(pElaboratedType.getName())) {
        return substitution;
      }
      if (pElaboratedType.getRealType() == null) {
        return pElaboratedType;
      }
      return new CElaboratedType(
          pElaboratedType.getQualifiers(),
          pElaboratedType.getKind(),
          pElaboratedType.getName(),
          pElaboratedType.getOrigName(),
          (CComplexType) pElaboratedType.getRealType().accept(this));
    }

    @Override
    public CType visit(CEnumType pEnumType) {
      if (substitution instanceof CEnumType && substitutedNames.contains(pEnumType.getName())) {
        return substitution;
      }
      // nothing to visit in CEnumType
      return pEnumType;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) {
      if (substitution instanceof CFunctionType
          && pFunctionType.getName() != null
          && substitutedNames.contains(pFunctionType.getName())) {
        return substitution;
      }
      CType returnTypeSubstitute = pFunctionType.getReturnType().accept(this);
      ImmutableList.Builder<CType> parameterSubstitutes = ImmutableList.builder();
      for (CType parameter : pFunctionType.getParameters()) {
        parameterSubstitutes.add(parameter.accept(this));
      }
      return new CFunctionType(
          returnTypeSubstitute, parameterSubstitutes.build(), pFunctionType.takesVarArgs());
    }

    @Override
    public CType visit(CPointerType pPointerType) {
      // getCanonicalType returns the CPointerType again and results in infinite recursion
      return new CPointerType(pPointerType.getQualifiers(), pPointerType.getType().accept(this));
    }

    @Override
    public CType visit(CTypedefType pTypedefType) {
      if (substitution instanceof CTypedefType
          && substitutedNames.contains(pTypedefType.getName())) {
        return substitution;
      }
      return new CTypedefType(
          pTypedefType.getQualifiers(),
          pTypedefType.getName(),
          pTypedefType.getRealType().accept(this));
    }

    @Override
    public CType visit(CBitFieldType pBitFieldType) {
      return new CBitFieldType(
          pBitFieldType.getType().accept(this), pBitFieldType.getBitFieldSize());
    }
  }
}
