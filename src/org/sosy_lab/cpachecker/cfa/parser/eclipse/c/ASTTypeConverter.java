// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
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
import org.eclipse.cdt.core.dom.ast.c.ICArrayType;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/** This Class contains functions, that convert types from C-source into CPAchecker-format. */
class ASTTypeConverter {

  private final Scope scope;
  private final ASTConverter converter;
  private final String filePrefix;
  private final ParseContext parseContext;

  ASTTypeConverter(
      Scope pScope, ASTConverter pConverter, String pFilePrefix, ParseContext pParseContext) {
    scope = pScope;
    converter = pConverter;
    filePrefix = pFilePrefix;
    parseContext = pParseContext;

    pParseContext.registerTypeMemoizationFilePrefixIfAbsent(filePrefix);
  }

  CType convert(IType t) {
    CType result = parseContext.getCType(t, filePrefix);
    if (result == null) {
      result = checkNotNull(convert0(t));
      // re-check, in some cases we updated the map already
      if (parseContext.getCType(t, filePrefix) == null) {
        parseContext.rememberCType(t, result, filePrefix);
      }
    }
    return result;
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  private CType convert0(IType t) {
    if (t instanceof IBasicType iBasicType) {
      return conv(iBasicType);

    } else if (t instanceof IPointerType iPointerType) {
      return conv(iPointerType);

    } else if (t instanceof ITypedef iTypedef) {
      return conv(iTypedef);

    } else if (t instanceof ICompositeType ct) {
      ComplexTypeKind kind =
          switch (ct.getKey()) {
            case ICompositeType.k_struct -> ComplexTypeKind.STRUCT;
            case ICompositeType.k_union -> ComplexTypeKind.UNION;
            default ->
                throw new CFAGenerationRuntimeException(
                    "Unknown key " + ct.getKey() + " for composite type " + t);
          };
      String name = ct.getName();
      String qualifiedName = kind.toASTString() + " " + name;

      @Nullable CComplexType oldType = scope.lookupType(qualifiedName);

      // We have seen this type already.
      // Replace it with a CElaboratedType.
      if (oldType != null) {
        return new CElaboratedType(
            false, false, kind, oldType.getName(), oldType.getOrigName(), oldType);
      }

      // empty linkedList for the Fields of the struct, they are created afterward
      // with the right references in case of pointers to a struct of the same type
      // otherwise they would not point to the correct struct
      // TODO: volatile and const cannot be checked here until no, so both is set
      //       to false
      CCompositeType compType = new CCompositeType(false, false, kind, name, name);

      // We need to cache compType before converting the type of its fields!
      // Otherwise, we run into an infinite recursion if the type of one field
      // is (a pointer to) the struct itself.
      // In order to prevent a recursive reference from compType to itself,
      // we cheat and put a CElaboratedType instance in the map.
      // This means that wherever the ICompositeType instance appears, it will be
      // replaced by a CElaboratedType.
      CElaboratedType elaborateType =
          new CElaboratedType(false, false, kind, name, compType.getOrigName(), compType);
      parseContext.rememberCType(t, elaborateType, filePrefix);

      compType.setMembers(conv(ct.getFields()));

      return compType;

    } else if (t instanceof IFunctionType ft) {
      IType[] parameters = ft.getParameterTypes();
      List<CType> newParameters = new ArrayList<>(parameters.length);
      for (IType p : parameters) {
        if (p instanceof IBasicType iBasicType && iBasicType.getKind() == IBasicType.Kind.eVoid) {
          // there may be a function declaration f(void), which is equal to f()
          // we don't want this dummy parameter "void"
          assert parameters.length == 1;
        } else {
          newParameters.add(convert(p));
        }
      }

      // TODO varargs
      return new CFunctionType(convert(ft.getReturnType()), newParameters, false);

    } else if (t instanceof ICArrayType iCArrayType) {
      return conv(iCArrayType);

    } else if (t instanceof IQualifierType iQualifierType) {
      return conv(iQualifierType);

    } else if (t instanceof IEnumeration iEnumeration) {
      return conv(iEnumeration);

    } else if (t instanceof IProblemType iProblemType) {
      // Of course, the obvious idea would be to throw an exception here.
      // However, CDT seems to give us ProblemTypes even for perfectly legal C code,
      // e.g. in cdaudio_safe.i.cil.c
      return new CProblemType(t + ": " + iProblemType.getMessage());

    } else if (t instanceof IProblemBinding problem) {
      if (problem.getASTNode().getRawSignature().equals("__label__")) {
        // This is a "local label" (a GNU C extension).
        // C.f. http://gcc.gnu.org/onlinedocs/gcc/Local-Labels.html#Local-Labels
        return new CProblemType(problem.getASTNode().getRawSignature());
      }
      throw parseContext.parseError(problem.getMessage(), problem.getASTNode());

    } else {
      throw new CFAGenerationRuntimeException("unknown type " + t.getClass().getSimpleName());
    }
  }

  private CType conv(final IBasicType t) {
    // The IBasicType has to be an ICBasicType or
    // an IBasicType of type "void" (then it is an ICPPBasicType)
    if (t.getKind() == org.eclipse.cdt.core.dom.ast.IBasicType.Kind.eVoid) {
      if (t.isComplex()
          || t.isImaginary()
          || t.isLong()
          || t.isLongLong()
          || t.isShort()
          || t.isSigned()
          || t.isUnsigned()) {
        throw new CFAGenerationRuntimeException("Void type with illegal modifier: " + t);
      }
      return CVoidType.VOID;

    } else if (t instanceof org.eclipse.cdt.core.dom.ast.c.ICBasicType c) {
      CBasicType type =
          switch (t.getKind()) {
            case eBoolean -> CBasicType.BOOL;
            case eChar -> CBasicType.CHAR;
            case eDouble -> CBasicType.DOUBLE;
            case eFloat -> CBasicType.FLOAT;
            case eFloat128 -> CBasicType.FLOAT128;
            case eInt -> CBasicType.INT;
            case eInt128 -> CBasicType.INT128;
            case eUnspecified -> CBasicType.UNSPECIFIED;
            case eVoid -> throw new AssertionError();
            default -> throw new CFAGenerationRuntimeException("Unknown basic type " + t.getKind());
          };
      // the three values isComplex, isImaginary, isLongLong are initialized
      // with FALSE, because we do not know about them
      if ((c.isShort() && c.isLong())
          || (c.isShort() && c.isLongLong())
          || (c.isLong() && c.isLongLong())
          || (c.isSigned() && c.isUnsigned())) {
        throw new CFAGenerationRuntimeException("Illegal combination of type identifiers");
      }

      // TODO why is there no isConst() and isVolatile() here?
      return new CSimpleType(
          false,
          false,
          type,
          c.isLong(),
          c.isShort(),
          c.isSigned(),
          c.isUnsigned(),
          c.isComplex(),
          c.isImaginary(),
          c.isLongLong());

    } else {
      throw new CFAGenerationRuntimeException("Unknown type " + t);
    }
  }

  private CPointerType conv(final IPointerType t) {
    return new CPointerType(t.isConst(), t.isVolatile(), convert(t.getType()));
  }

  private CTypedefType conv(final ITypedef t) {

    final String name = t.getName();

    CType oldType = scope.lookupTypedef(scope.getFileSpecificTypeName(name));

    // We have seen this type already.
    if (oldType != null) {
      return new CTypedefType(false, false, scope.getFileSpecificTypeName(name), oldType);
    } else { // New typedef type (somehow recognized by CDT, but not found in declared types)
      return new CTypedefType(
          false, false, scope.getFileSpecificTypeName(name), convert(t.getType()));
    }
  }

  private List<CCompositeTypeMemberDeclaration> conv(IField[] pFields) {
    List<CCompositeTypeMemberDeclaration> list = new ArrayList<>(pFields.length);

    for (IField pField : pFields) {
      list.add(new CCompositeTypeMemberDeclaration(convert(pField.getType()), pField.getName()));
    }
    return list;
  }

  private CArrayType conv(final ICArrayType t) {
    CExpression length = null;
    IValue v = t.getSize();
    if (v != null && v.numberValue() != null) {
      length =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              BigInteger.valueOf(v.numberValue().longValue()));
    } else {
      try {
        @SuppressWarnings("deprecation")
        IASTExpression arraySizeExpression = t.getArraySizeExpression();
        length = converter.convertExpressionWithoutSideEffects(arraySizeExpression);
        if (length != null) {
          length = converter.simplifyExpressionRecursively(length);
        }
      } catch (DOMException e) {
        throw new CFAGenerationRuntimeException(e);
      }
    }
    return new CArrayType(t.isConst(), t.isVolatile(), convert(t.getType()), length);
  }

