/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
