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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.eclipse.cdt.core.dom.ast.IType;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link Scope} for the global scope
 * (i.e., outside of functions).
 * Allows to register functions, types and global variables.
 */
class GlobalScope extends AbstractScope {

  private final Scope fallbackScope;
  private final Map<String, CSimpleDeclaration> globalVars;
  private final Map<String, CSimpleDeclaration> globalVarsWithNewNames;
  private final Map<String, CFunctionDeclaration> functions;
  private final Map<String, CComplexTypeDeclaration> types;
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final ProgramDeclarations programDeclarations;

  public GlobalScope(
      Map<String, CSimpleDeclaration> globalVars,
      Map<String, CSimpleDeclaration> globalVarsWithNewNames,
      Map<String, CFunctionDeclaration> functions,
      Map<String, CComplexTypeDeclaration> types,
      Map<String, CTypeDefDeclaration> typedefs,
      ProgramDeclarations programDeclarations,
      String currentFile,
      Scope pFallbackScope) {
    super(currentFile);
    this.globalVars = globalVars;
    this.globalVarsWithNewNames = globalVarsWithNewNames;
    this.functions = functions;
    this.types = types;
    this.typedefs = typedefs;
    this.programDeclarations = programDeclarations;
    this.fallbackScope = pFallbackScope;
  }

  public GlobalScope() {
    this(
        new HashMap<>(),
        new HashMap<>(),
        new HashMap<>(),
        new HashMap<>(),
        new HashMap<>(),
        new ProgramDeclarations(),
        "",
        CProgramScope.empty());
  }

  @Override
  public boolean isGlobalScope() {
    return true;
  }

  @Override
  public boolean variableNameInUse(String name) {
    return globalVarsWithNewNames.containsKey(checkNotNull(name))
        || programDeclarations.variableNameInUse(name)
        || fallbackScope.variableNameInUse(name);
  }

  @Override
  public CSimpleDeclaration lookupVariable(String name) {
    CSimpleDeclaration result = globalVars.get(checkNotNull(name));
    if (result == null) {
      result = fallbackScope.lookupVariable(name);
    }
    return result;
  }

  @Override
  public CFunctionDeclaration lookupFunction(String name) {
    CFunctionDeclaration result = functions.get(checkNotNull(name));
    if (result == null) {
      result = fallbackScope.lookupFunction(name);
    }
    return result;
  }

  @Override
  public CComplexType lookupType(String name) {
    checkNotNull(name);

    CComplexTypeDeclaration declaration;

    // if the name is already renamed to the file specific version we do not
    // need to it a second time, however it is necessary that we also test if
    // the type name is available in not renamed format
    if (isFileSpecificTypeName(name)) {
      declaration = types.get(name);
      name = removeFileSpecificPartOfTypeName(name);

      // if the type is not renamed already we first test if the file specific
      // version is in the types map
    } else {
      declaration = types.get(getFileSpecificTypeName(name));
    }

    if (declaration != null) {
      return declaration.getType();
    } else {
      declaration = types.get(name);
      if (declaration != null) {
        return declaration.getType();
      }
    }

    return fallbackScope.lookupType(name);
  }

  @Override
  public CType lookupTypedef(final String name) {
    checkNotNull(name);

    CTypeDefDeclaration declaration = typedefs.get(getFileSpecificTypeName(name));
    if (declaration != null) {
      return declaration.getType();
    } else {
      declaration = typedefs.get(name);
      if (declaration != null) {
        return declaration.getType();
      }
    }

    return fallbackScope.lookupTypedef(name);
  }

  public CTypeDefDeclaration lookupTypedefForTypename(final String name) {
    for (CTypeDefDeclaration d : typedefs.values()) {
      if (d.getType() instanceof CComplexType
          && ((CComplexType)d.getType()).getName().equals(name)) {
        return d;
      }
    }

    return null;
  }

  @Override
  public String createScopedNameOf(String pName) {
    return pName;
  }

