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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Verify.verify;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class ProgramDeclarations {

  private final Map<String, CSimpleDeclaration> globalVars;
  private final Map<String, CFunctionDeclaration> functions;
  private final Map<String, CComplexTypeDeclaration> types;
  private final Map<String, CElaboratedType> elaboratedTypes;
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final Multimap<String, String> origNamesToQualifiedNames;

  public ProgramDeclarations() {
    globalVars = new HashMap<>();
    functions = new HashMap<>();
    types = new HashMap<>();
    elaboratedTypes = new HashMap<>();
    typedefs = new HashMap<>();
    origNamesToQualifiedNames = HashMultimap.<String, String>create();
  }

  /**
   * Register a type in the program wide scope. This does not mean that
   * every other file of the program has access to this type, but it does mean
   * that if the same type is declared in another file these types will be
   * identical afterwards. (This happens in conjunction to the proper handling
   * of type declarations in the GlobalScope)
   */
  public void registerTypeDeclaration(CComplexTypeDeclaration declaration) {
    CComplexType type = declaration.getType();
    String qualifiedName = type.getQualifiedName();

    // if there is only an elaborated type we do not want to
    // store it in the normal declarations map but separated from it
    if (type.getCanonicalType() instanceof CElaboratedType){
      elaboratedTypes.put(qualifiedName, (CElaboratedType) type);
      origNamesToQualifiedNames.put(type.getOrigName(), type.getQualifiedName());
      return;
    }

    if (types.containsKey(qualifiedName)) {
      CComplexTypeDeclaration oldDecl = types.get(qualifiedName);
      if (!(oldDecl.getType().getCanonicalType() instanceof CElaboratedType
            && areEqualTypes(oldDecl.getType().getCanonicalType(), type.getCanonicalType()))) {
        throw new CFAGenerationRuntimeException("There is already a type registered with the qualified name: " + qualifiedName);
      }
    } else {
      origNamesToQualifiedNames.put(type.getOrigName(), type.getQualifiedName());
    }


    // check if there are elaborated types that need to be completed from other
    // files with the same name, then we complete them this can only happen
    // if the type is not anonymous
    if (!type.getOrigName().equals("")) {
      completeElaboratedTypes(type);
    }

    types.put(qualifiedName, declaration);
  }

  /**
   * Tries to complete all elaborated types either with the type from the parameter
   * or if this is already renamed with the complete type that is not renamed
   */
  private void completeElaboratedTypes(CComplexType type) {
    // this is a renamed type we cannot use it as realType because
    // of some restrictions in the setRealType method
    if (!type.getName().endsWith(type.getOrigName())) {
      type = types.get(type.getKind().toASTString() + " " + type.getOrigName()).getType();
    }

    for (String name : origNamesToQualifiedNames.get(type.getOrigName())) {

      // if a type with this name is in the map we check if it is still
      // elaborated and then set the realType for it if necessary
      if (elaboratedTypes.containsKey(name)) {
        CElaboratedType elabType = elaboratedTypes.get(name);

        // if the type is still elaborated then we can set the new type as
        // realtype for it and delete it from the elabTypes map
        if (elabType.getRealType() == null) {
          elabType.setRealType(type);
        }

        elaboratedTypes.remove(name);
      }
    }
  }

  public void registerTypeDefDeclaration(CTypeDefDeclaration declaration) {
    String name = declaration.getName();
    origNamesToQualifiedNames.put(declaration.getOrigName(), name);
    Object shouldBeNull = typedefs.put(name, declaration);
    verify(shouldBeNull == null, "There is already a typedeftype registered with the name: %s", name);
  }

  public void registerFunctionDeclaration(CFunctionDeclaration declaration) {
    String name = declaration.getName();

    if (globalVars.containsKey(name)) {
      throw new CFAGenerationRuntimeException("Name of global variable "
          + name + " from " + globalVars.get(name).getFileLocation()
          + " is reused as function declaration", declaration);
    }

    // TODO if there was previously a function with this name registered
    // it has to be the same as now, if not throw an exception
    functions.put(name, declaration);
  }

  public void registerVariableDeclaration(CVariableDeclaration declaration) {
    String name = declaration.getName();
    Object shouldBeNull = globalVars.put(name, declaration);
    verify(shouldBeNull == null, "There is already a global variable registered with the name: %s", name);
  }

  public boolean variableNameInUse(String name) {
    return globalVars.containsKey(name);
  }

  /**
   * This method looks up a type that is matching a certain typeName. If no type
   * can be found the origName is taken into consideration and a type that is not
   * exactly matching the typeName will be returned if found.
   *
   * @param typeName the exact typeName that should be found
   * @param origName the origName that is ok to be found if no exact match occured before
   */
  public CComplexType lookupType(String typeName, String origName) {
    CComplexTypeDeclaration returnType = types.get(typeName);

    // exact matching type found, just return it
    if (returnType != null) {
      return returnType.getType();

      // no exact matching type found, search for origName equivalents
    } else {

      // at first check if origName is a real struct
      returnType = types.get("struct " + origName);
      if (returnType != null) {
        return returnType.getType();
      }

      // if there is also no struct with the origName as name we check
      // all other possible renamed types
      Collection<String> typeNames = origNamesToQualifiedNames.get(origName);
      for (String name : typeNames) {
        returnType = types.get(name);
        if (returnType != null) {
          return returnType.getType();
        }
      }
    }

    // no matching type could be found
    return null;
  }

  public boolean containsTypeWithExactName(String typeName) {
    return types.containsKey(typeName);
  }

  public boolean containsTypeDefWithExactName(String typeDefName) {
    return typedefs.containsKey(typeDefName);
  }

  public boolean containsFunctionWithExactName(String functionName) {
    return functions.containsKey(functionName);
  }

  public boolean containsVariableWithExactName(String variableName) {
    return globalVars.containsKey(variableName);
  }

  public boolean containsEqualType(CComplexTypeDeclaration declaration) {
    return getOrContainsEqualType(declaration).getFirst();
  }

  public boolean containsEqualTypeDef(CTypeDefDeclaration declaration) {
    return getOrContainsEqualTypeDef(declaration).getFirst();
  }

  public CComplexTypeDeclaration getEqualType(CComplexTypeDeclaration declaration) {
    return getOrContainsEqualType(declaration).getSecond();
  }

  public CTypeDefDeclaration getEqualTypeDefDeclaration(CTypeDefDeclaration declaration) {
    return getOrContainsEqualTypeDef(declaration).getSecond();
  }

  private Pair<Boolean, CTypeDefDeclaration> getOrContainsEqualTypeDef(CTypeDefDeclaration declaration) {
    for (String name : origNamesToQualifiedNames.get(declaration.getOrigName())) {
      if (typedefs.containsKey(name)) {
        CType oldType = typedefs.get(name).getType().getCanonicalType();
        CType newType = declaration.getType().getCanonicalType();

        if (areEqualTypes(oldType.getCanonicalType(), newType.getCanonicalType())) {
          return Pair.of(true, typedefs.get(name));
        }
      }
    }
    return Pair.of(false, null);
  }

  private Pair<Boolean, CComplexTypeDeclaration> getOrContainsEqualType(CComplexTypeDeclaration declaration) {
    CComplexType newType = (CComplexType) declaration.getType().getCanonicalType();
    for (String name : origNamesToQualifiedNames.get(newType.getOrigName())) {

      // if a type with this name is in the map we continue checking the equality
      // of this type with the new one
      if (types.containsKey(name)) {
        CComplexTypeDeclaration oldDecl = types.get(name);

        // check the type equality with our definition of equality, not
        // the equals methods (these leave out the members of complex types)
        if (areEqualTypes(oldDecl.getType().getCanonicalType(), newType)) {
          return Pair.of(true, oldDecl);
        }
      }
    }
    return Pair.of(false, null);
  }

  /**
   * This methods completes all uncompleted elaborated types by setting another
   * elaborated type as realType (we cannot chose a real realtype as we do not
   * know which this should be)
   */
  public void completeUncompletedElaboratedTypes() {
    Set<String> handledTypes = new HashSet<>();
    for (CElaboratedType type : elaboratedTypes.values()) {
      String origName = type.getOrigName();

      // already handled
      if (handledTypes.contains(origName)) {
        continue;
      }

      handledTypes.add(origName);
      CElaboratedType newType = new CElaboratedType(type.isConst(), type.isVolatile(), type.getKind(), type.getOrigName(), type.getOrigName(), null);
      for (String name : origNamesToQualifiedNames.get(origName)) {
        if (elaboratedTypes.containsKey(name)) {
          CElaboratedType elabType = elaboratedTypes.get(name);

          // if the type is still elaborated then we can set the new type as
          // realtype for it and delete it from the elabTypes map
          if (elabType.getRealType() == null) {
            elabType.setRealType(newType);
          }
        }
      }
    }
  }

  /**
   * This method checks the equality of two types (with regards to fields inside
   * of structs).
   */
  private static boolean areEqualTypes(CType type1, CType type2) {
    return areEqualTypes(type1, type2, new HashMap<>());
  }

  private static boolean areEqualTypes(CType type1, CType type2, Map<Pair<CType, CType>, Boolean> foundTypes) {
    assert type1.equals(type1.getCanonicalType()) && type2.equals(type2.getCanonicalType());

    // shortcut for object identity, we do not need to test anything else in this case
    if (type1 == type2) {
      return true;

      // if types have not the same class they cannot be equal unless there is one type that
      // is only elaborated and one that is a complete complex type (or another elaborated type)
    } else if (!(type1.getClass() == type2.getClass()
              || (type1 instanceof CComplexType && type2 instanceof CElaboratedType)
              || (type2 instanceof CComplexType && type1 instanceof CElaboratedType))) {
      return false;
    }

    // the key of this types in the map
    Pair<CType, CType> typePair = Pair.of(type1, type2);

    // special case complex type is handled here this is necessary because
    // in the equals method of CComplexTypes the members are not compared
    if (type1 instanceof CComplexType) {
      boolean isOuterTypeEqual = ((CComplexType) type1).equalsWithOrigName(type2);

      // now we need to compare the members of the CComplexTypes
      // due to the checks before we now that at this stage both types
      // have the same class
      if (isOuterTypeEqual) {
        if (type1 instanceof CCompositeType) {
          if (!foundTypes.containsKey(typePair)) {
            boolean areEqual = areEqualCompositeTypes((CCompositeType)type1, (CCompositeType)type2, foundTypes);
            foundTypes.put(typePair, areEqual);
            return areEqual;

            // the type was already found before so we can return true here
          } else {
            return foundTypes.get(typePair);
          }

        } else if (type1 instanceof CEnumType) {
          return areEqualEnumTypes((CEnumType)type1, (CEnumType)type2);

          // no more checks necessary as the outer type is equal and the elaborated
          // type does not have any inner type right now
        } else if (type1 instanceof CElaboratedType) {
          return true;

          // in case new CComplexTypes get introduced
        } else {
          throw new AssertionError("Unhandled CComplexType with kind: " + type1.getClass());
        }

        // one or both types are only elaborated, so we can only check the name of the type
        // and not the members, as elaborated types are always renamed to the
        // file specific version this check has to be done on the original type
        // names
      } else if (type1.getClass() != type2.getClass() || type1 instanceof CElaboratedType) {
        return ((CComplexType)type1).getOrigName().equals(((CComplexType)type2).getOrigName());

        // the types are not equal
      } else {
        return false;
      }

      // a pointer could point to a struct type which needs to be compared
      // with this equality method, thus we have this special case here
    } else if (type1 instanceof CPointerType
               && (((CPointerType)type1).getType() instanceof CComplexType
                   || ((CPointerType)type1).getType() instanceof CFunctionType
                   || ((CPointerType)type1).getType() instanceof CPointerType)) {

        return areEqualTypes(((CPointerType)type1).getType(),
                            ((CPointerType)type1).getType(),
                            foundTypes);

        // handle the same issues we have with pointer types here for arrays
    } else if (type1 instanceof CArrayType
        && (((CArrayType)type1).getType() instanceof CComplexType
            || ((CArrayType)type1).getType() instanceof CFunctionType
            || ((CArrayType)type1).getType() instanceof CPointerType)) {

      // first check if the lengths are matching
      CArrayType a1 = (CArrayType) type1;
      CArrayType a2 = (CArrayType) type2;
      if (a1.getLength() instanceof CIntegerLiteralExpression && a2.getLength() instanceof CIntegerLiteralExpression) {
        if (!((CIntegerLiteralExpression)a1.getLength()).getValue().equals(((CIntegerLiteralExpression)a2.getLength()).getValue())) {
          return false;
        }
      } else {
        if (!Objects.equals(a1.getLength(), a2.getLength())) {
          return false;
        }
      }

      // length is ok, so check the if the types are ok, too
      return areEqualTypes(((CArrayType)type1).getType(),
                          ((CArrayType)type2).getType(),
                          foundTypes);


    } else if (type1 instanceof CFunctionType) {
      return areEqualFunctionTypes((CFunctionType)type1,
                                   (CFunctionType)type2,
                                   foundTypes);

      // no struct, union or enum we can just use the usual equals method
    } else {
      return type1.equals(type2);
    }
  }

  private static boolean areEqualCompositeTypes(CCompositeType type1, CCompositeType type2, Map<Pair<CType, CType>, Boolean> foundTypes) {
    List<CCompositeTypeMemberDeclaration> members1 = type1.getMembers();
    List<CCompositeTypeMemberDeclaration> members2 = type2.getMembers();

    // the types cannot be equal if they have different numbers of fields
    if (members1.size() != members2.size()) {
      return false;
    }

    boolean areEqual = true;
    for (int i = 0; i < members1.size() && areEqual; i++) {
      String member1Name = members1.get(i).getName();
      String member2Name = members2.get(i).getName();

      CType typeM1 = members1.get(i).getType();
      CType typeM2 = members2.get(i).getType();

      // if the members are anonymous we cannot rely on the name of the field
      // so we exclude it from the equality test
      boolean isAnonymousField = member1Name.contains("_anon_type_member_") && member2Name.contains("_anon_type_member_");
      areEqual = member1Name.equals(member2Name) || isAnonymousField;

      // if the name is already not matching (same or anonymous) we don't need to compare
      // the types of the fields
      areEqual = areEqual && areEqualTypes(typeM1.getCanonicalType(), typeM2.getCanonicalType(), foundTypes);
    }

    return areEqual;
  }

  private static boolean areEqualEnumTypes(CEnumType type1, CEnumType type2) {
    List<CEnumerator> members1 = type1.getEnumerators();
    List<CEnumerator> members2 = type2.getEnumerators();

    // the types cannot be equal if they have different numbers of enumerators
    if (members1.size() != members2.size()) {
      return false;
    }

    boolean areEqual = true;
    for (int i = 0; i < members1.size() && areEqual; i++) {
      CEnumerator member1 = members1.get(i);
      CEnumerator member2 = members2.get(i);
      areEqual = member1.getName().equals(member2.getName());
      areEqual = areEqual && ((member1.hasValue() && member2.hasValue() && member1.getValue() == member2.getValue())
                              || (!member1.hasValue() && !member2.hasValue()));
    }

    return areEqual;
  }

  private static boolean areEqualFunctionTypes(CFunctionType type1, CFunctionType type2, Map<Pair<CType, CType>, Boolean> foundTypes) {

    // check the function names but only if there is one
    boolean areEqual = (type1 instanceof CFunctionTypeWithNames
                         && type2 instanceof CFunctionTypeWithNames
                         && !((CFunctionTypeWithNames)type1).getName().equals(((CFunctionTypeWithNames)type2).getName()))
                       || !(type1 instanceof CFunctionTypeWithNames && type2 instanceof CFunctionTypeWithNames);

    // we only need to check the members if the return type is equal
    areEqual = areEqual && areEqualTypes(type1.getReturnType().getCanonicalType(), type2.getReturnType().getCanonicalType(), foundTypes);

    List<CType> params1 = type1.getParameters();
    List<CType> params2 = type2.getParameters();

      // unequal number of parameters, we can return false
    areEqual = areEqual && params1.size() == params2.size();

    for (int i = 0; areEqual && i < params1.size(); i++) {
      areEqual = areEqualTypes(params1.get(i).getCanonicalType(), params2.get(i).getCanonicalType(), foundTypes);
    }

    return areEqual;
  }
}
