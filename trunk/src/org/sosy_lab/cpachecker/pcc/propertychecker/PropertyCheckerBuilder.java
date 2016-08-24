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

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

import java.lang.reflect.Constructor;
import java.util.Arrays;


public class PropertyCheckerBuilder {

  public static PropertyChecker buildPropertyChecker(
      Class<? extends PropertyChecker> propertyCheckerClass, String pCheckerParamList)
      throws InvalidConfigurationException {
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
      Class<?>[] paramTypes = new Class<?>[param.length];
      Arrays.fill(paramTypes, String.class);
      Constructor<? extends PropertyChecker> constructor = propertyCheckerClass.getConstructor(paramTypes);
      return constructor.newInstance((Object[]) param);

    } catch (NoSuchMethodException e) {
      throw new InvalidConfigurationException(String.format(
          "Amount of %d given parameters in option cpa.propertychecker.parameters does not match any constructor of given property checker %s.",
          param.length, propertyCheckerClass.getName()),
          e);
    } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }


}