  private CType conv(final IQualifierType t) {
    CType i = convert(t.getType());
    final boolean isConst = t.isConst();
    final boolean isVolatile = t.isVolatile();

    // return a copy of the inner type with isConst and isVolatile overwritten
    i = CTypes.withConstSetTo(i, isConst);
    i = CTypes.withVolatileSetTo(i, isVolatile);

    assert i instanceof CProblemType || (isConst == i.isConst() && isVolatile == i.isVolatile());
    return i;
  }

  private CType conv(final IEnumeration e) {
    // TODO we ignore the enumerators here
    @Nullable CComplexType realType = scope.lookupType("enum " + e.getName());
    String name = e.getName();
    String origName = name;
    if (realType != null) {
      name = realType.getName();
      origName = realType.getOrigName();
    } else {
      name = scope.getFileSpecificTypeName(name);
    }
    return new CElaboratedType(false, false, ComplexTypeKind.ENUM, name, origName, realType);
  }

  /** converts types BOOL, INT,..., PointerTypes, ComplexTypes */
  CType convert(final IASTSimpleDeclSpecifier dd) {
    CBasicType type;
    switch (dd.getType()) {
      case IASTSimpleDeclSpecifier.t_bool -> type = CBasicType.BOOL;
      case IASTSimpleDeclSpecifier.t_char -> type = CBasicType.CHAR;
      case IASTSimpleDeclSpecifier.t_double -> type = CBasicType.DOUBLE;
      case IASTSimpleDeclSpecifier.t_float -> type = CBasicType.FLOAT;
      case IASTSimpleDeclSpecifier.t_float128 -> type = CBasicType.FLOAT128;
      case IASTSimpleDeclSpecifier.t_int -> type = CBasicType.INT;
      case IASTSimpleDeclSpecifier.t_int128 -> type = CBasicType.INT128;
      case IASTSimpleDeclSpecifier.t_unspecified -> type = CBasicType.UNSPECIFIED;
      case IASTSimpleDeclSpecifier.t_void -> {
        if (dd.isComplex()
            || dd.isImaginary()
            || dd.isLong()
            || dd.isLongLong()
            || dd.isShort()
            || dd.isSigned()
            || dd.isUnsigned()) {
          throw parseContext.parseError("Void type with illegal modifier", dd);
        }
        return CVoidType.create(dd.isConst(), dd.isVolatile());
      }
      case IASTSimpleDeclSpecifier.t_typeof -> {
        CType ctype;
        if (dd.getDeclTypeExpression() instanceof IASTTypeIdExpression typeId) {
          verify(
              typeId.getOperator() == IASTTypeIdExpression.op_typeof,
              "Unepxected type-id expression %s for typeof operator",
              typeId);
          ctype = converter.convert(typeId.getTypeId());
        } else {
          ctype = convert(dd.getDeclTypeExpression().getExpressionType());
        }

        // readd the information about isVolatile and isConst if they got lost in
        // the previous conversion
        if (dd.isConst()) {
          ctype = CTypes.withConst(ctype);
        }
        if (dd.isVolatile()) {
          ctype = CTypes.withVolatile(ctype);
        }
        return ctype;
      }
      default ->
          throw parseContext.parseError(
              "Unknown basic type " + dd.getType() + " " + dd.getClass().getSimpleName(), dd);
    }

    if ((dd.isShort() && dd.isLong())
        || (dd.isShort() && dd.isLongLong())
        || (dd.isLong() && dd.isLongLong())
        || (dd.isSigned() && dd.isUnsigned())) {
      throw parseContext.parseError("Illegal combination of type identifiers", dd);
    }

    return new CSimpleType(
        dd.isConst(),
        dd.isVolatile(),
        type,
        dd.isLong(),
        dd.isShort(),
        dd.isSigned(),
        dd.isUnsigned(),
        dd.isComplex(),
        dd.isImaginary(),
        dd.isLongLong());
  }

