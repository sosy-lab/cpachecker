// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rtt;

import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;

/**
 * Class for creation of fully qualified names of object fields.
 */
public class NameProvider {

  private static final NameProvider SINGLETON = new NameProvider();
  private static final String VARIABLE_DELIMITER = "::";
  private static final String INSTANCE_DELIMITER = "_";

  private NameProvider() {
    // DO NOTHING
  }

  public static NameProvider getInstance() {
    return SINGLETON;
  }

  public String getObjectScope(RTTState rttState, String methodName,
      JIdExpression notScopedField) {

    // Could not resolve var
    if (notScopedField.getDeclaration() == null) {
      return null;
    }

    if (notScopedField instanceof JFieldAccess) {
      String scopedFieldName = getScopedFieldName((JFieldAccess) notScopedField, methodName, rttState);

      if (rttState.contains(scopedFieldName)) {
        return rttState.getUniqueObjectFor(scopedFieldName);
      } else {
        return null;
      }
    } else {
      if (rttState.contains(RTTState.KEYWORD_THIS)) {
        return rttState.getUniqueObjectFor(RTTState.KEYWORD_THIS);
      } else {
        return null;
      }
    }
  }

  private String getScopedFieldName(JFieldAccess pFieldAccess, String pMethodName, RTTState pRttState) {
    JIdExpression qualifier = pFieldAccess.getReferencedVariable();

    JSimpleDeclaration declaration = qualifier.getDeclaration();
    String qualifierScope = getObjectScope(pRttState, pMethodName, qualifier);

    return getScopedVariableName(declaration, pMethodName, qualifierScope);
  }

  public String getScopedVariableName(String pVariableName, String pFunctionName, String pUniqueObject,
      RTTState pState) {

    if (pVariableName.equals(RTTState.KEYWORD_THIS)) {
      return pVariableName;
    }

    if (pState.isKnownAsStatic(pVariableName)) {
      return pVariableName;
    }

    if (pState.isKnownAsDynamic(pVariableName)) {
      return pUniqueObject + VARIABLE_DELIMITER + pVariableName;
    }

    return pFunctionName + VARIABLE_DELIMITER + pVariableName;
  }


  public String getScopedVariableName(JSimpleDeclaration pDeclaration, String functionName, String uniqueObject) {

    String variableName = pDeclaration.getName();

    if (pDeclaration instanceof JFieldDeclaration && ((JFieldDeclaration) pDeclaration).isStatic()) {
      return variableName;

    } else if (pDeclaration instanceof JFieldDeclaration) {
      return uniqueObject + VARIABLE_DELIMITER + variableName;
    } else {
      return functionName + VARIABLE_DELIMITER + variableName;
    }
  }

  public String getUniqueObjectName(String pJavaRunTimeClassName, String pId) {
    return pJavaRunTimeClassName + INSTANCE_DELIMITER + pId;
  }
}
