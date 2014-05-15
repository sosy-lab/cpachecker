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
package org.sosy_lab.cpachecker.core.counterexample;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
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
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.counterexample.ModelAtCFAEdge.Address;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;


public class AssignmentToEdgeAllocator {

  @SuppressWarnings("unused")
  private final LogManager logger;
  private final MachineModel machineModel;

  private final CFAEdge cfaEdge;
  private final Set<Assignment> newAssignmentsAtEdge;
  private final ModelAtCFAEdge modelAtEdge;

  public AssignmentToEdgeAllocator(LogManager pLogger,
      CFAEdge pCfaEdge, Set<Assignment> pNewAssignmentsAtEdge,
      ModelAtCFAEdge pModelAtEdge,
      MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;

    cfaEdge = pCfaEdge;
    newAssignmentsAtEdge = pNewAssignmentsAtEdge;
    modelAtEdge = pModelAtEdge;
  }

  public CFAEdgeWithAssignments allocateAssignmentsToEdge() {

    String codeAtEdge = createEdgeCode(cfaEdge);
    String comment = createComment(cfaEdge);

    return new CFAEdgeWithAssignments(cfaEdge, newAssignmentsAtEdge, codeAtEdge, comment);
  }

  private String createComment(CFAEdge pCfaEdge) {
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      return handleAssume((AssumeEdge) cfaEdge);
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return handleDclComment((ADeclarationEdge)cfaEdge);
    } else if(cfaEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
      return handleReturnStatementComment((AReturnStatementEdge) cfaEdge);
    }

