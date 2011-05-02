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
package org.sosy_lab.common.configuration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/** This class collects all @Options of CPAchecker. */
public class OptionCollector {

  /** THe main-method collects all classes of CPAchecker and 
   * then it searches for all @Options.
   * @param args not used */
  public static void main(String[] args) {

    try {
      for (Class<?> c : getClasses()) {
        collectOptions(c);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** This method collects all @Options of a class. 
   * @param c class where to take the @Options from */
  private static void collectOptions(Class<?> c) {
    for (final java.lang.reflect.Field field : c.getDeclaredFields()) {
      if (field.isAnnotationPresent(Option.class)) {
        final Option a = field.getAnnotation(Option.class);

        //TODO: store in file
        if ("".equals(a.name())) {
          System.out.println("\n< no name >");
        } else {
          System.out.println("\n" + a.name());
        }
        System.out.println("  field:     " + field);
        System.out.println("  fieldname: " + field.getName());
        System.out.println("  type:      " + field.getType());
      }
    }
  }

  /**
   * Scans all classes accessible from the context class loader which 
   * belong to the given package and subpackages.
   *
   * @param packageName The base package
   * @return The classes
   * @throws IOException
   */
  private static List<Class<?>> getClasses() throws IOException {
    final ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    final String packageName = "org.sosy_lab.cpachecker";
    final String path = packageName.replace(".", "/");
    final Enumeration<URL> resources = classLoader.getResources(path);
    final List<Class<?>> classes = new LinkedList<Class<?>>();

    while (resources.hasMoreElements()) {
      final File file = new File(resources.nextElement().getFile());
      collectClasses(file, packageName, classes);
    }

    return classes;
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory   The base directory
   * @param packageName The package name for classes found inside the base directory
   * @param classes     List where the classes are added.
   */
  private static void collectClasses(final File directory,
      final String packageName, final List<Class<?>> classes) {
    if (directory.exists()) {
      for (final File file : directory.listFiles()) {
        final String fileName = file.getName();
        if (file.isDirectory()) {
          assert !fileName.contains(".");
          collectClasses(file, packageName + "." + fileName, classes);

        } else if (fileName.endsWith(".class")

            // exclude some problematic files of Octagon and some testfiles
            // TODO: are excluded files important?
            && !fileName.contains("Octagon")
            && !fileName.contains("OctWrapper") && !fileName.contains("Test")
            && !fileName.contains("test")) {

          try {
            final Class<?> foundClass =
                Class.forName(packageName + '.'
                    + fileName.substring(0, fileName.length() - 6));

            // collect only real classes
            if (!Modifier.isAbstract(foundClass.getModifiers())
                && !Modifier.isInterface(foundClass.getModifiers())) {
              classes.add(foundClass);
            }
          } catch (ClassNotFoundException e) {
            /* ignore, there is no class available for this file */
          }
        }
      }
    }
  }

}
