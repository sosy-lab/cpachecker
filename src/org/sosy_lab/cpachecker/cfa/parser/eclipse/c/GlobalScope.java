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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
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
class GlobalScope implements Scope {

  private final Map<String, CSimpleDeclaration> globalVars;
  private final Map<String, CFunctionDeclaration> functions;
  private final Map<String, CComplexTypeDeclaration> types;
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final Map<String, CComplexTypeDeclaration> alreadyDeclaratedTypesInOtherFiles;
  private final String currentFile;

  public GlobalScope(Map<String, CSimpleDeclaration> globalVars,
                     Map<String, CFunctionDeclaration> functions,
                     Map<String, CComplexTypeDeclaration> types,
                     Map<String, CTypeDefDeclaration> typedefs,
                     Map<String, CComplexTypeDeclaration> alreadyDeclaratedTypesInOtherFiles,
                     String currentFile) {
    this.globalVars = globalVars;
    this.functions = functions;
    this.types = types;
    this.typedefs = typedefs;
    this.alreadyDeclaratedTypesInOtherFiles = alreadyDeclaratedTypesInOtherFiles;
    this.currentFile = currentFile;
  }

  public GlobalScope() {
    this(new HashMap<String, CSimpleDeclaration>(),
         new HashMap<String, CFunctionDeclaration>(),
         new HashMap<String, CComplexTypeDeclaration>(),
         new HashMap<String, CTypeDefDeclaration>(),
         new HashMap<String, CComplexTypeDeclaration>(),
         "");
  }

  @Override
  public boolean isGlobalScope() {
    return true;
  }

  @Override
  public boolean variableNameInUse(String name, String origName) {
      checkNotNull(name);
      checkNotNull(origName);

      CSimpleDeclaration binding = globalVars.get(origName);
      if (binding != null && binding.getName().equals(name)) {
        return true;
      }
      binding = globalVars.get(name);
      if (binding != null && binding.getName().equals(name)) {
        return true;
      }
      return false;
    }

  @Override
  public CSimpleDeclaration lookupVariable(String name) {
    checkNotNull(name);

    CSimpleDeclaration binding = globalVars.get(name);
    if (binding != null) {
      return binding;
    }

    return null;
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

    final CTypeDefDeclaration declaration = typedefs.get(name);
    if (declaration != null) {
      return declaration.getType();
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

    if (functions.containsKey(name)) {
      // TODO multiple function declarations are legal, as long as they are equal
      // check this and throw exception if not
//        throw new CFAGenerationRuntimeException("Function " + name + " already declared", declaration);
    }

    if (globalVars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of global variable "
          + name + " from line " + globalVars.get(name).getFileLocation().getStartingLineNumber()
          + " is reused as function declaration", declaration);
    }

    functions.put(name, declaration);
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration declaration) {
    assert declaration instanceof CVariableDeclaration
        || declaration instanceof CEnumerator
        : "Tried to register a declaration which does not define a name in the standard namespace: " + declaration;
    assert  !(declaration.getType() instanceof CFunctionTypeWithNames);

    String name = declaration.getOrigName();
    assert name != null;

    if (functions.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of function "
          + name + " from line " + functions.get(name).getFileLocation().getStartingLineNumber()
          + " is reused as identifier in global scope", declaration);
    }

    globalVars.put(name, declaration);
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

    if (types.containsKey(name)) {
      CComplexTypeDeclaration oldDeclaration = types.get(name);

      CComplexType oldType = oldDeclaration.getType();

      // in case we are analyzing multiple files as one cfa there may be several
      // identical type declarations, if it is one we just return false
      if ((type instanceof CElaboratedType && ((CElaboratedType) type).getRealType() == oldType)
          || type == oldType || (oldType instanceof CElaboratedType && ((CElaboratedType)oldType).getRealType() == type)
          || (type instanceof CElaboratedType && oldType instanceof CElaboratedType && ((CElaboratedType) type).getRealType() == ((CElaboratedType)oldType).getRealType())) {
        return false;
      }

      if (alreadyDeclaratedTypesInOtherFiles.containsKey(name) && !(type.getCanonicalType() instanceof CElaboratedType)) {
        CComplexTypeDeclaration otherFile = alreadyDeclaratedTypesInOtherFiles.get(name);
        CComplexType otherType = (CComplexType) otherFile.getType().getCanonicalType();
        type = (CComplexType) type.getCanonicalType();

        if (areEqualTypes(otherType, type, false)) {
          type = otherType;

        } else {
          // remove wrong named declaration from types map
          types.remove(name);
          declaration = createRenamedType(declaration);
          name = declaration.getType().getQualifiedName();
        }

      } else {

        // the current declaration just re-declares an existing type
        if (type instanceof CElaboratedType) {
          return false;
        }

        // two CCompositeTypes or two CEnumTypes
        // declaring struct twice is not allowed, even with equal signatures
        if (oldType.getClass() == type.getClass()) {
          throw new CFAGenerationRuntimeException("Redeclaring " + name
              + " in line " + declaration.getFileLocation().getStartingLineNumber()
              + ", originally declared in line " + oldDeclaration.getFileLocation().getStartingLineNumber());
        }
      }


      assert oldType instanceof CElaboratedType;

      // We now have a real declaration for a type for which we have seen a forward declaration
      // We set a reference to the full type in the old type
      // and update the types map with the full type.
      ((CElaboratedType)oldType).setRealType(type);

    } else if (alreadyDeclaratedTypesInOtherFiles.containsKey(name) && !(type.getCanonicalType() instanceof CElaboratedType)) {
      CComplexTypeDeclaration otherFile = alreadyDeclaratedTypesInOtherFiles.get(name);
      CComplexType knownType = (CComplexType) otherFile.getType().getCanonicalType();
      type = (CComplexType) type.getCanonicalType();

      if (areEqualTypes(knownType, type, false)) {
        declaration = alreadyDeclaratedTypesInOtherFiles.get(name);
      } else {
        declaration = createRenamedType(declaration);
        name = declaration.getType().getQualifiedName();
      }
    }

