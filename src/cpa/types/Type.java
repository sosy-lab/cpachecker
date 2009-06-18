package cpa.types;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface Type {
  
 public static enum TypeClass {
    PRIMITIVE,
    POINTER,
    ARRAY,
    ENUM,
    STRUCT,
    UNION,
    FUNCTION,
  }

  public static enum PrimitiveType implements Type {
    
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
    
    private PrimitiveType(int sizeOf, String name) {
      this.sizeOf = sizeOf;
      this.name = name;
    }
    
    @Override
    public int sizeOf() {
      return sizeOf;
    }
    
    @Override
    public TypeClass getTypeClass() {
      return TypeClass.PRIMITIVE;
    }
    
    @Override
    public String toString() {
      return name;
    }
  }

  public static final class PointerType implements Type {
    
    private final Type type; // target type
    private final int levelOfIndirection;
    
    public PointerType(Type type) {
      if (type == null) {
        throw new IllegalArgumentException();
      }
      this.type = type;
      if (type instanceof PointerType) {
        this.levelOfIndirection = ((PointerType)type).levelOfIndirection + 1;
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
    
    public Type getType() {
      return type;
    }
    
    public int getLevelOfIndirection() {
      return levelOfIndirection;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof PointerType)) {
        return false;
      }
      return type.equals(((PointerType)obj).type);
    }
    
    @Override
    public int hashCode() {
      return 254 * type.hashCode();
    }
    
    @Override
    public String toString() {
      return type.toString() + "*";
    }
  }
  
public static final class ArrayType implements Type {
    
    private final Type type; // target type
    private final int length;
    
    public ArrayType(Type type, int length) {
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
      return type.equals(((ArrayType)obj).type);
    }
    
    @Override
    public int hashCode() {
      return 255 * type.hashCode();
    }
    
    @Override
    public String toString() {
      return type.toString() + "[" + (length > 0 ? length : "") + "]";
    }
  }

  public static abstract class CompositeType implements Type {
    
    protected final LinkedHashMap<String, Type> members;
    protected final String name;
    
    public CompositeType(String name) {
      this.members = new LinkedHashMap<String, Type>();
      this.name = name;
    }
    
    public abstract int offsetOf(String name);
    
    protected void addMember(String name, Type type) {
      if (name == null || type == null) {
        throw new IllegalArgumentException();
      }
      if (members.containsKey(name)) {
        throw new IllegalArgumentException("Member " + name + " exists already");
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
        || (   name.equals(other.name)
            && members.equals(other.members));
    }
    
    @Override
    public int hashCode() {
      return name.hashCode();
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(" ");
      sb.append(name);
      sb.append(" { ");
      for (String member : members.keySet()) {
        sb.append(members.get(member));
        sb.append(" ");
        sb.append(member);
        sb.append("; ");
      }
      sb.append("}");
      return sb.toString();
    }
  }

  public static final class StructType extends CompositeType {
  
    public StructType(String name) {
      super(name);
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
    
    @Override
    public String toString() {
      return "struct" + super.toString();
    }
  }

  public static final class UnionType extends CompositeType {
  
    public UnionType(String name) {
      super(name);
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
    
    @Override
    public String toString() {
      return "union" + super.toString();
    }
  }

  public static final class EnumType implements Type {
  
    private final Map<String, Integer> enumerators;
    private final String name;
    
    public EnumType(String name) {
      this.enumerators = new HashMap<String, Integer>();
      this.name = name;
    }
    
    public void addEnumerator(String name, int value) {
      if (name == null) {
        throw new IllegalArgumentException();
      }
      if (enumerators.containsKey(name)) {
        throw new IllegalArgumentException("Enumerator " + name + " exists already");
      }
      enumerators.put(name, value);
    }
    
    public int getEnumerator(String name) {
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
        || (   name.equals(other.name)
            && enumerators.equals(other.enumerators));
    }
    
    @Override
    public int hashCode() {
      return name.hashCode();
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("enum ");
      sb.append(name);
      sb.append(" { ");
      int lastValue = -1;
      for (String enumerator : enumerators.keySet()) {
        sb.append(enumerator);
        int currentValue = enumerators.get(enumerator);
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
  
  public final static class FunctionType implements Type {
    
    private final String name;
    private final Type returnType;
    private final LinkedHashMap<String, Type> parameters;
    
    public FunctionType(String name, Type returnType) {
      this.name = name;
      this.returnType = returnType;
      this.parameters = new LinkedHashMap<String, Type>();
    }
    
    public Type getReturnType() {
      return returnType;
    }
    
    protected void addParameter(String name, Type type) {
      if (name == null || type == null) {
        throw new IllegalArgumentException();
      }
      if (parameters.containsKey(name)) {
        throw new IllegalArgumentException("Parameter " + name + " already exists");
      }
      parameters.put(name, type);
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
            && parameters.equals(other.returnType));
    }
    
    @Override
    public int hashCode() {
      return name.hashCode();
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
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

public int sizeOf(); 

 public TypeClass getTypeClass();
  
}