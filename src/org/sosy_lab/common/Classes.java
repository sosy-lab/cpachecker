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
package org.sosy_lab.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Helper class for various methods related to handling Java classes and types.
 */
public final class Classes {

  private Classes() { }
  
  /**
   * This method is thought to make handling of exceptions with wrapped exceptions
   * (like InvocationTargetException) easier. If it is possible to throw the
   * passed exception (which should be the cause of the wrapper exception), it
   * throws it.
   * Normally one type of exception is expected. This type has to be passed to
   * this method. Exceptions of this type are thrown in a type-safe way.
   * 
   * If the passed exception is a checked exception, but not of the passed type,
   * it is not possible to throw it. Then this method does nothing and returns
   * normally. The caller should not ignore this, but handle it himself.
   * 
   * @param <T> The type of (checked) exception that the caller expects.
   * @param t The exception to handle (normally a cause of another exception). 
   * @param c The class for T.
   * @throws Error If t is an Error, it is thrown.
   * @throws RuntimeException If t is a RuntimeException, it is thrown. 
   * @throws T If t is a T exception, it is thrown.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> void throwExceptionIfPossible(Throwable t, Class<T> c) throws T {
    if (t instanceof Error) {
      throw (Error)t;
    
    } else if (t instanceof RuntimeException) {
      throw (RuntimeException)t;
    
    } else if (c.isAssignableFrom(t.getClass())) {
      throw (T)t;
      
    }
  }
  
  /**
   * Exception thrown by {@link Classes#createInstance(String, String, Class[], Object[], Class)}.
   */
  public static class ClassInstantiationException extends Exception {

    private static final long serialVersionUID = 7862065219560550275L;
    
    public ClassInstantiationException(String className, String msg) {
      super("Cannot instantiate class " + className + ":" + msg);
    }
  }
  
  /**
   * Creates an instance of class className, passing the objects from argumentList
   * to the constructor and casting the object to class type.
   *
   * @param className The class name.
   * @param prefix An optional package name that is prefixed to the className if the class is not found.
   * @param argumentTypes Array with the types of the parameters of the desired constructor.
   * @param argumentValues Array with the values that will be passed to the constructor.
   * @param type The return type (has to be a super type of the class, of course).
   * @throws ClassInstantiationException If something goes wrong (like class cannot be found or has no constructor).
   * @throws InvocationTargetException If the constructor throws an exception.  
   */
  @SuppressWarnings("unchecked")
  public static <T> T createInstance(String className, String prefix,
      Class<?>[] argumentTypes, Object[] argumentValues, Class<T> type)
      throws ClassInstantiationException, InvocationTargetException {
    try {
      Class<?> cls;
      try {
        cls = Class.forName(className);
      } catch (ClassNotFoundException e1) {
        if (prefix != null && !prefix.isEmpty()) {
          try {
            // try with prefix added
            cls = Class.forName(prefix + "." + className);
          } catch (ClassNotFoundException e2) {
            throw e1; // re-throw original exception to get correct error message
          }
        } else {
          throw e1;
        }
      }
      Constructor<?> ct = cls.getConstructor(argumentTypes);
      Object obj = ct.newInstance(argumentValues);
      if (type.isAssignableFrom(obj.getClass())) {
        return (T)obj;
      } else {
        throw new ClassInstantiationException(className, "Not an instance of " + type.getCanonicalName());
      }
    } catch (ClassNotFoundException e) {
      throw new ClassInstantiationException(className, "Class not found!");
    } catch (SecurityException e) {
      throw new ClassInstantiationException(className, e.getMessage());
    } catch (NoSuchMethodException e) {
      throw new ClassInstantiationException(className, "Matching constructor not found!");
    } catch (InstantiationException e) {
      throw new ClassInstantiationException(className, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ClassInstantiationException(className, e.getMessage());
    } 
  }
}
