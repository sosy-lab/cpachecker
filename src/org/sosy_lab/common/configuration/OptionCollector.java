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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.common.Pair;

/** This class collects all {@link Option}s of CPAchecker. */
public class OptionCollector {

  private final static int CHARS_PER_LINE = 75; // for description

  /** The main-method collects all classes of CPAchecker and
   * then it searches for all {@link Option}s.
   *
   * @param args use '-v' for verbose output */
  public static void main(final String[] args) {

    // parse args
    boolean verbose = false;
    for (String arg : args) {
      if ("-v".equals(arg) || "-verbose".equals(arg)) {
        verbose = true;
      }
    }

    System.out.println(getCollectedOptions(verbose));
  }

  /** This function collect options from all classes
   * and return a formatted String. 
   * 
   * @param verbose short or long output? */
  public static String getCollectedOptions(final boolean verbose) {
    // TreeMap for alphabetical order of keys
    final SortedMap<String, Pair<String, String>> map =
        new TreeMap<String, Pair<String, String>>();

    for (Class<?> c : getClasses()) {
      collectOptions(c, map, verbose);
    }

    final StringBuilder content = new StringBuilder();
    String description = "";
    for (Pair<String, String> descriptionAndInfo : map.values()) {
      if (descriptionAndInfo.getFirst().isEmpty()
          || !description.equals(descriptionAndInfo.getFirst())) {
        content.append("\n");
        content.append(descriptionAndInfo.getFirst());
        description = descriptionAndInfo.getFirst();
      }
      content.append(descriptionAndInfo.getSecond());
    }

    return content.toString();
  }

  /** This method collects every {@link Option} of a class.
   *
   * @param c class where to take the Option from
   * @param map map with collected Options 
   * @param verbose short or long output? */
  private static void collectOptions(final Class<?> c,
      final SortedMap<String, Pair<String, String>> map, final boolean verbose) {
    for (final Field field : c.getDeclaredFields()) {

      if (field.isAnnotationPresent(Option.class)) {

        getOptionsDescription(c, map);

        // get info about option
        final String optionName = getOptionName(c, field);
        final String defaultValue = getDefaultValue(field);
        final StringBuilder optionInfo = new StringBuilder();
        optionInfo.append(optionName);

        if (verbose) {
          optionInfo.append("\n  field:    " + field.getName() + "\n");
          optionInfo.append("  class:    "
              + field.getDeclaringClass().toString().substring(6) + "\n");

          final String simpleType = field.getType().getSimpleName();
          optionInfo.append("  type:     " + simpleType + "\n");

          if (!defaultValue.isEmpty()) {
            optionInfo.append("  default value: " + defaultValue + "\n");
          }
        } else {
          if (!defaultValue.isEmpty()) {
            optionInfo.append(" = " + defaultValue);
          }
          optionInfo.append("\n");
        }
        optionInfo.append(getAllowedValues(field, verbose));

        // check if a option was found before, some options are used twice
        if (map.containsKey(optionName)) {
          Pair<String, String> oldValues = map.get(optionName);

          String description = getOptionDescription(field);
          if (!description.equals(oldValues.getFirst())) {
            description += oldValues.getFirst();
          }

          String commonOptionInfo = optionInfo.toString();
          if (!commonOptionInfo.equals(oldValues.getSecond())) {
            commonOptionInfo += oldValues.getSecond();
          }

          map.put(optionName, Pair.of(description, commonOptionInfo));

        } else {
          map.put(optionName,
              Pair.of(getOptionDescription(field), optionInfo.toString()));
        }
      }
    }
  }

  /** This function return the formatted description of an {@link Option}. 
   *
   * @param field field with the option */
  private static String getOptionDescription(final Field field) {
    final Option option = field.getAnnotation(Option.class);
    return formatText(option.description());
  }

  /** This function adds the formatted description of {@link Options} 
   * to the map, if a prefix is defined.
   * 
   * @param c class with options 
   * @param map where the formatted options-description is added */
  private static void getOptionsDescription(final Class<?> c,
      final SortedMap<String, Pair<String, String>> map) {
    if (c.isAnnotationPresent(Options.class)) {
      final Options classOption = c.getAnnotation(Options.class);
      if (!classOption.prefix().isEmpty()
          && !classOption.description().isEmpty()) {
        map.put(classOption.prefix(),
            Pair.of(formatText(classOption.description()), ""));
      }
    }
  }

  /** This function formats text and splits lines, if they are too long. */
  private static String formatText(final String text) {

    // split description into lines
    final String[] lines = text.split("\n");

    // split lines into more lines, if they are too long
    final List<String> splittedLines = new ArrayList<String>();
    for (String line : lines) {
      while (line.length() > CHARS_PER_LINE) {

        int spaceIndex = line.lastIndexOf(" ", CHARS_PER_LINE);
        if (spaceIndex == -1) {
          spaceIndex = line.indexOf(" ");
        }
        if (spaceIndex == -1) {
          spaceIndex = line.length() - 1;
        }

        final String start = line.substring(0, spaceIndex);
        if (!start.isEmpty()) {
          splittedLines.add(start);
        }
        line = line.substring(spaceIndex + 1);
      }
      splittedLines.add(line);
    }

    // remove last element, if empty (useful if previous line is too long)
    if (splittedLines.get(splittedLines.size() - 1).isEmpty()) {
      splittedLines.remove(splittedLines.size() - 1);
    }

    // add "# " before each line
    String formattedLines = "";
    if (!text.isEmpty()) {
      for (String line : splittedLines) {
        formattedLines += "# " + line + "\n";
      }
    }

    return formattedLines;
  }

