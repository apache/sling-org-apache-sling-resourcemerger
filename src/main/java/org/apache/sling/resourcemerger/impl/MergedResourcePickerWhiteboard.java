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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker2;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Registers all {@link MergedResourcePicker} and {@link MergedResourcePicker2} services as {@link MergingResourceProvider}s.
 */
@SuppressWarnings("deprecation")
@Component
public class MergedResourcePickerWhiteboard {

    private final @NotNull BundleContext bundleContext;

    private final Map<Long, ServiceRegistration<ResourceProvider<Void>>> resourceProvidersPerPickerServiceId = new ConcurrentHashMap<>();

    @Activate
    public MergedResourcePickerWhiteboard(final @NotNull BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    protected void deactivate() {
        for (ServiceRegistration<ResourceProvider<Void>> resourceProvider : resourceProvidersPerPickerServiceId.values()) {
            try {
                resourceProvider.unregister();
            } catch ( final IllegalStateException ise ) {
                // we ignore this as the service might already be gone
            }
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindMergedResourcePicker(MergedResourcePicker resourcePicker, Map<String, Object> properties) {
        registerMergingResourceProvider((resolver, relativePath, relatedResource) -> resourcePicker.pickResources(resolver, relativePath), properties);
    }

    public void unbindMergedResourcePicker(Map<String, Object> properties) {
        unregisterMergingResourceProvider(properties);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindMergedResourcePicker2(MergedResourcePicker2 resourcePicker, Map<String, Object> properties) {
        registerMergingResourceProvider(resourcePicker, properties);
    }

    public void unbindMergedResourcePicker2(Map<String, Object> properties) {
        unregisterMergingResourceProvider(properties);
    }

    @SuppressWarnings("unchecked")
    private void registerMergingResourceProvider(MergedResourcePicker2 resourcePicker, Map<String, Object> properties) {
        final String mergeRoot = (String)properties.getOrDefault(MergedResourcePicker2.MERGE_ROOT, null);
        if (mergeRoot != null) {
            boolean readOnly = PropertiesUtil.toBoolean(properties.get(MergedResourcePicker2.READ_ONLY), true);
            boolean traverseParent = PropertiesUtil.toBoolean(properties.get(MergedResourcePicker2.TRAVERSE_PARENT), false);

            ResourceProvider<Void> provider = readOnly ?
                    new MergingResourceProvider(mergeRoot, resourcePicker, true, traverseParent) :
                    new CRUDMergingResourceProvider(mergeRoot, resourcePicker, traverseParent);
            final Dictionary<String, Object> props = new Hashtable<>();
            props.put(ResourceProvider.PROPERTY_NAME, readOnly ? "Merging" : "CRUDMerging");
            props.put(ResourceProvider.PROPERTY_ROOT, mergeRoot);
            props.put(ResourceProvider.PROPERTY_MODIFIABLE, !readOnly);
            props.put(ResourceProvider.PROPERTY_AUTHENTICATE, ResourceProvider.AUTHENTICATE_NO);
            final Long key = (Long) properties.get(Constants.SERVICE_ID);
            final ServiceRegistration<ResourceProvider<Void>> resourceProvider = (ServiceRegistration<ResourceProvider<Void>>)bundleContext.registerService(ResourceProvider.class.getName(), provider, props);
            resourceProvidersPerPickerServiceId.put(key, resourceProvider);
        }
    }

    private void unregisterMergingResourceProvider(Map<String, Object> properties) {
        final Long key = (Long) properties.get(Constants.SERVICE_ID);
        if (key != null) {
            final ServiceRegistration<ResourceProvider<Void>> resourceProvider = resourceProvidersPerPickerServiceId.get(key);
            if (resourceProvider != null) {
                try {
                    resourceProvider.unregister();
                } catch ( final IllegalStateException ise ) {
                    // we ignore this as the service might already be gone
                }
            }
        }
    }
}