  public void registerFunctionDeclaration(CFunctionDeclaration declaration) {
    String name = declaration.getName();
    assert name != null;

    // TODO multiple function declarations are legal, as long as they are equal, check this and throw exception if not

    if (globalVars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of global variable "
          + name + " from " + globalVars.get(name).getFileLocation()
          + " is reused as function declaration", declaration);
    }

    functions.put(name, declaration);
    programDeclarations.registerFunctionDeclaration(declaration);
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration declaration) {
    assert declaration instanceof CVariableDeclaration
        || declaration instanceof CEnumerator
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType().getCanonicalType() instanceof CFunctionType)
        : "Tried to register a variable with the type of a function: " + declaration;

    String name = declaration.getOrigName();
    assert name != null;

    if (functions.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of function "
          + name + " from " + functions.get(name).getFileLocation()
          + " is reused as identifier in global scope", declaration);
    }

    globalVars.put(name, declaration);
    globalVarsWithNewNames.put(declaration.getName(), declaration);
  }

  /**
   * Register a type, e.g., a new struct type.
   *
   * @return True if the type actually needs to be declared, False if the declaration can be omitted because the type is already known.
   */
  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration declaration) {
    CComplexType type = declaration.getType();
    String name = type.getQualifiedName();
    boolean isOnlyElaboratedType = type.getCanonicalType() instanceof CElaboratedType;

    // This is an unnamed type like "enum { e }". We ignore it.
    if (type.getName().isEmpty()) {
      return true;

      // This is an elaborated type like "struct s__filename;" We check that it has the
      // proper name (the filename suffix)
      // if there is already a type with this name (or the not file specific version
      // registered we can quit here.
    } else if (type instanceof CElaboratedType) {
      if (isOnlyElaboratedType) {
        assert isFileSpecificTypeName(type.getName()) : "The type should have the correct name before registering it.";
      }

      // the current declaration just re-declares an existing type this could
      // be an elaborated type without realType (thus file specific) or an elaborated
      // type with realtype (not file specific) we have to check for both names
      if (types.containsKey(getFileSpecificTypeName(name))
          || types.containsKey(removeFileSpecificPartOfTypeName(name))) {
        return false;
      }
    }

    boolean programContainsEqualType = !isOnlyElaboratedType && programDeclarations.containsEqualType(declaration);
    boolean programContainsExactNamedType = !isOnlyElaboratedType && programDeclarations.containsTypeWithExactName(name);

    // when entering this if clause we know that the current type is not an elaborated
    // type, as this would have been captured in the if clause above, thus it was
    // not renamed before, however if there is a former elaborated type it was
    // renamed, thus we have to search for the renamed name in the types map
    if (types.containsKey(getFileSpecificTypeName(name))) {
      assert !(type.getCanonicalType() instanceof CElaboratedType);
      String fileSpecificTypeName = getFileSpecificTypeName(name);

      CComplexTypeDeclaration oldDeclaration = types.get(fileSpecificTypeName);
      CComplexType oldType = oldDeclaration.getType();

      // the old type is already complete and the new type is also a complete
      // type this may only be if the objects are identical (FillInBindingsVisitor
      // sets the realtype in some cases before the complete type is registered
      if (!(oldType.getCanonicalType() instanceof CElaboratedType)
          && ((CElaboratedType)oldType).getRealType() != type) {
        throw new CFAGenerationRuntimeException("Redeclaring " + name
            + " in " + declaration.getFileLocation()
            + ", originally declared in " + oldDeclaration.getFileLocation());
      }

      // there was already a declaration before and the found type is already known
      // from another file so we chose as realtype for the former declaration
      // the type from the other file
      if (programContainsEqualType) {
        CComplexTypeDeclaration oldProgDeclaration = programDeclarations.getEqualType(declaration);
        overwriteTypeIfNecessary(type, oldProgDeclaration.getType());
        type = oldProgDeclaration.getType();

      // there was already a declaration with this typename before, however
      // the types do not match so we need to rename the type for this file
      } else if (programContainsExactNamedType) {
        declaration = createRenamedTypeDeclaration(declaration);
        name = declaration.getType().getQualifiedName();
        overwriteTypeIfNecessary(type, declaration.getType());
      }

      // We now have a real declaration for a type for which we have seen a forward
      // declaration. We set a reference to the full type in the old type he types
      // map with the full type. But only if this was not done before
      if (oldType.getCanonicalType() instanceof CElaboratedType) {
        ((CElaboratedType)oldType).setRealType(type);
      }
      types.remove(fileSpecificTypeName);

      // there was no former type declaration here, but the TYPE that should
      // be declared is already known from another parsed file, so we take
      // the type from this file instead of the new one
    } else if (programContainsEqualType) {
      declaration = programDeclarations.getEqualType(declaration);
      overwriteTypeIfNecessary(type, declaration.getType());

      // there was no former type declaration here, but the NAME of the type that
      // should be declared is already known from another parsed file, so we rename
      // the new type
    } else if (programContainsExactNamedType) {
      declaration = createRenamedTypeDeclaration(declaration);
      name = declaration.getType().getQualifiedName();
      overwriteTypeIfNecessary(type, declaration.getType());
    }

    if (!programContainsEqualType) {
      programDeclarations.registerTypeDeclaration(declaration);
    }

    types.put(name, declaration);
    return true;
  }

  private void overwriteTypeIfNecessary(CType oldType, CType newType) {
    IType iType = ASTTypeConverter.getTypeFromTypeConversion(oldType, currentFile);
    if (iType != null) {
      ASTTypeConverter.overwriteType(iType, newType, currentFile);
    }
  }

  /**
   * This method creates a new CComplexTypeDeclaration with an unoccupied name for
   * unequal types with the same name.
   */
  private CComplexTypeDeclaration createRenamedTypeDeclaration(CComplexTypeDeclaration oldDeclaration) {
    assert !isFileSpecificTypeName(oldDeclaration.getType().getName()) : "The type is already renamed to its file specific version.";

    CComplexType oldType = (CComplexType) oldDeclaration.getType().getCanonicalType();

    return new CComplexTypeDeclaration(oldDeclaration.getFileLocation(),
                                       oldDeclaration.isGlobal(),
                                       createRenamedType(oldType));
  }

  /**
   * This method is a helper method for <code>createRenamedTypeDeclaration</code>.
   * It renames the given CComplexType to its files specific version.
   *
   * @param oldType The type that should be renamed.
   * @return The renamed type.
   */
  private CComplexType createRenamedType(CComplexType oldType) {
    assert !isFileSpecificTypeName(oldType.getName()) : "The type is already renamed to its file specific version.";

    String newName = getFileSpecificTypeName(oldType.getName());

    if (oldType instanceof CCompositeType) {
      CCompositeType oldCompositeType = (CCompositeType) oldType;
      CCompositeType renamedCompositeType = new CCompositeType(oldType.isConst(),
                                                               oldType.isVolatile(),
                                                               oldType.getKind(),
                                                               newName,
                                                               oldType.getOrigName());

      // overwrite the already found type in the types map of the ASTTypeConverter if necessary
      // we need to do this, that the members of the renamed CCompositeType get the correct type names
      // in case of members pointing to the renamed type itself
      CElaboratedType renamedElaboratedType = new CElaboratedType(renamedCompositeType.isConst(),
                                                                  renamedCompositeType.isVolatile(),
                                                                  renamedCompositeType.getKind(),
                                                                  renamedCompositeType.getName(),
                                                                  renamedCompositeType.getOrigName(),
                                                                  renamedCompositeType);
      overwriteTypeIfNecessary(oldType, renamedElaboratedType);

      List<CCompositeTypeMemberDeclaration> newMembers = new ArrayList<>(oldCompositeType.getMembers().size());
      for (CCompositeTypeMemberDeclaration decl : oldCompositeType.getMembers()) {


        // here we need to take care of the case that the pointer could be pointing
        // to the same that that is renamed currently
        // we need to put in the elaborated renamed type, otherwise there will be
        // infinite recursion in the types toASTString method, what we don't want
        if (decl.getType() instanceof CPointerType) {
          newMembers.add(new CCompositeTypeMemberDeclaration(createPointerField((CPointerType) decl.getType(), oldType,
              renamedElaboratedType), decl.getName()));

        // this member cannot be self referencing as it is no pointer
        } else {
          newMembers.add(new CCompositeTypeMemberDeclaration(decl.getType(), decl.getName()));

        }
      }
      renamedCompositeType.setMembers(newMembers);

      return renamedCompositeType;

    } else if (oldType instanceof CEnumType) {
      List<CEnumerator> list = new ArrayList<>(((CEnumType) oldType).getEnumerators().size());

      for (CEnumerator c : ((CEnumType) oldType).getEnumerators()) {
        CEnumerator newC = new CEnumerator(c.getFileLocation(), c.getName(), c.getQualifiedName(), c.hasValue() ? c.getValue() : null);
        list.add(newC);
      }

      CEnumType renamedEnumType = new CEnumType(oldType.isConst(), oldType.isVolatile(), list, newName, oldType.getOrigName());
      for (CEnumerator enumValue : renamedEnumType.getEnumerators()) {
        enumValue.setEnum(renamedEnumType);
      }
      return renamedEnumType;

    } else if (oldType instanceof CElaboratedType) {
      CComplexType renamedRealType = null;
      if (((CElaboratedType) oldType).getRealType() != null) {
        renamedRealType = createRenamedType(((CElaboratedType) oldType).getRealType());
      }
      return new CElaboratedType(oldType.isConst(), oldType.isVolatile(), oldType.getKind(), newName, oldType.getOrigName(), renamedRealType);

    } else {
      throw new AssertionError("Unhandled CComplexType.");
    }
  }

  /**
   * This method creates the CType for a referenced field of a CCompositeType.
   */
  private CType createPointerField(CPointerType oldType, CType eqType, CType newType) {
    if (oldType.getType() instanceof CPointerType) {
      return new CPointerType(oldType.isConst(), oldType.isVolatile(), createPointerField((CPointerType) oldType.getType(), eqType, newType));
    } else {
      if (oldType.getType().getCanonicalType().equals(eqType.getCanonicalType())) {
        return new CPointerType(oldType.isConst(), oldType.isVolatile(), newType);
      } else {
        return new CPointerType(oldType.isConst(), oldType.isVolatile(), oldType.getType());
      }
    }
  }

  /**
   * Register a typedef.
   *
   * @return True if the type actually needs to be declared, False if the declaration can be omitted because the type is already known.
   */
  public boolean registerTypeDeclaration(CTypeDefDeclaration declaration) {
    String name = declaration.getName();

    if (typedefs.containsKey(name)) {
      CTypeDefDeclaration oldDeclaration = typedefs.get(name);
      CType type = declaration.getType();

      CType oldType = oldDeclaration.getType();

      if (oldType.getCanonicalType() instanceof CElaboratedType && type.getCanonicalType() instanceof CCompositeType
          && ((CElaboratedType) oldType).getName().equals(((CCompositeType)type).getName())) {
        typedefs.put(name, declaration);
        return true;
      }

      if (!type.getCanonicalType().equals(oldType.getCanonicalType())) {
        throw new CFAGenerationRuntimeException("Redeclaring " + name
            + " in " + declaration.getFileLocation()
            + " with type " + type.toASTString("")
            + ", originally declared in " + oldDeclaration.getFileLocation()
            + " with type " + oldType.toASTString(""));
      }
      // redundant typedef, ignore it
      return false;
    }
    typedefs.put(name, declaration);
    return true;
  }

  public ImmutableMap<String, CFunctionDeclaration> getFunctions() {
    return ImmutableMap.copyOf(functions);
  }

  public ImmutableMap<String, CComplexTypeDeclaration> getTypes() {
    return ImmutableMap.copyOf(types);
  }

  public ImmutableMap<String, CSimpleDeclaration> getGlobalVars() {
    return ImmutableMap.copyOf(globalVars);
  }

  public ImmutableMap<String, CTypeDefDeclaration> getTypeDefs() {
    return ImmutableMap.copyOf(typedefs);
  }

  @Override
  public String toString() {
    return "Functions: " + Joiner.on(' ').join(functions.keySet());
  }
}
