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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICBasicType;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CDummyType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType.ElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNamedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedef;

/** This Class contains functions,
 * that convert types from C-source into CPAchecker-format. */
class ASTTypeConverter {

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  static CType conv(IType t) {
    if (t instanceof org.eclipse.cdt.core.dom.ast.IBasicType) {
      try {
        return conv((org.eclipse.cdt.core.dom.ast.IBasicType) t);
      } catch (DOMException e) {
        throw new CFAGenerationRuntimeException(e);
      }

    } else if (t instanceof IPointerType) {
      return conv((IPointerType) t);

    } else if (t instanceof ITypedef) {
      return conv((ITypedef) t);

    } else if (t instanceof IBinding) {
      return new CComplexType(((IBinding) t).getName());

    } else if (t instanceof org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType) {
      return conv((org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType)t);

    } else {
      return new CDummyType(t.toString());
    }
  }

  @SuppressWarnings("deprecation")
  private static CSimpleType conv(final IBasicType t) throws DOMException {

    // The IBasicType has to be an ICBasicType or
    // an IBasicType of type "void" (then it is an ICPPBasicType)
    if (t instanceof ICBasicType) {
      final ICBasicType c = (ICBasicType) t;

      CBasicType type;
      switch (t.getType()) {
      case ICBasicType.t_Bool:
        type = CBasicType.BOOL;
        break;
      case IBasicType.t_char:
        type = CBasicType.CHAR;
        break;
      case IBasicType.t_double:
        type = CBasicType.DOUBLE;
        break;
      case IBasicType.t_float:
        type = CBasicType.FLOAT;
        break;
      case IBasicType.t_int:
        type = CBasicType.INT;
        break;
      case IBasicType.t_unspecified:
        type = CBasicType.UNSPECIFIED;
        break;
      case IBasicType.t_void:
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
      return new CSimpleType(false, false, type,
          c.isLong(), c.isShort(), c.isSigned(), c.isUnsigned(),
          c.isComplex(), c.isImaginary(), c.isLongLong());

    } else if (t.getType() == IBasicType.t_void) {

      // the three values isComplex, isImaginary, isLongLong are initialized
      // with FALSE, because we do not know about them
      return new CSimpleType(false, false, CBasicType.VOID,
          t.isLong(), t.isShort(), t.isSigned(), t.isUnsigned(),
          false, false, false);

    } else {
      throw new CFAGenerationRuntimeException("Unknown type " + t.toString());
    }
  }

  private static CPointerType conv(final IPointerType t) {
    try {
      return new CPointerType(t.isConst(), t.isVolatile(), conv(getType(t)));
    } catch (DOMException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private static IType getType(final IPointerType t) throws DOMException {
    // This method needs to throw DOMException because t.getType() does so in Eclipse CDT 6.
    // Don't inline it, because otherwise Eclipse will complain about an unreachable catch block with Eclipse CDT 7.
    return t.getType();
  }

  private static CTypedef conv(final ITypedef t) {
    try {
      return new CTypedef(t.getName(), conv(getType(t)));
    } catch (DOMException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private static IType getType(final ITypedef t) throws DOMException {
    // This method needs to throw DOMException because t.getType() does so in Eclipse CDT 6.
    // Don't inline it, because otherwise Eclipse will complain about an unreachable catch block with Eclipse CDT 7.
    return t.getType();
  }

  private static CFunctionType conv(final org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType t) {
    final CType returnType = conv(t.getReturnType());

    final List<CParameterDeclaration> paramsList = new ArrayList<CParameterDeclaration>();
    for (IType p : t.getParameterTypes()) {
      paramsList.add(new CParameterDeclaration(null, conv(p), "")); // TODO add filelocation and name?
    }

    return new CFunctionType(false, false, returnType, paramsList, false);
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  @SuppressWarnings("deprecation")
  static CType conv(final IASTSimpleDeclSpecifier d) {
    if (!(d instanceof ICASTSimpleDeclSpecifier)) { throw new CFAGenerationRuntimeException("Unsupported type", d); }

    final ICASTSimpleDeclSpecifier dd = (ICASTSimpleDeclSpecifier) d;
    final CBasicType type;
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
      return ASTTypeConverter.conv(dd.getDeclTypeExpression().getExpressionType());
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

  /** converts the operator of an idExpression: alignOf, sizeOf, typeId, typeOf */
  static TypeIdOperator convTypeIdOperator(final IASTTypeIdExpression e) {
    switch (e.getOperator()) {
    case IASTTypeIdExpression.op_alignof:
      return TypeIdOperator.ALIGNOF;
    case IASTTypeIdExpression.op_sizeof:
      return TypeIdOperator.SIZEOF;
    case IASTTypeIdExpression.op_typeid:
      return TypeIdOperator.TYPEID;
    case IASTTypeIdExpression.op_typeof:
      return TypeIdOperator.TYPEOF;
    default:
      throw new CFAGenerationRuntimeException("Unknown type id operator", e);
    }
  }

  static CNamedType conv(final IASTNamedTypeSpecifier d) {
    return new CNamedType(d.isConst(), d.isVolatile(), ASTConverter.convert(d.getName()));
  }

  static CElaboratedType conv(final IASTElaboratedTypeSpecifier d) {
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

  static CStorageClass convCStorageClass(final IASTDeclSpecifier d) {
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

  /** returns a pointerType, that wraps the type. */
  static CPointerType conv(final IASTPointerOperator po, final CType type) {
    if (po instanceof IASTPointer) {
      IASTPointer p = (IASTPointer)po;
      return new CPointerType(p.isConst(), p.isVolatile(), type);

    } else {
      throw new CFAGenerationRuntimeException("Unknown pointer operator", po);
    }
  }

  /** returns a pointerType, that wraps all the converted types. */
  static CType convPointerOperators(final IASTPointerOperator[] ps, CType type) {
    for (IASTPointerOperator p : ps) {
      type = conv(p, type);
    }
    return type;
  }
}