    types.put(name, declaration);
    return true;
  }

  /**
   * This method checks CComplexTypes on equality. As members are usually not
   * checked by our equality methods these are here checked additionally, but
   * only by name.
   */
  private boolean areEqualTypes(CComplexType oldType, CComplexType forwardType, boolean compareWithoutName) {
    boolean areEqual = false;
    oldType = (CComplexType) oldType.getCanonicalType();
    forwardType = (CComplexType) forwardType.getCanonicalType();
    if (forwardType.equals(oldType) ||
        (compareWithoutName && forwardType.isConst() == oldType.isConst()
            && forwardType.isVolatile() == oldType.isVolatile()
            && forwardType.getKind() == oldType.getKind())) {

      if (forwardType instanceof CCompositeType) {
        List<CCompositeTypeMemberDeclaration> members = ((CCompositeType) forwardType).getMembers();
        List<CCompositeTypeMemberDeclaration> oldMembers = ((CCompositeType) oldType).getMembers();

        if (members.size() == oldMembers.size()) {
          areEqual = true;
          for (int i = 0; i < members.size() && areEqual; i++) {
            String member1 = members.get(i).getName();
            String member2 = oldMembers.get(i).getName();
            if (member1 == null) {
              areEqual = false;
            } else {
              areEqual = member1.equals(member2);
              CType typeM1 = members.get(i).getType();
              CType typeM2 = oldMembers.get(i).getType();
              if (!areEqual
                  && typeM1 instanceof CComplexType && typeM2 instanceof CComplexType
                  && (member1.contains("_anon_type_member") && member2.contains("_anon_type_member_"))) {
                areEqual = areEqualTypes((CComplexType)oldMembers.get(i).getType(), (CComplexType)members.get(i).getType(), true);
              }
            }
          }
        }
      } else if (forwardType instanceof CEnumType) {
        List<CEnumerator> members = ((CEnumType) forwardType).getEnumerators();
        List<CEnumerator> oldMembers = ((CEnumType) oldType).getEnumerators();

        if (members.size() == oldMembers.size()) {
          areEqual = true;
          for (int i = 0; i < members.size() && areEqual; i++) {
            areEqual = members.get(i).getName().equals(oldMembers.get(i).getName());
          }
        }
      }
    } else {

    // in files where only a forwards declaration can be found but no complete
    // type we assume that this type is equal to the before found type with the
    // same name this also works when the elaborated type is the old type, the
    // first type found which has the same name and a complete type will now be
    // the realType of the oldType
    areEqual = ((forwardType instanceof CElaboratedType && forwardType.getName().equals(oldType.getName()))
               || (oldType instanceof CElaboratedType && oldType.getName().equals(forwardType.getName())));
    }

    return areEqual;
  }

  /**
   * Returns the name for the type as it would be if it is renamed.
   */
  private String getRenamedTypeName(String type) {
    return type + "__" + currentFile;
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
                                          ImmutableList.<CCompositeTypeMemberDeclaration>of(), newName);

      IType key = ASTTypeConverter.getTypeFromTypeConversion(oldType, currentFile);
      if (key != null) {
        ASTTypeConverter.overwriteType(key, new CElaboratedType(ct.isConst(), ct.isVolatile(), ct.getKind(), ct.getName(), ct), currentFile);
      }

      List<CCompositeTypeMemberDeclaration> newMembers = new ArrayList<>(((CCompositeType)oldType).getMembers().size());
      for(CCompositeTypeMemberDeclaration decl : ((CCompositeType) oldType).getMembers()) {
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

      CEnumType et = new CEnumType(oldType.isConst(), oldType.isVolatile(), list, newName);
      for (CEnumerator enumValue : et.getEnumerators()) {
        enumValue.setEnum(et);
      }
      newD = new CComplexTypeDeclaration(newD.getFileLocation(), newD.isGlobal(), et);

    } else if (oldType instanceof CElaboratedType) {
      CElaboratedType et = new CElaboratedType(oldType.isConst(), oldType.isVolatile(),
                       oldType.getKind(), newName, null);
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
        return true;
      }

      if (!type.getCanonicalType().equals(oldType.getCanonicalType())) {
        throw new CFAGenerationRuntimeException("Redeclaring " + name
            + " in line " + declaration.getFileLocation().getStartingLineNumber()
            + " with type " + type.toASTString("")
            + ", originally declared in line " + oldDeclaration.getFileLocation().getStartingLineNumber()
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