  CType convert(final IASTNamedTypeSpecifier d) {
    org.eclipse.cdt.core.dom.ast.IASTName astName = d.getName();
    String name = ASTConverter.convert(astName);
    org.eclipse.cdt.core.dom.ast.IBinding binding = astName.resolveBinding();
    if (!(binding instanceof IType iType)) {
      throw parseContext.parseError("Unknown binding of typedef", d);
    }
    CType type = null;
    if (binding instanceof IProblemBinding) {
      type = scope.lookupTypedef(scope.getFileSpecificTypeName(name));
    }

    if (type == null) {
      type = convert(iType);
    }

    if (d.isConst()) {
      type = CTypes.withConst(type);
    }
    if (d.isVolatile()) {
      type = CTypes.withVolatile(type);
    }

    return type;
  }

  CStorageClass convertCStorageClass(final IASTDeclSpecifier d) {
    return switch (d.getStorageClass()) {
      case IASTDeclSpecifier.sc_unspecified,
          IASTDeclSpecifier.sc_auto,
          IASTDeclSpecifier.sc_register ->
          CStorageClass.AUTO;
      case IASTDeclSpecifier.sc_static -> CStorageClass.STATIC;
      case IASTDeclSpecifier.sc_extern -> CStorageClass.EXTERN;
      case IASTDeclSpecifier.sc_typedef -> CStorageClass.TYPEDEF;
      default -> throw parseContext.parseError("Unsupported storage class", d);
    };
  }

