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
package org.sosy_lab.cpachecker.appengine.entity;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.appengine.dao.TaskFileDAO;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Serialize;

@Entity
public class TaskFile {

  @Id
  private Long id;
  @Parent
  private Ref<Task> task;
  @Index
  private String path;
  @Index
  private String name;
  @Serialize(zip = true)
  private String content = "";
  @Ignore
  private Writer contentWriter;
  @Ignore
  private ByteArrayOutputStream contentOutputStream;

  public TaskFile() {}

  public TaskFile(long id) {
    setId(id);
  }

  public TaskFile(String path, Task task) {
    setPath(path);
    setTask(task);
  }

  public String getKey() {
    return Key.create(task.getKey(), TaskFile.class, getId()).getString();
  }

  public long getId() {
    return id;
  }

  public void setId(long pId) {
    id = pId;
  }

  public Task getTask() {
    return task.get();
  }

  public void setTask(Task pTask) {
    task = Ref.create(pTask);
  }

  public String getName() {
    return Paths.get(path).getName();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String pPath) {
    path = pPath;
  }

  public String getContent() {
    flushOuputStream();
    return content;
  }

  public void setContent(String pContent) {
    content = pContent;
  }

  /**
   * @see #getContentOutputStream()
   *
   * @param charset The charset to use for writing.
   * @return A Writer instance to write this instance's content
   */
  public Writer getContentWriter(Charset charset) {
    if (contentWriter == null) {
      contentWriter = new BufferedWriter(new OutputStreamWriter(getContentOutputStream(), charset));
    }

    return contentWriter;
  }

  /**
   * Returns an OutputStream to write the file's content.
   * If an output stream is used and something is actually written to it,
   * then anything set via {@link #setContent(String)} will be overwritten by the writer's contents
   * upon saving or retrieving the content via {@link #getContent()}.
   *
   * @return An OutputStream instance to write this instance's content.
   */
  public OutputStream getContentOutputStream() {
    if (contentOutputStream == null) {
      contentOutputStream = new SaveOnCloseByteArrayOutputStream(this);
    }

    return contentOutputStream;
  }

  /**
   * Flushes the content output stream before saving and sets the content to its contents.
   */
  @OnSave
  void flushOuputStream() {
    if (contentOutputStream != null && contentOutputStream.size() > 0) {
      content = contentOutputStream.toString();
    }
  }

  @OnSave
  void storeName() {
    name = getName();
  }

  /**
   * A ByteArrayOuputStream that saves the given {@link TaskFile} instance on calling close()
   */
  public static class SaveOnCloseByteArrayOutputStream extends ByteArrayOutputStream {

    private TaskFile file;

    public SaveOnCloseByteArrayOutputStream(TaskFile file) {
      this.file = file;
    }

    @Override
    public void close() throws IOException {
      super.close();
      TaskFileDAO.save(file);
    }
  }
}
