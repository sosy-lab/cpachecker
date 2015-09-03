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
package org.sosy_lab.cpachecker.appengine.dao;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.List;

import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;

import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;

/**
 * This class provides methods for loading, saving and deletion of {@link TaskFile}
 * instances.
 */
public class TaskFileDAO {

  /**
   * @see #load(Key)
   */
  public static TaskFile load(String key) {
    try {
      Key<TaskFile> fileKey = Key.create(key);
      return load(fileKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Retrieves and returns a {@link TaskFile} with the given key.
   *
   * @param key The key of the desired {@link TaskFile}
   * @return The desired {@link TaskFile} or null if it cannot be found
   */
  public static TaskFile load(Key<TaskFile> key) {
    return ofy().load().key(key).now();
  }

  /**
   * Returns the {@link TaskFile} that has the given path.
   *
   * @param path The path of the {@link TaskFile}
   * @param parent The parent of the {@link TaskFile}
   *
   * @return The {@link TaskFile} with the specified path or null if it cannot be found
   */
  public static TaskFile loadByPath(String path, Task parent) {
    return ofy().load().type(TaskFile.class).ancestor(parent).filter("path", path).first().now();
  }

  /**
   * Returns the {@link TaskFile} that has the given name.
   *
   * @param name The name of the {@link TaskFile}
   * @param parent The parent of the {@link TaskFile}
   *
   * @return The {@link TaskFile} with the specified name or null if it cannot be found
   */
  public static TaskFile loadByName(String name, Task parent) {
    return ofy().load().type(TaskFile.class).ancestor(parent).filter("name", name).first().now();
  }

  /**
   * @see #loadByName(String, Task)
   */
  public static TaskFile loadByName(String name, String parentKey) {
    Task parent = TaskDAO.load(parentKey);
    return loadByName(name, parent);
  }

  /**
   * Returns a list of all {@link TaskFile}s that are associated with the given {@link Task}.
   *
   * @param parent The parent
   *
   * @return A list of {@link TaskFile}s.
   */
  public static List<TaskFile> files(Task parent) {
    return ofy().load().type(TaskFile.class).ancestor(parent).list();
  }

  /**
   * Saves the given {@link TaskFile}.
   *
   * @param file The {@link TaskFile} to save
   *
   * @throws IOException If the {@link TaskFile} is too large to save or if
   * there is a problem with the underlying data store.
   */
  public static void save(TaskFile file) throws IOException {
    try {
      ofy().save().entity(file).now();
    } catch (RequestTooLargeException e) {
      throw new IOException(String.format("The file %s is too large to be saved.", file.getName()), e);
    } catch (DatastoreFailureException e) {
      throw new IOException(String.format("The file %s could not be saved.", file.getName()), e);
    }
  }

  /**
   * Deletes the given {@link TaskFile}.
   *
   * @param file The {@link TaskFile} to delete
   */
  public static void delete(final TaskFile file) {
    ofy().transact(new VoidWork() {

      @Override
      public void vrun() {
        ofy().save().entity(file.getTask()).now();
        ofy().delete().entity(file).now();
      }
    });
  }

  /**
   * Deletes all {@link TaskFile}s associated with the given {@link Task}.
   *
   * @param parent The {@link Task} that is associated with the
   *               {@link TaskFile}s to be deleted
   */
  public static void deleteAll(final Task parent) {
    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        ofy().delete().entities(files(parent)).now();
      }
    });
  }
}
