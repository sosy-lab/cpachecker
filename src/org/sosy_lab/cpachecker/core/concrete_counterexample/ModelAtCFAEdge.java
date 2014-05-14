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
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.Model.Function;

import com.google.common.collect.Multimap;


public class ModelAtCFAEdge {

  private static final String ADDRESS_PREFIX = "__ADDRESS_OF_";

  private final Map<String, Assignment> variableModel;
  private final Multimap<String, Assignment> uFModel;
  private final Map<String, Object> variableAddressMap;

  public ModelAtCFAEdge(Map<String, Assignment> pVariableModel,
      Multimap<String, Assignment> pUFModel,
      Map<String, Object> pVariableAddressMap) {
    variableAddressMap = pVariableAddressMap;
    uFModel = pUFModel;
    variableModel = pVariableModel;
  }

  public Object getValueFromUF(CType type, Address address) {

    String ufName = getUFName(type);

    if (ufName == null) {
      return null;
    }

    for (Assignment assignment : uFModel.get(ufName)) {
      Function function = (Function) assignment.getTerm();

      if (function.getArity() != 1) {
        break;
      }

      if (address.comparesToUFArgument(function.getArgument(0))) {
        return assignment.getValue();
      }
    }
    return null;
  }

  private String getUFName(CType pType) {
    String name = pType.getCanonicalType().accept(new TypeUFNameVisitor());

    if (name == null) {
      return null;
    }

    return "*" + name;
  }

  private class TypeUFNameVisitor implements CTypeVisitor<String, RuntimeException> {

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

  public boolean containsVariableName(String variableName) {
    return variableModel.containsKey(variableName);
  }

  public Object getVariableValue(String variableName) {
    if (variableModel.containsKey(variableName)) {
      return variableModel.get(variableName).getValue();
    } else {
      throw new IllegalArgumentException("Name of variable " + variableName + "not found in model.");
    }
  }

  public boolean containsVariableAddress(String varName) {
    String addressName = getAddressPrefix() + varName;
    return variableAddressMap.containsKey(addressName);
  }

  public Address getVariableAddress(String varName) {
    String addressName = getAddressPrefix() + varName;

    if (variableAddressMap.containsKey(addressName)) {
      Object addressO = variableAddressMap.get(addressName);
      return Address.valueOf(addressO);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static String getAddressPrefix() {
    return ADDRESS_PREFIX;
  }

  @Override
  public String toString() {
    return "ModelAtCFAEdge\n variableModel=" + variableModel + "\n uFModel=" + uFModel + "\n variableAddressMap="
        + variableAddressMap;
  }

  public static class Address {

    private final Number addressAsNumber;

    private final Object symbolicAddress;


    private Address(Object pAddress) {
      symbolicAddress = pAddress;

      if (pAddress instanceof Number) {
        addressAsNumber = (Number) pAddress;
      } else {
        addressAsNumber = null;
      }
    }

    public boolean comparesToUFArgument(Object pArgument) {

      if (pArgument instanceof BigDecimal && this.isNumericalType()) {
        ((BigDecimal) pArgument).compareTo(new BigDecimal(addressAsNumber.toString()));
      }

      if (symbolicAddress instanceof BigDecimal && pArgument instanceof Number) {
        ((BigDecimal) symbolicAddress).compareTo(new BigDecimal(((Number) pArgument).toString()));
      }

      return pArgument.equals(symbolicAddress);
    }

    public boolean isNumericalType() {
      return addressAsNumber != null;
    }

    public Address addOffset(Number pOffset) {

      if (addressAsNumber == null) { throw new IllegalStateException(
          "Can't add offsets to a non numerical type of address"); }

      BigDecimal address = new BigDecimal(pOffset.toString());
      BigDecimal offset = new BigDecimal(addressAsNumber.toString());

      return Address.valueOf(address.add(offset));
    }

    @Override
    public String toString() {
      return "Address = [" + symbolicAddress + "]";
    }

    public static Address valueOf(Object address) {
      return new Address(address);
    }

    public Object getSymbolicValue() {
      return symbolicAddress;
    }
  }
}