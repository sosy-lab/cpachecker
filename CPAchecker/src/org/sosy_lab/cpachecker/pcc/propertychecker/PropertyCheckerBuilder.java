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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;


public class PropertyCheckerBuilder {

  private static final String PROPERTYCHECKER_CLASS_PREFIX = "org.sosy_lab.cpachecker.pcc.propertychecker";

  public static PropertyChecker buildPropertyChecker(String propCheckerClassName, String pCheckerParamList)
      throws InvalidConfigurationException {
    if (propCheckerClassName == null) { throw new InvalidConfigurationException(
        "No property checker defined."); }

    Class<?> propertyCheckerClass;
    try {
      propertyCheckerClass = Classes.forName(propCheckerClassName, PROPERTYCHECKER_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Class for property checker  " + propCheckerClassName + " is unknown.", e);
    }

    if (!PropertyChecker.class.isAssignableFrom(propertyCheckerClass)) { throw new InvalidConfigurationException(
        "Option propertychecker.className must be set to a class implementing the PropertyChecker interface!"); }

    // get list of parameters
    String[] param;

    if (pCheckerParamList.equals("")) {
      param = new String[0];
    } else {
      String[] result = pCheckerParamList.split(",", -1);
      param = new String[result.length - 1];
      for (int i = 0; i < param.length; i++) {
        param[i] = result[i];
      }
    }
    // construct property checker instance
    try {
      Constructor<?>[] cons = propertyCheckerClass.getConstructors();

      Class<?>[] paramTypes;
      Constructor<?> constructor = null;
      for (Constructor<?> con : cons) {
        paramTypes = con.getParameterTypes();
        if (paramTypes.length != param.length) {
          continue;
        } else {
          for (Class<?> paramType : paramTypes) {
            if (paramType != String.class) {
              continue;
            }
          }
        }
        constructor = con;
        break;
      }

      if (constructor == null) { throw new UnsupportedOperationException(
          "Cannot create PropertyChecker " + propCheckerClassName + " if it does not provide a constructor with "
              + param.length + " String parameters."); }

      return (PropertyChecker) constructor.newInstance((Object[]) param);
    } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }


}
