/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.appengine.entity;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class JobFile {

  @Id Long id;
  @Parent Ref<Job> job;
  private String name;
  private String content;


  public JobFile() {}

  public JobFile(String name) {
    this.name = name;
  }

  public String getKeyString() {
    return Key.create(job.getKey(), JobFile.class, id).getString();
  }

  public Long getId() {
    return id;
  }

  public Job getJob() {
    return job.get();
  }

  public void setJob(Job pJob) {
    job = Ref.create(pJob);
  }

  public String getContent() {
    return content;
  }

  public void setContent(String pContent) {
    content = pContent;
  }

  public String getName() {
    return name;
  }
}
