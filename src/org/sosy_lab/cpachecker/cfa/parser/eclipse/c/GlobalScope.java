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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Implementation of {@link Scope} for the global scope
 * (i.e., outside of functions).
 * Allows to register functions, types and global variables.
 */
class GlobalScope extends AbstractScope {

  private final Map<String, CSimpleDeclaration> globalVars;
  private final Map<String, CSimpleDeclaration> globalVarsWithNewNames;
  private final Map<String, CFunctionDeclaration> functions;
  private final Map<String, CComplexTypeDeclaration> types;
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final ProgramDeclarations programDeclarations;

  public GlobalScope(Map<String, CSimpleDeclaration> globalVars,
                     Map<String, CSimpleDeclaration> globalVarsWithNewNames,
                     Map<String, CFunctionDeclaration> functions,
                     Map<String, CComplexTypeDeclaration> types,
                     Map<String, CTypeDefDeclaration> typedefs,
                     ProgramDeclarations programDeclarations,
                     String currentFile) {
    super(currentFile);
    this.globalVars = globalVars;
    this.globalVarsWithNewNames = globalVarsWithNewNames;
    this.functions = functions;
    this.types = types;
    this.typedefs = typedefs;
    this.programDeclarations = programDeclarations;
  }

  public GlobalScope() {
    this(new HashMap<String, CSimpleDeclaration>(),
         new HashMap<String, CSimpleDeclaration>(),
         new HashMap<String, CFunctionDeclaration>(),
         new HashMap<String, CComplexTypeDeclaration>(),
         new HashMap<String, CTypeDefDeclaration>(),
         new ProgramDeclarations(),
         "");
  }

  @Override
  public boolean isGlobalScope() {
    return true;
  }

  @Override
  public boolean variableNameInUse(String name) {
    return globalVarsWithNewNames.containsKey(checkNotNull(name)) || programDeclarations.variableNameInUse(name);
  }

  @Override
  public CSimpleDeclaration lookupVariable(String name) {
    return globalVars.get(checkNotNull(name));
  }

  @Override
  public CFunctionDeclaration lookupFunction(String name) {
    return functions.get(checkNotNull(name));
  }

  @Override
  public CComplexType lookupType(String name) {
    checkNotNull(name);
    CComplexTypeDeclaration declaration = types.get(getRenamedTypeName(name));
    if (declaration != null) {
      return declaration.getType();
    } else {
      declaration = types.get(name);
      if (declaration != null) {
        return declaration.getType();
      }
    }

    return null;
  }

  @Override
  public CType lookupTypedef(final String name) {
    checkNotNull(name);

    CTypeDefDeclaration declaration = typedefs.get(getRenamedTypeName(name));
    if (declaration != null) {
      return declaration.getType();
    } else {
      declaration = typedefs.get(name);
      if (declaration != null) {
        return declaration.getType();
      }
    }

    return null;
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

    if (type.getName().isEmpty()) {
      // This is an unnamed type like "enum { e }".
      // We ignore it.
      return true;
    }

    String name = type.getQualifiedName();

    boolean programContainsEqualType = programDeclarations.containsEqualType(declaration);
    boolean programContainsExactNamedType = programDeclarations.containsTypeWithExactName(name);

    if (types.containsKey(name)) {
      CComplexTypeDeclaration oldDeclaration = types.get(name);
      CComplexType oldType = oldDeclaration.getType();

      if (type instanceof CElaboratedType) {
        // the current declaration just re-declares an existing type
        return false;
      }

      if (oldType.getClass() == type.getClass()) {
        // two CCompositeTypes or two CEnumTypes
        // declaring struct twice is not allowed, even with equal signatures
        if (oldType.getClass() == type.getClass()) {
          if (declaration.getFileLocation().getFileName().equals(oldDeclaration.getFileLocation().getFileName())) {
            throw new CFAGenerationRuntimeException("Redeclaring " + name
                + " in " + declaration.getFileLocation()
                + ", originally declared in " + oldDeclaration.getFileLocation());
          } else {
            return true;
          }
        }
      }

      assert oldType instanceof CElaboratedType
             && !(type instanceof CElaboratedType);

      boolean realTypeAlreadySet = false;

      // there was already a declaration before and the found type is already known
      // from another file so we chose as realtype for the former declaration
      // the type from the other file
      if (programContainsEqualType) {
        CComplexTypeDeclaration oldProgDeclaration = programDeclarations.getEqualType(declaration);

        // if program wide only an elaborated type is found we set the realtype of this
        // elaborated type to the new found complete type and replace it afterwards
        // with the new found declaration without the elaborated type surrounding it
        if (oldProgDeclaration.getType().getCanonicalType() instanceof CElaboratedType) {
          programDeclarations.registerTypeDeclaration(declaration);
          ((CElaboratedType) oldProgDeclaration.getType()).setRealType(type);

          // if we set the real type here and the declaration from the program
          // scope is the same as the one from the globalscope we don't have
          // to set it later on
          realTypeAlreadySet = oldProgDeclaration.getType() == oldType;

        } else {
          overwriteTypeIfNecessary(type, oldProgDeclaration.getType());
          type = oldProgDeclaration.getType();
        }

      }

      // We now have a real declaration for a type for which we have seen a forward
      // declaration. We set a reference to the full type in the old type he types
      // map with the full type. But only if this was not done before
      if (!realTypeAlreadySet
          && !(!(oldType.getCanonicalType() instanceof CElaboratedType)
               && ProgramDeclarations.areEqualTypes((CComplexType) oldType.getCanonicalType(),
                                                    (CComplexType) type.getCanonicalType(),
                                                    false))) {
        ((CElaboratedType)oldType).setRealType(type);
      }

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
      declaration = createRenamedType(declaration);
      name = declaration.getQualifiedName();
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
  private CComplexTypeDeclaration createRenamedType(CComplexTypeDeclaration newD) {
    CComplexType oldType = newD.getType();
    String newName = getRenamedTypeName(oldType.getName());

    if (oldType instanceof CCompositeType) {
      CCompositeType ct = new CCompositeType(oldType.isConst(), oldType.isVolatile(), oldType.getKind(),
                                          ImmutableList.<CCompositeTypeMemberDeclaration>of(), newName, oldType.getOrigName());

      IType key = ASTTypeConverter.getTypeFromTypeConversion(oldType, currentFile);
      if (key != null) {
        ASTTypeConverter.overwriteType(key, new CElaboratedType(ct.isConst(), ct.isVolatile(), ct.getKind(), ct.getName(), ct.getOrigName(), ct), currentFile);
      }

      List<CCompositeTypeMemberDeclaration> newMembers = new ArrayList<>(((CCompositeType)oldType).getMembers().size());
      for (CCompositeTypeMemberDeclaration decl : ((CCompositeType) oldType).getMembers()) {
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

      CEnumType et = new CEnumType(oldType.isConst(), oldType.isVolatile(), list, newName, oldType.getOrigName());
      for (CEnumerator enumValue : et.getEnumerators()) {
        enumValue.setEnum(et);
      }
      newD = new CComplexTypeDeclaration(newD.getFileLocation(), newD.isGlobal(), et);

    } else if (oldType instanceof CElaboratedType) {
      CElaboratedType et = new CElaboratedType(oldType.isConst(), oldType.isVolatile(),
                       oldType.getKind(), newName, oldType.getOrigName(), null);
      newD = new CComplexTypeDeclaration(newD.getFileLocation(), true, et);
    }
    return newD;
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
        if (programDeclarations.containsEqualTypeDef(declaration)) {

        }
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
