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
package org.sosy_lab.cpachecker.cpa.types;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface Type {

  /**
   * Get an instance of the enum TypeClass that describes this type.
   */
  public TypeClass getTypeClass();

  /**
   * If the type was declared with modifier const;
   */
  public boolean isConst();

  /**
   * Get the size of an element of the type, just like the sizeof-Operator in C.
   */
  public int sizeOf();

  /**
   * Get a String representation of the full definition of the type.
   */
  public String getDefinition();


  /**
   * List of all possible classes of types in C.
   */
  public static enum TypeClass {
    /**
     * All primitive types like char, int, float, double and void.
     */
    PRIMITIVE,

    POINTER,

    ARRAY,

    ENUM,

    STRUCT,

    UNION,

    /**
     * A function like "int main()", NOT a pointer to a function!
     */
    FUNCTION,
  }

  public static enum Primitive {

    VOID(1, "void"),

    CHAR(1, "char"),
    SHORT(2, "short int"),
    LONG(4, "long int"),
    LONGLONG(8, "long long int"),

    FLOAT(4, "float"),
    DOUBLE(8, "double"),
    LONGDOUBLE(12, "long double"),
    ;

    private final int sizeOf;
    private final String name;

    private Primitive(int sizeOf, String name) {
      this.sizeOf = sizeOf;
      this.name = name;
    }

    public int sizeOf() {
      return sizeOf;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final class PrimitiveType extends AbstractType {

    private final Primitive primitiveType;
    private final boolean signed;

    /**
     * @param primitiveType
     * @param signed  ignored if not an integral type
     * @param constant
     */
    public PrimitiveType(Primitive primitiveType, boolean signed, boolean constant) {
      super(constant);
      this.primitiveType = primitiveType;

      switch (primitiveType) {
      case VOID:
        this.signed = false;
        break;

      case CHAR:
      case SHORT:
      case LONG:
      case LONGLONG:
        this.signed = signed;
        break;

      case FLOAT:
      case DOUBLE:
      case LONGDOUBLE:
        this.signed = true;
        break;

      default:
        throw new RuntimeException("Missing case clause");
      }
    }

    @Override
    public int sizeOf() {
      return primitiveType.sizeOf();
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.PRIMITIVE;
    }

    public boolean isSigned() {
      return signed;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof PrimitiveType)) {
        return false;
      }
      PrimitiveType other = (PrimitiveType)obj;
      return (this == other)
          || (   (primitiveType == other.primitiveType)
              && (signed == other.signed)
              && (super.equals(other)));
    }

    @Override
    public int hashCode() {
      return primitiveType.hashCode();
    }

    @Override
    public String toString() {
      String signedString = "";
      if (   primitiveType == Primitive.CHAR
          || primitiveType == Primitive.SHORT
          || primitiveType == Primitive.LONG
          || primitiveType == Primitive.LONGLONG) {

        signedString = (signed ? "signed " : "unsigned ");
      }

      return (isConst() ? "const "  : "")
           + signedString
           + primitiveType.toString();
    }
  }

  public static final class PointerType extends AbstractType {

    private final Type targetType; // target type
    private final int levelOfIndirection;

    public PointerType(Type targetType, boolean constant) {
      super(constant);
      if (targetType == null) {
        throw new IllegalArgumentException();
      }
      this.targetType = targetType;
      if (targetType instanceof PointerType) {
        this.levelOfIndirection = ((PointerType)targetType).levelOfIndirection + 1;
      } else {
        this.levelOfIndirection = 1;
      }
    }

    @Override
    public int sizeOf() {
      return 4; // architecture dependent
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.POINTER;
    }

    public Type getTargetType() {
      return targetType;
    }

    public int getLevelOfIndirection() {
      return levelOfIndirection;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof PointerType)) {
        return false;
      }
      PointerType other = (PointerType)obj;
      return (this == obj)
          || (   super.equals(other))
              && targetType.equals(other.targetType);
    }

    @Override
    public int hashCode() {
      return 254 * targetType.hashCode();
    }

    @Override
    public String toString() {
      return targetType.toString()
           + (isConst() ? " const " : "")
           + "*";
    }
  }

