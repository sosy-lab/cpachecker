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
import java.util.Collection;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;

import com.google.common.collect.Multimap;


public class ConcreteState {

  private static final String ADDRESS_PREFIX = "__ADDRESS_OF_";

  private final Map<String, Assignment> variableModel;


  private final Multimap<String, Assignment> uFModel;


  private final Map<String, Object> variableAddressMap;

  public ConcreteState(Map<String, Assignment> pVariableModel,
      Multimap<String, Assignment> pUFModel,
      Map<String, Object> pVariableAddressMap) {
    variableAddressMap = pVariableAddressMap;
    uFModel = pUFModel;
    variableModel = pVariableModel;
  }

  public Object getValueFromUF(CType pType, Address address) {

    CType type = pType.getCanonicalType();

    String ufName = getUFName(type);

    if (ufName == null) {
      return null;
    }

    Collection<Assignment> assignments = uFModel.get(ufName);

    for (Assignment assignment : assignments) {
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
    CType type = pType.getCanonicalType(false, false);

    //TODO Seems to work, for now
    //TODO ugly ... rewrite
    String name = type.toString().replace("volatile ", "").replace("const ", "").replace(" ", "_");

//    String name = type.accept(new TypeUFNameVisitor());

    if (name == null) {
      return null;
    }

    return "*" + name;
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
        return ((BigDecimal) pArgument).compareTo(new BigDecimal(addressAsNumber.toString())) == 0;
      }

      if (symbolicAddress instanceof BigDecimal && pArgument instanceof Number) {
        return ((BigDecimal) symbolicAddress).compareTo(new BigDecimal(((Number) pArgument).toString())) == 0;
      }

      return pArgument.equals(symbolicAddress);
    }

    public boolean isNumericalType() {
      return addressAsNumber != null;
    }

    public Address addOffset(Number pOffset) {

      if (addressAsNumber == null) {
        throw new IllegalStateException(
          "Can't add offsets to a non numerical type of address");
      }

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

    public Number getAsNumber() {
      return addressAsNumber;
    }
  }
}