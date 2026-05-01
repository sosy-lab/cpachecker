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
import java.util.Objects;
import java.util.Set;
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

  public static CType substitutePthreadObjectTypes(CType pType) {
    CType substituted = pType;
    for (PthreadObjectType pObjectType : PthreadObjectType.values()) {
      if (pObjectType.substituteType.isPresent()) {
        CElaboratedType substitutionType = pObjectType.substituteType.orElseThrow();
        CTypeSubstitutionVisitor substitutionVisitor =
            new CTypeSubstitutionVisitor(
                ImmutableSet.of(
                    pObjectType.name,
                    SequentializationParseTest.ANON_TYPE_KEYWORD + pObjectType.name),
                substitutionType);
        substituted = substitutionVisitor.visitDefault(substituted);
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
      // nothing to visit in CVoidType
      return pVoidType;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) {
      // nothing to visit in CSimpleType
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
      if (substitutedNames.contains(pCompositeType.getName())) {
        return substitution;
      }
      if (!visitedCompositeTypes.add(pCompositeType)) {
        return pCompositeType;
      }
      ImmutableList.Builder<CCompositeTypeMemberDeclaration> memberTypeSubstitutes =
          ImmutableList.builder();
      for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        CType memberTypeSubstitute = member.getType().accept(this);
        CCompositeTypeMemberDeclaration memberDeclarationSubstitute =
            new CCompositeTypeMemberDeclaration(memberTypeSubstitute, member.getName());
        memberTypeSubstitutes.add(memberDeclarationSubstitute);
      }
      return new CCompositeType(
          pCompositeType.getQualifiers(),
          pCompositeType.getKind(),
          memberTypeSubstitutes.build(),
          pCompositeType.getName(),
          pCompositeType.getOrigName());
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) {
      if (substitutedNames.contains(pElaboratedType.getName())) {
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
          (CComplexType) Objects.requireNonNull(pElaboratedType.getCanonicalType()).accept(this));
    }

    @Override
    public CType visit(CEnumType pEnumType) {
      if (substitutedNames.contains(pEnumType.getName())) {
        return substitution;
      }
      // nothing to visit in CEnumType
      return pEnumType;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) {
      if (pFunctionType.getName() != null && substitutedNames.contains(pFunctionType.getName())) {
        return substitution;
      }
      CType returnTypeSubstitute = pFunctionType.getReturnType().getCanonicalType().accept(this);
      ImmutableList.Builder<CType> parameterSubstitutes = ImmutableList.builder();
      for (CType parameter : pFunctionType.getParameters()) {
        parameterSubstitutes.add(parameter.getCanonicalType().accept(this));
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
      if (substitutedNames.contains(pTypedefType.getName())) {
        return substitution;
      }
      return new CTypedefType(
          pTypedefType.getQualifiers(),
          pTypedefType.getName(),
          pTypedefType.getRealType().getCanonicalType().accept(this));
    }

    @Override
    public CType visit(CBitFieldType pBitFieldType) {
      return new CBitFieldType(
          pBitFieldType.getCanonicalType().accept(this), pBitFieldType.getBitFieldSize());
    }
  }
}
