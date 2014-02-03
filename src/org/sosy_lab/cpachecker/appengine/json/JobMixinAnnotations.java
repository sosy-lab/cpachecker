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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner.Instance;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.entity.JobStatistic;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class contains a set of classes that are used to mixin annotations
 * with a {@link ObjectMapper} to control serialization/deserialization of a {@link Job} bean.
 */
public abstract class JobMixinAnnotations {

  @JsonAutoDetect(getterVisibility = Visibility.NONE, fieldVisibility = Visibility.NONE)
  public abstract class KeyOnly extends Job {

    @Override
    @JsonProperty
    public abstract String getKey();
  }

  public abstract class Minimal extends KeyOnly {

    @JsonProperty
    @JsonFormat(timezone = "UTC", pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    Date creationDate;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    @JsonFormat(timezone = "UTC", pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    Date executionDate;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    @JsonFormat(timezone = "UTC", pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    Date terminationDate;

    @JsonProperty
    Status status;

    @JsonProperty("result")
    @JsonInclude(Include.ALWAYS)
    Result resultOutcome;
  }

  public abstract class Full extends Minimal {

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    String statusMessage;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    String specification;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    String configuration;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    String sourceFileName;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    Instance instanceType;

    @JsonProperty
    int retries;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    Result resultMessage;

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    Map<String, String> options;

    @JsonProperty("files")
    @JsonInclude(Include.ALWAYS)
    @Override
    public abstract List<JobFile> getFilesLoaded();

    @JsonProperty
    @JsonInclude(Include.ALWAYS)
    @Override
    public abstract JobStatistic getStatistic();
  }
}
