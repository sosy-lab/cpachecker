package org.sosy_lab.cpachecker.cfa.types.c;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

public class SerializeCTypeVisitor implements CTypeVisitor<String, RuntimeException> {

  // A set of hash strings to track visited types in the current path
  private final Set<String> visitedTypeHashes = new HashSet<>();

  @Override
  public String visit(CArrayType pArrayType) {
    String arrayTypeHash = Integer.toString(pArrayType.hashCode());

    // Add to visited types
    visitedTypeHashes.add(arrayTypeHash);

    String result =
        "ArrayType("
            + pArrayType.isConst()
            + ", "
            + pArrayType.isVolatile()
            + ", "
            + pArrayType.getType().accept(this)
            + ")";

    // Remove from visited types after processing
    visitedTypeHashes.remove(arrayTypeHash);

    return result;
  }

  @Override
  public String visit(CPointerType pPointerType) {
    CType pointedToType = pPointerType.getType();

    // Get the hash string for the pointed-to type
    String pointedToTypeHash = Integer.toString(pointedToType.hashCode());

    // Check if the pointed-to type has already been visited using its hash
    if (visitedTypeHashes.contains(pointedToTypeHash)) {
      return "PointerType(AlreadyVisitedType)";
    }

    // Mark the pointed-to type as visited by storing its hash
    visitedTypeHashes.add(pointedToTypeHash);

    String result =
        "PointerType("
            + pPointerType.isConst()
            + ", "
            + pPointerType.isVolatile()
            + ", "
            + pointedToType.accept(this)
            + ")";

    // Remove the type from visited set after processing
    visitedTypeHashes.remove(pointedToTypeHash);

    return result;
  }

  @Override
  public String visit(CFunctionType pFunctionType) {
    String functionTypeHash = Integer.toString(pFunctionType.hashCode());

    visitedTypeHashes.add(functionTypeHash);

    StringBuilder parameters = new StringBuilder();
    for (CType param : pFunctionType.getParameters()) {
      parameters.append(param.accept(this)).append(", ");
    }
    if (parameters.length() > 0) {
      parameters.setLength(parameters.length() - 2);
    }

    String result =
        "FunctionType("
            + pFunctionType.getReturnType().accept(this)
            + ", ["
            + parameters
            + "], "
            + pFunctionType.takesVarArgs()
            + ")";

    visitedTypeHashes.remove(functionTypeHash);

    return result;
  }

  @Override
  public String visit(CSimpleType pSimpleType) {
    String simpleTypeHash = Integer.toString(pSimpleType.hashCode());

    visitedTypeHashes.add(simpleTypeHash);

    String result =
        "SimpleType("
            + pSimpleType.isConst()
            + ", "
            + pSimpleType.isVolatile()
            + ", "
            + pSimpleType.getType()
            + ", "
            + pSimpleType.hasLongSpecifier()
            + ", "
            + pSimpleType.hasShortSpecifier()
            + ", "
            + pSimpleType.hasSignedSpecifier()
            + ", "
            + pSimpleType.hasUnsignedSpecifier()
            + ", "
            + pSimpleType.hasComplexSpecifier()
            + ", "
            + pSimpleType.hasImaginarySpecifier()
            + ", "
            + pSimpleType.hasLongLongSpecifier()
            + ")";

    visitedTypeHashes.remove(simpleTypeHash);

    return result;
  }

