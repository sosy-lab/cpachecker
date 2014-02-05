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

import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;

/**
 * This class provides methods for loading, saving and deletion of {@link JobFile}
 * instances.
 */
public class JobFileDAO {

  /**
   * @see #load(Key)
   */
  public static JobFile load(String key) {
    try {
      Key<JobFile> fileKey = Key.create(key);
      return load(fileKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Retrieves and returns a file with the given key.
   *
   * @param key The key of the desired file
   * @return The desired file or null if it cannot be found
   */
  public static JobFile load(Key<JobFile> key) {
    return ofy().load().key(key).now();
  }

  /**
   * Returns the file that has the given path.
   *
   * @param path The path of the file
   * @param parent The parent of the file
   *
   * @return The file with the specified path or null if the file cannot be found
   */
  public static JobFile loadByPath(String path, Job parent) {
    return ofy().load().type(JobFile.class).ancestor(parent).filter("path", path).first().now();
  }

  /**
   * @see #loadByPath(String, Job)
   */
  public static JobFile loadByPath(String path, String parentKey) {
    Job parent = JobDAO.load(parentKey);
    return loadByPath(path, parent);
  }

  /**
   * Returns the file that has the given name.
   *
   * @param name The name of the file
   * @param parent The parent of the file
   *
   * @return The file with the specified name or null if the file cannot be found
   */
  public static JobFile loadByName(String name, Job parent) {
    return ofy().load().type(JobFile.class).ancestor(parent).filter("name", name).first().now();
  }

  /**
   * @see #loadByName(String, Job)
   */
  public static JobFile loadByName(String name, String parentKey) {
    Job parent = JobDAO.load(parentKey);
    return loadByName(name, parent);
  }

  /**
   * Returns a list of all files that are associated with the given job.
   *
   * @param parent The parent
   *
   * @return A list of files.
   */
  public static List<JobFile> files(Job parent) {
    return ofy().load().type(JobFile.class).ancestor(parent).list();
  }

  /**
   * Saves the given file.
   *
   * @param file The file to save
   *
   * @throws IOException If the file is too large to save or if there is a
   * problem with the underlying data store.
   */
  public static void save(JobFile file) throws IOException {
    try {
      ofy().save().entity(file).now();
    } catch (RequestTooLargeException e) {
      throw new IOException(String.format("The file %s is too large to be saved.", file.getName()), e);
    } catch (DatastoreFailureException e) {
      throw new IOException(String.format("The file %s could not be saved.", file.getName()), e);
    }
  }

  /**
   * Deletes the given file.
   *
   * @param file The file to delete
   */
  public static void delete(final JobFile file) {
    final Job parent = file.getJob();
    parent.removeFile(file);

    ofy().transact(new VoidWork() {

      @Override
      public void vrun() {
        ofy().save().entity(parent).now();
        ofy().delete().entity(file).now();
      }
    });
  }
}
