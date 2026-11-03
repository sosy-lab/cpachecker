// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
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

  public static CIdExpression substitutePthreadObjectType(CIdExpression pIdExpression) {
    CType typeSubstitute =
        substitutePthreadObjectTypes(pIdExpression.getExpressionType(), ImmutableSet.of()).type();

    CVariableDeclaration variableDeclaration =
        (CVariableDeclaration) pIdExpression.getDeclaration();
    CVariableDeclaration variableDeclarationWithTypeSubstitute =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            variableDeclaration.isGlobal(),
            variableDeclaration.getCStorageClass(),
            typeSubstitute,
            variableDeclaration.getName(),
            variableDeclaration.getOrigName(),
            variableDeclaration.getQualifiedName(),
            variableDeclaration.getInitializer(),
            variableDeclaration.hasThreadLocalStorage());

    return new CIdExpression(
        FileLocation.DUMMY,
        typeSubstitute,
        pIdExpression.getName(),
        variableDeclarationWithTypeSubstitute);
  }

  /**
   * Substitutes all pthread object types in {@code pType}.
   *
   * <p>Specific classes to substitute can be specified via {@code pClasses}. If {@code pClasses} is
   * empty, then all types will be substituted. This can be useful when substituting the types of
   * {@link CVariableDeclaration}, where it should be desired to substitute {@link CCompositeType}
   * but only {@link CElaboratedType} so that the {@link CCompositeType} is not redeclared.
   */
  public static CTypeSubstitutionResult substitutePthreadObjectTypes(
      CType pType, ImmutableSet<Class<? extends CType>> pClasses) {

    CTypeSubstitutionResult rSubstitutionResult =
        new CTypeSubstitutionResult(pType, ImmutableMap.of(), false);
    // replace all pthread object types, which is necessary for structs that contain multiple
    // pthread object types
    for (PthreadObjectType pObjectType : PthreadObjectType.values()) {
      for (CType substituteType : pObjectType.getSubstituteTypes()) {
        if (pClasses.isEmpty() || pClasses.stream().anyMatch(c -> c.isInstance(substituteType))) {
          CTypeSubstitutionVisitor substitutionVisitor =
              new CTypeSubstitutionVisitor(
                  ImmutableSet.of(
                      pObjectType.getName(),
                      SequentializationParseTest.ANON_TYPE_KEYWORD + pObjectType.getName()),
                  substituteType);
          rSubstitutionResult = substitutionVisitor.visitDefault(rSubstitutionResult.type());
        }
      }
    }
    return rSubstitutionResult;
  }

  /**
   * Record containing the result of a pthread object type substitution.
   *
   * @param type The type after substitution, may be the unmodified reference of the initial type.
   * @param paths The map of substitution paths to {@link CCompositeTypeMemberDeclaration}. This
   *     allows to reconstruct all inner members that were substituted in {@code type}, which may be
   *     useful when substituting a {@link CInitializerList} too.
   * @param isSubstituted Whether {@code type} or any inner {@link CType} was substituted.
   */
  public record CTypeSubstitutionResult(
      CType type,
      ImmutableMap<ImmutableList<Integer>, CCompositeTypeMemberDeclaration> paths,
      boolean isSubstituted) {}

  private static class CTypeSubstitutionVisitor
      extends DefaultCTypeVisitor<CTypeSubstitutionResult, NoException> {

    private final Set<CCompositeType> visitedCompositeTypes = new HashSet<>();

    /** The names of {@link CType} to substitute. */
    private final ImmutableSet<String> substitutedNames;

    private final CType substitution;

    private final Map<List<Integer>, CCompositeTypeMemberDeclaration> paths = new HashMap<>();

    private CTypeSubstitutionVisitor(ImmutableSet<String> pSubstitutedNames, CType pSubstitution) {
      substitutedNames = pSubstitutedNames;
      substitution = pSubstitution;
    }

    private CTypeSubstitutionResult result(CType pType, boolean pIsSubstituted) {
      ImmutableMap.Builder<ImmutableList<Integer>, CCompositeTypeMemberDeclaration> immutablePaths =
          ImmutableMap.builder();

      paths.forEach((path, member) -> immutablePaths.put(ImmutableList.copyOf(path), member));
      return new CTypeSubstitutionResult(pType, immutablePaths.build(), pIsSubstituted);
    }

    private CTypeSubstitutionResult result(CType pType) {
      return result(pType, false);
    }

    @Override
    public CTypeSubstitutionResult visitDefault(CType pType) {
      // Do not use getCanonicalType(), as this may replace CElaboratedType
      // with CCompositeType and lead to duplicate declarations.
      return pType.accept(this);
    }

    @Override
    public CTypeSubstitutionResult visit(CVoidType pVoidType) {
      return result(pVoidType);
    }

    @Override
    public CTypeSubstitutionResult visit(CSimpleType pSimpleType) {
      return result(pSimpleType);
    }

    @Override
    public CTypeSubstitutionResult visit(CArrayType pArrayType) {
      CTypeSubstitutionResult elementResult = pArrayType.getType().accept(this);

      return result(
          new CArrayType(pArrayType.getQualifiers(), elementResult.type(), pArrayType.getLength()),
          elementResult.isSubstituted());
    }

    @Override
    public CTypeSubstitutionResult visit(CCompositeType pCompositeType) {
      if (substitution instanceof CCompositeType
          && substitutedNames.contains(pCompositeType.getName())) {
        return result(substitution, true);
      }

      // Prevent circular searches.
      if (!visitedCompositeTypes.add(pCompositeType)) {
        return result(pCompositeType);
      }

      ImmutableList.Builder<CCompositeTypeMemberDeclaration> memberDeclarations =
          ImmutableList.builder();

      boolean isSubstituted = false;

      int memberIndex = 0;
      for (CCompositeTypeMemberDeclaration memberDeclaration : pCompositeType.getMembers()) {
        List<Integer> path = new ArrayList<>();
        path.add(memberIndex);

        CTypeSubstitutionResult memberResult = memberDeclaration.getType().accept(this);

        if (memberResult.isSubstituted()) {
          paths.put(List.copyOf(path), memberDeclaration);
        }

        memberDeclarations.add(
            new CCompositeTypeMemberDeclaration(memberResult.type(), memberDeclaration.getName()));

        isSubstituted |= memberResult.isSubstituted();
        memberIndex++;
      }

      return result(
          new CCompositeType(
              pCompositeType.getQualifiers(),
              pCompositeType.getKind(),
              memberDeclarations.build(),
              pCompositeType.getName(),
              pCompositeType.getOrigName()),
          isSubstituted);
    }

    @Override
    public CTypeSubstitutionResult visit(CElaboratedType pElaboratedType) {
      if (substitution instanceof CElaboratedType
          && substitutedNames.contains(pElaboratedType.getName())) {
        return result(substitution, true);
      }

      if (pElaboratedType.getRealType() == null) {
        return result(pElaboratedType);
      }

      CTypeSubstitutionResult realTypeResult = pElaboratedType.getRealType().accept(this);

      return result(
          new CElaboratedType(
              pElaboratedType.getQualifiers(),
              pElaboratedType.getKind(),
              pElaboratedType.getName(),
              pElaboratedType.getOrigName(),
              (CComplexType) realTypeResult.type()),
          realTypeResult.isSubstituted());
    }

    @Override
    public CTypeSubstitutionResult visit(CEnumType pEnumType) {
      if (substitution instanceof CEnumType && substitutedNames.contains(pEnumType.getName())) {
        return result(substitution, true);
      }

      return result(pEnumType);
    }

    @Override
    public CTypeSubstitutionResult visit(CFunctionType pFunctionType) {
      if (substitution instanceof CFunctionType
          && pFunctionType.getName() != null
          && substitutedNames.contains(pFunctionType.getName())) {
        return result(substitution, true);
      }

      CTypeSubstitutionResult returnTypeResult = pFunctionType.getReturnType().accept(this);

      ImmutableList.Builder<CType> parameterSubstitutes = ImmutableList.builder();

      boolean isSubstituted = returnTypeResult.isSubstituted();

      for (CType parameter : pFunctionType.getParameters()) {
        CTypeSubstitutionResult parameterResult = parameter.accept(this);
        parameterSubstitutes.add(parameterResult.type());
        isSubstituted |= parameterResult.isSubstituted();
      }

      return result(
          new CFunctionType(
              returnTypeResult.type(), parameterSubstitutes.build(), pFunctionType.takesVarArgs()),
          isSubstituted);
    }

    @Override
    public CTypeSubstitutionResult visit(CPointerType pPointerType) {
      CTypeSubstitutionResult innerResult = pPointerType.getType().accept(this);

      return result(
          new CPointerType(pPointerType.getQualifiers(), innerResult.type()),
          innerResult.isSubstituted());
    }

    @Override
    public CTypeSubstitutionResult visit(CTypedefType pTypedefType) {
      if (substitution instanceof CTypedefType
          && substitutedNames.contains(pTypedefType.getName())) {
        return result(substitution, true);
      }

      CTypeSubstitutionResult realTypeResult = pTypedefType.getRealType().accept(this);

      return result(
          new CTypedefType(
              pTypedefType.getQualifiers(), pTypedefType.getName(), realTypeResult.type()),
          realTypeResult.isSubstituted());
    }

    @Override
    public CTypeSubstitutionResult visit(CBitFieldType pBitFieldType) {
      CTypeSubstitutionResult innerResult = pBitFieldType.getType().accept(this);

      return result(
          new CBitFieldType(innerResult.type(), pBitFieldType.getBitFieldSize()),
          innerResult.isSubstituted());
    }
  }
}
