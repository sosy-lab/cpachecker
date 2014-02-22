/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.appengine.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.appengine.dao.TaskDAO;
import org.sosy_lab.cpachecker.appengine.dao.TaskFileDAO;
import org.sosy_lab.cpachecker.appengine.dao.TasksetDAO;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;
import org.sosy_lab.cpachecker.appengine.io.GAEPath;
import org.sosy_lab.cpachecker.cmdline.PropertyFileParser;
import org.sosy_lab.cpachecker.cmdline.PropertyFileParser.PropertyType;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.google.common.base.Preconditions;


/**
 * This class constructs a {@link Task} and an according {@link TaskFile} that
 * acts as its program from JSON input.
 */
public class TaskBuilder {

  private List<String> errors = new LinkedList<>();
  private String taskIdentifier;

  /**
   * Create a new {@link TaskBuilder}.
   */
  public TaskBuilder() { }

  /**
   * @see #fromJson(InputStream)
   */
  public Task fromJson(String json) {
    Preconditions.checkNotNull(json);
    InputStream in = new ByteArrayInputStream(json.getBytes());
    return parseSingleJson(in);
  }

  /**
   * Returns a {@link Task} instance that will be created from the JSON provided
   * by the given {@link InputStream}.
   * If any errors occur while building the {@link Task} null will be returned.
   * Therefore make sure to call {@link TaskBuilder#hasErrors()} before using
   * the returned object.
   *
   * @param json The input JSON
   * @return The created {@link Task} or null
   */
  public Task fromJson(InputStream json) {
    Preconditions.checkNotNull(json);
    return parseSingleJson(json);
  }

  /**
   * @see #fromJsonList(InputStream)
   */
  public Map<Task, String> fromJsonList(String json) {
    Preconditions.checkNotNull(json);
    InputStream in = new ByteArrayInputStream(json.getBytes());
    return parseMultiJson(in);
  }

  /**
   * Creates {@link Task} instances from the JSON provided by the given {@link InputStream}.
   * Returns a {@link Map} containing these {@link Task}s as its keys and their
   * identifier as its according values if an identifier was provided.
   *
   * If any errors occur while building the {@link Task}s null will be returned.
   * Therefore make sure to call {@link TaskBuilder#hasErrors()} before using
   * the returned object.
   *
   * @param json The input JSON
   * @return The created {@link Task}s or null
   */
  public Map<Task, String> fromJsonList(InputStream json) {
    Preconditions.checkNotNull(json);
    return parseMultiJson(json);
  }

  /**
   * Returns a {@link Task} instance that will be created from the provided
   * {@link Map}.
   * @see #fromJson(InputStream)
   */
  public Task fromMap(Map<String, Object> in) {
    Preconditions.checkNotNull(in);
    return parseSingleMap(in);
  }

  /**
   * Creates {@link Task} instances from the provided {@link Map}.
   * @see #fromJsonList(InputStream)
   */
  public Map<Task, String> fromMapList(List<Map<String, Object>> in) {
    Preconditions.checkNotNull(in);
    return parseMultiMap(in);
  }

  /**
   * Returns the identifier that was provided by a previous call to
   * {@link TaskBuilder#fromJson(InputStream)}
   *
   * @return The identifier of a previously created {@link Task}
   */
  public String identifier() {
    return taskIdentifier;
  }

