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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.configuration.Option.Type;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;
import com.google.common.primitives.Primitives;


/**
 * Immutable wrapper around a {@link Properties} instance, providing some
 * useful access helper methods.
 */
@Options
public class Configuration {
  
  public static class Builder {
    
    private Map<String, String> properties = null;
    private Configuration oldConfig = null;
    private String prefix = null;
    
    private Builder() { }
    
    private void setupProperties() {
      if (properties == null) {
        properties = new HashMap<String, String>();
      }
      if (oldConfig != null) {
        properties.putAll(oldConfig.properties);
      }
    }
    
    /**
     * Set a single option.
     */
    public Builder setOption(String name, String value) {
      Preconditions.checkNotNull(name);
      Preconditions.checkNotNull(value);
      setupProperties();
      
      properties.put(name, value);
      
      return this;
    }

    /**
     * Reset a single option to its default value.
     */
    public Builder clearOption(String name) {
      Preconditions.checkNotNull(name);
      setupProperties();
      
      properties.remove(name);
      
      return this;
    }
    
    /**
     * Add all options from a map.
     */
    public Builder setOptions(Map<String, String> options) {
      Preconditions.checkNotNull(options);
      setupProperties();
      
      properties.putAll(options);
      
      return this;
    }
    
    /**
     * Set the optional prefix for new configuration.
     */
    public Builder setPrefix(String prefix) {
      Preconditions.checkNotNull(prefix);
      
      this.prefix = prefix;
      
      return this;
    }
    
    /**
     * Copy everything from an existing Configuration instance. This also means
     * that the new configuration object created by this builder will share the
     * set of unused properties with the configuration instance passed to this
     * class.
     * 
     * If this method is called, it has to be the first method call on this
     * builder instance.
     */
    public Builder copyFrom(Configuration oldConfig) {
      Preconditions.checkNotNull(oldConfig);
      Preconditions.checkState(properties == null);
      Preconditions.checkState(oldConfig == null);
      
      this.oldConfig = oldConfig;
      
      return this;
    }
    
    /**
     * Load options from an InputStream with a "key = value" format.
     * @see Properties#load(InputStream)
     * 
     * If this method is called, it has to be the first method call on this
     * builder instance.
     * @throws IOException If the stream cannot be read.
     */
    public Builder loadFromStream(InputStream stream) throws IOException {
      Preconditions.checkNotNull(stream);
      Preconditions.checkState(properties == null);
      Preconditions.checkState(oldConfig == null);
      
      Properties p = new Properties();
      p.load(stream);
      
      properties = new HashMap<String, String>(p.size());
      for (Map.Entry<Object, Object> e : p.entrySet()) {
        properties.put((String)e.getKey(), (String)e.getValue());
      }
      
      return this;
    }
    
    /**
     * Load options from a file with a "key = value" format.
     * @see Properties#load(InputStream)
     * 
     * If this method is called, it has to be the first method call on this
     * builder instance.
     * @throws IOException If the file cannot be read.
     */
    public Builder loadFromFile(String filename) throws IOException {
      Preconditions.checkNotNull(filename);
      
      InputStream stream = null;
      try {
        stream = new FileInputStream(filename);
        loadFromStream(stream);
      } finally {
        Closeables.closeQuietly(stream);
      }
      return this;
    }

    /**
     * Create a Configuration instance with the settings specified by method
     * calls on this builder instance.
     * 
     * This method resets the builder instance, so that after this method has
     * returned it is exactly in the same state as directly after instantiation.
     * 
     * @throws InvalidConfigurationException if the settings contained invalid values for the configuration options of the Configuration class
     */
    public Configuration build() throws InvalidConfigurationException {
      ImmutableMap<String, String> newProperties;
      if (properties == null) {
        // we can re-use the old properties instance because it is immutable
        if (oldConfig != null) {
          newProperties = oldConfig.properties;
        } else {
          newProperties = ImmutableMap.of();
        }
      } else {
        newProperties = ImmutableMap.copyOf(properties); 
      }

      String newPrefix;
      if (prefix == null) {
        if (oldConfig != null) {
          newPrefix = oldConfig.prefix;
        } else {
          newPrefix = "";
        }
      } else {
        newPrefix = prefix;   
      }
      
      Set<String> newUnusedProperties;
      if (oldConfig != null) {
        // share the same set of unused properties
        newUnusedProperties = oldConfig.unusedProperties;
      } else {
        newUnusedProperties = new HashSet<String>();
        for (Object key : newProperties.keySet()) {
          newUnusedProperties.add((String)key);
        }
      }
      
      Configuration newConfig = new Configuration(newProperties, newPrefix, newUnusedProperties);
      newConfig.inject(newConfig);

      // reset builder instance so that it may be re-used
      properties = null;
      prefix = null;
      oldConfig = null;
      
      return newConfig;
    }
  }
  
  /**
   * Create a new Builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }
  
  /**
   * Creates a configuration with all values set to default.
   */
  public static Configuration defaultConfiguration() {
    return new Configuration(ImmutableMap.<String, String>of(), "", new HashSet<String>(0));
  }
  
  /**
   * Creates a copy of a configuration with just the prefix set to a new value.
   */
  public static Configuration copyWithNewPrefix(Configuration oldConfig, String newPrefix) {
    Configuration newConfig = new Configuration(oldConfig.properties, newPrefix, oldConfig.unusedProperties);
    
    // instead of calling inject() set options manually
    // this avoids the "throws InvalidConfigurationException" in the signature
    newConfig.disableOutput = oldConfig.disableOutput;
    newConfig.outputDirectory = oldConfig.outputDirectory;
    newConfig.rootDirectory = oldConfig.rootDirectory;
    
    return newConfig;
  }
    
