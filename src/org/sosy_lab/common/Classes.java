/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import static com.google.common.base.Strings.isNullOrEmpty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Helper class for various methods related to handling Java classes and types.
 */
public final class Classes {

  private Classes() { }

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
   * An exception that should be used if a checked exception is encountered in
   * a situation where it is not excepted
   * (e.g., when getting the result from a {@link Callable} of which you know
   * it shouldn't throw such exceptions).
   */
  public final static class UnexpectedCheckedException extends RuntimeException {

    private static final long serialVersionUID = -8706288432548996095L;

    public UnexpectedCheckedException(String message, Throwable source) {
      super("Unexpected checked exception "
            + source.getClass().getSimpleName()
            + (isNullOrEmpty(message)             ? "" : " during "  + message)
            + (isNullOrEmpty(source.getMessage()) ? "" : ": " + source.getMessage()),
        source);

      assert (source instanceof Exception) && !(source instanceof RuntimeException);
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
  public static <T> T createInstance(String className, String prefix,
      Class<?>[] argumentTypes, Object[] argumentValues, Class<T> type)
      throws ClassInstantiationException, InvocationTargetException {

    return createInstance(className, prefix, argumentTypes, argumentValues, type, null);
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
   * @param cl An optional class loader to load the class (may be null).
   * @throws ClassInstantiationException If something goes wrong (like class cannot be found or has no constructor).
   * @throws InvocationTargetException If the constructor throws an exception.
   */
  public static <T> T createInstance(String className, String prefix,
      Class<?>[] argumentTypes, Object[] argumentValues, Class<T> type,
      ClassLoader cl)
      throws ClassInstantiationException, InvocationTargetException {
    try {
      Class<?> cls = forName(className, prefix, cl);
      return createInstance(cls, argumentTypes, argumentValues, type);

    } catch (ClassNotFoundException e) {
      throw new ClassInstantiationException(className, "Class not found!");
    } catch (SecurityException e) {
      throw new ClassInstantiationException(className, e.getMessage());
    }
  }


  /**
   * Creates an instance of class cls, passing the objects from argumentList
   * to the constructor and casting the object to class type.
   *
   * @param cls The class to instantiate.
   * @param argumentTypes Array with the types of the parameters of the desired constructor.
   * @param argumentValues Array with the values that will be passed to the constructor.
   * @param type The return type (has to be a super type of the class, of course).
   * @param cl An optional class loader to load the class (may be null).
   * @throws ClassInstantiationException If something goes wrong (like class cannot be found or has no constructor).
   * @throws InvocationTargetException If the constructor throws an exception.
   */
  public static <T> T createInstance(Class<?> cls,
      Class<?>[] argumentTypes, Object[] argumentValues, Class<T> type)
      throws ClassInstantiationException, InvocationTargetException {
    try {
      Constructor<?> ct = cls.getConstructor(argumentTypes);
      Object obj = ct.newInstance(argumentValues);
      return type.cast(obj);

    } catch (SecurityException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e.getMessage());
    } catch (NoSuchMethodException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), "Matching constructor not found!");
    } catch (InstantiationException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e.getMessage());
    } catch (ClassCastException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), "Not an instance of " + type.getCanonicalName());
    }
  }

  /**
   * Similar to {@link Class#forName(String)}, but if the class is not found this
   * method re-tries with a package name prefixed.
   *
   * @param name The class name.
   * @param prefix An optional package name as prefix.
   * @return The class object for  name  or  prefix + "." + name
   * @throws ClassNotFoundException If none of the two classes can be found.
   */
  public static Class<?> forName(String name, String prefix) throws ClassNotFoundException, SecurityException {
    return forName(name, prefix, null);
  }

  /**
   * Similar to {@link Class#forName(String)} and {@link ClassLoader#loadClass(String)},
   * but if the class is not found this
   * method re-tries with a package name prefixed.
   *
   * @param name The class name.
   * @param prefix An optional package name as prefix.
   * @param cl An optional class loader to load the class (may be null).
   * @return The class object for  name  or  prefix + "." + name
   * @throws ClassNotFoundException If none of the two classes can be found.
   */
  private static Class<?> forName(String name, String prefix, ClassLoader cl) throws ClassNotFoundException, SecurityException {
    if (cl == null) {
      // use the class loader of this class to simulate the behaviour
      // of Class#forName(String)
      cl = Classes.class.getClassLoader();
    }
    if (prefix == null || prefix.isEmpty()) {
      return cl.loadClass(name);
    }

    try {
      return cl.loadClass(name);

    } catch (ClassNotFoundException e) {
      try {
        return cl.loadClass(prefix + "." + name); // try with prefix added
      } catch (ClassNotFoundException _) {
        throw e; // re-throw original exception to get correct error message
      }
    }
  }
}
