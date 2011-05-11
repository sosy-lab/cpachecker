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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/** This class collects all @Options of CPAchecker. */
public class OptionCollector {

  /** The main-method collects all classes of CPAchecker and
   * then it searches for all @Options.
   * @param args not used */
  public static void main(final String[] args) {
    final LinkedList<String> list = new LinkedList<String>();

    try {
      for (Class<?> c : getClasses()) {
        collectOptions(c, list);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    String[] sortedList = list.toArray(new String[0]);
    java.util.Arrays.sort(sortedList);
    for (String s : sortedList) {
      System.out.println(s);
    }
  }

  /** This method collects all @Options of a class.
   * @param c class where to take the @Options from
   * @param list list with collected options */
  private static void collectOptions(final Class<?> c, final List<String> list) {
    for (final Field field : c.getDeclaredFields()) {

      if (field.isAnnotationPresent(Option.class)) {
        StringBuilder optionInfo = new StringBuilder("");

        // get prefix from Options-annotation of class
        if (c.isAnnotationPresent(Options.class)) {
          final Options classOption = c.getAnnotation(Options.class);
          if (!classOption.prefix().isEmpty()) {
            optionInfo.append(classOption.prefix() + ".");
          }
        }

        // get info about option
        final Option option = field.getAnnotation(Option.class);
        if (option.name().isEmpty()) {
          optionInfo.append(field.getName() + "\n");
        } else {
          optionInfo.append(option.name() + "\n");
        }

        optionInfo.append("  field:    " + field.getName() + "\n");
        optionInfo.append("  class:    "
            + field.getDeclaringClass().toString().substring(6) + "\n");

        final String simpleType = field.getType().getSimpleName();
        optionInfo.append("  type:     " + simpleType + "\n");

        if (simpleType.equals("boolean") || simpleType.equals("String")) {
          optionInfo.append(getDefaultValue(field));
        }
        optionInfo.append(getAllowedValues(field.getType(), option));

        list.add(optionInfo.toString());
      }
    }
  }

  /** This function searches for the default field values of an Options
   * in the sourcefile of the actual field/class and returns it
   * or an emtpy String, if the value not found.
   * 
   * This part only works, if you have the source code of CPAchecker.
   * 
   * @param field where to get the default value */
  private static String getDefaultValue(final Field field) {
    String defaultValueLine = "";
    String filename = "";
    try {

      // get filename of java-file
      String cpacheckerPath = new File("").getCanonicalPath();
      filename =
          field.getDeclaringClass().toString().substring(6).replace(".", "/");

      // encapsulated classes have a "$" in filename
      if (filename.contains("$")) {
        filename = filename.substring(0, filename.indexOf("$"));
      }

      // get content of file, filename is in '/src/' and ends with '.java'
      final BufferedReader reader =
          new BufferedReader(new InputStreamReader(new FileInputStream(
              new File(cpacheckerPath + "/src/" + filename + ".java"))));
      final StringBuilder contentOfFile = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        contentOfFile.append(line);
      }
      final String content = contentOfFile.toString();

      // get declaration of field from file
      // example fieldString: 'private boolean shouldCheck'
      final String fieldString =
          Modifier.toString(field.getModifiers()) + " "
              + field.getType().getSimpleName() + " " + field.getName();

      // search for fieldString and get the whole content after it (=rest),
      // in 'rest' search for ';' and return all before it (=defaultValue)
      if (content.contains(fieldString)) {
        final String rest = content.substring(content.indexOf(fieldString));
        String defaultValue =
            rest.substring(fieldString.length(), rest.indexOf(";")).trim();

        // remove unnecessary parts of field
        if (defaultValue.startsWith("=")) {
          defaultValue = defaultValue.substring(1);

          // remove comments
          while (defaultValue.contains("/*")) {
            defaultValue =
                defaultValue.substring(0, defaultValue.indexOf("/*"))
                    + defaultValue.substring(defaultValue.indexOf("*/") + 2);
          }
          if (defaultValue.contains("//")) {
            defaultValue =
                defaultValue.substring(0, defaultValue.indexOf("//"));
          }

          // create output
          if (!defaultValue.isEmpty()) {
            defaultValueLine = "  default value: " + defaultValue.trim() + "\n";
          }
        }
      }
    } catch (IOException e) {
      // if file not found or not readable, do nothing,
      // default values are only additional information
    }
    return defaultValueLine;
  }

  /** This function return the allowed values or interval for a field.
   * @param type the type of the field
   * @param option the option-annotation of the field */
  private static String getAllowedValues(final Class<?> type,
      final Option option) {
    String allowedValues = "";

    // if the type is int, long or enum,
    // the allowed values can be extracted from option or the enum-class
    /*
    if (type == int.class || type == long.class) {
      allowedValues =
          "  interval: [" + option.min() + ", " + option.max() + "]\n";

    } else 
    */
    if (type.isEnum()) {
      try {
        final Field[] enums =
            Class.forName(type.toString().substring(6)).getFields();
        final String[] enumTitles = new String[enums.length];
        for (int i = 0; i < enums.length; i++) {
          enumTitles[i] = enums[i].getName();
        }
        allowedValues =
            "  enum:     " + java.util.Arrays.toString(enumTitles) + "\n";
      } catch (ClassNotFoundException e) {
        // ignore, exception should not happen
      }
    }

    // sometimes the allowed values are part of the option-annotation
    if (option.values().length != 0) {
      allowedValues +=
          "  allowed values: " + java.util.Arrays.toString(option.values())
              + "\n";
    }

    // sometimes the allowed values must match a regexp
    if (!option.regexp().isEmpty()) {
      allowedValues += "  regexp:   " + option.regexp() + "\n";
    }
    
    // sometimes the allowed values must be uppercase
    if (option.toUppercase()) {
      allowedValues += "  uppercase: true\n";
    }

    return allowedValues;
  }

  /**
   * Scans all classes accessible from the context class loader which
   * belong to the given package and subpackages.
   *
   * @return The classes
   * @throws IOException
   */
  private static List<Class<?>> getClasses() throws IOException {
    final ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    final Enumeration<URL> resources = classLoader.getResources("");
    final List<Class<?>> classes = new LinkedList<Class<?>>();

    while (resources.hasMoreElements()) {
      final File file = new File(resources.nextElement().getFile());
      collectClasses(file, "", classes);
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

          if (packageName.isEmpty()) {
            collectClasses(file, fileName, classes);
          } else {
            collectClasses(file, packageName + "." + fileName, classes);
          }

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