    return null;
  }

  private String handleReturnStatementComment(AReturnStatementEdge pCfaEdge) {

    if (pCfaEdge.getExpression() instanceof CExpression) {
      CExpression returnExp = (CExpression) pCfaEdge.getExpression();

      if(returnExp instanceof CLiteralExpression) {
        /*boring expression*/
        return null;
      }

      String functionname = pCfaEdge.getPredecessor().getFunctionName();

      LModelValueVisitor v = new LModelValueVisitor(functionname);

      Number value = v.evaluateNumericalValue(returnExp);

      if(value == null) {
        return null;
      }

      return returnExp.toASTString() + " = " + value.toString();
    }

    return null;
  }

  private String handleDclComment(ADeclarationEdge pCfaEdge) {

    if (pCfaEdge instanceof CDeclarationEdge) {
      return addressOfDcl((CSimpleDeclaration) pCfaEdge.getDeclaration(), pCfaEdge);
    }

    return null;
  }

  private String addressOfDcl(CSimpleDeclaration dcl, CFAEdge edge) {

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    /* function name may be null*/
    LModelValueVisitor v = new LModelValueVisitor(functionName);
    Address address = v.getAddress(dcl);

    if (address == null) {
      return null;
    }

    return "Symbolic address of declaration " + dcl.getName()
        + " is " + address.getAsNumber().toString();
  }

  @Nullable
  private String createEdgeCode(CFAEdge pCFAEdge) {

    if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return handleDeclaration(((ADeclarationEdge) pCFAEdge).getDeclaration());
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return handleStatement(((AStatementEdge) pCFAEdge).getStatement());
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      return handleFunctionCall(((FunctionCallEdge) pCFAEdge));
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
      throw new AssertionError("Multi-edges should be resolved by this point.");
    }

    return null;
  }

  private String handleAssume(AssumeEdge pCfaEdge) {

    if (pCfaEdge instanceof CAssumeEdge) {
      return handleAssume((CAssumeEdge)pCfaEdge);
    }

    return null;
  }

  private String handleAssume(CAssumeEdge pCfaEdge) {

    CExpression pCExpression = pCfaEdge.getExpression();

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    if(pCExpression instanceof CBinaryExpression) {

      CBinaryExpression binExp = ((CBinaryExpression) pCExpression);

      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();

      String result1 = handleAssumeOp(op1, functionName);

      String result2 = handleAssumeOp(op2, functionName);

      if (result1 != null && result2 != null) {
        return result1 + System.lineSeparator() + result2;
      } else if (result1 != null) {
        return result1;
      } else if (result2 != null) {
        return result2;
      }

      return null;
    }

    return null;
  }

  private String handleAssumeOp(CExpression op, String functionName) {

    if(op instanceof CLiteralExpression) {
      /*boring expression*/
      return null;
    }

    if (op instanceof CLeftHandSide) {
      return handleAssignment((CLeftHandSide) op);
    } else {
      Object value = getValueObject(op, functionName);

      if (value != null) {
        return op.toASTString() + " == " + value.toString();
      } else {
        return null;
      }
    }
  }

  private Object getValueObject(CExpression pOp1, String pFunctionName) {

    LModelValueVisitor v = new LModelValueVisitor(pFunctionName);

    return v.evaluateNumericalValue(pOp1);
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

    for (Assignment valuePair : newAssignmentsAtEdge) {

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

        ValueCodes valueAsCode = getValueAsCode(valuePair.getValue(),
            formalParameterDeclaration.getType(),
            formalParameterDeclaration.getName(),
            functionName);

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
  private String handleAssignment(IALeftHandSide leftHandSide) {

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    Object value = getValueObject(leftHandSide, functionName);

    if (value == null) {
      return null;
    }

    Type expectedType = leftHandSide.getExpressionType();
    ValueCodes valueAsCode = getValueAsCode(value, expectedType, leftHandSide.toASTString(), functionName);

    return handleSimpleValueCodesAssignments(valueAsCode, leftHandSide.toASTString());
  }

  @Nullable
  private String handleAssignment(IAssignment assignment) {
    IALeftHandSide leftHandSide = assignment.getLeftHandSide();
    return handleAssignment(leftHandSide);
  }

  private Object getValueObject(IALeftHandSide pLeftHandSide, String pFunctionName) {

    if(pLeftHandSide instanceof CLeftHandSide) {
      CLeftHandSide cLeftHandSide = (CLeftHandSide) pLeftHandSide;
      LModelValueVisitor v = new LModelValueVisitor(pFunctionName);
      return cLeftHandSide.accept(v);
    }

    return null;
  }

  @Nullable
  /*
   * The Parameter leftHandSide may be null, it is needed if
   * structs are to be resolved.
   */
  private ValueCodes getValueAsCode(Object pValue,
      Type pExpectedType,
      String leftHandSide,
      String functionName) {

    // TODO processing for other languages
    if (pExpectedType instanceof CType) {
      CType cType = ((CType) pExpectedType).getCanonicalType();

      ValueCodesVisitor v = new ValueCodesVisitor(pValue);
      ValueCodes valueCodes = cType.accept(v);
      v.resolveStruct(cType, valueCodes, leftHandSide, functionName);
      return valueCodes;
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

      String functionName = cfaEdge.getPredecessor().getFunctionName();

      Object value = getValueObject(varDcl, functionName);

      if (value == null) {
        return null;
      }

      Type dclType = varDcl.getType();
      ValueCodes valueAsCode = getValueAsCode(value, dclType, dcl.getName(), functionName);

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

    Joiner joiner = Joiner.on(System.lineSeparator());

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

  private class LModelValueVisitor implements CLeftHandSideVisitor<Object, RuntimeException> {

    private final String functionName;
    private final AddressValueVisitor addressVisitor;

    public LModelValueVisitor(String pFunctionName) {
      functionName = pFunctionName;
      addressVisitor = new AddressValueVisitor(this);
    }

    private Address getAddress(CSimpleDeclaration dcl) {
      return addressVisitor.getAddress(dcl);
    }

    private final Number evaluateNumericalValue(CExpression exp) {

      Value addressV;
      try {
        ModelExpressionValueVisitor v = new ModelExpressionValueVisitor(functionName, machineModel, new LogManagerWithoutDuplicates(logger));
        addressV = exp.accept(v);
      } catch (UnrecognizedCCodeException e1) {
        throw new IllegalArgumentException(e1);
      }

      if (addressV.isUnknown() && !addressV.isNumericValue()) {
        return null;
      }

      return addressV.asNumericValue().getNumber();
    }

    private final Address evaluateNumericalValueAsAddress(CExpression exp) {

      Number result = evaluateNumericalValue(exp);

      if (result == null) {
        return null;
      }

      return Address.valueOf(result);
    }

    /*This method evaluates the address of the lValue, not the address the expression evaluates to*/
    private Address evaluateAddress(CLeftHandSide pExp) {
      return pExp.accept(addressVisitor);
    }

    @Override
    public Object visit(CArraySubscriptExpression pIastArraySubscriptExpression) {

      Address valueAddress = evaluateAddress(pIastArraySubscriptExpression);

      if(valueAddress == null) {
        return null;
      }

      CType type = pIastArraySubscriptExpression.getExpressionType().getCanonicalType();

      Object value = modelAtEdge.getValueFromUF(type, valueAddress);

      return value;
    }

    @Override
    public Object visit(CFieldReference pIastFieldReference) {

      Address address = evaluateAddress(pIastFieldReference);

      if(address == null) {
        return lookupReference(pIastFieldReference);
      }

      CType type = pIastFieldReference.getExpressionType().getCanonicalType();

      Object value = modelAtEdge.getValueFromUF(type, address);

      if (value == null) {
        return lookupReference(pIastFieldReference);
      }

      return value;
    }

    private Object lookupReference(CFieldReference pIastFieldReference) {

      if(pIastFieldReference.isPointerDereference()) {
        return null;
      }

      /* Fieldreferences are sometimes represented as variables,
         e.g a.b.c in main is main::a$b$c */
      String fieldReferenceVariableName = getFieldReferenceVariableName(pIastFieldReference);

      if (fieldReferenceVariableName != null && modelAtEdge.containsVariableName(fieldReferenceVariableName)) {
        return modelAtEdge.getVariableValue(fieldReferenceVariableName);
      }

      return null;
    }

    private BigDecimal getFieldOffset(CFieldReference fieldReference) {
      CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();
      return getFieldOffset(fieldOwnerType, fieldReference.getFieldName());
    }

    private BigDecimal getFieldOffset(CType ownerType, String fieldName) {

      if (ownerType instanceof CElaboratedType) {

        CType realType = ((CElaboratedType) ownerType).getRealType();

        if (realType == null) {
          return null;
        }

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
          offset = offset + machineModel.getSizeof(typeMember.getType().getCanonicalType());
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

        CIdExpression idExpression = (CIdExpression) reference.getFieldOwner();

        fieldNameList.add(0, idExpression.getName());

        Joiner joiner = Joiner.on("$");

        if (ForwardingTransferRelation.isGlobal(idExpression)) {
          return joiner.join(fieldNameList);
        } else {
          return functionName + "::" + joiner.join(fieldNameList);
        }
      } else {
        return null;
      }
    }

    @Override
    public Object visit(CIdExpression pCIdExpression) {

      CType type = pCIdExpression.getExpressionType().getCanonicalType();

      if (type instanceof CSimpleType || type instanceof CPointerType) {
        return handleSimpleVariableDeclaration(pCIdExpression.getDeclaration());
      }

      if(type instanceof CArrayType || isStructOrUnionType(type)) {
        /*The evaluation of an array is its address*/
        Address address = evaluateAddress(pCIdExpression);

        if(address != null) {
          return address.getSymbolicValue();
        }

      }

      return null;
    }

    @Nullable
    private Object handleVariableDeclaration(CSimpleDeclaration pVarDcl) {

      if (pVarDcl == null || functionName == null || (!(pVarDcl instanceof CVariableDeclaration)
          && !(pVarDcl instanceof CParameterDeclaration))) {
        return null;
      }

      CType type = pVarDcl.getType();

      if (type instanceof CSimpleType || type instanceof CPointerType) {
        return handleSimpleVariableDeclaration(pVarDcl);
      }

      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        /*The evaluation of an array or a struct is its address*/
        Address address = addressVisitor.getAddress(pVarDcl);

        if (address != null) {
          return address.getSymbolicValue();
        }

      }

      return null;
    }

    private Object handleSimpleVariableDeclaration(CSimpleDeclaration pVarDcl) {

      /* The variable might not exist anymore in the variable environment,
         search in the address space of the function environment*/

      Address address = addressVisitor.getAddress(pVarDcl);

      if (address == null) {
        return lookupVariable(pVarDcl);
      }

      CType type = pVarDcl.getType().getCanonicalType();

      Object value = modelAtEdge.getValueFromUF(type, address);

      if (value == null) {
        return lookupVariable(pVarDcl);
      }

      return value;
    }

    private Object lookupVariable(CSimpleDeclaration pVarDcl) {
      String varName = getName(pVarDcl);

      if (modelAtEdge.containsVariableName(varName)) {
        return modelAtEdge.getVariableValue(varName);
      } else {
        return null;
      }
    }

    private String getName(CSimpleDeclaration pDcl) {

      String name = pDcl.getName();

      if (pDcl instanceof CParameterDeclaration ||
          (pDcl instanceof CVariableDeclaration
              && !((CVariableDeclaration) pDcl).isGlobal())) {
        return functionName + "::" + name;
      } else {
        return name;
      }
    }

    @Override
    public Object visit(CPointerExpression pPointerExpression) {

      CExpression exp = pPointerExpression.getOperand();

      /*Quick jump to the necessary method.
       * the address of a dereference is the evaluation of its operand*/
      Address address = evaluateNumericalValueAsAddress(exp);

      if(address == null) {
        return null;
      }

      CType type = exp.getExpressionType().getCanonicalType();

      if (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
      } else if (type instanceof CArrayType) {
        type = ((CArrayType) type).getType();
      } else {
        return null;
      }

      return modelAtEdge.getValueFromUF(type, address);
    }

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

    private class AddressValueVisitor implements CLeftHandSideVisitor<Address, RuntimeException> {

      private final LModelValueVisitor valueVisitor;

      public AddressValueVisitor(LModelValueVisitor pValueVisitor) {
        valueVisitor = pValueVisitor;
      }

      public Address getAddress(CSimpleDeclaration dcl) {

        String name = getName(dcl);

        if (modelAtEdge.containsVariableAddress(name)) {
          return modelAtEdge.getVariableAddress(name);
        }

        return null;
      }

      @Override
      public Address visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
        CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

        // we can evaluate this here, because arrays will be evaluated to their address
        // TODO BUG!? what if array types are intermingled with pointer types
        Address address = evaluateNumericalValueAsAddress(arrayExpression);

        if(address == null) {
          return null;
        }

        CExpression subscriptCExpression = pIastArraySubscriptExpression.getSubscriptExpression();

        Number subscriptValueNumber = evaluateNumericalValue(subscriptCExpression);

        if(subscriptValueNumber == null) {
          return null;
        }

        BigDecimal subscriptValue = new BigDecimal(subscriptValueNumber.toString());

        BigDecimal typeSize = BigDecimal.valueOf(machineModel.getSizeof(pIastArraySubscriptExpression.getExpressionType().getCanonicalType()));

        BigDecimal subscriptOffset = subscriptValue.multiply(typeSize);

        if (!address.isNumericalType()) {
          return null;
        }

        return address.addOffset(subscriptOffset);
      }

      @Override
      public Address visit(CFieldReference pIastFieldReference) {

        CExpression fieldOwner = pIastFieldReference.getFieldOwner();

        if (pIastFieldReference.isPointerDereference()) {

          Address fieldOwneraddress = evaluateNumericalValueAsAddress(fieldOwner);

          if (fieldOwneraddress == null) {
            return null;
          }

          BigDecimal fieldOffset = getFieldOffset(pIastFieldReference);

          if(fieldOffset == null) {
            return null;
          }

          return fieldOwneraddress.addOffset(fieldOffset);
        }

        if (!(fieldOwner instanceof CLeftHandSide)) {
          //TODO Investigate
          return lookupReferenceAddress(pIastFieldReference);
        }

        Address fieldOwnerAddress = evaluateAddress((CLeftHandSide) fieldOwner);

        if (fieldOwnerAddress == null) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        BigDecimal fieldOffset = getFieldOffset(pIastFieldReference);

        if(fieldOffset == null && !fieldOwnerAddress.isNumericalType()) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        Address address = fieldOwnerAddress.addOffset(fieldOffset);

        if (address == null) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        return address;
      }

      private Address lookupReferenceAddress(CFieldReference pIastFieldReference) {
        /* Fieldreferences are sometimes represented as variables,
        e.g a.b.c in main is main::a$b$c */
        String fieldReferenceVariableName = getFieldReferenceVariableName(pIastFieldReference);

        if (fieldReferenceVariableName != null) {
          if (modelAtEdge.containsVariableAddress(fieldReferenceVariableName)) {
            return modelAtEdge.getVariableAddress(fieldReferenceVariableName);
          }
        }

        return null;
      }

      @Override
      public Address visit(CIdExpression pIastIdExpression) {
        return getAddress(pIastIdExpression.getDeclaration());
      }

      @Override
      public Address visit(CPointerExpression pPointerExpression) {
        /*The address of a pointer dereference is the evaluation of its operand*/
        return valueVisitor.evaluateNumericalValueAsAddress(pPointerExpression.getOperand());
      }

      @Override
      public Address visit(CComplexCastExpression pComplexCastExpression) {
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

          Number addressValueNumber = addressValueV.asNumericValue().getNumber();

          BigDecimal addressValue = new BigDecimal(addressValueNumber.toString());

          // Because address and offset value may be interchanged, use BigDecimal for both
          Number offsetValueNumber = offsetValueV.asNumericValue().getNumber();

          BigDecimal offsetValue = new BigDecimal(offsetValueNumber.toString());

          BigDecimal typeSize = BigDecimal.valueOf(getSizeof(elementType));

          BigDecimal pointerOffsetValue = offsetValue.multiply(typeSize);

          switch (binaryOperator) {
          case PLUS:
            return new NumericValue(addressValue.add(pointerOffsetValue));
          case MINUS:
            if (lVarIsAddress) {
              return new NumericValue(addressValue.subtract(pointerOffsetValue));
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
      public Value visit(CUnaryExpression pUnaryExpression) throws UnrecognizedCCodeException {

        if (pUnaryExpression.getOperator() == UnaryOperator.AMPER) {

          CExpression operand = pUnaryExpression.getOperand();

          if (operand instanceof CLeftHandSide) {
            //TODO assumed? Problems with casts

            Address address = evaluateAddress((CLeftHandSide) operand);

            if(address != null && address.isNumericalType()) {
              return new NumericValue(address.getAsNumber());
            }
          }
        }

        return super.visit(pUnaryExpression);
      }

      @Override
      protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
          throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pCPointerExpression);

        if (value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number) value);
      }

      @Override
      protected Value evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {

        Object value = LModelValueVisitor.this.visit(pCIdExpression);

        if(value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number)value);
      }

      @Override
      protected Value evaluateJIdExpression(JIdExpression pVarName) {
        return Value.UnknownValue.getInstance();
      }

      @Override
      protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if(value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number)value);
      }

      @Override
      protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
          throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if (value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number) value);
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
    public ValueCodes visit(CElaboratedType pT) throws RuntimeException {

      CType realType = pT.getRealType();

      if (realType != null) {
        return realType.accept(this);
      }

      return createUnknownValueCodes();
    }

    @Override
    public ValueCodes visit(CEnumType pT) throws RuntimeException {

      /*We don't need to resolve enum types */
      return createUnknownValueCodes();
    }

    @Override
    public ValueCodes visit(CFunctionType pT) throws RuntimeException {

      // TODO Investigate
      return createUnknownValueCodes();
    }

    @Override
    public ValueCodes visit(CSimpleType simpleType) throws RuntimeException {
      return new ValueCodes(getValueCode(simpleType.getType(), value));
    }

    @Override
    public ValueCodes visit(CProblemType pT) throws RuntimeException {
      return createUnknownValueCodes();
    }

    @Override
    public ValueCodes visit(CTypedefType pT) throws RuntimeException {
      return pT.getRealType().accept(this);
    }

    @Override
    public ValueCodes visit(CCompositeType compType) throws RuntimeException {

      if(compType.getKind() == ComplexTypeKind.ENUM) {
        return createUnknownValueCodes();
      }

      ValueCodes valueCodes = new ValueCodes(handleAddress(value));

      ValueCodeVisitor v = new ValueCodeVisitor(value, valueCodes, "", "");

      compType.accept(v);

      return valueCodes;
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

    public void resolveStruct(CType type, ValueCodes pValueCodes, String pLeftHandSide, String pFunctionName) {
      if (isStructOrUnionType(type)) {
        type.accept(new ValueCodeStructResolver(pValueCodes, pLeftHandSide, pFunctionName, "", ""));
      }
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

    /**
     * Resolves all subexpressions that can be resolved.
     * Stops at duplicate memory location.
     */
    private class ValueCodeVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

      /*Contains references already visited, to avoid descending indefinitely.
       *Shares a reference with all instanced Visitors resolving the given type.*/
      private final Set<Pair<CType, Object>> visited;

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
        visited = new HashSet<>();
      }

      private ValueCodeVisitor(Object pAddress, ValueCodes pValueCodes,
          String pPrefix, String pPostfix, Set<Pair<CType, Object>> pVisited) {
        address = pAddress;
        valueCodes = pValueCodes;
        prefix = pPrefix;
        postfix = pPostfix;
        visited = pVisited;
      }

      @Override
      public Void visitDefault(CType pT) throws RuntimeException {
        return null;
      }

      @Override
      public Void visit(CTypedefType pT) throws RuntimeException {
        return pT.getRealType().accept(this);
      }

      @Override
      public Void visit(CElaboratedType pT) throws RuntimeException {

        CType realType = pT.getRealType();

        if (realType == null) {
          return null;
        }

        return realType.getCanonicalType().accept(this);
      }

      @Override
      public Void visit(CEnumType pT) throws RuntimeException {
        return null;
      }

      @Override
      public Void visit(CCompositeType compType) throws RuntimeException {

        if (compType.getKind() == ComplexTypeKind.ENUM) {
          return null;
        }

        if(compType.getKind() == ComplexTypeKind.UNION) {

        }

        if(compType.getKind() == ComplexTypeKind.STRUCT) {
          handleStruct(compType);
        }

        return null;
      }

      private void handleStruct(CCompositeType pCompType) {

        ValueCode addressCode = handleAddress(address);

        if (addressCode.isUnknown()) {
          return;
        }

        Address fieldAddress = Address.valueOf(new BigDecimal(addressCode.getValueCode()));

        for (CCompositeType.CCompositeTypeMemberDeclaration memberType : pCompType.getMembers()) {

          handleMemberField(memberType, fieldAddress);
          int offsetToNextField = machineModel.getSizeof(memberType.getType());

          if (!fieldAddress.isNumericalType()) {
            return;
          }

          fieldAddress = fieldAddress.addOffset(BigDecimal.valueOf(offsetToNextField));
        }
      }

      private void handleMemberField(CCompositeTypeMemberDeclaration pType, Address fieldAddress) {
        CType realType = pType.getType().getCanonicalType();
        Object fieldValue = modelAtEdge.getValueFromUF(realType, fieldAddress);

        if(fieldValue == null) {
          return;
        }

        ValueCode valueCode;

        if (realType instanceof CSimpleType) {
          valueCode = getValueCode(((CSimpleType) realType).getType(), fieldValue);
        } else {
          valueCode = handleAddress(fieldValue);
        }

        if(valueCode.isUnknown()) {
          return;
        }

        Object fieldAddressObject = fieldAddress.getSymbolicValue();
        Pair<CType, Object> visits = Pair.of(realType, fieldAddressObject);

        if (!visited.contains(visits)) {

          visited.add(visits);

          String fieldPrefix = "(" + prefix;
          String fieldPostfix = postfix + "." + pType.getName() + ")";

          SubExpressionValueCode subExpression =
              SubExpressionValueCode.valueOf(valueCode.getValueCode(), fieldPrefix, fieldPostfix);
          valueCodes.addSubExpressionValueCode(subExpression);

          realType.accept(new ValueCodeVisitor(fieldValue, valueCodes, fieldPrefix, fieldPostfix, visited));
        }
      }

      @Override
      public Void visit(CArrayType arrayType) throws RuntimeException {

        CType expectedType = arrayType.getType().getCanonicalType();

        int subscript = 0;

        ValueCode arrayAddressCode = handleAddress(value);

        if(arrayAddressCode.isUnknown()) {
          return null;
        }


        Address arrayAddress = Address.valueOf(new BigDecimal(arrayAddressCode.getValueCode()));

        boolean memoryHasValue = true;
        while (memoryHasValue) {
          memoryHasValue = handleArraySubscript(arrayAddress, subscript, expectedType);
          subscript++;
        }

        return null;
      }

      private boolean handleArraySubscript(Address pArrayAddress, int pSubscript, CType pExpectedType) {

        int typeSize = machineModel.getSizeof(pExpectedType);
        int subscriptOffset = pSubscript * typeSize;

        if (!pArrayAddress.isNumericalType()) {
          return false;
        }

        Address address = pArrayAddress.addOffset(BigDecimal.valueOf(subscriptOffset));

        Object value = modelAtEdge.getValueFromUF(pExpectedType, address);

        if (value == null) {
          return false;
        }

        //TODO the following code is duplicated over several methods, remove Code duplication

        ValueCode valueCode;

        if (pExpectedType instanceof CSimpleType) {
          valueCode = getValueCode(((CSimpleType) pExpectedType).getType(), value);
        } else {
          valueCode = handleAddress(value);
        }

        if (valueCode.isUnknown()) {
          /*Stop, because it is highly
           * unlikely that following values can be identified*/
          return false;
        }

        Object addressO = address.getSymbolicValue();
        Pair<CType, Object> visits = Pair.of(pExpectedType, addressO);

        if (!visited.contains(visits)) {

          visited.add(visits);

          String lValuePrefix = "(" + prefix;
          String lValuePostfix = postfix + "[" + pSubscript + "])";

          SubExpressionValueCode subExpressionValueCode =
              new SubExpressionValueCode(valueCode.getValueCode(), lValuePrefix, lValuePostfix);

          valueCodes.addSubExpressionValueCode(subExpressionValueCode);

          ValueCodeVisitor v = new ValueCodeVisitor(value, valueCodes, lValuePrefix, lValuePostfix, visited);

          pExpectedType.accept(v);
        }

        return true;
      }

      @Override
      public Void visit(CPointerType pointerType) throws RuntimeException {

        CType expectedType = pointerType.getType().getCanonicalType();

        Object value = getPointerValue(expectedType);

        if (value == null) {
          if(isStructOrUnionType(expectedType)) {
            handleFieldPointerDereference(expectedType);
          }
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

        Pair<CType, Object> visits = Pair.of(expectedType, address);

        if (!visited.contains(visits)) {
          SubExpressionValueCode subExpressionValueCode =
              new SubExpressionValueCode(valueCode.getValueCode(), lValuePrefix, lValuePostfix);

          valueCodes.addSubExpressionValueCode(subExpressionValueCode);

          /*Tell all instanced visitors that you visited this memory location*/
          visited.add(visits);

          ValueCodeVisitor v = new ValueCodeVisitor(value, valueCodes, lValuePrefix, lValuePostfix, visited);

          expectedType.accept(v);
        }

        return null;
      }

      private void handleFieldPointerDereference(CType pExpectedType) {
        /* a->b <=> *(a).b */

        String newPrefix = "*(" + prefix;
        String newPostfix = ")";
        pExpectedType.accept(new ValueCodeVisitor(address, valueCodes, newPrefix, newPostfix, visited));
      }

      private Object getPointerValue(CType expectedType) {

        ValueCode addressCode = handleAddress(address);

        if (addressCode.isUnknown()) {
          return null;
        }

        Address address = Address.valueOf(new BigDecimal(addressCode.getValueCode()));

        return modelAtEdge.getValueFromUF(expectedType, address);
      }
    }

    /*Resolve structs or union fields that are stored in the variable environment*/
    private class ValueCodeStructResolver extends DefaultCTypeVisitor<Void, RuntimeException> {

      private final ValueCodes valueCodes;
      private final String leftHandSide;
      private final String functionName;
      private final String prefix;
      private final String postfix;

      public ValueCodeStructResolver(ValueCodes pValueCodes, String pLeftHandSide,
          String pFunctionName, String pPrefix, String pPostfix) {
        valueCodes = pValueCodes;
        leftHandSide = pLeftHandSide;
        functionName = pFunctionName;
        prefix = pPrefix;
        postfix = pPostfix;
      }

      @Override
      public Void visitDefault(CType pT) throws RuntimeException {
        return null;
      }

      @Override
      public Void visit(CElaboratedType type) throws RuntimeException {

        CType realType = type.getRealType();

        if (realType == null) {
          return null;
        }

        return realType.getCanonicalType().accept(this);
      }

      @Override
      public Void visit(CTypedefType pType) throws RuntimeException {
        return pType.getRealType().accept(this);
      }

      @Override
      public Void visit(CCompositeType compType) throws RuntimeException {

        if (compType.getKind() == ComplexTypeKind.ENUM) {
          return null;
        }

        for(CCompositeTypeMemberDeclaration memberType : compType.getMembers()) {
          handleField(memberType.getName(), memberType.getType());
        }

        return null;
      }

      private void handleField(String pFieldName, CType pMemberType) {

        String referenceName = functionName + "::" + leftHandSide + "$" + pFieldName;

        if (modelAtEdge.containsVariableName(referenceName)) {
          Object referenceValue = modelAtEdge.getVariableValue(referenceName);
          addStructSubexpression(referenceValue, pFieldName, pMemberType);
        }

        String fieldPrefix = "(" + prefix;
        String fieldPostfix = postfix + "." + pFieldName + ")";
        String newLeftHandSide = functionName + "::" + leftHandSide + "$" + pFieldName;


        ValueCodeStructResolver resolver =
            new ValueCodeStructResolver(valueCodes, newLeftHandSide,
                functionName, fieldPrefix, fieldPostfix);

        pMemberType.accept(resolver);
      }

      private void addStructSubexpression(Object pFieldValue, String pFieldName, CType pMemberType) {

        CType realType = pMemberType.getCanonicalType();

        String fieldPrefix = "(" + prefix;
        String fieldPostfix = postfix + "." + pFieldName + ")";

        ValueCode valueCode;

        if (realType instanceof CSimpleType) {
          valueCode = getValueCode(((CSimpleType) realType).getType(), pFieldValue);
        } else {
          valueCode = handleAddress(pFieldValue);
        }

        if(valueCode.isUnknown()) {
          return;
        }

        SubExpressionValueCode subExpression =
            SubExpressionValueCode.valueOf(valueCode.getValueCode(), fieldPrefix, fieldPostfix);

        valueCodes.addSubExpressionValueCode(subExpression);
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
}