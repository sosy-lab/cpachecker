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
import java.math.BigInteger;
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

    private final Object address;

    private Address(Object pAddress) {
      address = pAddress;
    }

    public boolean comparesToUFArgument(Object pArgument) {

      if(address instanceof BigDecimal) {
        if(pArgument instanceof BigDecimal) {
          return ((BigDecimal) address).compareTo((BigDecimal) pArgument) == 0;
        } else if(pArgument instanceof BigInteger) {
          return ((BigDecimal) address).compareTo(BigDecimal.valueOf(((BigInteger) pArgument).longValue())) == 0;
        }
      }

      if(pArgument instanceof BigDecimal && address instanceof BigInteger) {
        return ((BigDecimal) pArgument).compareTo(BigDecimal.valueOf(((BigInteger) address).longValue())) == 0;
      }

      return pArgument.equals(address);
    }

    public boolean isNumericalType() {
      return address instanceof BigDecimal || address instanceof BigInteger;
    }

    public Address addOffset(BigDecimal offset) {

      if (address instanceof BigDecimal) {
        BigDecimal result = ((BigDecimal) address).add(offset);
        return Address.valueOf(result);
      }

      if (address instanceof BigInteger) {
        long offsetL = offset.longValue();

        if (offset.compareTo(BigDecimal.valueOf(offsetL)) == 0) {
          BigInteger result = ((BigInteger) address).add(BigInteger.valueOf(offsetL));
          return Address.valueOf(result);
        } else {
          /*BigInteger will be casted to BigDecimal*/
          BigDecimal result = BigDecimal.valueOf(((BigInteger) address).longValue()).add(offset);
          return Address.valueOf(result);
        }
      }

      throw new IllegalStateException("Can't add offsets to a non numerical type of address");
    }

    @Override
    public String toString() {
      return "Address = [" + address + "]";
    }

    public static Address valueOf(Object address) {
      return new Address(address);
    }

    public Object getSymbolicValue() {
      return address;
    }
  }
}