  CElaboratedType convert(final IASTElaboratedTypeSpecifier d) {
    ComplexTypeKind type =
        switch (d.getKind()) {
          case IASTElaboratedTypeSpecifier.k_enum -> ComplexTypeKind.ENUM;
          case IASTElaboratedTypeSpecifier.k_struct -> ComplexTypeKind.STRUCT;
          case IASTElaboratedTypeSpecifier.k_union -> ComplexTypeKind.UNION;
          default -> throw parseContext.parseError("Unknown elaborated type", d);
        };
    String name = ASTConverter.convert(d.getName());
    String origName = name;
    @Nullable CComplexType realType = scope.lookupType(type.toASTString() + " " + name);
    if (realType != null) {
      name = realType.getName();
      origName = realType.getOrigName();
    } else {
      name = scope.getFileSpecificTypeName(name);
    }

    return new CElaboratedType(d.isConst(), d.isVolatile(), type, name, origName, realType);
  }

  /** returns a pointerType, that wraps the type. */
  CPointerType convert(final IASTPointerOperator po, final CType type) {
    if (po instanceof IASTPointer p) {
      return new CPointerType(p.isConst(), p.isVolatile(), type);

    } else {
      throw parseContext.parseError("Unknown pointer operator", po);
    }
  }

  /** returns a pointerType, that wraps all the converted types. */
  CType convertPointerOperators(final IASTPointerOperator[] ps, CType type) {
    for (IASTPointerOperator p : ps) {
      type = convert(p, type);
    }
    return type;
  }

  /** returns a bitfield type */
  CType convertBitFieldType(final int bitFieldSize, final CType pType) {
    return new CBitFieldType(pType, bitFieldSize);
  }
}
