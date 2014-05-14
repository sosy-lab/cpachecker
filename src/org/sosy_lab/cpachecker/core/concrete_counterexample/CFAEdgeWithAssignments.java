/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.concrete_counterexample;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.Model.Function;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import apache.harmony.math.BigInteger;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;


public class CFAEdgeWithAssignments {

  private final CFAEdge edge;
  private final Set<Assignment> assignments;
  private final Map<String, Object> addressMap;
  @SuppressWarnings("unused")
  private final Map<Function, Object> functionMap;
  @SuppressWarnings("unused")
  private final SSAMap map;
  private final Map<String, Assignment> variableEnvironment;
  private final Multimap<String, Assignment> functionEnvoirment;

  // TODO Get correct machine Model.
  private final MachineModel machineModel = MachineModel.LINUX32;

  public CFAEdgeWithAssignments(CFAEdge pEdge, Set<Assignment> pAssignments,
      Map<String, Object> pAddressMap, Map<Function, Object> pFunctionMap,
      SSAMap pMap, Map<String, Assignment> pVariableEnvoirment,
      Multimap<String, Assignment> pFunctionEnvoirment) {
    edge = pEdge;
    assignments = pAssignments;
    addressMap = pAddressMap;
    functionMap = pFunctionMap;
    map = pMap;
    variableEnvironment = pVariableEnvoirment;
    functionEnvoirment = pFunctionEnvoirment;
  }

  public Set<Assignment> getAssignments() {
    return assignments;
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  @Override
  public String toString() {
    return edge.toString() + " " + assignments.toString();
  }

  @Nullable
  private String getAsCode(CFAEdge pEdge) {

    if (assignments.size() < 0) {
      return null;
    }

    if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return handleDeclaration(((ADeclarationEdge) pEdge).getDeclaration());
    } else if (pEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return handleStatement(((AStatementEdge) pEdge).getStatement());
    } else if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      return handleFunctionCall( ((FunctionCallEdge)pEdge));
    } else if(pEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
      return handleMultiEdge((MultiEdge)pEdge);
    }

