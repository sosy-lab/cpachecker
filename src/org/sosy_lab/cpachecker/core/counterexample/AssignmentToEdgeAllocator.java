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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
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
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;


public class AssignmentToEdgeAllocator {

  private final LogManager logger;
  private final MachineModel machineModel;

  private final CFAEdge cfaEdge;
  private final ConcreteState modelAtEdge;

  private static final int FIRST = 0;

  public AssignmentToEdgeAllocator(LogManager pLogger,
      CFAEdge pCfaEdge,
      ConcreteState pModelAtEdge,
      MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;

    cfaEdge = pCfaEdge;
    modelAtEdge = pModelAtEdge;
    assert modelAtEdge != null;
  }

  public CFAEdgeWithAssignments allocateAssignmentsToEdge() {

    List<IAssignment> assignmentsAtEdge = createAssignmentsAtEdge(cfaEdge);
    String comment = createComment(cfaEdge);

    return new CFAEdgeWithAssignments(cfaEdge, assignmentsAtEdge, comment);
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
  private List<IAssignment> createAssignmentsAtEdge(CFAEdge pCFAEdge) {

    if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return handleDeclaration(((ADeclarationEdge) pCFAEdge).getDeclaration());
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return handleStatement(((AStatementEdge) pCFAEdge).getStatement());
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      return handleFunctionCall(((FunctionCallEdge) pCFAEdge));
    } else if (cfaEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
      throw new AssertionError("Multi-edges should be resolved by this point.");
    }

    return Collections.emptyList();
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

      List<IAssignment> assignments = handleAssignment((CLeftHandSide) op);

      if(assignments.size() == 0) {
        return null;
      } else {

        List<String> result = new ArrayList<>(assignments.size());

        for (IAssignment assignment : assignments) {
          result.add(assignment.toASTString());
        }

        return Joiner.on(System.lineSeparator()).join(result);
      }

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

  private List<IAssignment> handleFunctionCall(FunctionCallEdge pFunctionCallEdge) {

    if(!(pFunctionCallEdge instanceof CFunctionCallEdge)) {
      return Collections.emptyList();
    }

    CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pFunctionCallEdge;

    CFunctionEntryNode functionEntryNode = functionCallEdge.getSuccessor();

    List<CParameterDeclaration> dcls = functionEntryNode.getFunctionParameters();

    List<IAssignment> assignments = new ArrayList<>();

    for(CParameterDeclaration dcl : dcls) {
      assignments.addAll(handleDeclaration(dcl));
    }

    return assignments;
  }

  @Nullable
  private List<IAssignment> handleAssignment(CLeftHandSide leftHandSide) {

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    Object value = getValueObject(leftHandSide, functionName);

    if (value == null) {
      return Collections.emptyList();
    }

    Type expectedType = leftHandSide.getExpressionType();
    ValueLiterals valueAsCode = getValueAsCode(value, expectedType, leftHandSide, functionName);

    return handleSimpleValueLiteralsAssignments(valueAsCode, leftHandSide);
  }

  private List<IAssignment> handleAssignment(CAssignment assignment) {
    CLeftHandSide leftHandSide = assignment.getLeftHandSide();
    return handleAssignment(leftHandSide);
  }

  private Object getValueObject(CLeftHandSide pLeftHandSide, String pFunctionName) {

    LModelValueVisitor v = new LModelValueVisitor(pFunctionName);
    return pLeftHandSide.accept(v);
  }

  private ValueLiterals getValueAsCode(Object pValue,
      Type pExpectedType,
      CLeftHandSide leftHandSide,
      String functionName) {

    // TODO processing for other languages
    if (pExpectedType instanceof CType) {
      CType cType = ((CType) pExpectedType).getCanonicalType();

      ValueLiteralsVisitor v = new ValueLiteralsVisitor(pValue, leftHandSide);
      ValueLiterals valueLiterals = cType.accept(v);

      if (isStructOrUnionType(cType) && leftHandSide instanceof CIdExpression) {
        v.resolveStruct(cType, valueLiterals, (CIdExpression) leftHandSide, functionName);
      }

      return valueLiterals;
    }

    return new ValueLiterals();
  }

  private List<IAssignment> handleStatement(IAStatement pStatement) {

    if (pStatement instanceof CFunctionCallAssignmentStatement) {
      CAssignment assignmentStatement =
          ((CFunctionCallAssignmentStatement) pStatement);
      return handleAssignment(assignmentStatement);
    }

    if (pStatement instanceof CExpressionAssignmentStatement) {
      CAssignment assignmentStatement =
          ((CExpressionAssignmentStatement) pStatement);
      return handleAssignment(assignmentStatement);
    }

    return Collections.emptyList();
  }

  private List<IAssignment> handleDeclaration(IASimpleDeclaration dcl) {

    if (dcl instanceof CSimpleDeclaration) {

      CSimpleDeclaration cDcl = (CSimpleDeclaration) dcl;

      String functionName = cfaEdge.getPredecessor().getFunctionName();

      Object value = getValueObject(cDcl, functionName);

      if (value == null) {
        return Collections.emptyList();
      }

      CIdExpression idExpression = new CIdExpression(dcl.getFileLocation(), cDcl);

      Type dclType = cDcl.getType();
      ValueLiterals valueAsCode =  getValueAsCode(value, dclType, idExpression, functionName);

      CIdExpression idExp = new CIdExpression(FileLocation.DUMMY, cDcl);

      return handleSimpleValueLiteralsAssignments(valueAsCode, idExp);
    }

    return Collections.emptyList();
  }

  private List<IAssignment> handleSimpleValueLiteralsAssignments(ValueLiterals pValueLiterals, CLeftHandSide pLValue) {

    Set<SubExpressionValueLiteral> subValues = pValueLiterals.getSubExpressionValueLiteral();

    List<IAssignment> statements = new ArrayList<>(subValues.size() + 1);

    if (!pValueLiterals.hasUnknownValueLiteral()) {
      IAssignment statement =
          new CExpressionAssignmentStatement(pLValue.getFileLocation(),
              pLValue, pValueLiterals.getExpressionValueLiteralAsCExpression());

      statements.add(statement);
    }

    for (SubExpressionValueLiteral subValueLiteral : subValues) {
      IAssignment statement =
          new CExpressionAssignmentStatement(pLValue.getFileLocation(),
              subValueLiteral.getSubExpression(),
              subValueLiteral.getValueLiteralAsCExpression());

      statements.add(statement);
    }

    return statements;
  }

  private Object getValueObject(CSimpleDeclaration pDcl, String pFunctionName) {
    return new LModelValueVisitor(pFunctionName).handleVariableDeclaration(pDcl);
  }

  private boolean isStructOrUnionType(CType rValueType) {

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

  //TODO Move to Utility?
  private FieldReference getFieldReference(CFieldReference pIastFieldReference,
      String pFunctionName) {

    List<String> fieldNameList = new ArrayList<>();

    CFieldReference reference = pIastFieldReference;

    fieldNameList.add(FIRST, reference.getFieldName());

    while (reference.getFieldOwner() instanceof CFieldReference
        && !reference.isPointerDereference()) {
      reference = (CFieldReference) reference.getFieldOwner();
      fieldNameList.add(FIRST, reference.getFieldName());
    }

    if (reference.getFieldOwner() instanceof CIdExpression) {

      CIdExpression idExpression = (CIdExpression) reference.getFieldOwner();

      if (ForwardingTransferRelation.isGlobal(idExpression)) {
        return new FieldReference(idExpression.getName(), fieldNameList);
      } else {
        return new FieldReference(idExpression.getName(), pFunctionName, fieldNameList);
      }
    } else {
      return null;
    }
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
      } catch(ArithmeticException e) {
        logger.logDebugException(e);
        logger.log(Level.WARNING, "The expression " + exp.toASTString() +
            "could not be correctly evaluated while calculating the concrete values "
            + "in the counterexample path.");
        return null;
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

      if (valueAddress == null) {
        return null;
      }

      Object value = modelAtEdge.getValueFromMemory(pIastArraySubscriptExpression,
          valueAddress);

      return value;
    }

    @Override
    public Object visit(CFieldReference pIastFieldReference) {

      Address address = evaluateAddress(pIastFieldReference);

      if(address == null) {
        return lookupReference(pIastFieldReference);
      }

      Object value = modelAtEdge.getValueFromMemory(pIastFieldReference, address);

      if (value == null) {
        return lookupReference(pIastFieldReference);
      }

      return value;
    }

    private Object lookupReference(CFieldReference pIastFieldReference) {

      /* Fieldreferences are sometimes represented as variables,
         e.g a.b.c in main is main::a$b$c */
      FieldReference fieldReference = getFieldReference(pIastFieldReference, functionName);

      if (fieldReference != null &&
          modelAtEdge.hasValueForLeftHandSide(fieldReference)) {

        return modelAtEdge.getVariableValue(fieldReference);
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
          return address.getAsNumber();
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
          return address.getAsNumber();
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

      CIdExpression idExp = new CIdExpression(FileLocation.DUMMY, pVarDcl);

      Object value = modelAtEdge.getValueFromMemory(idExp, address);

      if (value == null) {
        return lookupVariable(pVarDcl);
      }

      return value;
    }

    private Object lookupVariable(CSimpleDeclaration pVarDcl) {
      IDExpression varName = getIDExpression(pVarDcl);

      if (modelAtEdge.hasValueForLeftHandSide(varName)) {
        return modelAtEdge.getVariableValue(varName);
      } else {
        return null;
      }
    }

    //TODO Move to util
    private IDExpression getIDExpression(CSimpleDeclaration pDcl) {

      //TODO use original name?
      String name = pDcl.getName();

      if (pDcl instanceof CDeclaration && ((CDeclaration) pDcl).isGlobal()) {
        return new IDExpression(name, functionName);
      } else {
        return new IDExpression(name);
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

      return modelAtEdge.getValueFromMemory(pPointerExpression, address);
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

        IDExpression name = getIDExpression(dcl);

        if (modelAtEdge.hasAddressOfVaribable(name)) {
          return modelAtEdge.getVariableAddress(name);
        }

        return null;
      }

      @Override
      public Address visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
        CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

        // This works because arrays and structs evaluate to addresses
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

        if(fieldOffset == null) {
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
        FieldReference fieldReferenceName = getFieldReference(pIastFieldReference, functionName);

        if (fieldReferenceName != null) {
          if (modelAtEdge.hasAddressOfVaribable(fieldReferenceName)) {
            return modelAtEdge.getVariableAddress(fieldReferenceName);
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

            if(address != null) {
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

  private class ValueLiteralsVisitor extends DefaultCTypeVisitor<ValueLiterals, RuntimeException> {

    private final Object value;
    private final CExpression exp;

    public ValueLiteralsVisitor(Object pValue, CExpression pExp) {
      value = pValue;
      exp = pExp;
    }

    @Override
    public ValueLiterals visitDefault(CType pT) throws RuntimeException {
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CPointerType pointerType) throws RuntimeException {

      Address address = Address.valueOf(value);

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address);

      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);

      ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, exp);

      pointerType.accept(v);

      return valueLiterals;
    }

    @Override
    public ValueLiterals visit(CArrayType arrayType) throws RuntimeException {
      Address address = Address.valueOf(value);

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address);

      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);

      ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, exp);

      arrayType.accept(v);

      return valueLiterals;
    }

    @Override
    public ValueLiterals visit(CElaboratedType pT) throws RuntimeException {

      CType realType = pT.getRealType();

      if (realType != null) {
        return realType.accept(this);
      }

      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CEnumType pT) throws RuntimeException {

      /*We don't need to resolve enum types */
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CFunctionType pT) throws RuntimeException {

      // TODO Investigate
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CSimpleType simpleType) throws RuntimeException {
      return new ValueLiterals(getValueLiteral(simpleType.getType(), value));
    }

    @Override
    public ValueLiterals visit(CProblemType pT) throws RuntimeException {
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CTypedefType pT) throws RuntimeException {
      return pT.getRealType().accept(this);
    }

    @Override
    public ValueLiterals visit(CCompositeType compType) throws RuntimeException {

      if (compType.getKind() == ComplexTypeKind.ENUM) {
        return createUnknownValueLiterals();
      }

      Address address = Address.valueOf(value);

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address);

      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);

      ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, exp);

      compType.accept(v);

      return valueLiterals;
    }

