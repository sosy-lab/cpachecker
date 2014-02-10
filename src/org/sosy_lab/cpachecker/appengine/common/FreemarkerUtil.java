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
package org.sosy_lab.cpachecker.appengine.common;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.ext.freemarker.TemplateRepresentation;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.template.Configuration;


public class FreemarkerUtil {

  private FreemarkerUtil() {}

  /**
   * Returns a new Builder instance to build a Freemarker template representation.
   *
   * @return A Freemarker template builder
   */
  public static TemplateBuilder templateBuilder() {
    return new TemplateBuilder();
  }

  public static class TemplateBuilder {

    private Context context;
    private Request request;
    private Locale fallbackLocale;
    private Locale locale;
    private String resourceBundleName;
    private ResourceBundle resourceBundle;
    private String templateDirectory;
    private Map<String, Object> data;
    private MediaType mediaType;
    private String templateName;
    private String labelsKey;

    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    public static final String DEFAULT_TEMPLATE_DIRECTORY = "war:///WEB-INF/templates";
    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.TEXT_HTML;
    public static final String DEFAULT_RESOURCE_BUNDLE_NAME = "messages";
    public static final String DEFAULT_LABELS_KEY = "msg";

    private TemplateBuilder() {
      context = null;
      request = null;
      fallbackLocale = DEFAULT_LOCALE;
      locale = null;
      resourceBundleName = DEFAULT_RESOURCE_BUNDLE_NAME;
      resourceBundle = null;
      templateDirectory = DEFAULT_TEMPLATE_DIRECTORY;
      data = new HashMap<>();
      mediaType = DEFAULT_MEDIA_TYPE;
      templateName = null;
      labelsKey = DEFAULT_LABELS_KEY;
    }

    /**
     * Sets the request's context.
     *
     * @param context The context.
     * @return The builder instance.
     */
    public TemplateBuilder context(Context context) {
      this.context = context;
      return this;
    }

    /**
     * Sets the request.
     *
     * @param request The request.
     * @return The builder instance.
     */
    public TemplateBuilder request(Request request) {
      this.request = request;
      return this;
    }

    /**
     * Sets the locale to use in case none of the languages accepted by the client is available.
     * Defaults to English.
     *
     * @param locale The fallback locale.
     * @return The builder instance.
     */
    public TemplateBuilder fallbackLocale(Locale locale) {
      this.fallbackLocale = locale;
      return this;
    }

    /**
     * Sets the locale. Overwrites the client's accepted language.
     *
     * @param locale The locale.
     * @return The builder instance.
     */
    public TemplateBuilder locale(Locale locale) {
      this.locale = locale;
      return this;
    }

    /**
     * Sets the name of the resource bundle.
     * Defaults to {@link TemplateBuilder#DEFAULT_RESOURCE_BUNDLE_NAME}
     *
     * @param name The resource bundle's name.
     * @return The builder instance.
     */
    public TemplateBuilder resourceBundleName(String name) {
      resourceBundleName = name;
      return this;
    }

    /**
     * Sets the resource bundle to use with the template.
     * Setting the bundle will circumvent any language detection based on the client's preferences.
     *
     * @param bundle The resource bundle.
     * @return The builder instance.
     */
    public TemplateBuilder resourceBundle(ResourceBundle bundle) {
      resourceBundle = bundle;
      return this;
    }

    /**
     * Sets the directory in which to look for the template files.
     * Defaults to {@link TemplateBuilder#DEFAULT_TEMPLATE_DIRECTORY}
     *
     * @param dir The template directory.
     * @return The builder instance.
     */
    public TemplateBuilder templateDirectory(String dir) {
      this.templateDirectory = dir;
      return this;
    }

    /**
     * Adds data to the template's data model. Replaces any existing value associated with the key.
     * Do not use the key {@link TemplateBuilder#DEFAULT_LABELS_KEY} since it will be overridden to hold the labels from the resource bundle.
     *
     * @param key The key.
     * @param obj The data object.
     * @return The builder instance.
     */
    public TemplateBuilder addData(String key, Object obj) {
      data.put(key, obj);
      return this;
    }

    /**
     * Sets the key to use in the data map to refer to the resource bundle labels.
     * @param key
     * @return
     */
    public TemplateBuilder labelsKey(String key) {
      labelsKey = key;
      return this;
    }

    /**
     * Sets the template's media type.
     * Defaults to {@link TemplateBuilder#DEFAULT_MEDIA_TYPE}
     *
     * @param type The media type.
     * @return The builder instance.
     */
    public TemplateBuilder mediaType(MediaType type) {
      this.mediaType = type;
      return this;
    }

    /**
     * Sets the name of the template.
     *
     * @param name The name of the template.
     * @return The builder instance.
     */
    public TemplateBuilder templateName(String name) {
      this.templateName = name;
      return this;
    }

    /**
     * Builds and returns a Freemarker template according the the settings made to the builder.
     *
     * @return The Freemarker template.
     */
    public TemplateRepresentation build() throws IllegalStateException {

      if (context == null) {
        throw new IllegalStateException("The context must not be null.");
      }

      if (templateName == null || templateName.isEmpty()) {
        throw new IllegalStateException("The template name must not be null or empty.");
      }

      if (labelsKey == null || labelsKey.isEmpty()) {
        throw new IllegalStateException("The labels key must not be null or empty.");
      }

      if (locale != null && resourceBundle == null) {
        resourceBundle = ResourceBundle.getBundle(resourceBundleName, locale);
      }

      // Try to create a resource bundle depending on the client's accepted languages
      if (request != null && resourceBundle == null) {

        if (resourceBundleName == null || resourceBundleName.isEmpty()) {
          throw new IllegalStateException("The resource bundle name must not be null or empty.");
        }

        List<Preference<Language>> acceptedLanguages = request.getClientInfo().getAcceptedLanguages();

        if (acceptedLanguages.size() > 0) {
          for (Preference<Language> pref : acceptedLanguages) {
            locale = Locale.forLanguageTag(pref.getMetadata().getPrimaryTag());

            try {
              resourceBundle = ResourceBundle.getBundle(resourceBundleName, locale);
              break; // bundle was successfully created
            } catch (NullPointerException | MissingResourceException e) {
              /*
               * We do not care about the exceptions since we will use the default bundle
               * if none can be created from the request.
               */
            }

          }
        }
      }

      if (resourceBundleName == null || resourceBundleName.isEmpty()) {
        throw new IllegalStateException("The resource bundle name must not be null or empty.");
      }

      // no bundle could be created so create bundle based on defaults
      if (resourceBundle == null) {
        resourceBundle = ResourceBundle.getBundle(resourceBundleName, fallbackLocale);
      }

      ResourceBundleModel mdl = new ResourceBundleModel(resourceBundle, BeansWrapper.getDefaultInstance());
      addData(labelsKey, mdl);


      ContextTemplateLoader loader = new ContextTemplateLoader(context, templateDirectory);
      Configuration config = new Configuration();
      config.setTemplateLoader(loader);
      locale = (locale == null) ? fallbackLocale : locale;
      config.setLocale(locale);
      return new TemplateRepresentation(templateName, config, data, mediaType);
    }
  }
}
