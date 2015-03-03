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
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class ProgramDeclarations {

  private final Map<String, CSimpleDeclaration> globalVars;
  private final Map<String, CFunctionDeclaration> functions;
  private final Map<String, CComplexTypeDeclaration> types;
  private final Map<String, CTypeDefDeclaration> typedefs;
  private final Multimap<String, String> origNamesToQualifiedNames;

  public ProgramDeclarations() {
    globalVars = new HashMap<>();
    functions = new HashMap<>();
    types = new HashMap<>();
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

    if (types.containsKey(qualifiedName)) {
      CComplexTypeDeclaration oldDecl = types.get(qualifiedName);
      if (!(oldDecl.getType().getCanonicalType() instanceof CElaboratedType
            && areEqualTypes(oldDecl.getType(), type, false))) {
        throw new CFAGenerationRuntimeException("There is already a type registered with the qualified name: " + qualifiedName);
      }
    } else {
      origNamesToQualifiedNames.put(type.getOrigName(), type.getQualifiedName());
    }

    types.put(qualifiedName, declaration);
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
   * @return
   */
  public CComplexType lookupType(String typeName, String origName) {
    CComplexTypeDeclaration returnType = types.get(typeName);

    // exact matching type found, just return it
    if (returnType != null) {
      return returnType.getType();

      // no exact matching type found, search for origName equivalents
    } else {
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

        if ((oldType instanceof CComplexType && newType instanceof CComplexType && areEqualTypes((CComplexType)oldType, (CComplexType)newType, false))
            || !(oldType instanceof CComplexType && !(newType instanceof CComplexType) && oldType.equals(newType))) {
          return Pair.of(true, typedefs.get(name));
        }
      }
    }
    return Pair.of(false, null);
  }

  private Pair<Boolean, CComplexTypeDeclaration> getOrContainsEqualType(CComplexTypeDeclaration declaration) {
    for (String name : origNamesToQualifiedNames.get(declaration.getType().getOrigName())) {
      if (types.containsKey(name) && areEqualTypes(types.get(name).getType(), declaration.getType(), false)) {
        return Pair.of(true, types.get(name));
      }
    }
    return Pair.of(false, null);
  }

  /**
   * This method checks CComplexTypes on equality. As members are usually not
   * checked by our equality methods these are here checked additionally, but
   * only by name.
   */
  public static boolean areEqualTypes(CComplexType oldType, CComplexType forwardType, boolean compareWithoutName) {
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
      } else if (forwardType instanceof CElaboratedType && oldType instanceof CElaboratedType) {
        areEqual = forwardType.getQualifiedName().equals(oldType.getQualifiedName());
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
}