    protected ValueLiteral getValueLiteral(CBasicType basicType, Object pValue) {

      switch (basicType) {
      case BOOL:
      case INT:
        return handleIntegerNumbers(pValue);
      case FLOAT:
      case DOUBLE:
        return handleFloatingPointNumbers(pValue);
      }

      return UnknownValueLiteral.getInstance();
    }

    private ValueLiterals createUnknownValueLiterals() {
      return new ValueLiterals();
    }

    private ValueLiteral handleFloatingPointNumbers(Object pValue) {

      String value = pValue.toString();

      if (value.matches("((-)?)((\\d*)|(.(\\d*))|((\\d*).)|((\\d*).(\\d*)))")) {
        BigDecimal val = new BigDecimal(value);
        return ExplicitValueLiteral.valueOf(val);
      }

      return UnknownValueLiteral.getInstance();
    }

    public void resolveStruct(CType type, ValueLiterals pValueLiterals,
        CIdExpression pOwner, String pFunctionName) {

      ValueLiteralStructResolver v = new ValueLiteralStructResolver(pValueLiterals, pFunctionName, pOwner);
      type.accept(v);
    }

    private ValueLiteral handleIntegerNumbers(Object pValue) {

      String value = pValue.toString();

      if (value.matches("((-)?)\\d*")) {
        BigInteger integerValue = new BigInteger(value);
        return ExplicitValueLiteral.valueOf(integerValue);
      } else {
        String[] numberParts = value.split("\\.");

        if (numberParts.length == 2 &&
            numberParts[1].matches("0*") &&
            numberParts[0].matches("((-)?)\\d*")) {

          BigInteger integerValue = new BigInteger(numberParts[0]);
          return ExplicitValueLiteral.valueOf(integerValue);
        }
      }

      ValueLiteral valueLiteral = handleFloatingPointNumbers(pValue);

      if (valueLiteral.isUnknown()) {
        return valueLiteral;
      } else {
        return valueLiteral.addCast(CNumericTypes.INT);
      }
    }

