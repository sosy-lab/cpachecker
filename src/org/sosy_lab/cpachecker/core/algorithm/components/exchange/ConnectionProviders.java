// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ConnectionProviders {

  /**
   *
   * @param clazz ConnectionProvider that is responsible for creating the list
   * @param numberConnections number of Connections to generate
   * @param <C> explicit type of the ConnectionProvider
   * @return list of connections
   */
  public static <C extends ConnectionProvider<?>> List<? extends Connection> getConnections(Class<C> clazz, int numberConnections)
      throws InstantiationException, IllegalAccessException, NoSuchMethodException,
             InvocationTargetException, IOException {
    C connectionProvider = clazz.getDeclaredConstructor().newInstance();
    return connectionProvider.createConnections(numberConnections);
  }

}
