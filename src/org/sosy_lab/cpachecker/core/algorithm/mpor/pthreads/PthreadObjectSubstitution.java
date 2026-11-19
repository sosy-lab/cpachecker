// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
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

    // replace all pthread object types, necessary for structs with multiple pthread object types
    for (PthreadObjectType currentObjectType : PthreadObjectType.values()) {
      for (CType substituteType : currentObjectType.getSubstituteTypes()) {
        if (pClasses.isEmpty() || pClasses.stream().anyMatch(c -> c.isInstance(substituteType))) {
          CTypeSubstitutionVisitor substitutionVisitor =
              new CTypeSubstitutionVisitor(
                  ImmutableSet.of(
                      currentObjectType.getName(),
                      SequentializationParseTest.ANON_TYPE_KEYWORD + currentObjectType.getName()),
                  substituteType);

          CTypeSubstitutionResult currentResult =
              substitutionVisitor.visitDefault(rSubstitutionResult.type());
          Map<ImmutableList<Integer>, CCompositeTypeMemberDeclaration> paths =
              new HashMap<>(rSubstitutionResult.paths());
          paths.putAll(currentResult.paths());
          rSubstitutionResult =
              new CTypeSubstitutionResult(
                  currentResult.type(),
                  ImmutableMap.copyOf(paths),
                  rSubstitutionResult.isSubstituted() || currentResult.isSubstituted());
        }
      }
    }
    return rSubstitutionResult;
  }

  public static CInitializer substitutePthreadObjectInitializers(
      CTypeSubstitutionResult pSubstitutionResult, CInitializer pInitializer) {

    checkArgument(
        pInitializer instanceof CInitializerList, "pInitializer must be CInitializerList");

    CInitializer result = pInitializer;
    for (PthreadObjectType objectType : PthreadObjectType.values()) {
      Optional<CInitializer> substituteInitializer = objectType.getSubstituteInitializer();

      if (substituteInitializer.isPresent()) {
        if (pSubstitutionResult.isSubstituted() && pSubstitutionResult.paths().isEmpty()) {
          if (isPthreadObjectType(pSubstitutionResult.type(), objectType)) {
            return substituteInitializer.orElseThrow();
          }
        }
        // substitute nested initializers inside CInitializerList, if an inner type was substituted
        for (Map.Entry<ImmutableList<Integer>, CCompositeTypeMemberDeclaration> entry :
            pSubstitutionResult.paths().entrySet()) {
          CCompositeTypeMemberDeclaration member = entry.getValue();
          if (isPthreadObjectType(member.getType(), objectType)) {
            result =
                substituteInitializerAtPath(
                    result, entry.getKey(), 0, member, substituteInitializer.orElseThrow());
          }
        }
      }
    }
    return result;
  }

  static boolean isPthreadObjectType(CType pType, PthreadObjectType pObjectType) {
    String name = pObjectType.getName();
    return switch (pType) {
      case CArrayType arrayType -> isPthreadObjectType(arrayType.getType(), pObjectType);
      case CPointerType pointerType -> isPthreadObjectType(pointerType.getType(), pObjectType);
      case CBitFieldType bitFieldType -> isPthreadObjectType(bitFieldType.getType(), pObjectType);
      case CTypedefType typedefType ->
          typedefType.getName().equals(name)
              || typedefType.getName().equals(SequentializationParseTest.ANON_TYPE_KEYWORD + name);
      case CElaboratedType elaboratedType ->
          elaboratedType.getName().equals(name)
              || elaboratedType
                  .getName()
                  .equals(SequentializationParseTest.ANON_TYPE_KEYWORD + name);
      case CCompositeType compositeType ->
          compositeType.getName().equals(name)
              || compositeType
                  .getName()
                  .equals(SequentializationParseTest.ANON_TYPE_KEYWORD + name);
      default -> false;
    };
  }

  private static CInitializer substituteInitializerAtPath(
      CInitializer pInitializer,
      ImmutableList<Integer> pPath,
      int pPathIndex,
      CCompositeTypeMemberDeclaration pMember,
      CInitializer pSubstituteInitializer) {

    // the path was consumed completely
    if (pPathIndex == pPath.size()) {
      return pSubstituteInitializer;
    }

    if (!(pInitializer instanceof CInitializerList initializerList)) {
      return pInitializer;
    }

    int targetIndex = pPath.get(pPathIndex);
    List<CInitializer> newInitializers = new ArrayList<>(initializerList.getInitializers());
    int actualIndex = targetIndex;

    // A designated initializer uses its field name instead of its positional index.
    for (int i = 0; i < newInitializers.size(); i++) {
      CInitializer initializer = newInitializers.get(i);
      if (initializer instanceof CDesignatedInitializer designatedInitializer
          && designatedInitializer.getDesignators().stream()
              .filter(CFieldDesignator.class::isInstance)
              .map(CFieldDesignator.class::cast)
              .anyMatch(designator -> designator.getFieldName().equals(pMember.getName()))) {
        actualIndex = i;
        break;
      }
    }

    if (actualIndex < 0 || actualIndex >= newInitializers.size()) {
      return pInitializer;
    }

    CInitializer targetInitializer = newInitializers.get(actualIndex);

    if (targetInitializer instanceof CDesignatedInitializer designatedInitializer) {
      CInitializer newRight =
          substituteInitializerAtPath(
              designatedInitializer.getRightHandSide(),
              pPath,
              pPathIndex + 1,
              pMember,
              pSubstituteInitializer);

      newInitializers.set(
          actualIndex,
          new CDesignatedInitializer(
              designatedInitializer.getFileLocation(),
              designatedInitializer.getDesignators(),
              newRight));

    } else {
      newInitializers.set(
          actualIndex,
          substituteInitializerAtPath(
              targetInitializer, pPath, pPathIndex + 1, pMember, pSubstituteInitializer));
    }

    return new CInitializerList(initializerList.getFileLocation(), newInitializers);
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

    private final Map<ImmutableList<Integer>, CCompositeTypeMemberDeclaration> paths =
        new HashMap<>();

    /** The current path taken through composite type members. */
    private final List<Integer> currentPath = new ArrayList<>();

    private CTypeSubstitutionVisitor(ImmutableSet<String> pSubstitutedNames, CType pSubstitution) {
      substitutedNames = pSubstitutedNames;
      substitution = pSubstitution;
    }

    private CTypeSubstitutionResult result(CType pType, boolean pIsSubstituted) {
      return new CTypeSubstitutionResult(pType, ImmutableMap.copyOf(paths), pIsSubstituted);
    }

    @Override
    public CTypeSubstitutionResult visitDefault(CType pType) {
      return pType.accept(this);
    }

    @Override
    public CTypeSubstitutionResult visit(CVoidType pVoidType) {
      return result(pVoidType, false);
    }

    @Override
    public CTypeSubstitutionResult visit(CSimpleType pSimpleType) {
      return result(pSimpleType, false);
    }

    @Override
    public CTypeSubstitutionResult visit(CArrayType pArrayType) {
      // Do not use getCanonicalType() on CArrayType, as this may replace CElaboratedType
      // with CCompositeType and lead to duplicate declarations.
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
        return result(pCompositeType, false);
      }

      ImmutableList.Builder<CCompositeTypeMemberDeclaration> memberDeclarations =
          ImmutableList.builder();
      boolean isSubstituted = false;

      for (int i = 0; i < pCompositeType.getMembers().size(); i++) {
        CCompositeTypeMemberDeclaration memberDeclaration = pCompositeType.getMembers().get(i);
        // enter the current composite member
        currentPath.add(i);
        CTypeSubstitutionResult memberResult = memberDeclaration.getType().accept(this);
        // record the complete path if this member contains any substitution
        if (memberResult.isSubstituted()) {
          paths.put(ImmutableList.copyOf(currentPath), memberDeclaration);
        }
        // leave the current composite member
        currentPath.removeLast();

        memberDeclarations.add(memberDeclaration.withType(memberResult.type()));

        isSubstituted |= memberResult.isSubstituted();
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
        return result(pElaboratedType, false);
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

      return result(pEnumType, false);
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
