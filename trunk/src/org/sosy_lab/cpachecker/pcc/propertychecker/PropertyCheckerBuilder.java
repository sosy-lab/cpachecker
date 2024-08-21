// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import com.google.common.base.Splitter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

public final class PropertyCheckerBuilder {

  private PropertyCheckerBuilder() {}

  public static PropertyChecker buildPropertyChecker(
      Class<? extends PropertyChecker> propertyCheckerClass, String pCheckerParamList)
      throws InvalidConfigurationException {
    // get list of parameters
    String[] param;

    if (pCheckerParamList.isEmpty()) {
      param = new String[0];
    } else {
      List<String> result = Splitter.on(',').splitToList(pCheckerParamList);
      param = result.subList(0, result.size() - 1).toArray(new String[0]);
    }

    // construct property checker instance
    try {
      Class<?>[] paramTypes = new Class<?>[param.length];
      Arrays.fill(paramTypes, String.class);
      Constructor<? extends PropertyChecker> constructor =
          propertyCheckerClass.getConstructor(paramTypes);
      return constructor.newInstance((Object[]) param);

    } catch (NoSuchMethodException e) {
      throw new InvalidConfigurationException(
          String.format(
              "Amount of %d given parameters in option cpa.propertychecker.parameters does not"
                  + " match any constructor of given property checker %s.",
              param.length, propertyCheckerClass.getName()),
          e);
    } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }
}
