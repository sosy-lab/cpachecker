/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDummyType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType.ElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CNamedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedef;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** This Class contains functions,
 * that convert types from C-source into CPAchecker-format. */
class ASTTypeConverter {

  /** cache for all ITypes, so that they don't have to be parsed again and again
   *  (Eclipse seems to give us identical objects for identical types already). */
  private final static Map<IType, CType> typeConversions = Maps.newIdentityHashMap();

  static CType convert(IType t) {
    CType result = typeConversions.get(t);
    if (result == null) {
      result = checkNotNull(convert0(t));
      typeConversions.put(t, result);
    }
    return result;
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  private static CType convert0(IType t) {
    if (t instanceof IBasicType) {
      return conv((IBasicType) t);

    } else if (t instanceof IPointerType) {
      return conv((IPointerType) t);

    } else if (t instanceof ITypedef) {
      return conv((ITypedef) t);

    } else if(t instanceof ICompositeType) {
      ICompositeType ct = (ICompositeType) t;

      // empty linkedList for the Fields of the struct, they are created afterwards
      // with the right references in case of pointers to a struct of the same type
      // otherwise they would not point to the correct struct
      // TODO: volatile and const cannot be checked here until no, so both is set
      //       to false
      CCompositeType compType = new CCompositeType(false, false, ct.getKey(), new LinkedList<CCompositeTypeMemberDeclaration>(), ct.getName());

      // We need to cache compType before converting the type of its fields!
      // Otherwise we run into an infinite recursion if the type of one field
      // is (a pointer to) the struct itself.
      typeConversions.put(t, compType);

      compType.setMembers(conv(ct.getFields()));

      return compType;

    } else if (t instanceof IFunctionType) {
      IFunctionType ft = (IFunctionType) t;

      IType[] parameters = ft.getParameterTypes();
      List<CType> newParameters = Lists.newArrayListWithExpectedSize(parameters.length);
      for (IType p : parameters) {
        newParameters.add(convert(p));
      }

      // TODO varargs
      return new CFunctionPointerType(false, false, convert(ft.getReturnType()), newParameters, false);

    } else if (t instanceof IBinding) {
      return new CComplexType(((IBinding) t).getName());

    } else {
      return new CDummyType(t.toString());
    }
  }

  @SuppressWarnings("deprecation")
  private static CSimpleType conv(final IBasicType t) {
    try {

      // The IBasicType has to be an ICBasicType or
      // an IBasicType of type "void" (then it is an ICPPBasicType)
      if (t instanceof org.eclipse.cdt.core.dom.ast.c.ICBasicType) {
        final org.eclipse.cdt.core.dom.ast.c.ICBasicType c =
            (org.eclipse.cdt.core.dom.ast.c.ICBasicType) t;

        CBasicType type;
        switch (t.getType()) {
        case org.eclipse.cdt.core.dom.ast.c.ICBasicType.t_Bool:
          type = CBasicType.BOOL;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_char:
          type = CBasicType.CHAR;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_double:
          type = CBasicType.DOUBLE;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_float:
          type = CBasicType.FLOAT;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_int:
          type = CBasicType.INT;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_unspecified:
          type = CBasicType.UNSPECIFIED;
          break;
        case org.eclipse.cdt.core.dom.ast.IBasicType.t_void:
          type = CBasicType.VOID;
          break;
        default:
          throw new CFAGenerationRuntimeException("Unknown basic type " + t.getType());
        }

        if ((c.isShort() && c.isLong())
            || (c.isShort() && c.isLongLong())
            || (c.isLong() && c.isLongLong())
            || (c.isSigned() && c.isUnsigned())) { throw new CFAGenerationRuntimeException(
            "Illegal combination of type identifiers"); }

        // TODO why is there no isConst() and isVolatile() here?
        return new CSimpleType(false, false, type, c.isLong(), c.isShort(),
            c.isSigned(), c.isUnsigned(), c.isComplex(), c.isImaginary(), c.isLongLong());

      } else if (t.getType() == org.eclipse.cdt.core.dom.ast.IBasicType.t_void) {

        // the three values isComplex, isImaginary, isLongLong are initialized
        // with FALSE, because we do not know about them
        return new CSimpleType(false, false, CBasicType.VOID, t.isLong(), t.isShort(),
            t.isSigned(), t.isUnsigned(), false, false, false);

      } else {
        throw new CFAGenerationRuntimeException("Unknown type " + t.toString());
      }

    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private static CPointerType conv(final IPointerType t) {
      return new CPointerType(t.isConst(), t.isVolatile(), convert(t.getType()));
  }

  private static CTypedef conv(final ITypedef t) {
      return new CTypedef(t.getName(), convert(t.getType()));
  }

  private static List<CCompositeTypeMemberDeclaration> conv(IField[] pFields) {
    List<CCompositeTypeMemberDeclaration> list = new LinkedList<CCompositeTypeMemberDeclaration>();

    for(int i = 0; i < pFields.length; i++) {
      list.add(new CCompositeTypeMemberDeclaration(convert(pFields[i].getType()), pFields[i].getName()));
    }
    return list;
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  @SuppressWarnings("deprecation")
  static CType convert(final IASTSimpleDeclSpecifier d) {
    if (!(d instanceof ICASTSimpleDeclSpecifier)) { throw new CFAGenerationRuntimeException("Unsupported type", d); }
    ICASTSimpleDeclSpecifier dd = (ICASTSimpleDeclSpecifier) d;

    CBasicType type;
    switch (dd.getType()) {
    case ICASTSimpleDeclSpecifier.t_Bool:
      type = CBasicType.BOOL;
      break;
    case IASTSimpleDeclSpecifier.t_char:
      type = CBasicType.CHAR;
      break;
    case IASTSimpleDeclSpecifier.t_double:
      type = CBasicType.DOUBLE;
      break;
    case IASTSimpleDeclSpecifier.t_float:
      type = CBasicType.FLOAT;
      break;
    case IASTSimpleDeclSpecifier.t_int:
      type = CBasicType.INT;
      break;
    case IASTSimpleDeclSpecifier.t_unspecified:
      type = CBasicType.UNSPECIFIED;
      break;
    case IASTSimpleDeclSpecifier.t_void:
      type = CBasicType.VOID;
      break;
    case IASTSimpleDeclSpecifier.t_typeof:
      // TODO This might loose some information of dd or dd.getDeclTypeExpression()
      // (the latter should be of type IASTTypeIdExpression)
      return convert(dd.getDeclTypeExpression().getExpressionType());
    default:
      throw new CFAGenerationRuntimeException("Unknown basic type " + dd.getType() + " "
          + dd.getClass().getSimpleName(), d);
    }

    if ((dd.isShort() && dd.isLong())
        || (dd.isShort() && dd.isLongLong())
        || (dd.isLong() && dd.isLongLong())
        || (dd.isSigned() && dd.isUnsigned())) { throw new CFAGenerationRuntimeException(
        "Illegal combination of type identifiers", d); }

    return new CSimpleType(dd.isConst(), dd.isVolatile(), type,
        dd.isLong(), dd.isShort(), dd.isSigned(), d.isUnsigned(),
        dd.isComplex(), dd.isImaginary(), dd.isLongLong());
  }

  static CNamedType convert(final IASTNamedTypeSpecifier d) {
    return new CNamedType(d.isConst(), d.isVolatile(), ASTConverter.convert(d.getName()));
  }

  static CStorageClass convertCStorageClass(final IASTDeclSpecifier d) {
    switch (d.getStorageClass()) {
    case IASTDeclSpecifier.sc_unspecified:
    case IASTDeclSpecifier.sc_auto:
    case IASTDeclSpecifier.sc_register:
      return CStorageClass.AUTO;

    case IASTDeclSpecifier.sc_static:
      return CStorageClass.STATIC;

    case IASTDeclSpecifier.sc_extern:
      return CStorageClass.EXTERN;

    case IASTDeclSpecifier.sc_typedef:
      return CStorageClass.TYPEDEF;

    default:
      throw new CFAGenerationRuntimeException("Unsupported storage class", d);
    }
  }

  static CElaboratedType convert(final IASTElaboratedTypeSpecifier d) {
    ElaboratedType type;
    switch (d.getKind()) {
    case IASTElaboratedTypeSpecifier.k_enum:
      type = ElaboratedType.ENUM;
      break;
    case IASTElaboratedTypeSpecifier.k_struct:
      type = ElaboratedType.STRUCT;
      break;
    case IASTElaboratedTypeSpecifier.k_union:
      type = ElaboratedType.UNION;
      break;
    default:
      throw new CFAGenerationRuntimeException("Unknown elaborated type", d);
    }

    return new CElaboratedType(d.isConst(), d.isVolatile(), type, ASTConverter.convert(d.getName()));
  }

  /** returns a pointerType, that wraps the type. */
  static CPointerType convert(final IASTPointerOperator po, final CType type) {
    if (po instanceof IASTPointer) {
      IASTPointer p = (IASTPointer) po;
      return new CPointerType(p.isConst(), p.isVolatile(), type);

    } else {
      throw new CFAGenerationRuntimeException("Unknown pointer operator", po);
    }
  }

  /** returns a pointerType, that wraps all the converted types. */
  static CType convertPointerOperators(final IASTPointerOperator[] ps, CType type) {
    for (IASTPointerOperator p : ps) {
      type = convert(p, type);
    }
    return type;
  }
}