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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

import com.google.common.base.Throwables;

/**
 * Helper class for various methods related to handling Java classes and types.
 */
public final class Classes {

  private Classes() { }

  /**
   * Exception thrown by {@link Classes#createInstance(Class, Class[], Object[], Class)}.
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
  public static <T> T createInstance(Class<? extends T> cls,
      Class<?>[] argumentTypes, Object[] argumentValues, Class<T> type)
      throws ClassInstantiationException, InvocationTargetException {
    try {
      Constructor<? extends T> ct = cls.getConstructor(argumentTypes);
      return ct.newInstance(argumentValues);

    } catch (SecurityException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e.getMessage());
    } catch (NoSuchMethodException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), "Matching constructor not found!");
    } catch (InstantiationException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ClassInstantiationException(cls.getCanonicalName(), e.getMessage());
    }
  }


  /**
   * Creates an instance of class cls, passing the objects from argumentList
   * to the constructor and casting the object to class type.
   *
   * If there is no matching constructor or the the class cannot be instantiated,
   * an InvalidConfigurationException is thrown.
   *
   * @param type The return type (has to be a super type of the class, of course).
   * @param cls The class to instantiate.
   * @param argumentTypes Array with the types of the parameters of the desired constructor (optional).
   * @param argumentValues Array with the values that will be passed to the constructor.
   */
  public static <T> T createInstance(Class<T> type, Class<? extends T> cls, Class<?>[] argumentTypes, Object[] argumentValues) throws InvalidConfigurationException {
    return createInstance(type, cls, argumentTypes, argumentValues, RuntimeException.class);
  }

  /**
   * Creates an instance of class cls, passing the objects from argumentList
   * to the constructor and casting the object to class type.
   *
   * If there is no matching constructor or the the class cannot be instantiated,
   * an InvalidConfigurationException is thrown.
   *
   * @param type The return type (has to be a super type of the class, of course).
   * @param cls The class to instantiate.
   * @param argumentTypes Array with the types of the parameters of the desired constructor (optional).
   * @param argumentValues Array with the values that will be passed to the constructor.
   * @param exceptionType An exception type the constructor is allowed to throw.
   */
  public static <T, X extends Exception> T createInstance(Class<T> type, Class<? extends T> cls, Class<?>[] argumentTypes, Object[] argumentValues, Class<X> exceptionType) throws X, InvalidConfigurationException {
    if (argumentTypes == null) {
      // fill argumenTypes array
      argumentTypes = new Class<?>[argumentValues.length];
      int i = 0;
      for (Object obj : argumentValues) {
        argumentTypes[i++] = obj.getClass();
      }

    } else {
      checkArgument(argumentTypes.length == argumentValues.length);
    }

    String className = cls.getSimpleName();
    String typeName = type.getSimpleName();

    // get constructor
    Constructor<? extends T> ct;
    try {
      ct = cls.getConstructor(argumentTypes);
    } catch (NoSuchMethodException e) {
      throw new InvalidConfigurationException("Invalid " + typeName + " " + className + ", no matching constructor", e);
    }

    // verify signature
    String exception = Classes.verifyDeclaredExceptions(ct, exceptionType, InvalidConfigurationException.class);
    if (exception != null) {
      throw new InvalidConfigurationException("Invalid " + typeName + " " + className + ", constructor declares unsupported checked exception " + exception);
    }

    // instantiate
    try {
      return ct.newInstance(argumentValues);

    } catch (InstantiationException e) {
      throw new InvalidConfigurationException("Invalid " + typeName + " " + className + ", class cannot be instantiated (" + e.getMessage() + ")", e);

    } catch (IllegalAccessException e) {
      throw new InvalidConfigurationException("Invalid " + typeName + " " + className + ", constructor is not accessible", e);

    } catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, exceptionType, InvalidConfigurationException.class);

      throw new UnexpectedCheckedException("instantiation of " + typeName + " " + className, t);
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

  /**
   * Verify that a constructor declares no other checked exceptions except a
   * given type.
   *
   * Returns the name of any violating exception, or null if there is none.
   *
   * @param constructor The constructor to check.
   * @param allowedExceptionType The type of exception that is allowed.
   * @return Null or the name of a declared exception.
   */
  public static String verifyDeclaredExceptions(Constructor<?> constructor, Class<?>... allowedExceptionTypes) {
    return verifyDeclaredExceptions(constructor.getExceptionTypes(), allowedExceptionTypes);
  }

  /**
   * Verify that a method declares no other checked exceptions except a
   * given type.
   *
   * Returns the name of any violating exception, or null if there is none.
   *
   * @param method The method to check.
   * @param allowedExceptionType The type of exception that is allowed.
   * @return Null or the name of a declared exception.
   */
  public static String verifyDeclaredExceptions(Method method, Class<?>... allowedExceptionTypes) {
    return verifyDeclaredExceptions(method.getExceptionTypes(), allowedExceptionTypes);
  }

  private static String verifyDeclaredExceptions(Class<?>[] declaredExceptionTypes, Class<?>[] allowedExceptionTypes) {
    for (Class<?> declaredException : declaredExceptionTypes) {

      if (Exception.class.isAssignableFrom(declaredException)) {
        // it's an exception, not an error

        if (Runtime.class.isAssignableFrom(declaredException)) {
          // it's a runtime exception
          continue;
        }

        boolean ok = false;
        for (Class<?> allowedExceptionType : allowedExceptionTypes) {
          if (allowedExceptionType.isAssignableFrom(declaredException)) {
            ok = true;
            break;
          }
        }

        if (!ok) {
          return declaredException.getSimpleName();
        }
      }
    }
    return null;
  }
}
