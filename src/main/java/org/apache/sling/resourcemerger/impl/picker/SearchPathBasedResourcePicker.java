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
package org.apache.sling.resourcemerger.impl.picker;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourcemerger.api.ResourceMergerService;
import org.apache.sling.resourcemerger.impl.MergedResource;
import org.apache.sling.resourcemerger.impl.MergedResourceConstants;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker2;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(name="org.apache.sling.resourcemerger.impl.MergedResourceProviderFactory")
@Designate(ocd=SearchPathBasedResourcePicker.Configuration.class)
/**
 * The <code>SearchPathBasedResourcePicker</code> delivers merged resources based on the resource resolver's search path.
 */
public class SearchPathBasedResourcePicker implements MergedResourcePicker2, ResourceMergerService {

    public static final String DEFAULT_ROOT = "/mnt/overlay";

    @ObjectClassDefinition(
            id = "org.apache.sling.resourcemerger.impl.MergedResourceProviderFactory",
            name = "Apache Sling Resource Merger - Search Path Based Resource Picker",
            description = "This resource picker delivers merged resources based on the search paths (overlay approach).")
    @interface Configuration {
        @AttributeDefinition(name = "Root", description = "The mount point of merged resources.")
        String merge_root() default SearchPathBasedResourcePicker.DEFAULT_ROOT;
        @AttributeDefinition(name = "Read Only", description = "Specifies if the resources are read-only or can be modified.")
        boolean merge_readOnly() default true;
    }

    private String mergeRootPath;

    @Override
    public List<Resource> pickResources(final ResourceResolver resolver, final String relativePath,
                                        final Resource relatedResource) {
        List<Resource> relatedMappedResources = null;
        if (relatedResource instanceof MergedResource) {
            relatedMappedResources = ((MergedResource) relatedResource).getMergedResources();

            // Check if the path is the same
            if (relatedResource.getPath().equals(mergeRootPath + '/' + relativePath)) {
                return relatedMappedResources;
            }
        }

        final List<Resource> resources = new ArrayList<>();
        final String[] searchPaths = resolver.getSearchPath();
        for (int i = searchPaths.length - 1; i >= 0; i--) {
            final String basePath = searchPaths[i];
            final String fullPath = basePath + relativePath;

            int baseIndex = resources.size();
            Resource baseResource = null;
            if (relatedMappedResources != null && relatedMappedResources.size() > baseIndex) {
                baseResource = relatedMappedResources.get(baseIndex);
            }

            Resource resource = (baseResource != null) ? getFromBaseResource(resolver, baseResource, fullPath) : null;
            if (resource == null) {
                resource = resolver.getResource(fullPath);
                if (resource == null) {
                    resource = new NonExistingResource(resolver, fullPath);
                }
            }
            resources.add(resource);
        }
        return resources;
    }

    /**
     * @return <code>null</code> if it did not try to resolve the resource. {@link NonExistingResource} if it could not
     * find the resource.
     */
    private Resource getFromBaseResource(final ResourceResolver resolver, final Resource baseResource,
                                         final String path) {
        final Resource resource;
        final String baseResourcePath = baseResource.getPath();
        // Check if the path is a child of the base resource
        if (path.startsWith(baseResourcePath + '/')) {
            String relPath = path.substring(baseResourcePath.length() + 1);
            resource = baseResource.getChild(relPath);
        }
        // Check if the path is a direct parent of the base resource
        else if (baseResourcePath.startsWith(path) && baseResourcePath.lastIndexOf('/') == path.length()) {
            resource = baseResource.getParent();
        }
        // The two resources are not related enough, retrieval cannot be optimised
        else {
            return null;
        }
        return (resource != null) ? resource : new NonExistingResource(resolver, path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMergedResourcePath(final String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("Provided relative path is null");
        }

        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Provided path is not a relative path");
        }

        return mergeRootPath + "/" + relativePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getMergedResource(final Resource resource) {
        if (resource != null) {
            final ResourceResolver resolver = resource.getResourceResolver();
            final String[] searchPaths = resolver.getSearchPath();
            for (final String searchPathPrefix : searchPaths) {
                if (resource.getPath().startsWith(searchPathPrefix)) {
                    final String searchPath = searchPathPrefix.substring(0, searchPathPrefix.length() - 1);
                    return resolver.getResource(resource.getPath().replaceFirst(searchPath, mergeRootPath));
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMergedResource(final Resource resource) {
        if (resource == null) {
            return false;
        }

        return Boolean.TRUE.equals(resource.getResourceMetadata().get(MergedResourceConstants.METADATA_FLAG));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourcePath(final String searchPath, final String mergedResourcePath) {
        if( searchPath == null || !searchPath.startsWith("/") || !searchPath.endsWith("/") ) {
            throw new IllegalArgumentException("Provided path is not a valid search path: " + searchPath);
        }
        if ( mergedResourcePath == null || !mergedResourcePath.startsWith(this.mergeRootPath + "/") ) {
            throw new IllegalArgumentException("Provided path does not point to a merged resource: " + mergedResourcePath);
        }
        return searchPath + mergedResourcePath.substring(this.mergeRootPath.length() + 1);
    }

    @Activate
    protected void configure(final Configuration configuration) {
        mergeRootPath = configuration.merge_root();
    }
}
