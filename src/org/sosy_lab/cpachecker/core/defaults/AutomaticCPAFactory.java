/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * CPAFactory implementation that can be used to automatically instantiate
 * classes with a single constructor that has parameters.
 * 
 * Parameters can be marked as optional with an annotation to specify that the
 * factory may pass null for them.
 */
public class AutomaticCPAFactory implements CPAFactory {
  
  /**
   * Marker interface for optional constructor parameters.
   * The factory may decide to pass null for such parameters.
   */
  @Target(ElementType.PARAMETER)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Optional { }
  
  private final Class<? extends ConfigurableProgramAnalysis> type;
  private final ClassToInstanceMap<Object> injects = MutableClassToInstanceMap.create();
  
  public static CPAFactory forType(Class<? extends ConfigurableProgramAnalysis> type) {
    return new AutomaticCPAFactory(type);
  }
  
  public AutomaticCPAFactory(Class<? extends ConfigurableProgramAnalysis> type) {
    this.type = type;
  }
  
  @Override
  public ConfigurableProgramAnalysis createInstance()
      throws InvalidConfigurationException, CPAException {
    
    Constructor<?>[] allConstructors = type.getDeclaredConstructors();
    if (allConstructors.length != 1) {
      // TODO if necessary, provide method which constructor should be chosen
      // or choose automatically
      throw new UnsupportedOperationException("Cannot automatically create CPAs " +
      		"with more than one constructor!");
    }
    Constructor<?> cons = allConstructors[0];
    cons.setAccessible(true);
        
    Class<?> formalParameters[] = cons.getParameterTypes();
    Annotation parameterAnnotations[][] = cons.getParameterAnnotations();
    
    Object actualParameters[] = new Object[formalParameters.length];
    for (int i = 0; i < formalParameters.length; i++) {
      Class<?> formalParam = formalParameters[i];
      Object actualParam = get(formalParam);

      boolean optional = false;
      for (Annotation a : parameterAnnotations[i]) {
        if (a instanceof Optional) {
          optional = true;
          break;
        }
      }
      
      if (!optional) {
        Preconditions.checkNotNull(actualParam,
            formalParam.getSimpleName() + " instance needed to create " + type.getSimpleName() + "-CPA!");
      }
      actualParameters[i] = actualParam;
    }
    
    try {
      return type.cast(cons.newInstance(actualParameters));
    } catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, CPAException.class, InvalidConfigurationException.class);
      throw new RuntimeException("Unexpected checked exception", t);

    } catch (InstantiationException e) {
      throw new UnsupportedOperationException("Cannot automatically create CPAs " +
          "that are declared abstract!");

    } catch (IllegalAccessException e) {
      throw new UnsupportedOperationException("Cannot automatically create CPAs " +
          "without an accessible constructor!");
    }
  }

  @Override
  public CPAFactory setLogger(LogManager pLogger) {
    return set(pLogger, LogManager.class);
  }

  @Override
  public CPAFactory setConfiguration(Configuration pConfiguration) {
    return set(pConfiguration, Configuration.class);
  }

  @Override
  public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
      throws UnsupportedOperationException {
    return set(pChild, ConfigurableProgramAnalysis.class);
  }
  
  @Override
  public <T> CPAFactory set(T obj, Class<T> cls) throws UnsupportedOperationException {
    Preconditions.checkNotNull(cls);
    Preconditions.checkNotNull(obj);
    Preconditions.checkState(!injects.containsKey(cls),
        "Cannot store two objects of class " + cls.getSimpleName());

    injects.putInstance(cls, obj);
    return this;
  }

  public <T> T get(Class<T> cls) {
    return injects.getInstance(cls);
  }
  
  @Override
  public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot automatically create CPAs " +
      "with multiple children CPAs!");
  }
}