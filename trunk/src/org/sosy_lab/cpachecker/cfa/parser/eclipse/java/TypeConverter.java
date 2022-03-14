// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.checkArgument;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

abstract class TypeConverter {

  public final JType convert(Type t) {

    // TODO Not all Types implemented (Paramized, Wildcard)

    // The Reason for this method is, that not all Types
    // have to be gotten by resolving their binding.
    // It is unnecessary for Array Types and primitive Types e.g.

    if (t.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
      return convert((PrimitiveType) t);
    } else if (t.getNodeType() == ASTNode.ARRAY_TYPE) {
      return convert((ArrayType) t);
    } else if (t.getNodeType() == ASTNode.QUALIFIED_TYPE) {
      return convert((QualifiedType) t);
    } else if (t.getNodeType() == ASTNode.SIMPLE_TYPE) {
      return convert((SimpleType) t);
    } else if (t.getNodeType() == ASTNode.PARAMETERIZED_TYPE) {
      return convert(((ParameterizedType) t).getType());
    } else {
      return JSimpleType.getUnspecified();
    }
  }

  private JType convert(QualifiedType t) {
    ITypeBinding binding = t.resolveBinding();

    boolean canBeResolved = binding != null;

    if (canBeResolved) {
      return convert(binding);
    } else {
      return JSimpleType.getUnspecified();
    }
  }

  private JType convert(SimpleType t) {
    ITypeBinding binding = t.resolveBinding();
    boolean canBeResolved = binding != null;

    if (canBeResolved) {
      return convert(binding);
    } else {
      return JSimpleType.getUnspecified();
    }
  }

  public final JType convert(ITypeBinding t) {
    // TODO Needs to be completed (Wildcard, Parameterized type etc)

    if (t == null) {
      return JSimpleType.getUnspecified();
    } else if (t.isPrimitive()) {
      return convertPrimitiveType(t.getName());
    } else if (t.isArray()) {
      return new JArrayType(convert(t.getElementType()), t.getDimensions());
    } else if (t.isClass() || t.isEnum()) {
      return convertClassType(t);
    } else if (t.isInterface()) {
      return convertInterfaceType(t);
    } else {
      return JSimpleType.getUnspecified();
    }
  }

  public final JType convert(Expression pExpression) {
    ITypeBinding binding = pExpression.resolveTypeBinding();
    return convert(binding);
  }

  public abstract JClassType convertClassType(ITypeBinding t);

  public abstract JInterfaceType convertInterfaceType(ITypeBinding t);

  public final JClassOrInterfaceType convertClassOrInterfaceType(ITypeBinding pT) {

    checkArgument(pT.isClass() || pT.isEnum() || pT.isInterface());

    if (pT.isInterface()) {
      return convertInterfaceType(pT);
    } else {
      return convertClassType(pT);
    }
  }

  private JSimpleType convert(final PrimitiveType t) {

    PrimitiveType.Code primitiveTypeName = t.getPrimitiveTypeCode();
    return convertPrimitiveType(primitiveTypeName.toString());
  }

  private JSimpleType convertPrimitiveType(String primitiveTypeName) {

    JSimpleType type;
    switch (primitiveTypeName) {
      case "boolean":
        type = JSimpleType.getBoolean();
        break;
      case "char":
        type = JSimpleType.getChar();
        break;
      case "double":
        type = JSimpleType.getDouble();
        break;
      case "float":
        type = JSimpleType.getFloat();
        break;
      case "int":
        type = JSimpleType.getInt();
        break;
      case "void":
        type = JSimpleType.getVoid();
        break;
      case "long":
        type = JSimpleType.getLong();
        break;
      case "short":
        type = JSimpleType.getShort();
        break;
      case "byte":
        type = JSimpleType.getByte();
        break;
      default:
        throw new CFAGenerationRuntimeException("Unknown primitive type " + primitiveTypeName);
    }

    return type;
  }

  private JArrayType convert(final ArrayType t) {
    return new JArrayType(convert(t.getElementType()), t.getDimensions());
  }
}
