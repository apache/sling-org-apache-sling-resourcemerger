/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.resourcemerger.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * A <code>MergedValueMap</code> is a {@link ValueMap} aggregated from the
 * different resources mapped to a {@link MergedResource}.
 */
public class MergedValueMap extends ValueMapDecorator {

    /**
     * Set of properties to exclude from override
     */
    private static final Set<String> EXCLUDED_PROPERTIES = new HashSet<>();

    static {
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_HIDE_PROPERTIES);
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_HIDE_RESOURCE);
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_HIDE_CHILDREN);
        EXCLUDED_PROPERTIES.add(MergedResourceConstants.PN_ORDER_BEFORE);
    }

    /**
     * Constructor
     *
     * @param valueMaps a list of value maps to be aggregated into <i>this</i> value map
     */
    public MergedValueMap(final List<ValueMap> valueMaps) {
        super(new HashMap<>());
        
        // Iterate over value maps
        for (final ValueMap vm : valueMaps) {
            // Get properties to hide from local or underlying value maps
            String[] hideSettings = vm.get(MergedResourceConstants.PN_HIDE_PROPERTIES, String[].class);
            if (hideSettings != null) {
                HideItemPredicate hidePredicate = new HideItemPredicate(hideSettings, MergedResourceConstants.PN_HIDE_PROPERTIES);
                
                // go over the already existing properties
                this.entrySet().removeIf(entry -> hidePredicate.testItem(entry.getKey(), false));
                
                // then go over the new properties
                this.putAll(vm.entrySet().stream()
                        .filter(entry -> !(EXCLUDED_PROPERTIES.contains(entry.getKey())) && !(hidePredicate.testItem(entry.getKey(), true)) )
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } else {
                this.putAll(vm.entrySet().stream()
                        .filter(entry -> !(EXCLUDED_PROPERTIES.contains(entry.getKey())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
        }
    }
}
