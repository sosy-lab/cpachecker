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
package common.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import exceptions.InvalidConfigurationException;
import fql.fllesh.util.CPAchecker;

/**
 * Immutable wrapper around a {@link Properties} instance, providing some
 * useful access helper methods.
 */
public class Configuration {

  private static final long serialVersionUID = -5910186668866464153L;
  
  /** Delimiters to create string arrays */
  private static final String DELIMS = "[;, ]+";

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
  
  /**
   * Constructor for creating a Configuration with values set from a file.
   * Also allows for passing an optional map of settings overriding those from
   * the file.
   * @param fileName The complete path to the configuration file.
   * @param pOverrides A set of option values
   * @throws IOException If the file cannot be read.
   */
  public Configuration(String fileName, Map<String, String> pOverrides) throws IOException {
    properties = new Properties();
    prefix = "";
    
    loadFile(fileName);
    
    if (pOverrides != null) {
      properties.putAll(pOverrides);
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
  }
  
  private String getDefaultConfigFileName() {
    // TODO use resources for this?
    URL binDir = getClass().getProtectionDomain().getCodeSource().getLocation();
    
    File defaultFile = new File("..", "default.properties");
    defaultFile = new File(binDir.getPath(), defaultFile.getPath());
    return defaultFile.getPath();
  }

  /**
   * Load the file as property file see {@link Properties}
   * @param fileName name of the property file
   * @return true if file is loaded successfully
   */
  private void loadFile(String fileName) throws IOException {
    if (fileName == null || fileName.isEmpty()) {
      fileName = getDefaultConfigFileName();
    }

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
    
    if (result == null && !prefix.isEmpty()) {
      result = properties.getProperty(key);
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
    return (s != null) ? s.split(DELIMS) : new String[0];
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
          CPAchecker.logger.logException(Level.FINE, t,
              "Unexpected checked exception in method invoked by Configuration.inject(Object). It was ignored.");
          assert false : t.getMessage();
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
      result = valueStr.split("\\s*,\\s*");
    
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
}