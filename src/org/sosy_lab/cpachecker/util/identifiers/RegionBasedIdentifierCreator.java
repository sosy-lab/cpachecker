/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.identifiers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class RegionBasedIdentifierCreator extends IdentifierCreator {
  protected final Set<String> addressedVariables;
  protected final Multimap<CCompositeType, String> addressedFields;

  public RegionBasedIdentifierCreator(
      String pFunc,
      Optional<VariableClassification> pVariableClassification) {
    super(pFunc);
    if (pVariableClassification.isPresent()) {
      addressedVariables = pVariableClassification.get().getAddressedVariables();
      addressedFields = pVariableClassification.get().getAddressedFields();
    } else {
      addressedVariables = null;
      addressedFields = null;
    }
  }

  public RegionBasedIdentifierCreator(
      Optional<VariableClassification> pVariableClassification) {
    if (pVariableClassification.isPresent()) {
      addressedVariables = pVariableClassification.get().getAddressedVariables();
      addressedFields = pVariableClassification.get().getAddressedFields();
    } else {
      addressedVariables = null;
      addressedFields = null;
    }
  }

  private RegionBasedIdentifierCreator(
      String pFunc,
      Set<String> pVars,
      Multimap<CCompositeType, String> pFields) {
    super(pFunc);
    addressedVariables = pVars;
    addressedFields = pFields;
  }

  @Override
  public AbstractIdentifier createIdentifier(CSimpleDeclaration decl, int pDereference) {
    Preconditions.checkNotNull(decl);
    String name = decl.getName();
    String scopedName = decl.getQualifiedName();
    CType type = decl.getType();

    if (decl instanceof CVariableDeclaration) {
      if (pDereference == 0 && !addressedVariables.contains(scopedName)) {
        if (((CDeclaration) decl).isGlobal()) {
          return new GlobalVariableIdentifier(name, type, pDereference);
        } else {
          return new LocalVariableIdentifier(name, type, function, pDereference);
        }
      } else {
        return new RegionIdentifier(type.toASTString(""), type);
      }
    } else if (decl instanceof CFunctionDeclaration) {
      return new FunctionIdentifier(name, type, pDereference);
    } else if (decl instanceof CParameterDeclaration) {
      if (pDereference == 0 && !addressedVariables.contains(scopedName)) {
        return new LocalVariableIdentifier(name, type, function, pDereference);
      } else {
        return new RegionIdentifier(type.toASTString(""), type);
      }
    } else if (decl instanceof CEnumerator) {
      return new ConstantIdentifier(name, pDereference);
    } else {
      // Composite type
      return null;
    }
  }

  @Override
  public AbstractIdentifier visit(CFieldReference expression) {
    CExpression owner = expression.getFieldOwner();
    String fieldName = expression.getFieldName();

    CType structType = owner.getExpressionType();
    while (!(structType instanceof CCompositeType)) {
      if (structType instanceof CPointerType) {
        structType = ((CPointerType) structType).getType();
      } else if (structType instanceof CTypedefType) {
        structType = ((CTypedefType) structType).getRealType();
      } else if (structType instanceof CElaboratedType) {
        structType = ((CElaboratedType) structType).getRealType();
      } else {
        throw new UnsupportedOperationException("Unknown CType: " + structType.toASTString(""));
      }
    }
    String typeName = ((CCompositeType) structType).getQualifiedName();

    if (dereference > 0 || addressedFields.get((CCompositeType) structType).contains(fieldName)) {
      return new RegionIdentifier(typeName, structType);
    } else {
      return new RegionIdentifier(fieldName, structType);
    }
  }

  @Override
  public IdentifierCreator copy() {
    return new RegionBasedIdentifierCreator(
        function,
        ImmutableSet.copyOf(addressedVariables),
        LinkedHashMultimap.create(addressedFields));
  }
}