    /**
     * Resolves all subexpressions that can be resolved.
     * Stops at duplicate memory location.
     */
    private class ValueLiteralVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

      /*Contains references already visited, to avoid descending indefinitely.
       *Shares a reference with all instanced Visitors resolving the given type.*/
      private final Set<Pair<CType, Address>> visited;

      /*
       * Contains the address of the super type of the visited type.
       *
       */
      private final Address address;
      private final ValueLiterals valueLiterals;

      private final CExpression subExpression;

      public ValueLiteralVisitor(Address pAddress, ValueLiterals pValueLiterals, CExpression pSubExp) {
        address = pAddress;
        valueLiterals = pValueLiterals;
        visited = new HashSet<>();
        subExpression = pSubExp;
      }

      private ValueLiteralVisitor(Address pAddress, ValueLiterals pValueLiterals,
          CExpression pSubExp, Set<Pair<CType, Address>> pVisited) {
        address = pAddress;
        valueLiterals = pValueLiterals;
        visited = pVisited;
        subExpression = pSubExp;
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
          //TODO Union
        }

        if(compType.getKind() == ComplexTypeKind.STRUCT) {
          handleStruct(compType);
        }

        return null;
      }

      private void handleStruct(CCompositeType pCompType) {

        Address fieldAddress = address;

        for (CCompositeType.CCompositeTypeMemberDeclaration memberType : pCompType.getMembers()) {

          handleMemberField(memberType, fieldAddress, pCompType);
          int offsetToNextField = machineModel.getSizeof(memberType.getType());

          fieldAddress = fieldAddress.addOffset(offsetToNextField);
        }
      }

      private void handleMemberField(CCompositeTypeMemberDeclaration pType, Address fieldAddress,
          CCompositeType structType) {
        CType expectedType = pType.getType().getCanonicalType();

        assert isStructOrUnionType(subExpression.getExpressionType().getCanonicalType());

        CExpression subExp;
        boolean isPointerDeref;

        if (subExpression instanceof CPointerExpression) {
          // *a.b <=> a->b
          subExp = ((CPointerExpression) subExpression).getOperand();
          isPointerDeref = true;
        } else {
          subExp = subExpression;
          isPointerDeref = false;
        }

        CFieldReference fieldReference =
            new CFieldReference(subExp.getFileLocation(),
                expectedType, pType.getName(), subExp, isPointerDeref);

        Object fieldValue = modelAtEdge.getValueFromMemory(fieldReference, fieldAddress);

        if(fieldValue == null) {
          return;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = null;

        if (expectedType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) expectedType).getType(), fieldValue);
        } else {
          valueAddress = Address.valueOf(fieldValue);
          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress);
        }

        Pair<CType, Address> visits = Pair.of(expectedType, fieldAddress);

        if (visited.contains(visits)) {
          return;
        }

        if (!valueLiteral.isUnknown()) {
          visited.add(visits);
          SubExpressionValueLiteral subExpression = new SubExpressionValueLiteral(valueLiteral, fieldReference);
          valueLiterals.addSubExpressionValueLiteral(subExpression);
        }

        if (valueAddress != null) {
          ValueLiteralVisitor v =
              new ValueLiteralVisitor(valueAddress, valueLiterals, fieldReference, visited);
          expectedType.accept(v);
        }
      }

      @Override
      public Void visit(CArrayType arrayType) throws RuntimeException {

        CType expectedType = arrayType.getType().getCanonicalType();

        int subscript = 0;

        boolean memoryHasValue = true;
        while (memoryHasValue) {
          memoryHasValue = handleArraySubscript(address, subscript, expectedType);
          subscript++;
        }

        return null;
      }

      private boolean handleArraySubscript(Address pArrayAddress, int pSubscript, CType pExpectedType) {

        int typeSize = machineModel.getSizeof(pExpectedType);
        int subscriptOffset = pSubscript * typeSize;

        Address address = pArrayAddress.addOffset(subscriptOffset);

        BigInteger subscript = BigInteger.valueOf(pSubscript);
        CIntegerLiteralExpression litExp =
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, subscript);
        CArraySubscriptExpression arraySubscript =
            new CArraySubscriptExpression(subExpression.getFileLocation(), pExpectedType, subExpression, litExp);

        Object value = modelAtEdge.getValueFromMemory(arraySubscript, address);

        if (value == null) {
          return false;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = null;

        if (pExpectedType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) pExpectedType).getType(), value);
        } else {
          valueAddress = Address.valueOf(value);
          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress);
        }

        boolean contin = false;

        if (!valueLiteral.isUnknown()) {

          SubExpressionValueLiteral subExpressionValueLiteral =
              new SubExpressionValueLiteral(valueLiteral, arraySubscript);

          valueLiterals.addSubExpressionValueLiteral(subExpressionValueLiteral);

          /*Stop, because it is highly
           * unlikely that following values can be identified*/
          contin = true;
        }

        if (valueAddress != null) {
          Pair<CType, Address> visits = Pair.of(pExpectedType, valueAddress);

          if (visited.contains(visits)) {
            return false;
          }

          visited.add(visits);

          ValueLiteralVisitor v = new ValueLiteralVisitor(valueAddress, valueLiterals, arraySubscript, visited);
          pExpectedType.accept(v);
        }

        return contin;
      }

      @Override
      public Void visit(CPointerType pointerType) throws RuntimeException {

        CType expectedType = pointerType.getType().getCanonicalType();

        CPointerExpression pointerExp = new CPointerExpression(subExpression.getFileLocation(), expectedType, subExpression);

        Object value = modelAtEdge.getValueFromMemory(pointerExp, address);

        if (value == null) {
          if(isStructOrUnionType(expectedType)) {
            handleFieldPointerDereference(expectedType, pointerExp);
          }
          return null;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = null;

        if (expectedType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) expectedType).getType(), value);
        } else {
          valueAddress = Address.valueOf(value);
          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress);
        }

        if (!valueLiteral.isUnknown()) {

          SubExpressionValueLiteral subExpressionValueLiteral =
              new SubExpressionValueLiteral(valueLiteral, pointerExp);

          valueLiterals.addSubExpressionValueLiteral(subExpressionValueLiteral);
        }

        if (valueAddress != null) {

          Pair<CType, Address> visits = Pair.of(expectedType, valueAddress);

          if (visited.contains(visits)) {
            return null;
          }

          /*Tell all instanced visitors that you visited this memory location*/
          visited.add(visits);

          ValueLiteralVisitor v = new ValueLiteralVisitor(valueAddress, valueLiterals, pointerExp, visited);
          expectedType.accept(v);

        }

        return null;
      }

      private void handleFieldPointerDereference(CType pExpectedType, CExpression pointerExpression) {
        /* a->b <=> *(a).b */

        ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, pointerExpression, visited);
        pExpectedType.accept(v);
      }
    }

    /*Resolve structs or union fields that are stored in the variable environment*/
    private class ValueLiteralStructResolver extends DefaultCTypeVisitor<Void, RuntimeException> {

      private final ValueLiterals valueLiterals;
      private final String functionName;
      private final CExpression prevSub;

      public ValueLiteralStructResolver(ValueLiterals pValueLiterals,
          String pFunctionName, CFieldReference pPrevSub) {
        valueLiterals = pValueLiterals;
        functionName = pFunctionName;
        prevSub = pPrevSub;
      }

      public ValueLiteralStructResolver(ValueLiterals pValueLiterals, String pFunctionName, CIdExpression pOwner) {
        valueLiterals = pValueLiterals;
        functionName = pFunctionName;
        prevSub = pOwner;
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

        // Can't have pointer dereferences here.
        CFieldReference reference =
            new CFieldReference(prevSub.getFileLocation(), pMemberType, pFieldName, prevSub, false);

        FieldReference fieldReferenceName = getFieldReference(reference, functionName);

        if (modelAtEdge.hasValueForLeftHandSide(fieldReferenceName)) {
          Object referenceValue = modelAtEdge.getVariableValue(fieldReferenceName);
          addStructSubexpression(referenceValue, reference);
        }

        ValueLiteralStructResolver resolver =
            new ValueLiteralStructResolver(valueLiterals,
                functionName, reference);

        pMemberType.accept(resolver);
      }

      private void addStructSubexpression(Object pFieldValue, CFieldReference reference) {

        CType realType = reference.getExpressionType();

        ValueLiteral valueLiteral;
        Address valueAddress = null;

        if (realType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) realType).getType(), pFieldValue);
        } else {
          valueAddress = Address.valueOf(pFieldValue);
          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress);
        }


        if (valueLiteral.isUnknown()) {
          return;
        }

        SubExpressionValueLiteral subExpression = new SubExpressionValueLiteral(valueLiteral, reference);
        valueLiterals.addSubExpressionValueLiteral(subExpression);
      }
    }
  }

  public final static class ValueLiterals {

    /*Contains values for possible sub expressions */
    private final List<SubExpressionValueLiteral> subExpressionValueLiterals = new ArrayList<>();

    private final ValueLiteral expressionValueLiteral;

    public ValueLiterals() {
      expressionValueLiteral = UnknownValueLiteral.getInstance();
    }

    public ValueLiterals(ValueLiteral valueLiteral) {
      expressionValueLiteral = valueLiteral;
    }

    public ValueLiteral getExpressionValueLiteral() {
      return expressionValueLiteral;
    }

    public CExpression getExpressionValueLiteralAsCExpression() {
      return expressionValueLiteral.getValueLiteral();
    }

    public void addSubExpressionValueLiteral(SubExpressionValueLiteral code) {
      subExpressionValueLiterals.add(code);
    }

    public boolean hasUnknownValueLiteral() {
      return expressionValueLiteral.isUnknown();
    }

    public Set<SubExpressionValueLiteral> getSubExpressionValueLiteral() {
      return ImmutableSet.copyOf(subExpressionValueLiterals);
    }

    @Override
    public String toString() {

      StringBuilder result = new StringBuilder();

      result.append("ValueLiteral : ");
      result.append(expressionValueLiteral.toString());
      result.append(", SubValueLiterals : ");
      Joiner joiner = Joiner.on(", ");
      result.append(joiner.join(subExpressionValueLiterals));

      return result.toString();
    }
  }

  public static interface ValueLiteral {

    public CExpression getValueLiteral();
    public boolean isUnknown();

    public ValueLiteral addCast(CSimpleType pType);
  }

  public static class UnknownValueLiteral implements ValueLiteral {

    private static final UnknownValueLiteral instance = new UnknownValueLiteral();

    private UnknownValueLiteral() {}

    public static UnknownValueLiteral getInstance() {
      return instance;
    }

    @Override
    public CLiteralExpression getValueLiteral() {
      throw new UnsupportedOperationException("Can't get the value code of an unknown value");
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public ValueLiteral addCast(CSimpleType pType) {
      throw new UnsupportedOperationException("Can't get the value code of an unknown value");
    }

    @Override
    public String toString() {
      return "UNKNOWN";
    }
  }

  public static class ExplicitValueLiteral implements ValueLiteral {

    private final CLiteralExpression explicitValueLiteral;

    protected ExplicitValueLiteral(CLiteralExpression pValueLiteral) {
      explicitValueLiteral = pValueLiteral;
    }

    public static ValueLiteral valueOf(Address address) {

      Number number = address.getAsNumber();

      if (number instanceof BigInteger) {
        CLiteralExpression lit = new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.LONG_LONG_INT, (BigInteger) number);
        return new ExplicitValueLiteral(lit);
      } else if (number instanceof BigDecimal) {
        CLiteralExpression lit = new CFloatLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.LONG_DOUBLE, (BigDecimal) number);
        return new ExplicitValueLiteral(lit);
      } else {

        BigDecimal val = BigDecimal.valueOf(number.doubleValue());
        CLiteralExpression lit =
            new CFloatLiteralExpression(FileLocation.DUMMY, CNumericTypes.LONG_DOUBLE, val);
        return new ExplicitValueLiteral(lit);
      }
    }

    protected ExplicitValueLiteral(CLiteralExpression pValueLiteral, CCastExpression pCastedValue) {
      explicitValueLiteral = pValueLiteral;
    }

    @Override
    public ValueLiteral addCast(CSimpleType pType) {

      CExpression castedValue = getValueLiteral();

      CCastExpression castExpression = new CCastExpression(castedValue.getFileLocation(), pType, castedValue);
      return new CastedExplicitValueLiteral(explicitValueLiteral, castExpression);
    }

    public static ValueLiteral valueOf(BigInteger value) {
      CIntegerLiteralExpression literal = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, value);
      return new ExplicitValueLiteral(literal);
    }

    public static ValueLiteral valueOf(BigDecimal value) {

      CFloatLiteralExpression literal = new CFloatLiteralExpression(FileLocation.DUMMY, CNumericTypes.DOUBLE, value);
      return new ExplicitValueLiteral(literal);
    }

    @Override
    public CExpression getValueLiteral() {
      return explicitValueLiteral;
    }

    public CLiteralExpression getExplicitValueLiteral() {
      return explicitValueLiteral;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public String toString() {
      return explicitValueLiteral.toASTString();
    }
  }

  public static final class CastedExplicitValueLiteral extends ExplicitValueLiteral {

    private final CCastExpression castExpression;

    protected CastedExplicitValueLiteral(CLiteralExpression pValueLiteral, CCastExpression exp) {
      super(pValueLiteral);
      castExpression = exp;
    }

    @Override
    public CExpression getValueLiteral() {
      return castExpression;
    }
  }

  public static final class SubExpressionValueLiteral {

    private final ValueLiteral valueLiteral;
    private final CLeftHandSide subExpression;

    private SubExpressionValueLiteral(ValueLiteral pValueLiteral, CLeftHandSide pSubExpression) {
      valueLiteral = pValueLiteral;
      subExpression = pSubExpression;
    }

    public CExpression getValueLiteralAsCExpression() {
      return valueLiteral.getValueLiteral();
    }

    public ValueLiteral getValueLiteral() {
      return valueLiteral;
    }

    public CLeftHandSide getSubExpression() {
      return subExpression;
    }
  }
}