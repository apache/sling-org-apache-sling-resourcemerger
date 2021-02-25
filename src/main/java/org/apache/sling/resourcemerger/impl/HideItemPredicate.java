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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Item may be either a {@link Resource} or a property which should be hidden (depending on its name).
 * 
 *
 */
public class HideItemPredicate {

    private Boolean isAllowList = null; // true in case the names list items to hide, otherwise false
    private boolean isWildcard = false;
    private final Set<String> names;

    private static final Logger LOGGER = LoggerFactory.getLogger(HideItemPredicate.class);

    public HideItemPredicate(String[] settings, String propertyPath) {
        names = new HashSet<>();
        // negated and non-negated values must not be mixed
        for (String setting : settings) {
            if (setting.equals("*")) {
                isWildcard = true;
            } else {
                boolean isNegated = setting.startsWith("!") && !setting.startsWith("!!");
                if (setting.startsWith("!")) {
                    setting = setting.substring(1);
                }
                if (isAllowList == null) {
                    isAllowList = !isNegated;
                } else {
                    if (isAllowList == isNegated) {
                        LOGGER.warn("Negated and non-negated values mixed in {}, skipping value '{}'", propertyPath, setting);
                        continue;
                    }
                }
                names.add(setting);
            }
        }

        if (isAllowList == null) {
            isAllowList = true;
        }
    }

    /**
     * Returns {@code true} for items which should be hidden
     * @param name
     * @param isLocal
     * @return {@code true} for items which should be hidden
     */
    public boolean testItem(String name, boolean isLocal) {
        if (isLocal) {
            if (names.contains(name)) {
                return isAllowList;
            }
            return !isAllowList;
        } else {
            // consider wildcard only for non-local names
            if (names.contains(name)) {
                return isAllowList;
            }
            return isWildcard;
        }
    }
    
    boolean isWildcard() {
        return isWildcard;
    }
}
