/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.entity.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import org.apache.brooklyn.api.catalog.CatalogConfig;
import org.apache.brooklyn.api.effector.Effector;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.effector.Effectors;
import org.apache.brooklyn.core.entity.EntityInternal;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.brooklyn.util.core.text.TemplateProcessor;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.stream.KnownSizeInputStream;
import org.apache.brooklyn.util.text.Strings;

public class DatastoreMixins {

    private DatastoreMixins() {}
    
    
    public static final AttributeSensor<String> DATASTORE_URL = HasDatastoreUrl.DATASTORE_URL;
    
    public static interface HasDatastoreUrl {
        public static final AttributeSensor<String> DATASTORE_URL = Sensors.newStringSensor("datastore.url",
            "Primary contact URL for a datastore (e.g. mysql://localhost:3306/)");
    }

    
    public static final Effector<String> EXECUTE_SCRIPT = CanExecuteScript.EXECUTE_SCRIPT;
    
    public static interface CanExecuteScript {
        Effector<String> EXECUTE_SCRIPT = Effectors.effector(String.class, "executeScript")
            .description("executes the given script contents")
            .parameter(ConfigKeys.newStringConfigKey("commands"))
            .buildAbstract();
    }

    
    public static final ConfigKey<String> CREATION_SCRIPT_CONTENTS = CanGiveCreationScript.CREATION_SCRIPT_CONTENTS;
    public static final ConfigKey<String> CREATION_SCRIPT_URL = CanGiveCreationScript.CREATION_SCRIPT_URL;
    public static final ConfigKey<String> CREATION_SCRIPT_TEMPLATE = CanGiveCreationScript.CREATION_SCRIPT_TEMPLATE;

    public static interface CanGiveCreationScript {
        @CatalogConfig(label = "Creation script contents")
        @SetFromFlag("creationScriptContents")
        public static final ConfigKey<String> CREATION_SCRIPT_CONTENTS = ConfigKeys.newStringConfigKey(
                "datastore.creation.script.contents",
                "Contents of creation script to initialize the datastore",
                "");

        @CatalogConfig(label = "Creation script URL")
        @SetFromFlag("creationScriptUrl")
        public static final ConfigKey<String> CREATION_SCRIPT_URL = ConfigKeys.newStringConfigKey(
                "datastore.creation.script.url",
                "URL of creation script to use to initialize the datastore (ignored if creationScriptContents is specified)",
                "");

        @CatalogConfig(label = "Creation script template URL")
        @SetFromFlag("creationScriptTemplateUrl")
        public static final ConfigKey<String> CREATION_SCRIPT_TEMPLATE = ConfigKeys.newStringConfigKey(
                "datastore.creation.script.template.url",
                "URL of creation script Freemarker template used to initialize the datastore "
                        + "(ignored if datastore.creation.script.contents or datastore.creation.script.url is specified)",
                "");
    }

    /** returns the creation script contents, if it exists, or null if none is defined (error if it cannot be loaded) */
    @Nullable public static InputStream getDatabaseCreationScript(Entity entity) {
        String url = entity.getConfig(DatastoreMixins.CREATION_SCRIPT_URL);
        if (!Strings.isBlank(url))
            return new ResourceUtils(entity).getResourceFromUrl(url);

        String templateUrl = entity.getConfig(DatastoreMixins.CREATION_SCRIPT_TEMPLATE);
        if (!Strings.isBlank(templateUrl)) {
            String template = TemplateProcessor.processTemplateContents("datastore mixin "+templateUrl, new ResourceUtils(entity).getResourceAsString(templateUrl), (EntityInternal) entity, ImmutableMap.<String, Object>of());
            try {
                return new ByteArrayInputStream(template.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw Exceptions.propagate(e);
            }
        }

        String contents = entity.getConfig(DatastoreMixins.CREATION_SCRIPT_CONTENTS);
        if (!Strings.isBlank(contents))
            return KnownSizeInputStream.of(contents);
        return null;
    }

    /** returns the creation script contents, if it exists, or null if none is defined (error if it cannot be loaded) */
    @Nullable public static String getDatabaseCreationScriptAsString(Entity entity) {
        String url = entity.getConfig(DatastoreMixins.CREATION_SCRIPT_URL);
        if (!Strings.isBlank(url))
            return new ResourceUtils(entity).getResourceAsString(url);

        String templateUrl = entity.getConfig(DatastoreMixins.CREATION_SCRIPT_TEMPLATE);
        if (!Strings.isBlank(templateUrl))
            return TemplateProcessor.processTemplateContents("datastore mixin "+templateUrl, new ResourceUtils(entity).getResourceAsString(templateUrl), (EntityInternal) entity, ImmutableMap.<String, Object>of());

        String contents = entity.getConfig(DatastoreMixins.CREATION_SCRIPT_CONTENTS);
        if (!Strings.isBlank(contents))
            return contents;
        return null;
    }
    
    /** An entity with the most common datastore config, sensors, and effectors */ 
    public interface DatastoreCommon extends Entity, DatastoreMixins.HasDatastoreUrl, DatastoreMixins.CanExecuteScript, DatastoreMixins.CanGiveCreationScript {
    }

}