public static final class ArrayType extends AbstractType {

    private final Type type; // target type
    private final int length;

    public ArrayType(Type type, int length) {
      super(false);
      if (type == null) {
        throw new IllegalArgumentException();
      }
      if (length < 0) {
        throw new IllegalArgumentException();
      }
      this.type = type;
      this.length = length;
    }

    @Override
    public int sizeOf() {
      return type.sizeOf() * length;
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.ARRAY;
    }

    public Type getType() {
      return type;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof ArrayType)) {
        return false;
      }
      ArrayType other = (ArrayType)obj;
      return (this == other)
          || (   super.equals(other)
              && type.equals(other.type));
    }


    @Override
    public int hashCode() {
      return 255 * type.hashCode();
    }

    @Override
    public String toString() {
      return type.toString()
           + (isConst() ? " const " : "")
           + "[" + (length > 0 ? length : "") + "]";
    }
  }

  public static abstract class CompositeType extends AbstractType {

    protected final LinkedHashMap<String, Type> members;
    protected final String name;

    public CompositeType(String name, boolean constant) {
      super(constant);
      this.members = new LinkedHashMap<String, Type>();
      this.name = name;
    }

    public abstract int offsetOf(String name);

    protected void addMember(String name, Type type) {
      if (name == null || type == null) {
        throw new IllegalArgumentException();
      }
      if (members.containsKey(name)) {
        throw new IllegalArgumentException("Member " + name + " exists already in type " + name);
      }
      members.put(name, type);
    }

    public Type getMemberType(String name) {
      if (!members.containsKey(name)) {
        throw new IllegalArgumentException("No such member");
      }
      return members.get(name);
    }

    public Set<String> getMembers() {
      return members.keySet();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof CompositeType)) {
        return false;
      }
      CompositeType other = (CompositeType)obj;

      return (obj == this)
        || (   super.equals(other)
            && name.equals(other.name)
            && members.equals(other.members));
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      if (isConst()) {
        return "const " + name;
      } else {
        return name;
      }
    }

    @Override
    public String getDefinition() {
      StringBuilder sb = new StringBuilder();
      sb.append(toString());
      sb.append(" { ");
      for (String member : members.keySet()) {
        sb.append(members.get(member));
        sb.append(" ");
        sb.append(member);
        sb.append("; ");
      }
      sb.append("}");
      return sb.toString();    }
  }

  public static final class StructType extends CompositeType {

    public StructType(String name, boolean constant) {
      super(name, constant);
    }

    @Override
    public int offsetOf(String name) {
      if (!members.containsKey(name)) {
        throw new IllegalArgumentException("No such member!");
      }
      int result = 0;
      for (String member : members.keySet()) {
        if (member.equals(name)) {
          break;
        }
        result += Math.ceil(members.get(name).sizeOf()/4.0)*4; // assume padding to 4 bytes
      }
      return result;
    }

    @Override
    public int sizeOf() {
      int result = 0;
      for (Type member : members.values()) {
        result += Math.ceil(member.sizeOf()/4.0)*4; // assume padding to 4 bytes
      }
      return result;
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.STRUCT;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof StructType) && super.equals(obj);
    }
  }

  public static final class UnionType extends CompositeType {

    public UnionType(String name, boolean constant) {
      super(name, constant);
    }

    @Override
    public int offsetOf(String name) {
      return 0;
    }

    @Override
    public int sizeOf() {
      int result = 0;
      for (Type member : members.values()) {
        result += Math.max(member.sizeOf(), result);
      }
      return result;
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.UNION;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof UnionType) && super.equals(obj);
    }
  }

  public static final class EnumType extends AbstractType {

    private final Map<String, Long> enumerators;
    private final String name;

    public EnumType(String name, boolean constant) {
      super(constant);
      this.enumerators = new HashMap<String, Long>();
      this.name = name;
    }

    public void addEnumerator(String name, long pL) {
      if (name == null) {
        throw new IllegalArgumentException();
      }
      if (enumerators.containsKey(name)) {
        throw new IllegalArgumentException("Enumerator " + name + " exists already");
      }
      enumerators.put(name, pL);
    }

    public long getEnumerator(String name) {
      if (!enumerators.containsKey(name)) {
        throw new IllegalArgumentException("No such enumerator");
      }
      return enumerators.get(name);
    }

    public Set<String> getEnumerators() {
      return enumerators.keySet();
    }

    @Override
    public int sizeOf() {
      return 4;
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.ENUM;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof EnumType)) {
        return false;
      }
      EnumType other = (EnumType)obj;

      return (obj == this)
        || (   super.equals(other)
            && name.equals(other.name)
            && enumerators.equals(other.enumerators));
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      if (isConst()) {
        return "const " + name;
      } else {
        return name;
      }
    }

    @Override
    public String getDefinition() {
      StringBuilder sb = new StringBuilder();
      sb.append(toString());
      sb.append(" { ");
      long lastValue = -1;
      for (String enumerator : enumerators.keySet()) {
        sb.append(enumerator);
        long currentValue = enumerators.get(enumerator);
        if (currentValue != lastValue+1) {
          sb.append("=");
          sb.append(currentValue);
        }
        lastValue = currentValue;
        sb.append(", ");
      }
      sb.deleteCharAt(sb.length()-2);
      sb.append("}");
      return sb.toString();
    }
  }

  public final static class FunctionType extends AbstractType {

    private static int uniqueNameId = 0;

    private final String name;
    private final Type returnType;
    private final LinkedHashMap<String, Type> parameters;
    private boolean hasVarArgs;

    public FunctionType(String name, Type returnType, boolean hasVarArgs) {
      super(false);
      this.name = name;
      this.returnType = returnType;
      this.parameters = new LinkedHashMap<String, Type>();
      this.hasVarArgs = hasVarArgs;
    }

    public Type getReturnType() {
      return returnType;
    }

    protected void addParameter(String name, Type type) {
      if (type == null) {
        throw new IllegalArgumentException();
      }
      if (name ==  null || name.equals("")) {
        synchronized (this.getClass()) {
          name = "__cpa_anon_param_" + uniqueNameId++;
        }
      }
      if (parameters.containsKey(name)) {
        throw new IllegalArgumentException("Parameter " + name + " already exists");
      }
      parameters.put(name, type);
    }

    public String getName() {
      return name;
    }

    public Type getParameterType(String name) {
      if (!parameters.containsKey(name)) {
        throw new IllegalArgumentException("No such parameter");
      }
      return parameters.get(name);
    }

    public Set<String> getParameters() {
      return parameters.keySet();
    }

    public boolean hasVarArgs() {
      return hasVarArgs;
    }

    @Override
    public TypeClass getTypeClass() {
      return TypeClass.FUNCTION;
    }

    @Override
    public int sizeOf() {
      return 1; // yes, this is really the result of sizeof(func)
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof FunctionType)) {
        return false;
      }
      FunctionType other = (FunctionType)obj;

      return (obj == this)
        || (   name.equals(other.name)
            && returnType.equals(other.returnType)
            && parameters.equals(other.parameters)
            && hasVarArgs == other.hasVarArgs);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return name + "()";
    }

    @Override
    public String getDefinition() {
      StringBuilder sb = new StringBuilder();
      sb.append(returnType);
      sb.append(" ");
      sb.append(name);
      sb.append("(");
      if (parameters.size() > 0) {
        for (String parameter : parameters.keySet()) {
          sb.append(parameters.get(parameter));
          sb.append(" ");
          sb.append(parameter);
          sb.append(", ");
        }
        sb.deleteCharAt(sb.length()-2);
      }
      sb.append(")");
      return sb.toString();
    }
  }

  abstract static class AbstractType implements Type {

    private final boolean constant;

    public AbstractType(boolean constant) {
      this.constant = constant;
    }

    @Override
    public boolean isConst() {
      return constant;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof Type)) {
        return false;
      } else {
        return equals((Type)obj);
      }
    }

    public boolean equals(Type other) {
      return (other != null) && (isConst() == other.isConst());
    }

    @Override
    public abstract int hashCode();

    @Override
    public String getDefinition() {
      return toString();
    }
  }
}