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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker2;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Registers all {@link MergedResourcePicker} and {@link MergedResourcePicker2} services as {@link MergingResourceProvider}s.
 */
@Component
public class MergedResourcePickerWhiteboard {

    
    private BundleContext bundleContext;

    private final Map<String, ServiceRegistration<ResourceProvider>> resourceProvidersPerRoot = new ConcurrentHashMap<>();

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    protected void deactivate() {
        for (ServiceRegistration<ResourceProvider> resourceProvider : resourceProvidersPerRoot.values()) {
            resourceProvider.unregister();
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    public void bindMergedResourcePicker(MergedResourcePicker resourcePicker, Map<String, String> properties) {
        final MergedResourcePicker2 resourcePicker2 = new MergedResourcePicker2() {

            @Override
            public List<Resource> pickResources(ResourceResolver resolver, String relativePath, Resource relatedResource) {
                return resourcePicker.pickResources(resolver, relativePath);
            }
        };
        registerMergingResourceProvider(resourcePicker2, properties);
    }

    public void unbindMergedResourcePicker(Map<String, String> properties) {
        unregisterMergingResourceProvider(properties);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    public void bindMergedResourcePicker2(MergedResourcePicker2 resourcePicker, Map<String, String> properties) {
        registerMergingResourceProvider(resourcePicker, properties);
    }

    public void unbindMergedResourcePicker2(Map<String, String> properties) {
        unregisterMergingResourceProvider(properties);
    }

    private void registerMergingResourceProvider(MergedResourcePicker2 resourcePicker, Map<String, String> properties) {
        final String mergeRoot = properties.getOrDefault(MergedResourcePicker2.MERGE_ROOT, null);
        if (mergeRoot != null) {
            boolean readOnly = PropertiesUtil.toBoolean(properties.get(MergedResourcePicker2.READ_ONLY), true);
            boolean traverseParent = PropertiesUtil.toBoolean(properties.get(MergedResourcePicker2.TRAVERSE_PARENT), false);

            MergingResourceProvider provider = readOnly ?
                    new MergingResourceProvider(mergeRoot, resourcePicker, true, traverseParent) :
                    new CRUDMergingResourceProvider(mergeRoot, resourcePicker, traverseParent);
    
            final Dictionary<String, Object> props = new Hashtable<>();
            props.put(ResourceProvider.PROPERTY_NAME, readOnly ? "Merging" : "CRUDMerging");
            props.put(ResourceProvider.PROPERTY_ROOT, mergeRoot);
            props.put(ResourceProvider.PROPERTY_MODIFIABLE, !readOnly);
            props.put(ResourceProvider.PROPERTY_AUTHENTICATE, ResourceProvider.AUTHENTICATE_NO);
    
            final ServiceRegistration<ResourceProvider> resourceProvider = bundleContext.registerService(ResourceProvider.class, provider, props);
            resourceProvidersPerRoot.put(mergeRoot, resourceProvider);
        }
    }

    private void unregisterMergingResourceProvider(Map<String, String> properties) {
        final String mergeRoot = properties.getOrDefault(MergedResourcePicker2.MERGE_ROOT, null);
        if (mergeRoot != null) {
            final ServiceRegistration<ResourceProvider> resourceProvider = resourceProvidersPerRoot.get(mergeRoot);
            if (resourceProvider != null) {
                resourceProvider.unregister();
            }
        }
    }
}
