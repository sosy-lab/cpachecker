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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


/**
 * Immutable wrapper around a {@link Properties} instance, providing some
 * useful access helper methods.
 */
public class Configuration {

  private static final long serialVersionUID = -5910186668866464153L;

  /** Split pattern to create string arrays */
  private static final Pattern ARRAY_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");

  private static final ImmutableMap<Class<?>, Class<?>> PRIMITIVE_TYPES
      = new ImmutableMap.Builder<Class<?>, Class<?>>()
                     .put(boolean.class, Boolean.class)
                     .put(byte.class,    Byte.class)
                     .put(char.class,    Character.class)
                     .put(double.class,  Double.class)
                     .put(float.class,   Float.class)
                     .put(int.class,     Integer.class)
                     .put(long.class,    Long.class)
                     .put(short.class,   Short.class)
                     .build();


  private final Properties properties;

  private final String prefix;

  private final Set<String> unusedProperties;

  /**
   * Constructor for creating a Configuration with values set from a file.
   * Also allows for passing an optional map of settings overriding those from
   * the file.
   *
   * Either the fileName or the map of overrides may be null, not both. If the
   * fileName is null, this constructor behaves identically to the constructor
   * {@link #Configuration(Map)}.
   *
   * @param fileName The optional complete path to the configuration file.
   * @param pOverrides An optional set of option values.
   * @throws IOException If the file cannot be read.
   */
  public Configuration(String fileName, Map<String, String> pOverrides) throws IOException {
    Preconditions.checkArgument(fileName != null || pOverrides != null);
    properties = new Properties();
    prefix = "";

    if (fileName != null) {
      loadFile(fileName);
    }

    if (pOverrides != null) {
      properties.putAll(pOverrides);
    }

    unusedProperties = new HashSet<String>(properties.size());
    for (Object key : properties.keySet()) {
      unusedProperties.add((String)key);
    }
  }
  /**
   * Constructor for creating a Configuration with values set from an inputStream.
   * Also allows for passing an optional map of settings overriding those from
   * the stream.
   *
   * Either the stream or the map of overrides may be null, not both. If the
   * stream is null, this constructor behaves identically to the constructor
   * {@link #Configuration(Map)}.
   *
   * @param inStream The optional inputStream containing the key-value pairs in propertyFile-format.
   * @param pOverrides An optional set of option values.
   * @throws IOException If the file cannot be read.
   */
  public Configuration(InputStream inStream, Map<String, String> pOverrides) throws IOException {
    Preconditions.checkArgument(inStream != null || pOverrides != null);
    properties = new Properties();
    prefix = "";
    properties.load(inStream);

    if (pOverrides != null) {
      properties.putAll(pOverrides);
    }

    unusedProperties = new HashSet<String>(properties.size());
    for (Object key : properties.keySet()) {
      unusedProperties.add((String)key);
    }
  }
  /**
   * Constructor for creating a Configuration with values set from a given map.
   * @param pValues The values this configuration should represent.
   */
  public Configuration(Map<String, String> pValues) {
    Preconditions.checkNotNull(pValues);
    properties = new Properties();
    prefix = "";

    properties.putAll(pValues);

    unusedProperties = new HashSet<String>(properties.size());
    for (Object key : properties.keySet()) {
      unusedProperties.add((String)key);
    }
  }

  /**
   * Constructor for creating Configuration from a given configuration.
   * Allows to pass a prefix. Options with the prefix will override those with
   * the same key but without the prefix in the new configuration.
   * @param pConfig An old configuration.
   * @param pPrefix A prefix for overriding configuration options.
   */
  public Configuration(Configuration pConfig, String pPrefix) {
    Preconditions.checkNotNull(pConfig);
    Preconditions.checkNotNull(pPrefix);

    properties = pConfig.properties;
    prefix = pPrefix.isEmpty() ? "" : pPrefix + ".";
    unusedProperties = pConfig.unusedProperties; // use same instance here!
  }

  /**
   * Load the file as property file see {@link Properties}
   * @param fileName name of the property file
   * @return true if file is loaded successfully
   */
  private void loadFile(String fileName) throws IOException {
    FileInputStream file = new FileInputStream(fileName);
    try {
      properties.load(file);
    } finally {
      file.close();
    }
  }