  @Option(name="output.path")
  private String outputDirectory = "test/output/";
  
  @Option(name="output.disable")
  private boolean disableOutput = false;
  
  @Option
  private String rootDirectory = ".";

  private static final long serialVersionUID = -5910186668866464153L;

  /** Split pattern to create string arrays */
  private static final Pattern ARRAY_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");
  
  private final ImmutableMap<String, String> properties;

  private final String prefix;

  private final Set<String> unusedProperties;
  
  /*
   * This constructor does not set the fields annotated with @Option!
   */
  private Configuration(ImmutableMap<String, String> pProperties, String pPrefix, Set<String> pUnusedProperties) {
    properties = pProperties;
    prefix = (pPrefix.isEmpty() ? "" : (pPrefix + "."));
    unusedProperties = pUnusedProperties;
  }

  /**
   * @see Properties#getProperty(String)
   */
  public String getProperty(String key) {
    String result = properties.get(prefix + key);
    unusedProperties.remove(prefix + key);

    if (result == null && !prefix.isEmpty()) {
      result = properties.get(key);
      unusedProperties.remove(key);
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
      
      // try to read default value
      Object defaultValue = null;
      try {
        defaultValue = field.get(obj);

      } catch (IllegalArgumentException e) {
        assert false : "Type checks above were not successful apparently.";
      } catch (IllegalAccessException e) {
        assert false : "Accessibility setting failed silently above.";
      }
      
      Object value = convertValue(name, valueStr, defaultValue, type, option.type());
      
      // options which were not specified need not to be set
      // but do set OUTPUT_FILE options for disableOutput to work
      if (value == null && (option.type() != Type.OUTPUT_FILE)) {
        continue;
      }

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

      Object value = convertValue(name, valueStr, null, type, option.type());
      
      // options which were not specified need not to be set
      // but do set OUTPUT_FILE options for disableOutput to work
      if (value == null && (option.type() != Type.OUTPUT_FILE)) {
        continue;
      }

      // set value to field
      try {
        method.invoke(obj, value);

      } catch (IllegalArgumentException e) {
        assert false : "Type checks above were not successful apparently.";
      } catch (IllegalAccessException e) {
        assert false : "Accessibility setting failed silently above.";
      } catch (InvocationTargetException e) {
        // ITEs always have a wrapped exception which is the real one thrown by
        // the invoked method. We want to handle this exception.
        Throwable t = e.getCause();
        try {
          Throwables.propagateIfPossible(t, InvalidConfigurationException.class);
        
        } catch (IllegalArgumentException iae) {
          throw new InvalidConfigurationException("Invalid value in configuration file: \""
              + name + " = " + valueStr + '\"'
              + (iae.getMessage() != null ? " (" + iae.getMessage() + ")" : ""));
        }
        
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

  private Object convertValue(String name, String valueStr, Object defaultValue, Class<?> type, Type typeInfo)
                     throws UnsupportedOperationException, InvalidConfigurationException {
    // convert value to correct type
    Object result;

    if (type.equals(File.class)) {
      if (typeInfo == Type.NOT_APPLICABLE) {
        throw new UnsupportedOperationException("Type File and type=NOT_APPLICABLE do not match for option " + name);
      }
      
      result = handleFileOption(name, valueStr, defaultValue, typeInfo);
      
    } else {
      if (typeInfo != Type.NOT_APPLICABLE) {
        throw new UnsupportedOperationException("Type " + type.getSimpleName()
            + " and type=" + typeInfo + " do not match for option " + name);
      }
      
      if (valueStr == null) {
        result = null;
      
      } else if (type.isArray()) {
        if (!type.equals(String[].class)) {
          throw new UnsupportedOperationException("Currently only arrays of type String are supported for configuration options");
        }
        result = ARRAY_SPLIT_PATTERN.split(valueStr);
  
      } else if (type.isPrimitive()) {
        Class<?> wrapperType = Primitives.wrap(type); // get wrapper type in order to use valueOf method
  
        result = valueOf(wrapperType, name, valueStr);
  
      } else if (type.isEnum()) {
        // all enums have valueOf method
        result = valueOf(type, name, valueStr);
  
      } else if (type.equals(String.class)) {
        result = valueStr;
        
      } else if (type.equals(Files.class)) {
        result = handleFileOption(name, valueStr, defaultValue, typeInfo);
  
      } else {
        throw new UnsupportedOperationException("Unimplemented type for option: " + type.getSimpleName());
      }
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

  private File handleFileOption(String name, String valueStr, Object defaultValue, Type typeInfo)
                  throws UnsupportedOperationException, InvalidConfigurationException {
    File file;
    if (valueStr != null) {
      file = new File(valueStr);
    } else if (defaultValue != null) {
      file = (File) defaultValue;
    } else {
      return null;
    }

    if (typeInfo == Type.OUTPUT_FILE) {
      if (disableOutput) {
        return null;
      }
      
      if (!file.isAbsolute()) {
        file = new File(outputDirectory, file.getPath());
      }
    }
    
    if (!file.isAbsolute()) {
      file = new File(rootDirectory, file.getPath());    
    }
    
    if (typeInfo == Type.REQUIRED_INPUT_FILE) {
      try {
        Files.checkReadableFile(file);
      } catch (FileNotFoundException e) {
        throw new InvalidConfigurationException("Option " + name
            + " specifies an invalid input file: " + e.getMessage());
      }
    }
    
    return file;
  }
  
  public String getRootDirectory() {
    return this.rootDirectory;
  }
  
}