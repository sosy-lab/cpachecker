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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.appengine.entity.DefaultOptions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;


public class JobsResourceJSONModule extends SimpleModule {

  private static final long serialVersionUID = 1L;

  public JobsResourceJSONModule() {
    super();
    addDeserializer(Map.class, new SettingsDeserializer());
  }

  private class SettingsDeserializer extends JsonDeserializer<Map<String, Object>> {

    @Override
    public Map<String, Object> deserialize(JsonParser parser, DeserializationContext pArg1) throws IOException, JsonProcessingException {
      parser.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);

      Map<String, Object> settings = new HashMap<>();
      while (parser.nextToken() != null) {
        JsonToken token = parser.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          switch (parser.getCurrentName()) {
          case "specification":
            settings.put("specification", parser.nextTextValue());
            break;
          case "configuration":
            settings.put("configuration", parser.nextTextValue());
            break;
          case "programText":
            settings.put("programText", parser.nextTextValue());
            break;
          case "options":
            if ((token = parser.nextToken()) == JsonToken.START_OBJECT) {
              DefaultOptions options = new DefaultOptions();
              while (parser.nextValue() == JsonToken.VALUE_STRING) {
                options.setOption(parser.getCurrentName(), parser.getValueAsString());
              }
              settings.put("options", options.getUsedOptions());
            }
            break;
          }
        }
      }

      return settings;
    }

    @Override
    public Class<?> handledType() {return Map.class;}

  }

}
