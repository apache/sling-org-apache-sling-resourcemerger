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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker2;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MergingResourceProviderTest {

    private MergingResourceProvider mergingResourceProvider;

    private MergedResourcePicker2 picker;
    @Mock
    private ResolveContext<Void> resolveContext;
    @Mock
    private ResourceContext resourceContext;
    @Mock
    private ResourceResolver resourceResolver;
    private MockResource resource;

    private static final String MERGE_ROOT = "/mnt/override";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        picker = new MockPicker();

        Mockito.when(resolveContext.getResourceResolver()).thenReturn(resourceResolver);

        // Act like a resource provider is registered at MergingResourceProviderTest.MERGE_ROOT
        Mockito.when(resourceResolver.getResource(Mockito.startsWith(MergingResourceProviderTest.MERGE_ROOT)))
                .thenAnswer(new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        return mergingResourceProvider.getResource(resolveContext, args[0].toString(), resourceContext, null);
                    }
                });

        resource = new MockResource("/apps", null, resourceResolver);
        Mockito.when(resourceResolver.getResource("/apps"))
                .thenReturn(resource);

        mergingResourceProvider = new MergingResourceProvider(MergingResourceProviderTest.MERGE_ROOT,
                picker,false, true);
    }

    @Test
    public void testGetResource() {
        String path = MergingResourceProviderTest.MERGE_ROOT + "/apps";

        Resource rsc = mergingResourceProvider.getResource(resolveContext, path, resourceContext, null);
        Mockito.verify(resourceResolver, Mockito.times(1)).getResource("/apps");
        assertTrue(rsc instanceof MergedResource);
        assertTrue(rsc.getPath().equals(path));
    }

    @Test
    public void testGetResourceWithMultipleMergedRoots() {
        String path = MergingResourceProviderTest.MERGE_ROOT + MergingResourceProviderTest.MERGE_ROOT
                + MergingResourceProviderTest.MERGE_ROOT + MergingResourceProviderTest.MERGE_ROOT
                + "/apps";

        Resource rsc = mergingResourceProvider.getResource(resolveContext, path, resourceContext, null);
        Mockito.verify(resourceResolver, Mockito.times(0)).getResource(Mockito.startsWith(MergingResourceProviderTest.MERGE_ROOT));
        Mockito.verify(resourceResolver, Mockito.times(0)).getResource(Mockito.startsWith("/apps"));
        assertNull(rsc);
    }

    class MockPicker implements MergedResourcePicker2 {

        public List<Resource> pickResources(ResourceResolver resolver, String relativePath, Resource relatedResource) {

            String absPath = "/" + relativePath;
            final List<Resource> resources = new ArrayList<Resource>();
            final Set<String> roots = new HashSet<String>();

            Resource currentTarget = resolver.getResource(absPath);

            if (currentTarget == null) {
                currentTarget = new StubResource(resolver, absPath);
            }
            resources.add(currentTarget);
            return resources;
        }
    }

}