  /**
   * @see Properties#getProperty(String)
   */
  public String getProperty(String key) {
    String result = properties.getProperty(prefix + key);
    unusedProperties.remove(prefix + key);

    if (result == null && !prefix.isEmpty()) {
      result = properties.getProperty(key);
      unusedProperties.remove(key);
    }
    // try to create the output directory (lazy, only if a CPA requests the path)
    if ("output.path".equals(key)) {
      if (result != null) {
      boolean couldCreateDirs = (new File(result)).mkdirs();
        if (!couldCreateDirs) {
          // log? i don't find access to a logger
          // TODO: i think we should provide a "default" output directory in this case (one that is probably already existent)
        }
      }
    }
    return result;
  }

  /**
   * @see Properties#getProperty(String, String)
   */
  public String getProperty(String key, String defaultValue) {
    String result = getProperty(key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  /**
   * If there are a number of properties for a given key, this method will split them
   * using {@link Configuration#DELIMS} and return the array of properties
   * @param key the key for the property
   * @return array of properties or empty array if property is not specified
   */
  public String[] getPropertiesArray(String key){
    String s = getProperty(key);
    return (s != null) ? ARRAY_SPLIT_PATTERN.split(s) : new String[0];
  }

  public Set<String> getUnusedProperties() {
    return Collections.unmodifiableSet(unusedProperties);
  }

  /**
   * Inject the values of configuration options into an object.
   * The class of the object has to have a {@link Options} annotation, and each
   * field to set / method to call has to have a {@link Option} annotation.
   *
   * @param obj The object in which the configuration options should be injected.
   * @throws InvalidConfigurationException If the user specified configuration is wrong.
   */
  public void inject(Object obj) throws InvalidConfigurationException {
    inject(obj, obj.getClass());
  }

  /**
   * @see #inject(Object)
   *
   * Use this method if the calling class is likely to be sub-classed, so that
   * the options of the calling class get injected, not the options of the
   * dynamic class type of the object.
   *
   * @param cls The static class type of the object to inject.
   */
  public void inject(Object obj, Class<?> cls) throws InvalidConfigurationException {
    Preconditions.checkNotNull(obj);
    Preconditions.checkNotNull(cls);
    Preconditions.checkArgument(cls.isAssignableFrom(obj.getClass()));

    Options options = cls.getAnnotation(Options.class);
    Preconditions.checkNotNull(options, "Class must have @Options annotation.");

    String prefix = options.prefix();
    if (!prefix.isEmpty()) {
      prefix += ".";
    }

    // handle fields of the class
    Field[] fields = cls.getDeclaredFields();
    Field.setAccessible(fields, true); // override all final & private modifiers

    for (Field field : fields) {
      Option option = field.getAnnotation(Option.class);
      if (option == null) {
        // ignore all non-option fields
        continue;
      }

      String name = getOptionName(prefix, field, option);
      Class<?> type = field.getType();

      String valueStr = getOptionValue(name, option, type.isEnum());
      if (valueStr == null) {
        continue;
      }

      Object value = convertValue(name, valueStr, type);

      // set value to field
      try {
        field.set(obj, value);

      } catch (IllegalArgumentException e) {
        assert false : "Type checks above were not successful apparently.";
      } catch (IllegalAccessException e) {
        assert false : "Accessibility setting failed silently above.";
      }
    }

    // handle methods of the class
    Method[] methods = cls.getDeclaredMethods();
    Method.setAccessible(methods, true); // override all final & private modifiers

    for (Method method : methods) {
      Option option = method.getAnnotation(Option.class);
      if (option == null) {
        // ignore all non-option fields
        continue;
      }

      Class<?>[] parameters = method.getParameterTypes();
      if (parameters.length != 1) {
        throw new IllegalArgumentException("Method with @Option must have exactly one parameter!");
      }

      String name = getOptionName(prefix, method, option);
      Class<?> type = parameters[0];

      String valueStr = getOptionValue(name, option, type.isEnum());
      if (valueStr == null) {
        continue;
      }

      Object value = convertValue(name, valueStr, type);

      // set value to field
      try {
        method.invoke(obj, value);

      } catch (IllegalArgumentException e) {
        assert false : "Type checks above were not successful apparently.";
      } catch (IllegalAccessException e) {
        assert false : "Accessibility setting failed silently above.";
      } catch (InvocationTargetException e) {
        // ITEs always have a wrapped exception which is the real one thrown by
        // the invoked method. We want to handle this exception, so throw it
        // (again) and catch it immediately.
        try {
          throw e.getCause();

        } catch (IllegalArgumentException iae) {
          throw new InvalidConfigurationException("Invalid value in configuration file: \""
              + name + " = " + valueStr + '\"'
              + (iae.getMessage() != null ? " (" + iae.getMessage() + ")" : ""));

        } catch (RuntimeException re) {
          throw re; // for these exceptions it is easy, we can just re-throw without declaring them

        } catch (Error err) {
          throw err; // errors should never be caught!

        } catch (Throwable t) {
          // We can't handle it correctly, but we can't throw it either.
          InvalidConfigurationException newException = new InvalidConfigurationException(
              "Unexpected checked exception in method "
              + method.toGenericString()
              + ", which was invoked by Configuration.inject()");
          newException.initCause(t);
          throw newException;
        }
      }
    }
  }

  private String getOptionName(String prefix, Member field, Option option) {
    // get name for configuration option
    String name = option.name();
    if (name.isEmpty()) {
      name = field.getName();
    }
    name = prefix + name;
    return name;
  }

  private String getOptionValue(String name, Option option, boolean alwaysUppercase) throws InvalidConfigurationException {
    // get value in String representation
    String valueStr = getProperty(name);
    if (valueStr == null || valueStr.isEmpty()) {
      if (option.required()) {
        throw new InvalidConfigurationException("Required configuration option " + name  + " is missing!");
      }
      return null;
    }

    valueStr = valueStr.trim();

    if (alwaysUppercase || option.toUppercase()) {
      valueStr = valueStr.toUpperCase();
    }

    // check if it is included in the allowed values list
    String[] allowedValues = option.values();
    if (allowedValues.length > 0) {
      boolean invalid = true;
      for (String allowedValue : allowedValues) {
        if (valueStr.equals(allowedValue)) {
          invalid = false;
          break;
        }
      }
      if (invalid) {
        throw new InvalidConfigurationException("Invalid value in configuration file: \""
            + name + " = " + valueStr + '\"'
            + " (not listed as allowed value)");
      }
    }

    // check if it matches the specification regexp
    String regexp = option.regexp();
    if (!regexp.isEmpty()) {
      if (!valueStr.matches(regexp)) {
        throw new InvalidConfigurationException("Invalid value in configuration file: \""
            + name + " = " + valueStr + '\"'
            + " (does not match RegExp \"" + regexp + "\")");
      }
    }

    return valueStr;
  }

  private Object convertValue(String name, String valueStr, Class<?> type) throws UnsupportedOperationException,
                     InvalidConfigurationException {
    // convert value to correct type
    Object result;

    if (type.isArray()) {
      if (!type.equals(String[].class)) {
        throw new UnsupportedOperationException("Currently only arrays of type String are supported for configuration options");
      }
      result = ARRAY_SPLIT_PATTERN.split(valueStr);

    } else if (type.isPrimitive()) {
      Class<?> wrapperType = PRIMITIVE_TYPES.get(type); // get wrapper type in order to use valueOf method

      result = valueOf(wrapperType, name, valueStr);

    } else if (type.isEnum()) {
      // all enums have valueOf method
      result = valueOf(type, name, valueStr);

    } else if (type.equals(String.class)) {
      result = valueStr;

    } else {
      throw new UnsupportedOperationException("Unimplemented type for option: " + type.getSimpleName());
    }
    return result;
  }

  private Object valueOf(Class<?> type, String name, String value) throws InvalidConfigurationException {
    try {
      Method valueOf = type.getMethod("valueOf", String.class);
      return valueOf.invoke(null, value);

    } catch (NoSuchMethodException e) {
      throw new AssertionError("Primitive type class without valueOf(String) method!");
    } catch (IllegalAccessException e) {
      throw new AssertionError("Primitive type class without accessible valueOf(String) method!");
    } catch (InvocationTargetException e) {
      throw new InvalidConfigurationException("Could not parse \"" + name + " = " + value
          + "\" (" + e.getTargetException().getMessage() + ")");
    }
  }
  
  /** Debug method to write a Configuration into a properties file.
   * @param filename
   */
  public void writeToFile(String filename) {
    FileWriter fw = null;
    try {
      fw = new FileWriter(filename);
      fw.write("");// clear the file
      for (Entry<Object, Object> e : properties.entrySet()) {
        fw.append(e.getKey().toString() + " = " + e.getValue().toString() + "\n");
      }
      fw.flush();
      fw.close();
    } catch (IOException e1) {
      e1.printStackTrace();
      // this is a debug method. No proper Exception handling
    }
  }
  /**
   * sets the value of the property
   * @param pKey
   * @param pValue
   */
  public void setProperty(String pKey, String pValue) {
    // not nice since configurations should be immutable. 
    // Plugin needs it (this is the only way i can think of at the moment)
    //TODO: Check if this is ok!
    this.properties.put(prefix + pKey, pValue);
    unusedProperties.add(prefix + pKey);
  }
}