  /** This function return the name of an {@link Option}. 
   * If no optionname is defined, the name of the field is returned.
   * If a prefix is defined, it is added in front of the name. 
   *
   * @param c class with the field
   * @param field field with the option */
  private static String getOptionName(final Class<?> c, final Field field) {
    String optionName = "";

    // get prefix from Options-annotation of class
    if (c.isAnnotationPresent(Options.class)) {
      final Options classOption = c.getAnnotation(Options.class);
      if (!classOption.prefix().isEmpty()) {
        optionName += classOption.prefix() + ".";
      }
    }

    // get info about option
    final Option option = field.getAnnotation(Option.class);
    if (option.name().isEmpty()) {
      optionName += field.getName();
    } else {
      optionName += option.name();
    }
    return optionName;
  }

  /** This function searches for the default field values of an {@link Option}
   * in the sourcefile of the actual field/class and returns it
   * or an emtpy String, if the value not found.
   *
   * This part only works, if you have the source code of CPAchecker.
   *
   * @param field where to get the default value */
  private static String getDefaultValue(final Field field) {
    final String content = getContentOfFile(field);

    // get declaration of field from file
    // example fieldString: 'private boolean shouldCheck'
    final String fieldString =
        Modifier.toString(field.getModifiers()) + " "
            + field.getType().getSimpleName() + " " + field.getName();

    return getDefaultValueFromContent(content, fieldString);

  }

  /** This function returns the content of a sourcefile as String.
   *
   * @param field the field, the sourcefile belongs to */
  private static String getContentOfFile(final Field field) {

    // get path to CPAchecker
    String cpacheckerPath = "";
    try {
      cpacheckerPath = new File("").getCanonicalPath();
    } catch (IOException e) {
      System.err.println("Could not read canonical path.");
      return "";
    }

    // get name of javafile
    String filename =
        field.getDeclaringClass().toString().substring(6).replace(".", "/");

    // encapsulated classes have a "$" in filename
    if (filename.contains("$")) {
      filename = filename.substring(0, filename.indexOf("$"));
    }

    // get name of source file
    filename = cpacheckerPath + "/src/" + filename + ".java";

    try {
      return com.google.common.io.Files.toString(new File(filename),
          Charset.defaultCharset());
    } catch (IOException e) {
      System.err.println("Could not read file '" + filename + "'.");
      return "";
    }
  }

  /** This function searches for fieldstring in content and 
   * returns the value of the field.
   *
   * @param content sourccode where to search
   * @param fieldString name of the field, which value is returned */
  private static String getDefaultValueFromContent(final String content,
      final String fieldString) {
    // search for fieldString and get the whole content after it (=rest),
    // in 'rest' search for ';' and return all before it (=defaultValue)
    String defaultValue = "";
    if (content.contains(fieldString)) {
      final String rest = content.substring(content.indexOf(fieldString));
      defaultValue =
          rest.substring(fieldString.length(), rest.indexOf(";")).trim();

      // remove unnecessary parts of field
      if (defaultValue.startsWith("=")) {
        defaultValue = defaultValue.substring(1).trim();

        // remove comments
        while (defaultValue.contains("/*")) {
          defaultValue =
              defaultValue.substring(0, defaultValue.indexOf("/*"))
                  + defaultValue.substring(defaultValue.indexOf("*/") + 2);
        }
        if (defaultValue.contains("//")) {
          defaultValue = defaultValue.substring(0, defaultValue.indexOf("//"));
        }

        // remove brckets from file: new File("example.txt") --> "example.txt"
        if (defaultValue.startsWith("new File(")) {
          defaultValue = defaultValue.substring(9, defaultValue.length() - 1);
        }
      }
    }
    return defaultValue.trim();
  }

  /** This function return the allowed values or interval for a field.
   *
   * @param type the type of the field
   * @param option the {@link Option}-annotation of the field 
   * @param verbose short or long output? */
  private static String getAllowedValues(final Field field,
      final boolean verbose) {
    String allowedValues = "";

    final Class<?> type = field.getType();

    // if the type is enum,
    // the allowed values can be extracted the enum-class
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
    final Option option = field.getAnnotation(Option.class);
    if (option.values().length != 0) {
      allowedValues +=
          "  allowed values: " + java.util.Arrays.toString(option.values())
              + "\n";
    }

    // sometimes the allowed values must match a regexp
    if (verbose && !option.regexp().isEmpty()) {
      allowedValues += "  regexp:   " + option.regexp() + "\n";
    }

    // sometimes the allowed values must be uppercase
    if (verbose && option.toUppercase()) {
      allowedValues += "  uppercase: true\n";
    }

    return allowedValues;
  }

  /**
   * Collects all classes accessible from the context class loader which
   * belong to the given package and subpackages.
   *
   * @return list of classes
   */
  private static List<Class<?>> getClasses() {
    final ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();
    assert classLoader != null;

    Enumeration<URL> resources = null;
    try {
      resources = classLoader.getResources("");
    } catch (IOException e) {
      System.err.println("Could not get recources of classloader.");
    }

    final List<Class<?>> classes = new ArrayList<Class<?>>();
    while (resources.hasMoreElements()) {
      final File file = new File(resources.nextElement().getFile());
      collectClasses(file, "", classes);
    }

    return classes;
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory the base directory
   * @param packageName the package name for classes found inside the base directory
   * @param classes list where the classes are added.
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
