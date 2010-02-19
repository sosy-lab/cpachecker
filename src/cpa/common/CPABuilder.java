/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import exceptions.CPAException;
import exceptions.InvalidConfigurationException;

public class CPABuilder {

  private final CPAConfiguration config;
  private final LogManager logger;

  public CPABuilder(CPAConfiguration pConfig, LogManager pLogger) {
    this.config = pConfig;
    this.logger = pLogger;
  }

  public ConfigurableProgramAnalysis buildCPAs() throws CPAException {

    String cpaName = config.getProperty("cpa");
    if (cpaName == null) {
      throw new InvalidConfigurationException("Option cpa is not set in the configuration file!");
    }

    return buildCPAs(cpaName, "cpa", new HashSet<String>());
  }

  private ConfigurableProgramAnalysis buildCPAs(String optionValue, String optionName, Set<String> usedAliases) throws CPAException {
    Preconditions.checkNotNull(optionValue);
    
    // parse option (may be of syntax "classname alias"
    String[] optionParts = optionValue.trim().split("\\s+");
    String cpaName = optionParts[0];
    String cpaAlias = getCPAAlias(optionValue, optionName, optionParts, cpaName);      
    
    if (!usedAliases.add(cpaAlias)) {
      throw new InvalidConfigurationException("Alias " + cpaAlias + " used twice for a CPA.");
    }
    
    // first get instance of appropriate factory
    
    Class<?> cpaClass = getCPAClass(optionName, cpaName);

    Method factoryMethod = getFactoryMethod(cpaName, cpaClass);

    CPAFactory factory = getFactoryInstance(cpaName, factoryMethod);
    
    // now use factory to get an instance of the CPA
    
    factory.setConfiguration(new CPAConfiguration(config, cpaAlias));
    factory.setLogger(logger);
    
    createAndSetChildrenCPAs(cpaName, cpaAlias, factory, usedAliases);
    
    // finally call createInstance
    ConfigurableProgramAnalysis cpa;
    try {
      cpa = factory.createInstance();
    } catch (IllegalStateException e) { 
      throw new InvalidConfigurationException(e.getMessage());
    }
    return cpa;
  }

  private String getCPAAlias(String optionValue, String optionName,
      String[] optionParts, String cpaName) throws InvalidConfigurationException {
    
    if (optionParts.length == 1) {
      // no user-specified alias, use last part of class name
      int dotIndex = cpaName.lastIndexOf('.');
      return (dotIndex >= 0 ? cpaName.substring(dotIndex+1) : cpaName);

    } else if (optionParts.length == 2) {
      return optionParts[1];
    
    } else {
      throw new InvalidConfigurationException("Option " + optionName + " contains invalid CPA specification \"" + optionValue + "\"!");
    }
  }

  private Class<?> getCPAClass(String optionName, String cpaName) throws InvalidConfigurationException {
    Class<?> cpaClass;
    try {
      cpaClass = Class.forName(cpaName);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException("Option " + optionName + " is set to unknown CPA " + cpaName);
    }

    if (!ConfigurableProgramAnalysis.class.isAssignableFrom(cpaClass)) {
      throw new InvalidConfigurationException(
        "Option " + optionName + " has to be set to a class implementing the ConfigurableProgramAnalysis interface!");
    }
    return cpaClass;
  }

  private Method getFactoryMethod(String cpaName, Class<?> cpaClass) throws CPAException {
    try {
      return cpaClass.getMethod("factory", (Class<?>[]) null);
    } catch (NoSuchMethodException e) {
      throw new CPAException("Each CPA class has to offer a public static method factory with zero parameters, but " + cpaName + " does not!");
    }
  }

  private CPAFactory getFactoryInstance(String cpaName, Method factoryMethod) throws CPAException {
    Object factoryObj;
    try {
       factoryObj = factoryMethod.invoke(null, (Object[])null);
    } catch (IllegalArgumentException e) {
      // method is not static
      throw new CPAException("Each CPA class has to offer a public static method factory with zero parameters, but " + cpaName + " does not!");
    } catch (IllegalAccessException e) {
      // method is not public
      throw new CPAException("Each CPA class has to offer a public static method factory with zero parameters, but " + cpaName + " does not!");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Error) {
        throw (Error)cause; // errors should never be caught
      }
      logger.logException(Level.FINE, cause, "CPA factory methods should never throw an exception!");
      throw new CPAException("Cannot create CPA because of unexpected exception: " + cause.getMessage());
    }
    
    if ((factoryObj == null) || !(factoryObj instanceof CPAFactory)) {
      throw new CPAException("The factory method of a CPA has to return an instance of CPAFactory!");
    }
    
    return (CPAFactory)factoryObj;
  }

  private void createAndSetChildrenCPAs(String cpaName, String cpaAlias,
      CPAFactory factory, Set<String> usedAliases) throws InvalidConfigurationException, CPAException {
    String childOptionName = cpaAlias + ".cpa";
    String childrenOptionName = cpaAlias + ".cpas";
    String childCpaName = config.getProperty(childOptionName);
    String childrenCpaNames = config.getProperty(childrenOptionName);
    
    if (childCpaName != null) {
      // only one child CPA
      if (childrenCpaNames != null) {
        throw new InvalidConfigurationException("Ambiguous configuration: both "
            + childOptionName + " and " + childrenOptionName + " are specified!");
      }
      
      try {
        factory.setChild(buildCPAs(childCpaName, childOptionName, usedAliases));
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(cpaName + " is no wrapper CPA, but option " + childOptionName + " was specified!");
      }
    
    } else if (childrenCpaNames != null) {
      // several children CPAs
      ImmutableList.Builder<ConfigurableProgramAnalysis> childrenCpas = ImmutableList.builder();
      
      for (String currentChildCpaName : childrenCpaNames.split("\\s*,\\s*")) {
        childrenCpas.add(buildCPAs(currentChildCpaName, childrenOptionName, usedAliases));
      }
      
      try {
        factory.setChildren(childrenCpas.build());
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(cpaName + " is no wrapper CPA, but option " + childrenOptionName + " was specified!");
      }
    }
  }
}