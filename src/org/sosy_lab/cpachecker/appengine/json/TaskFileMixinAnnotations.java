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
package org.sosy_lab.cpachecker.appengine.json;

import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Ref;

/**
 * This class contains a set of classes that are used to mixin annotations
 * with a {@link ObjectMapper} to control serialization/deserialization of a
 * {@link TaskFile} bean.
 */
public abstract class TaskFileMixinAnnotations {

  @JsonAutoDetect(getterVisibility = Visibility.NONE, fieldVisibility = Visibility.NONE)
  public abstract class Minimal extends TaskFile {

    @JsonProperty
    @Override
    public abstract String getKey();

    @JsonProperty
    @Override
    public abstract String getName();
  }

  public abstract class Full extends Minimal {

    @JsonProperty
    String content;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonAutoDetect(setterVisibility = Visibility.NONE)
  public abstract class FromJSONAPI {
    @JsonIgnore
    Ref<Task> task;

    @JsonProperty("sourceFileName")
    String path;

    @JsonProperty("programText")
    String content;
  }
}
