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

import java.math.BigInteger;
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
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IProblemType;
import org.eclipse.cdt.core.dom.ast.IQualifierType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.IValue;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICArrayType;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** This Class contains functions,
 * that convert types from C-source into CPAchecker-format. */
class ASTTypeConverter {

  private final Scope scope;

  ASTTypeConverter(Scope pScope) {
    scope = pScope;
  }

  /** cache for all ITypes, so that they don't have to be parsed again and again
   *  (Eclipse seems to give us identical objects for identical types already). */
  private final static Map<IType, CType> typeConversions = Maps.newIdentityHashMap();

  CType convert(IType t) {
    CType result = typeConversions.get(t);
    if (result == null) {
      result = checkNotNull(convert0(t));
      typeConversions.put(t, result);
    }
    return result;
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  private CType convert0(IType t) {
    if (t instanceof IBasicType) {
      return conv((IBasicType) t);

    } else if (t instanceof IPointerType) {
      return conv((IPointerType) t);

    } else if (t instanceof ITypedef) {
      return conv((ITypedef) t);

    } else if(t instanceof ICompositeType) {
      ICompositeType ct = (ICompositeType) t;

      ComplexTypeKind kind;
      switch (ct.getKey()) {
      case ICompositeType.k_struct:
        kind = ComplexTypeKind.STRUCT;
        break;
      case ICompositeType.k_union:
        kind = ComplexTypeKind.UNION;
      break;
      default:
        throw new CFAGenerationRuntimeException("Unknown key " + ct.getKey() + " for composite type " + t);
      }

      // empty linkedList for the Fields of the struct, they are created afterwards
      // with the right references in case of pointers to a struct of the same type
      // otherwise they would not point to the correct struct
      // TODO: volatile and const cannot be checked here until no, so both is set
      //       to false
      CCompositeType compType = new CCompositeType(false, false, kind, new LinkedList<CCompositeTypeMemberDeclaration>(), ct.getName());

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

    } else if (t instanceof ICArrayType) {
      return conv((ICArrayType)t);

    } else if (t instanceof IQualifierType) {
      return conv((IQualifierType)t);

    } else if (t instanceof IEnumeration) {
      return conv((IEnumeration)t);

    } else if (t instanceof IProblemType) {
      // Of course, the obvious idea would be to throw an exception here.
      // However, CDT seems to give us ProblemTypes even for perfectly legal C code,
      // e.g. in cdaudio_safe.i.cil.c
      return new CProblemType(t.toString() + ": " + ((IProblemType)t).getMessage());

    } else if (t instanceof IProblemBinding) {
      IProblemBinding problem = (IProblemBinding)t;
      if (problem.getASTNode().getRawSignature().equals("__label__")) {
        // This is a "local label" (a GNU C extension).
        // C.f. http://gcc.gnu.org/onlinedocs/gcc/Local-Labels.html#Local-Labels
        return new CProblemType(problem.getASTNode().getRawSignature());
      }
      throw new CFAGenerationRuntimeException(problem.getMessage(), problem.getASTNode());

    } else {
      throw new CFAGenerationRuntimeException("unknown type " + t.getClass().getSimpleName());
    }
  }

  @SuppressWarnings("deprecation")
  private CSimpleType conv(final IBasicType t) {
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

  private CPointerType conv(final IPointerType t) {
      return new CPointerType(t.isConst(), t.isVolatile(), convert(t.getType()));
  }

  private CTypedefType conv(final ITypedef t) {
      return new CTypedefType(false, false, t.getName(), convert(t.getType()));
  }

  private List<CCompositeTypeMemberDeclaration> conv(IField[] pFields) {
    List<CCompositeTypeMemberDeclaration> list = new LinkedList<>();

    for(int i = 0; i < pFields.length; i++) {
      list.add(new CCompositeTypeMemberDeclaration(convert(pFields[i].getType()), pFields[i].getName()));
    }
    return list;
  }

  private CArrayType conv(final ICArrayType t) {
    CExpression length = null;
    IValue v = t.getSize();
    if (v != null && v.numericalValue() != null) {
      CSimpleType intType = new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false);
      length = new CIntegerLiteralExpression(null, intType, BigInteger.valueOf(v.numericalValue()));
    } else {
      // TODO handle cases like int[x] by converting t.getArraySizeExpression()
    }
    return new CArrayType(t.isConst(), t.isVolatile(), convert(t.getType()), length);
  }

  private CType conv(final IQualifierType t) {
    CType i = convert(t.getType());
    boolean isConst = t.isConst();
    boolean isVolatile = t.isVolatile();

    if (isConst == i.isConst() && isVolatile == i.isVolatile()) {
      return i;
    }

    // return a copy of the inner type with isConst and isVolatile overwritten
    if (i instanceof CArrayType) {
      return new CArrayType(isConst, isVolatile, ((CArrayType) i).getType(), ((CArrayType) i).getLength());
    } else if (i instanceof CCompositeType) {
      CCompositeType c = (CCompositeType) i;
      return new CCompositeType(isConst, isVolatile, c.getKind(), c.getMembers(), c.getName());
    } else if (i instanceof CElaboratedType) {
      return new CElaboratedType(isConst, isVolatile, ((CElaboratedType) i).getKind(), ((CElaboratedType) i).getName(), ((CElaboratedType) i).getRealType());
    } else if (i instanceof CEnumType) {
      return new CEnumType(isConst, isVolatile, ((CEnumType) i).getEnumerators(), ((CEnumType) i).getName());
    } else if (i instanceof CFunctionPointerType) {
      CFunctionPointerType p = (CFunctionPointerType) i;
      return new CFunctionPointerType(isConst, isVolatile, p.getReturnType(), p.getParameters(), p.takesVarArgs());
    } else if (i instanceof CFunctionType) {
      // TODO what does it mean that a function is qualified with const or volatile?
      CFunctionType f = (CFunctionType) i;
      return new CFunctionType(isConst, isVolatile, f.getReturnType(), f.getParameters(), f.takesVarArgs());
    } else if (i instanceof CPointerType) {
      return new CPointerType(isConst, isVolatile, ((CPointerType) i).getType());
    } else if (i instanceof CSimpleType) {
      CSimpleType s = (CSimpleType)i;
      return new CSimpleType(isConst, isVolatile, s.getType(), s.isLong(), s.isShort(), s.isSigned(), s.isUnsigned(), s.isComplex(), s.isImaginary(), s.isLongLong());
    } else if (i instanceof CTypedefType) {
      return new CTypedefType(isConst, isVolatile, ((CTypedefType) i).getName(), ((CTypedefType) i).getRealType());
    } else {
      throw new AssertionError();
    }
  }

  private CType conv(final IEnumeration e) {
    // TODO we ignore the enumerators here
    CComplexType realType = scope.lookupType("enum " + e.getName());
    return new CElaboratedType(false, false, ComplexTypeKind.ENUM, e.getName(), realType);
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  @SuppressWarnings("deprecation")
  CType convert(final IASTSimpleDeclSpecifier d) {
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

  CTypedefType convert(final IASTNamedTypeSpecifier d) {
    org.eclipse.cdt.core.dom.ast.IASTName name = d.getName();
    org.eclipse.cdt.core.dom.ast.IBinding binding = name.resolveBinding();
    if (!(binding instanceof IType)) {
      throw new CFAGenerationRuntimeException("Unknown binding of typedef", d);
    }
    return new CTypedefType(d.isConst(), d.isVolatile(), ASTConverter.convert(name), convert((IType)binding));
  }

  CStorageClass convertCStorageClass(final IASTDeclSpecifier d) {
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

  CElaboratedType convert(final IASTElaboratedTypeSpecifier d) {
    ComplexTypeKind type;
    switch (d.getKind()) {
    case IASTElaboratedTypeSpecifier.k_enum:
      type = ComplexTypeKind.ENUM;
      break;
    case IASTElaboratedTypeSpecifier.k_struct:
      type = ComplexTypeKind.STRUCT;
      break;
    case IASTElaboratedTypeSpecifier.k_union:
      type = ComplexTypeKind.UNION;
      break;
    default:
      throw new CFAGenerationRuntimeException("Unknown elaborated type", d);
    }

    return new CElaboratedType(d.isConst(), d.isVolatile(), type, ASTConverter.convert(d.getName()));
  }

  /** returns a pointerType, that wraps the type. */
  CPointerType convert(final IASTPointerOperator po, final CType type) {
    if (po instanceof IASTPointer) {
      IASTPointer p = (IASTPointer) po;
      return new CPointerType(p.isConst(), p.isVolatile(), type);

    } else {
      throw new CFAGenerationRuntimeException("Unknown pointer operator", po);
    }
  }

  /** returns a pointerType, that wraps all the converted types. */
  CType convertPointerOperators(final IASTPointerOperator[] ps, CType type) {
    for (IASTPointerOperator p : ps) {
      type = convert(p, type);
    }
    return type;
  }
}