    return null;
  }

  @Nullable
  public String getAsCode() {
    return getAsCode(edge);
  }

  private String handleMultiEdge(MultiEdge pEdge) {

    Set<String> result = new HashSet<>(pEdge.getEdges().size());

    for (CFAEdge edge : pEdge) {
      String code = getAsCode(edge);

      if (code != null && !result.contains(code)) {
        result.add(code);
      }
    }

    if(result.size() < 1) {
      return null;
    } else {
      return Joiner.on(" ").join(result);
    }
  }

  private  String handleFunctionCall(FunctionCallEdge pFunctionCallEdge) {

    FunctionEntryNode functionEntryNode = pFunctionCallEdge.getSuccessor();

    String functionName = functionEntryNode.getFunctionName();

    List<? extends AParameterDeclaration> formalParameters =
        functionEntryNode.getFunctionParameters();

    List<String> formalParameterNames =
        functionEntryNode.getFunctionParameterNames();


    if (formalParameters == null) {
      return null;
    }

    //TODO Refactor, no splitting of strings!

    String[] parameterValuesAsCode = new String[formalParameters.size()];

    for (Assignment valuePair : assignments) {

      String termName = valuePair.getTerm().getName();
      String[] termFunctionAndVariableName = termName.split("::");

      if (!(termFunctionAndVariableName.length == 2)) {
        return null;
      }

      String termVariableName = termFunctionAndVariableName[1];
      String termFunctionName = termFunctionAndVariableName[0];

      if (!termFunctionName.equals(functionName)) {
        return null;
      }

      if (formalParameterNames.contains(termVariableName)) {

        int formalParameterPosition =
            formalParameterNames.indexOf(termVariableName);

        AParameterDeclaration formalParameterDeclaration =
            formalParameters.get(formalParameterPosition);

        ValueCodes valueAsCode = getValueAsCode(valuePair.getValue(), formalParameterDeclaration.getType());

        if (valueAsCode.hasUnknownValueCode() ||
            !formalParameterDeclaration.getName().equals(termVariableName)) {
          return null;
        }

        parameterValuesAsCode[formalParameterPosition] = valueAsCode.getExpressionValueCodeAsString();
      } else {
        return null;
      }
    }

    if (parameterValuesAsCode.length < 1) {
      return null;
    }

    for(String value : parameterValuesAsCode) {
      if(value == null) {
        return null;
      }
    }

    Joiner joiner = Joiner.on(", ");
    String arguments = "(" + joiner.join(parameterValuesAsCode) + ")";

    return functionName + arguments + ";";
  }

  @Nullable
  private String handleAssignment(IAssignment assignment) {

    IALeftHandSide leftHandSide = assignment.getLeftHandSide();

    String functionName = edge.getPredecessor().getFunctionName();

    Object value = getValueObject(leftHandSide, functionName);

    if (value == null) {
      return null;
    }

    Type expectedType = leftHandSide.getExpressionType();
    ValueCodes valueAsCode = getValueAsCode(value, expectedType);

    return handleSimpleValueCodesAssignments(valueAsCode, leftHandSide.toASTString());
  }

  private Object getValueObject(IALeftHandSide pLeftHandSide, String pFunctionName) {

    //If their is only one value, its the one we search
    if (assignments.size() == 1) {
      return assignments.iterator().next().getValue();
    }

    if(pLeftHandSide instanceof CLeftHandSide) {
      CLeftHandSide cLeftHandSide = (CLeftHandSide) pLeftHandSide;
      LModelValueVisitor v = new LModelValueVisitor(pFunctionName);
      return cLeftHandSide.accept(v);
    }

    return null;
  }

  @Nullable
  private ValueCodes getValueAsCode(Object pValue, Type pExpectedType) {

    // TODO processing for other languages
    if (pExpectedType instanceof CType) {
      return ((CType) pExpectedType).accept(new ValueCodesVisitor(pValue));
    }

    return new ValueCodes();
  }

  @Nullable
  private String handleStatement(IAStatement pStatement) {

    if (pStatement instanceof AFunctionCallAssignmentStatement) {
      IAssignment assignmentStatement =
          ((AFunctionCallAssignmentStatement) pStatement);
      return handleAssignment(assignmentStatement);
    }

    if (pStatement instanceof AExpressionAssignmentStatement) {
      IAssignment assignmentStatement =
          ((AExpressionAssignmentStatement) pStatement);
      return handleAssignment(assignmentStatement);
    }

    return null;
  }

  private String handleDeclaration(IADeclaration dcl) {

    if (dcl instanceof CVariableDeclaration) {

      CVariableDeclaration varDcl = (CVariableDeclaration) dcl;

      String functionName = edge.getPredecessor().getFunctionName();

      Object value = getValueObject(varDcl, functionName);

      if (value == null) {
        return null;
      }

      Type dclType = varDcl.getType();
      ValueCodes valueAsCode = getValueAsCode(value, dclType);

      return handleSimpleValueCodesAssignments(valueAsCode, varDcl.getName());
    }

    return null;
  }

  private String handleSimpleValueCodesAssignments(ValueCodes pValueAsCodes, String pLValue) {

    Set<SubExpressionValueCode> subValues = pValueAsCodes.getSubExpressionValueCode();

    List<String> statements = new ArrayList<>(subValues.size() + 1);

    if (!pValueAsCodes.hasUnknownValueCode()) {

      String statement = getAssumptionStatements(pLValue, "", "",
          pValueAsCodes.getExpressionValueCodeAsString());

      statements.add(statement);
    }

    for (SubExpressionValueCode subCode : subValues) {
      String statement = getAssumptionStatements(pLValue, subCode.getPrefix(), subCode.getPostfix(),
          subCode.getValueCode());

      statements.add(statement);
    }

    if (statements.size() == 0) {
      return null;
    }

    Joiner joiner = Joiner.on("\n");

    return joiner.join(statements);
  }

  private String getAssumptionStatements(String pLValue,
      String pPrefix, String pPostfix, String value) {

    StringBuilder result = new StringBuilder();
    result.append(pPrefix);
    result.append(pLValue);
    result.append(pPostfix);
    result.append(" = ");
    result.append(value);
    result.append(";");

    return result.toString();
  }

  private Object getValueObject(CVariableDeclaration pVarDcl, String pFunctionName) {
    return new LModelValueVisitor(pFunctionName).handleVariableDeclaration(pVarDcl);
  }

  private class LModelValueVisitor implements CLeftHandSideVisitor<Object, RuntimeException> {

    private final String functionName;
    private final AddressValueVisitor addressVisitor;

    public LModelValueVisitor(String pFunctionName) {
      functionName = pFunctionName;
      addressVisitor = new AddressValueVisitor(this);
    }

    private final BigDecimal evaluateNumericalValue(CExpression exp) {

      Value addressV;
      try {
        addressV = exp.accept(new ModelExpressionValueVisitor(functionName, machineModel, null));
      } catch (UnrecognizedCCodeException e1) {
        throw new IllegalArgumentException(e1);
      }

      if (addressV.isUnknown() && !addressV.isNumericValue()) {
        return null;
      }

      return BigDecimal.valueOf(addressV.asNumericValue().longValue());
    }

    /*This method evaluates the address of the lValue, not the address the expression evaluates to*/
    private final BigDecimal evaluateNumericalAddress(CLeftHandSide exp) {

      Object addressV = evaluateAddress(exp);

      if (addressV == null) {
        return null;
      }

      try {
        return new BigDecimal(addressV.toString());
      } catch (NumberFormatException e) {
        //TODO Ugly Refactor
        return null;
      }
    }

    /*This method evaluates the address of the lValue, not the address the expression evaluates to*/
    private Object evaluateAddress(CLeftHandSide pExp) {
      return pExp.accept(addressVisitor);
    }

    @Override
    public Object visit(CArraySubscriptExpression pIastArraySubscriptExpression) {

      Object valueAddress = evaluateAddress(pIastArraySubscriptExpression);

      String ufName = TypeUFNameVisitor.getUFName(pIastArraySubscriptExpression.getExpressionType());

      Object value = TypeUFNameVisitor.getValueFromUF(ufName, valueAddress, functionEnvoirment);

      return value;
    }

    private int getSizeof(CType pExpressionType) {

      return machineModel.getSizeof(pExpressionType);
    }

    @Override
    public Object visit(CFieldReference pIastFieldReference) {

      if (!pIastFieldReference.isPointerDereference()) {

        /* Fieldreferences are sometimes represented as variables,
           e.g a.b.c in main is main::a$b$c */
        String fieldReferenceVariableName = getFieldReferenceVariableName(pIastFieldReference);

        if (fieldReferenceVariableName != null && variableEnvironment.containsKey(fieldReferenceVariableName)) {
          return variableEnvironment.get(fieldReferenceVariableName).getValue();
        }
      }

      BigDecimal address = evaluateNumericalAddress(pIastFieldReference);

      String ufName = TypeUFNameVisitor.getUFName(pIastFieldReference.getExpressionType());

      return TypeUFNameVisitor.getValueFromUF(ufName, address, functionEnvoirment);
    }

    private BigDecimal getFieldOffset(CFieldReference fieldReference) {
      CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();
      return getFieldOffset(fieldOwnerType, fieldReference.getFieldName());
    }

    private BigDecimal getFieldOffset(CType ownerType, String fieldName) {

      if (ownerType instanceof CElaboratedType) {

        CType realType = ((CElaboratedType) ownerType).getRealType();

        if (realType == null) { return null; }

        return getFieldOffset(realType.getCanonicalType(), fieldName);
      } else if (ownerType instanceof CCompositeType) {
        return getFieldOffset((CCompositeType) ownerType, fieldName);
      } else if (ownerType instanceof CPointerType) {

        /* We do not explicitly transform x->b,
        so when we try to get the field b the ownerType of x
        is a pointer type.*/

        CType type = ((CPointerType) ownerType).getType().getCanonicalType();

        return getFieldOffset(type, fieldName);
      }

      throw new AssertionError();
    }

    private BigDecimal getFieldOffset(CCompositeType ownerType, String fieldName) {

      List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

      int offset = 0;

      for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
        String memberName = typeMember.getName();
        if (memberName.equals(fieldName)) {
          return BigDecimal.valueOf(offset);
        }

        if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {
          offset = offset + getSizeof(typeMember.getType().getCanonicalType());
        }
      }
      return null;
    }

    private String getFieldReferenceVariableName(CFieldReference pIastFieldReference) {

      List<String> fieldNameList = new ArrayList<>();

      CFieldReference reference = pIastFieldReference;

      fieldNameList.add(0 ,reference.getFieldName());

      while(reference.getFieldOwner() instanceof CFieldReference) {
        reference = (CFieldReference) reference.getFieldOwner();
        fieldNameList.add(0 ,reference.getFieldName());
      }

      if (reference.getFieldOwner() instanceof CIdExpression) {
        fieldNameList.add(0, ((CIdExpression) reference.getFieldOwner()).getName());

        Joiner joiner = Joiner.on("$");
        return functionName + "::" + joiner.join(fieldNameList);
      } else {
        return null;
      }
    }

    @Override
    public Object visit(CIdExpression pCIdExpression) {

      CType type = pCIdExpression.getExpressionType();

      if (type instanceof CSimpleType || type instanceof CPointerType) {
        // CIdExpression of simple types or pointer types
        // can be handled the same way as declarations.
        return handleSimpleVariableDeclaration(pCIdExpression.getDeclaration());
      }

      if(type instanceof CArrayType) {
        return evaluateAddress(pCIdExpression);
      }

      return null;
    }

    @Nullable
    private Object handleVariableDeclaration(CSimpleDeclaration pVarDcl) {

      //If their is only one value, its the one we search
      if (assignments.size() == 1) {
        return assignments.iterator().next().getValue();
      }

      if (pVarDcl == null || functionName == null || (!(pVarDcl instanceof CVariableDeclaration)
          && !(pVarDcl instanceof CParameterDeclaration))) {
        return null;
      }

      CType type = pVarDcl.getType();

      if(type instanceof CSimpleType) {
        handleSimpleVariableDeclaration(pVarDcl);
      }

      return null;
    }

    private Object handleSimpleVariableDeclaration(CSimpleDeclaration pVarDcl) {

      String varName = getVarName(pVarDcl);

      if (variableEnvironment.containsKey(varName)) {
        return variableEnvironment.get(varName).getValue();
      } else {
        /* The variable might not exist anymore in the variable environment,
           search in the address space of the function environment*/

        Object address = addressVisitor.getAddress(pVarDcl);

        if(address == null) {
          return null;
        }

        CType type = pVarDcl.getType();
        String ufName = TypeUFNameVisitor.getUFName(type);

        return TypeUFNameVisitor.getValueFromUF(ufName, address, functionEnvoirment);
      }
    }

    private String getVarName(CSimpleDeclaration pVarDcl) {

      String varName = pVarDcl.getName();

      if (pVarDcl instanceof CParameterDeclaration ||
          (!((CVariableDeclaration) pVarDcl).isGlobal())) {
        return functionName + "::" + varName;
      } else {
        return varName;
      }
    }

    @Override
    public Object visit(CPointerExpression pPointerExpression) {

      CExpression exp = pPointerExpression.getOperand();

      /*Quick jump to the necessary method.
       * the address of a dereference is the evaluation of its operand*/
      BigDecimal address = evaluateNumericalValue(exp);

      CType type = exp.getExpressionType();

      if (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
      } else if (type instanceof CArrayType) {
        type = ((CArrayType) type).getType();
      } else {
        return null;
      }

      String ufName = TypeUFNameVisitor.getUFName(type);

      Object value = TypeUFNameVisitor.getValueFromUF(ufName, address, functionEnvoirment);

      return value;
    }

    @SuppressWarnings("unused")
    boolean isStructOrUnionType(CType rValueType) {

      rValueType = rValueType.getCanonicalType();

      if (rValueType instanceof CElaboratedType) {
        CElaboratedType type = (CElaboratedType) rValueType;
        return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
      }

      if (rValueType instanceof CCompositeType) {
        CCompositeType type = (CCompositeType) rValueType;
        return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
      }

      return false;
    }

    private class AddressValueVisitor implements CLeftHandSideVisitor<Object, RuntimeException> {

      private final LModelValueVisitor valueVisitor;

      public AddressValueVisitor(LModelValueVisitor pValueVisitor) {
        valueVisitor = pValueVisitor;
      }

      public Object getAddress(CSimpleDeclaration varDecl) {

        String varName = getVarName(varDecl);
        String addressName = CFAPathWithAssignments.getAddressPrefix() + varName;

        if (addressMap.containsKey(addressName)) {
          return addressMap.get(addressName);
        }

        return null;
      }

      @Override
      public Object visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
        CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

        BigDecimal address = evaluateNumericalValue(arrayExpression);

        if(address == null) {
          return null;
        }

        CExpression subscriptCExpression = pIastArraySubscriptExpression.getSubscriptExpression();

        BigDecimal subscriptValue = evaluateNumericalValue(subscriptCExpression);

        if(subscriptValue == null) {
          return null;
        }

        BigDecimal typeSize = BigDecimal.valueOf(getSizeof(pIastArraySubscriptExpression.getExpressionType()));

        BigDecimal subscriptOffset = subscriptValue.multiply(typeSize);

        return address.add(subscriptOffset);
      }

      @Override
      public Object visit(CFieldReference pIastFieldReference) {

        CExpression fieldOwner = pIastFieldReference.getFieldOwner();

        if (pIastFieldReference.isPointerDereference()) {

          BigDecimal fieldOwneraddress = evaluateNumericalValue(fieldOwner);

          if (fieldOwneraddress == null) {
            return null;
          }

          BigDecimal fieldOffset = getFieldOffset(pIastFieldReference);

          if(fieldOffset == null) {
            return null;
          }

          return fieldOwneraddress.add(fieldOffset);
        }

        /* Fieldreferences are sometimes represented as variables,
        e.g a.b.c in main is main::a$b$c */
        String fieldReferenceVariableName = getFieldReferenceVariableName(pIastFieldReference);

        if (fieldReferenceVariableName != null) {

          String key = CFAPathWithAssignments.getAddressPrefix() + fieldReferenceVariableName;

          if (addressMap.containsKey(key)) {
            return addressMap.get(key);
          }
        }

        if (!(fieldOwner instanceof CLeftHandSide)) {
          //TODO Investigate
          return null;
        }

        BigDecimal fieldOwnerAddress = evaluateNumericalAddress((CLeftHandSide) fieldOwner);

        if (fieldOwnerAddress == null) {
          return null;
        }

        BigDecimal fieldOffset = getFieldOffset(pIastFieldReference);

        if(fieldOffset == null) {
          return null;
        }

        return fieldOwnerAddress.add(fieldOffset);
      }

      @Override
      public Object visit(CIdExpression pIastIdExpression) {
        return getAddress(pIastIdExpression.getDeclaration());
      }

      @Override
      public Object visit(CPointerExpression pPointerExpression) {
        /*The address of a pointer dereference is the evaluation of its operand*/
        return valueVisitor.evaluateNumericalValue(pPointerExpression.getOperand());
      }

      @Override
      public Object visit(CComplexCastExpression pComplexCastExpression) {
        // TODO Implement complex Cast Expression when predicate models it.
        return null;
      }
    }

    private class ModelExpressionValueVisitor extends AbstractExpressionValueVisitor {

      public ModelExpressionValueVisitor(String pFunctionName, MachineModel pMachineModel,
          LogManagerWithoutDuplicates pLogger) {
        super(pFunctionName, pMachineModel, pLogger);
      }

      @Override
      public Value visit(CBinaryExpression binaryExp) throws UnrecognizedCCodeException {

        CExpression lVarInBinaryExp = binaryExp.getOperand1();
        CExpression rVarInBinaryExp = binaryExp.getOperand2();
        CType lVarInBinaryExpType = lVarInBinaryExp.getExpressionType().getCanonicalType();
        CType rVarInBinaryExpType = rVarInBinaryExp.getExpressionType().getCanonicalType();

        boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType
            || lVarInBinaryExpType instanceof CArrayType;
        boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType
            || rVarInBinaryExpType instanceof CArrayType;

        CExpression address = null;
        CExpression pointerOffset = null;
        CType addressType = null;

        if (lVarIsAddress && rVarIsAddress) {
          return Value.UnknownValue.getInstance();
        } else if (lVarIsAddress) {
          address = lVarInBinaryExp;
          pointerOffset = rVarInBinaryExp;
          addressType = lVarInBinaryExpType;
        } else if (rVarIsAddress) {
          address = rVarInBinaryExp;
          pointerOffset = lVarInBinaryExp;
          addressType = rVarInBinaryExpType;
        } else {
          return super.visit(binaryExp);
        }

        BinaryOperator binaryOperator = binaryExp.getOperator();

        CType elementType = addressType instanceof CPointerType ?
            ((CPointerType)addressType).getType().getCanonicalType() :
                            ((CArrayType)addressType).getType().getCanonicalType();

        switch (binaryOperator) {
        case PLUS:
        case MINUS: {

          Value addressValueV = address.accept(this);

          Value offsetValueV = pointerOffset.accept(this);

          if (addressValueV.isUnknown() || offsetValueV.isUnknown()
              || !addressValueV.isNumericValue() || !offsetValueV.isNumericValue()) {
            return Value.UnknownValue
              .getInstance();
          }

          long addressValue = addressValueV.asNumericValue().longValue();

          long offsetValue = offsetValueV.asNumericValue().longValue();

          long typeSize = getSizeof(elementType);

          long pointerOffsetValue = offsetValue * typeSize;

          switch (binaryOperator) {
          case PLUS:
            return new NumericValue(addressValue + pointerOffsetValue);
          case MINUS:
            if (lVarIsAddress) {
              return new NumericValue(addressValue - pointerOffsetValue);
            } else {
              throw new UnrecognizedCCodeException("Expected pointer arithmetic "
                  + " with + or - but found " + binaryExp.toASTString(), binaryExp);
            }
          default:
            throw new AssertionError();
          }
        }

        default:
          return Value.UnknownValue.getInstance();
        }
      }

      @Override
      protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
          throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pCPointerExpression);

        if (value == null || !(value instanceof BigDecimal)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((BigDecimal) value);
      }

      @Override
      protected Value evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {

        Object value = LModelValueVisitor.this.visit(pCIdExpression);

        if(value == null || !(value instanceof BigDecimal)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((BigDecimal)value);
      }

      @Override
      protected Long evaluateJIdExpression(JIdExpression pVarName) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if(value == null || !(value instanceof BigDecimal)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((BigDecimal)value);
      }

      @Override
      protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
          throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if (value == null || !(value instanceof BigDecimal)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((BigDecimal) value);
      }
    }

    @Override
    public Object visit(CComplexCastExpression pComplexCastExpression) {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private class ValueCodesVisitor extends DefaultCTypeVisitor<ValueCodes, RuntimeException> {

    private final Object value;

    public ValueCodesVisitor(Object pValue) {
      value = pValue;
    }

    @Override
    public ValueCodes visitDefault(CType pT) throws RuntimeException {
      return createUnknownValueCodes();
    }

    @Override
    public ValueCodes visit(CPointerType pointerType) throws RuntimeException {

      ValueCodes valueCodes = new ValueCodes(handleAddress(value));

      ValueCodeVisitor v = new ValueCodeVisitor(value, valueCodes, "", "");

      pointerType.accept(v);

      return valueCodes;
    }

    @Override
    public ValueCodes visit(CArrayType arrayType) throws RuntimeException {
      ValueCodes valueCodes = new ValueCodes(handleAddress(value));

      ValueCodeVisitor v = new ValueCodeVisitor(value, valueCodes, "", "");

      arrayType.accept(v);

      return valueCodes;
    }

    @Override
    public ValueCodes visit(CSimpleType simpleType) throws RuntimeException {
      return new ValueCodes(getValueCode(simpleType.getType(), value));
    }

    //TODO Move to Utility?
    protected ValueCode handleAddress(Object pValue) {

      /*addresses are modeled as floating point numbers*/
      return handleFloatingPointNumbers(pValue);
    }

    protected ValueCode getValueCode(CBasicType basicType, Object pValue) {

      switch (basicType) {
      case BOOL:
      case INT:
        return handleIntegerNumbers(pValue);
      case FLOAT:
      case DOUBLE:
        return handleFloatingPointNumbers(pValue);
      }

      return UnknownValueCode.getInstance();
    }

    private ValueCodes createUnknownValueCodes() {
      return new ValueCodes();
    }

    private ValueCode handleFloatingPointNumbers(Object pValue) {

      //TODO Check length in given constraints.

      String value = pValue.toString();

      if (value.matches("((-)?)((\\d*)|(.(\\d*))|((\\d*).)|((\\d*).(\\d*)))")) {
        return ExplicitValueCode.valueOf(value);
      }

      return UnknownValueCode.getInstance();
    }

    private ValueCode handleIntegerNumbers(Object pValue) {

      //TODO Check length in given constraints.
      String value = pValue.toString();

      if (value.matches("((-)?)\\d*")) {
        return ExplicitValueCode.valueOf(value);
      } else {
        String[] numberParts = value.split("\\.");

        if (numberParts.length == 2 &&
            numberParts[1].matches("0*") &&
            numberParts[0].matches("((-)?)\\d*")) {

          return ExplicitValueCode.valueOf(numberParts[0]);
        }
      }

      ValueCode valueCode = handleFloatingPointNumbers(pValue);

      if (valueCode.isUnknown()) {
        return valueCode;
      } else {
        return valueCode.addCast(CBasicType.INT);
      }
    }

    private class ValueCodeVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

      /*
       * Contains the address of the super type of the visited type.
       * It is assigned by the model of the predicate Analysis.
       */
      private final Object address;
      private final ValueCodes valueCodes;

      /*
       * Contains the prefix and postfix, that have to be added
       * to the root expression to get the result, which has the super
       * type of the visited type as type.
       */
      private final String prefix;
      private final String postfix;

      public ValueCodeVisitor(Object pAddress, ValueCodes pValueCodes,
          String pPrefix, String pPostfix) {
        address = pAddress;
        valueCodes = pValueCodes;
        prefix = pPrefix;
        postfix = pPostfix;
      }

      @Override
      public Void visitDefault(CType pT) throws RuntimeException {
        return null;
      }

      @Override
      public Void visit(CPointerType pointerType) throws RuntimeException {

        CType expectedType = pointerType.getType().getCanonicalType();

        Object value = getPointerOrArrayValue(expectedType);

        if (value == null) {
          return null;
        }

        ValueCode valueCode;

        if (expectedType instanceof CSimpleType) {
          valueCode = getValueCode(((CSimpleType) expectedType).getType(), value);
        } else {
          valueCode = handleAddress(value);
        }

        if (valueCode.isUnknown()) {
          return null;
        }

        String lValuePrefix = "(*" + prefix;
        String lValuePostfix = postfix + ")";

        SubExpressionValueCode subExpressionValueCode =
            new SubExpressionValueCode(valueCode.getValueCode(), lValuePrefix, lValuePostfix);

        valueCodes.addSubExpressionValueCode(subExpressionValueCode);

        ValueCodeVisitor v = new ValueCodeVisitor(value, valueCodes, lValuePrefix, lValuePostfix);

        expectedType.accept(v);

        return null;
      }

      private Object getPointerOrArrayValue(CType expectedType) {

        ValueCode addressCode = handleAddress(address);

        if (addressCode.isUnknown()) {
          return null;
        }

        BigDecimal address = new BigDecimal(addressCode.getValueCode());

        String ufName = TypeUFNameVisitor.getUFName(expectedType);

        return TypeUFNameVisitor.getValueFromUF(ufName, address, functionEnvoirment);
      }
    }
  }

  public final static class ValueCodes {

    /*Contains values for possible sub expressions */
    private final Set<SubExpressionValueCode> subExpressionValueCodes = new HashSet<>();

    private final ValueCode expressionValueCode;

    public ValueCodes() {
      expressionValueCode = UnknownValueCode.getInstance();
    }

    public ValueCodes(ValueCode valueCode) {
      expressionValueCode = valueCode;
    }

    public ValueCode getExpressionValueCode() {
      return expressionValueCode;
    }

    public String getExpressionValueCodeAsString() {
      return expressionValueCode.getValueCode();
    }

    public void addSubExpressionValueCode(SubExpressionValueCode code) {
      subExpressionValueCodes.add(code);
    }

    public boolean hasUnknownValueCode() {
      return expressionValueCode.isUnknown();
    }

    public Set<SubExpressionValueCode> getSubExpressionValueCode() {
      return ImmutableSet.copyOf(subExpressionValueCodes);
    }

    @Override
    public String toString() {

      StringBuilder result = new StringBuilder();

      result.append("ValueCode : ");
      result.append(expressionValueCode.toString());
      result.append(", SubValueCodes : ");
      Joiner joiner = Joiner.on(", ");
      result.append(joiner.join(subExpressionValueCodes));

      return result.toString();
    }
  }

  public static interface ValueCode {

    public String getValueCode();
    public boolean isUnknown();

    public ValueCode addCast(CBasicType pType);
  }

  public static class UnknownValueCode implements ValueCode {

    private static final UnknownValueCode instance = new UnknownValueCode();

    private UnknownValueCode() {}

    public static UnknownValueCode getInstance() {
      return instance;
    }

    @Override
    public String getValueCode() {
      throw new UnsupportedOperationException("Can't get the value code of an unknown value");
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public ValueCode addCast(CBasicType pType) {
      throw new UnsupportedOperationException("Can't get the value code of an unknown value");
    }

    @Override
    public String toString() {
      return "UNKNOWN";
    }
  }

  public static class ExplicitValueCode implements ValueCode {

    private final String valueCode;

    protected ExplicitValueCode(String value) {
      valueCode = value;
    }

    @Override
    public ValueCode addCast(CBasicType pType) {

      switch (pType) {
      case CHAR:
        return ExplicitValueCode.valueOf("(char)" + valueCode);
      case DOUBLE:
        return ExplicitValueCode.valueOf("(double)" + valueCode);
      case FLOAT:
        return ExplicitValueCode.valueOf("(float)" + valueCode);
      case BOOL:
      case INT:
        return ExplicitValueCode.valueOf("(int)" + valueCode);
      case UNSPECIFIED:
        break;
      case VOID:
        break;
      default:
        break;
      }

      return this;
    }

    public static ValueCode valueOf(String value) {
      return new ExplicitValueCode(value);
    }

    public static ValueCode valueOf(BigDecimal value) {
      return new ExplicitValueCode(value.toPlainString());
    }

    public static ValueCode valueOf(BigInteger value) {
      return new ExplicitValueCode(value.toString());
    }

    @Override
    public String getValueCode() {
      return valueCode;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public String toString() {
      return valueCode;
    }
  }

  public static final class SubExpressionValueCode extends ExplicitValueCode {

    private final String prefix;
    private final String postfix;

    private SubExpressionValueCode(String value, String pPrefix, String pPostfix) {
      super(value);
      prefix = pPrefix;
      postfix = pPostfix;
    }

    public static SubExpressionValueCode valueOf(String value, String prefix, String postfix) {
      return new SubExpressionValueCode(value, prefix, postfix);
    }

    public static SubExpressionValueCode valueOf(BigDecimal value, String prefix, String postfix) {
      return new SubExpressionValueCode(value.toPlainString(), prefix, postfix);
    }

    public static SubExpressionValueCode valueOf(BigInteger value, String prefix, String postfix) {
      return new SubExpressionValueCode(value.toString(), prefix, postfix);
    }

    public String getPrefix() {
      return prefix;
    }

    public String getPostfix() {
      return postfix;
    }

    @Override
    public String toString() {

      return "<value code : " + super.toString() + ", prefix : " + prefix + ", postfix : " + postfix + ">";
    }
  }

  private static class TypeUFNameVisitor implements CTypeVisitor<String, RuntimeException>{

    public static String getUFName(CType pType) {
      String name = pType.getCanonicalType().accept(new TypeUFNameVisitor());

      if(name == null) {
        return null;
      }

      return "*" + name;
    }

    //TODO Move to Utility
    private static Object getValueFromUF(String pUfName, Object address, Multimap<String, Assignment> functionEnvoirment) {

      if (pUfName == null) {
        return null;
      }

      for (Assignment assignment : functionEnvoirment.get(pUfName)) {
        Function function = (Function) assignment.getTerm();

        if (function.getArity() != 1) {
          break;
        }

        if (function.getArgument(0).equals(address)) {
          return assignment.getValue();
        }
      }
      return null;
    }

    @Override
    public String visit(CArrayType pArrayType) throws RuntimeException {
      return null;
    }

    @Override
    public String visit(CCompositeType pCompositeType) throws RuntimeException {

      if(pCompositeType.getKind() == ComplexTypeKind.STRUCT) {
        return "struct_" + pCompositeType.getName();
      }

      return null;
    }

    @Override
    public String visit(CElaboratedType pElaboratedType) throws RuntimeException {

      CComplexType realType = pElaboratedType.getRealType();

      if (realType != null) {
        return realType.accept(this);
      }

      return null;
    }

    @Override
    public String visit(CEnumType pEnumType) throws RuntimeException {
      return pEnumType.getName();
    }

    @Override
    public String visit(CFunctionType pFunctionType) throws RuntimeException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visit(CPointerType pPointerType) throws RuntimeException {

      String ufName = pPointerType.getType().getCanonicalType().accept(this);

      if(ufName == null) {
        return null;
      }

      return "(" + ufName + ")*";
    }

    @Override
    public String visit(CProblemType pProblemType) throws RuntimeException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String visit(CSimpleType pSimpleType) throws RuntimeException {

      switch (pSimpleType.getType()) {
      case INT: return "signed_int";

      }

      return null;
    }

    @Override
    public String visit(CTypedefType pTypedefType) throws RuntimeException {
      return pTypedefType.getRealType().accept(this);
    }
  }
}