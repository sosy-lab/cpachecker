// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;


public class FirstPartRenamingFct implements java.util
    .function.Function<String,
    String> {
  String[] arrayVariablesForFormulasHere;
  String[] otherArrayVariables;

  FirstPartRenamingFct(String[]
      arrayVariablesThatAreNotUsedInBothParts, String[] variablesToChange){
    arrayVariablesForFormulasHere = variablesToChange;
    otherArrayVariables = arrayVariablesThatAreNotUsedInBothParts;
  }
  @Override public String apply (String name){
    StringBuilder myBuilder = new StringBuilder();
    myBuilder.append(name);
    boolean equalsOtherArrayVariable = true;
    for (int i = 0; i < arrayVariablesForFormulasHere.length;
         i++){
      if (name.equals(arrayVariablesForFormulasHere[i])){
        myBuilder.append("#");
        while (equalsOtherArrayVariable) {
          equalsOtherArrayVariable = false;
          String currentVariableName = myBuilder.toString();
          for (int j = 0; j < otherArrayVariables.length; j++) {
            if (currentVariableName.equals(otherArrayVariables[j])) {
              myBuilder.append("#");
              equalsOtherArrayVariable = true;
              break;
            }
          }
        }

      }
    }
    return myBuilder.toString();
  }
}