  /**
   * A {@link List} of errors. If none occurred the {@link List} will be empty.
   *
   * @return A {@link List} of errors or an empty {@link List}
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Indicates whether any errors occurred while building.
   *
   * @return True, if any errors occurred, false otherwise
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  private Task createTask(InputBean input) {
    Task task = new Task(TaskDAO.allocateKey().getId());
    TaskFile program = new TaskFile(input.programName, task);

    if (input.options == null) {
      input.options = new HashMap<>();
    }

    taskIdentifier = input.identifier;

    DefaultOptions optionParser = new DefaultOptions();

    if (input.commandline != null) {
      parseCmdLine(input, task);
    }
    if (input.properties != null && input.commandline == null) {
      parseProperties(input, task);
    }

    if (input.configuration != null) {
      if (!input.configuration.endsWith(".properties")) {
        input.configuration += ".properties";
      }
      task.setConfiguration(input.configuration.substring(input.configuration.lastIndexOf('/')+1));
    }
    if (input.specification != null) {
      if (!input.specification.endsWith(".spc")) {
        input.specification += ".spc";
      }
      task.setSpecification(input.specification.substring(input.specification.lastIndexOf('/')+1));
    }
    if (input.programName != null) {
      program.setPath(input.programName.substring(input.programName.lastIndexOf('/')+1));
      optionParser.setOption("analysis.programNames", program.getPath());
    }
    program.setContent(input.programText);

    optionParser.setOptions(input.options);
    task.setOptions(optionParser.getOptions());

    if (input.taskset != null) {
      task.setTaskset(TasksetDAO.load(input.taskset));
    }

    validateSpecOrConfSet(task);
    validateSpecExists(task);
    validateConfExists(task);
    validateProgramExists(program);
    validateProgramNameExists(program);

    if (!hasErrors()) {
      try {
        TaskFileDAO.save(program);
        task.setProgram(program);
        TaskDAO.save(task);
      } catch (IOException e) {
        if (e.getCause() instanceof RequestTooLargeException) {
          errors.add("task.program.TooLarge");
        } else {
          errors.add("task.program.CouldNotUpload");
        }
      }
    }

    return task;
  }

  private void parseProperties(InputBean input, Task task) {
    if (input.properties == null || input.properties.isEmpty()) { return; }
    Path propertyFile = new GAEPath("Properties.prp", task);
    try (OutputStream out = propertyFile.asByteSink().openBufferedStream()) {
      out.write(input.properties.getBytes());
    } catch (IOException e) {
      errors.add("task.properties.WriteError");
    }

    PropertyFileParser parser = new PropertyFileParser(propertyFile);
    try {
      parser.parse();
    } catch (IllegalArgumentException e) {
      errors.add("task.properties.ParseError");
    }
    if (input.options != null && !input.options.containsKey("analysis.entryFunction")) {
      input.options.put("analysis.entryFunction", parser.getEntryFunction());
    }

    Set<PropertyType> properties = parser.getProperties();
    if (properties.equals(EnumSet.of(PropertyType.REACHABILITY_LABEL))) {
      input.specification = "sv-comp.spc";
    } else if (properties.equals(EnumSet.of(PropertyType.REACHABILITY))) {
      input.specification = "sv-comp-reachability.spc";
    }
  }

  private void parseCmdLine(InputBean input, Task task) {
    if (input.commandline == null) { return; }

    Scanner sc = new Scanner(input.commandline);
    while (sc.hasNext() && !hasErrors()) {
      String token = sc.next();
      switch (token) {
      case "-stats":
        input.options.put("statistics.export", "true");
        break;
      case "-noout":
        input.options.put("output.disable", "true");
        break;
      case "-32":
        input.options.put("analysis.machineModel", "Linux32");
        break;
      case "-64":
        input.options.put("analysis.machineModel", "Linux64");
        break;
      case "-entryfunction":
        if (ensureHasNextToken(sc)) {
          input.options.put("analysis.entryFunction", sc.next());
        }
        break;
      case "-config":
        if (ensureHasNextToken(sc)) {
          String conf = sc.next();
          if (!conf.endsWith(".properties")) {
            conf += ".properties";
          }
          if (isValidConfigFile(conf)) {
            input.configuration = conf;
          } else {
            errors.add("task.conf.DoesNotExist");
          }
        }
        break;
      case "-spec":
        if (ensureHasNextToken(sc)) {
          String spec = sc.next();
          if (spec.endsWith(".prp")) {
            parseProperties(input, task);
          } else {
            if (!spec.endsWith(".spc")) {
              spec += ".spc";
            }
            if (isValidSpecFile(spec)) {
              input.specification = spec;
            } else {
              errors.add("task.spec.DoesNotExist");
            }
          }
        }
        break;
      case "-cmc":
        if (ensureHasNextToken(sc)) {
          input.options.put("analysis.restartAfterUnknown", "true");
          String conf = sc.next();
          if (isValidConfigFile(conf)) {
            String files;
            if ((files = input.options.get("restartAlgorithm.configFiles")) != null) {
              files +=","+conf;
              input.options.put("restartAlgorithm.configFiles", files);
            }
          }
        }
        break;
      case "-cpas":
        if (ensureHasNextToken(sc)) {
          input.options.put("cpa", CompositeCPA.class.getName());
          input.options.put(CompositeCPA.class.getSimpleName() + ".cpas", sc.next());
        }
        break;
      case "-nolog":
        input.options.put("log.level", "OFF");
        input.options.put("log.consoleLevel", "OFF");
        break;
      case "-skipRecursion":
        input.options.put("analysis.summaryEdges", "true");
        input.options.put("cpa.callstack.skipRecursion", "true");
        break;
      case "-setprop":
        if (ensureHasNextToken(sc)) {
          String[] option = sc.next().split("=");
          if (option.length != 2) {
            errors.add("task.cmdLine.SetpropWrongFormat");
          } else {
            input.options.put(option[0], option[1]);
          }
        }
        break;
       default:
         if (token.startsWith("-")) {
           String arg = token.substring(1);
           if (!arg.endsWith("properties")) {
             arg += ".properties";
           }
           if (isValidConfigFile(arg)) {
             input.configuration = arg;
           }
         } else {
           input.options.put("analysis.programNames", token);
           input.programName = token;
         }
      }
    }
    sc.close();
  }

  private boolean ensureHasNextToken(Scanner sc) {
    if (!sc.hasNext()) {
      errors.add("task.cmdline.ArgMissing");
      return false;
    }

    return true;
  }

  private boolean isValidConfigFile(String file) {
    try {
      return DefaultOptions.getConfigurations().contains(file);
    } catch (IOException e) {
      return false;
    }
  }

  private boolean isValidSpecFile(String file) {
    return DefaultOptions.getSpecifications().contains(file);
  }

  private void validateSpecOrConfSet(Task task) {
    if ((task.getSpecification() == null
        || task.getSpecification().isEmpty())
        && (task.getConfiguration() == null
        || task.getConfiguration().isEmpty())) {
      errors.add("task.specOrConf.IsBlank");
    }
  }

  private void validateSpecExists(Task task) {
    if (task.getSpecification() != null && !task.getSpecification().isEmpty()) {
      if (!isValidSpecFile(task.getSpecification())) {
        errors.add("task.spec.DoesNotExist");
      }
    }
  }

  private void validateConfExists(Task task) {
    if (task.getConfiguration() != null && !task.getConfiguration().isEmpty()) {
      if (!isValidConfigFile(task.getConfiguration())) {
        errors.add("task.conf.DoesNotExist");
      }
    }
  }

  private void validateProgramExists(TaskFile program) {
    if (program.getContent() == null || program.getContent().isEmpty()) {
      errors.add("task.program.IsBlank");
    }
  }

  private void validateProgramNameExists(TaskFile program) {
    if (program.getPath() == null || program.getPath().isEmpty()) {
      errors.add("task.program.NameIsBlank");
    }
  }

  private Task parseSingleJson(InputStream in) {
    InputBean inputBean = new InputBean();
    ObjectMapper mapper = new ObjectMapper();
    try {
      inputBean = mapper.readValue(in, InputBean.class);
    } catch (JsonParseException e) {
      errors.add("error.jsonNotWellFormed");
    } catch (JsonMappingException e) {
      errors.add("error.jsonNotMapped");
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    Task task = createTask(inputBean);
    if (hasErrors()) {
      return null;
    }
    return task;
  }

  private Map<Task, String> parseMultiJson(InputStream in) {
    List<InputBean> inputBeans = new LinkedList<>();
    ObjectMapper mapper = new ObjectMapper();
    try {
      inputBeans = mapper.readValue(in, new TypeReference<List<InputBean>>() {});
    } catch (JsonParseException e) {
      errors.add("error.jsonNotWellFormed");
    } catch (JsonMappingException e) {
      errors.add("error.jsonNotMapped");
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    Map<Task, String> tasks = new HashMap<>();
    for (InputBean inputBean : inputBeans) {
      if (!hasErrors()) {
        tasks.put(createTask(inputBean), inputBean.identifier);
      } else {
        break;
      }
    }

    if (hasErrors()) {
      return null;
    }
    return tasks;
  }

  private Task parseSingleMap(Map<String, Object> map) {
    ObjectMapper mapper = new ObjectMapper();
    String json = "";
    try {
      json = mapper.writeValueAsString(map);
    } catch (JsonGenerationException e) {
      errors.add("error.mapNotWellFormed");
    } catch (JsonMappingException e) {
      errors.add("error.jsonNotMapped");
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    InputStream jsonStream = new ByteArrayInputStream(json.getBytes());
    return parseSingleJson(jsonStream);
  }

  private Map<Task, String> parseMultiMap(List<Map<String, Object>> map) {
    ObjectMapper mapper = new ObjectMapper();
    String json = "";
    try {
      json = mapper.writeValueAsString(map);
    } catch (JsonGenerationException e) {
      errors.add("error.mapNotWellFormed");
    } catch (JsonMappingException e) {
      errors.add("error.jsonNotMapped");
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    InputStream jsonStream = new ByteArrayInputStream(json.getBytes());
    return parseMultiJson(jsonStream);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, setterVisibility = Visibility.ANY)
  static class InputBean {
    String commandline;
    String configuration;
    String specification;
    String properties;
    String identifier;
    String programName;
    String programText;
    String taskset;
    Map<String, String> options;
  }
}
