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
package org.sosy_lab.cpachecker.appengine.io;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.junit.Test;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.appengine.common.DatastoreTest;
import org.sosy_lab.cpachecker.appengine.dao.TaskDAO;
import org.sosy_lab.cpachecker.appengine.dao.TaskFileDAO;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;

import com.google.common.io.FileWriteMode;


public class GAEPathTest extends DatastoreTest {

  private Task task;
  private TaskFile file;
  private Path path;

  @Override
  public void setUp() {
    super.setUp();

    task = new Task(1L);
    file = new TaskFile("test.tmp", task);
    file.setContent("lorem ipsum dolor sit amet");
    try {
      TaskFileDAO.save(file);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    TaskDAO.save(task);

    path = new GAEPath("test.tmp", task);
  }

  @Test
  public void shouldDeleteFile() throws Exception {
    path.delete();

    assertNull(TaskFileDAO.loadByName("test.tmp", task));
  }

  @Test
  public void shouldReturnWorkingByteSource() throws Exception {
    try (InputStream in = path.asByteSource().openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int b;
      while ((b = in.read()) != -1) {
        out.write(b);
      }

      assertEquals(file.getContent(), out.toString());
    }
  }

  @Test
  public void shouldReturnWorkingCharSource() throws Exception {
    try (Reader reader = path.asCharSource(null).openStream();
        StringWriter w = new StringWriter()) {
      int c;
      while ((c = reader.read()) != -1) {
        w.write(c);
      }

      assertEquals(file.getContent(), w.toString());
    }
  }

  @Test
  public void shouldReturnWorkingByteSink() throws Exception {
    try (OutputStream out = path.asByteSink().openStream()) {
      out.write(new String("test").getBytes());
    }

    assertEquals("test", file.getContent());
  }

  @Test
  public void shouldReturnWorkingCharSink() throws Exception {
    try (Writer writer = path.asCharSink(Charset.defaultCharset()).openStream()) {
      writer.write("test");
    }

    assertEquals("test", file.getContent());
  }

  @Test
  public void shouldAppendWithByteSink() throws Exception {
    String oldContent = file.getContent();
    try (OutputStream out = path.asByteSink(FileWriteMode.APPEND).openStream()) {
      out.write(new String("test").getBytes());
    }

    assertEquals(oldContent + "test", file.getContent());
  }

  @Test
  public void shouldAppendWithCharSink() throws Exception {
    String oldContent = file.getContent();
    try (Writer writer = path.asCharSink(Charset.defaultCharset(), FileWriteMode.APPEND).openStream()) {
      writer.write("test");
    }

    assertEquals(oldContent + "test", file.getContent());
  }

  @Test
  public void shouldBeFile() throws Exception {
    Path path = new GAEPath("foo.bar", task);

    assertTrue(path.isFile());
  }

  @Test
  public void shouldNotBeFile() throws Exception {
    Path path = new GAEPath("foo", task);

    assertFalse(path.isFile());
  }
}