  @Override
  public String visit(CCompositeType pCompositeType) {
    String compositeTypeHash = Integer.toString(pCompositeType.hashCode());

    visitedTypeHashes.add(compositeTypeHash);

    StringBuilder result = new StringBuilder();
    result.append("CompositeType(")
        .append(pCompositeType.isConst())
        .append(", ")
        .append(pCompositeType.isVolatile())
        .append(", ")
        .append(pCompositeType.getKind())
        .append(", ")
        .append(pCompositeType.getName())
        .append(", ")
        .append(pCompositeType.getOrigName());

    if (pCompositeType.getMembers() != null && !pCompositeType.getMembers().isEmpty()) {
      result.append(", [");
      for (CCompositeType.CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        result.append(member.getType().accept(this))
            .append(":")
            .append(member.getName())
            .append(", ");
      }
      if (result.length() > 0) {
        result.setLength(result.length() - 2);
      }
      result.append("]");
    } else {
      result.append(", null");
    }

    result.append(")");

    visitedTypeHashes.remove(compositeTypeHash);

    return result.toString();
  }

  @Override
  public String visit(CProblemType pProblemType) {
    String problemTypeHash = Integer.toString(pProblemType.hashCode());

    visitedTypeHashes.add(problemTypeHash);

    String result = "ProblemType(" + pProblemType.toString() + ")";

    visitedTypeHashes.remove(problemTypeHash);

    return result;
  }

  @Override
  public String visit(CTypedefType pTypedefType) {
    String typedefTypeHash = Integer.toString(pTypedefType.hashCode());

    visitedTypeHashes.add(typedefTypeHash);

    String result =
        "TypedefType("
            + pTypedefType.isConst()
            + ", "
            + pTypedefType.isVolatile()
            + ", "
            + pTypedefType.getName()
            + ", "
            + pTypedefType.getCanonicalType().accept(this)
            + ")";

    visitedTypeHashes.remove(typedefTypeHash);

    return result;
  }

  @Override
  public String visit(CVoidType pVoidType) {
    String voidTypeHash = Integer.toString(pVoidType.hashCode());

    visitedTypeHashes.add(voidTypeHash);

    String result = "VoidType(" + pVoidType.isConst() + ", " + pVoidType.isVolatile() + ")";

    visitedTypeHashes.remove(voidTypeHash);

    return result;
  }

  @Override
  public String visit(CBitFieldType pBitFieldType) {
    String bitFieldTypeHash = Integer.toString(pBitFieldType.hashCode());

    visitedTypeHashes.add(bitFieldTypeHash);

    String result =
        "BitFieldType("
            + pBitFieldType.getBitFieldSize()
            + ", "
            + pBitFieldType.getType().accept(this)
            + ")";

    visitedTypeHashes.remove(bitFieldTypeHash);

    return result;
  }

  @Override
  public String visit(CElaboratedType pElaboratedType) {
    String elaboratedTypeHash = Integer.toString(pElaboratedType.hashCode());

    visitedTypeHashes.add(elaboratedTypeHash);

    String result =
        "ElaboratedType("
            + pElaboratedType.isConst()
            + ", "
            + pElaboratedType.isVolatile()
            + ", "
            + pElaboratedType.getKind()
            + ", "
            + pElaboratedType.getName()
            + ", "
            + pElaboratedType.getOrigName()
            + ")";

    visitedTypeHashes.remove(elaboratedTypeHash);

    return result;
  }

  @Override
  public String visit(CEnumType pEnumType) {
    String enumTypeHash = Integer.toString(pEnumType.hashCode());

    visitedTypeHashes.add(enumTypeHash);

    StringBuilder enumerators = new StringBuilder();
    for (CEnumerator enumerator : pEnumType.getEnumerators()) {
      enumerators.append(enumerator.toASTString()).append(", ");
    }
    String originName = pEnumType.getOrigName();
    if (originName == null || originName.isEmpty()) {
      originName = "null";
    }
    if (enumerators.length() > 0) {
      enumerators.setLength(enumerators.length() - 2);
    }

    String result =
        "EnumType("
            + pEnumType.isConst()
            + ","
            + pEnumType.isVolatile()
            + ","
            + pEnumType.getCompatibleType().accept(this)
            + ",["
            + enumerators
            + "]"
            + ","
            + pEnumType.getName()
            + ","
            + originName
            + ")";

    visitedTypeHashes.remove(enumTypeHash);

    return result;
  }
}
