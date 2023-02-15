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

import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;

/**
 * {@inheritDoc}
 */
public class MergedResource extends AbstractResource {

    /** The resource resolver. */
    private final ResourceResolver resolver;

    /** Full path of the resource. */
    private final String path;

    /** Resource type. */
    private final String resourceType;

    /** Resource super type. */
    private final String resourceSuperType;

    /** Resource meta data. */
    private final ResourceMetadata metadata = new ResourceMetadata();

    /** Cache value map. */
    private final ValueMap properties;

    /** Resources which are merged together. */
    private final List<Resource> mergedResources;

    /**
     * Constructor
     *
     * @param resolver      Resource resolver
     * @param mergeRootPath   Merge root path
     * @param relativePath    Relative path
     * @param mergedResources List of physical merged resources
     * @param valueMaps List of value maps for the merged resources
     */
    MergedResource(final ResourceResolver resolver,
                   final String mergeRootPath,
                   final String relativePath,
                   final List<Resource> mergedResources,
                   final List<ValueMap> valueMaps) {
        this.resolver = resolver;
        this.path = (relativePath.length() == 0 ? mergeRootPath : mergeRootPath + "/" + relativePath);
        this.mergedResources = mergedResources;
        this.properties = new DeepReadValueMapDecorator(this, new MergedValueMap(valueMaps));

        this.resourceType = detectResourceType(relativePath);
        this.resourceSuperType = detectResourceSuperType();

        metadata.put(MergedResourceConstants.METADATA_FLAG, true);
        final String[] resourcePaths = new String[mergedResources.size()];
        int i = 0;
        for(final Resource rsrc : mergedResources) {
            resourcePaths[i] = rsrc.getPath();
            i++;
        }
        metadata.put(MergedResourceConstants.METADATA_RESOURCES, resourcePaths);
    }

    /**
     * Detect the resource type by returning the resource type of the last resource.
     * Falls back for testing or invalid resource implementations to the relative path
     * @param relativePath The relative path
     * @return The resource type
     */
    private String detectResourceType(final String relativePath) {
        String type = this.mergedResources.get(this.mergedResources.size() - 1).getResourceType();
        if ( type == null ) {
            type = relativePath.length() == 0 ? "/" : relativePath;
        }
        return type;
    }

    /**
     * Detect the resource super type by returning the value of a resource type property
     * if it is not equal to the detect resource type.
     * @param valueMaps The value maps of the merged resources
     * @return The resource super type or {@code null}
     */
    private String detectResourceSuperType() {
        final String type = this.properties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, String.class);
        if ( type != null && !type.equals(this.resourceType)) {
            return type;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return this.path;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceType() {
        return this.resourceType;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceSuperType() {
        return this.resourceSuperType;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    public List<Resource> getMergedResources() {
        return this.mergedResources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) this.properties;
        }
        return super.adaptTo(type);
    }


    // ---- Object ------------------------------------------------------------

    /**
     * Merged resources are considered equal if their paths are equal,
     * regardless of the list of mapped resources.
     *
     * @param o Object to compare with
     * @return Returns <code>true</code> if the two merged resources have the
     *         same path.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        final Resource r = (Resource) o;
        return r.getPath().equals(getPath());
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

    @Override
    public String toString() {
        final Object resources = this.metadata.get(MergedResourceConstants.METADATA_RESOURCES);
        return "MergedResource [path=" + this.path + ", resources="
                + ((resources instanceof String[]) ? Arrays.toString((String[]) resources) : resources.toString())
                + "]";
    }
}
