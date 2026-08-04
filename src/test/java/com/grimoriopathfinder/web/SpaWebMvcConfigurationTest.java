package com.grimoriopathfinder.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.ResourceResolverChain;

class SpaWebMvcConfigurationTest {

  @TempDir
  Path staticDirectory;

  @Test
  void resolvesClientRouteToIndexDocument() throws Exception {
    var index = new ByteArrayResource("index".getBytes());
    var resolver = new SpaWebMvcConfiguration.SpaResourceResolver(index);
    var chain = resourceResolverChainReturning(null);

    var resolved = resolver.resolveResource(
        new MockHttpServletRequest(),
        "spells/42",
        List.of(new FileSystemResource(staticDirectory.toFile())),
        chain);

    assertEquals(index, resolved);
  }

  @Test
  void doesNotFallbackApiActuatorOrAssetRequests() throws Exception {
    var index = new ByteArrayResource("index".getBytes());
    var resolver = new SpaWebMvcConfiguration.SpaResourceResolver(index);
    var chain = resourceResolverChainReturning(null);
    var location = new FileSystemResource(staticDirectory.toFile());

    assertNull(resolver.resolveResource(new MockHttpServletRequest(), "api/missing", List.of(location), chain));
    assertNull(resolver.resolveResource(new MockHttpServletRequest(), "actuator/missing", List.of(location), chain));
    assertNull(resolver.resolveResource(new MockHttpServletRequest(), "assets/missing.js", List.of(location), chain));
    assertNull(resolver.resolveResource(new MockHttpServletRequest(), "assets/missing", List.of(location), chain));
  }

  @Test
  void doesNotFallbackMissingResourceWithExtension() throws Exception {
    var index = new ByteArrayResource("index".getBytes());
    var resolver = new SpaWebMvcConfiguration.SpaResourceResolver(index);
    var chain = resourceResolverChainReturning(null);

    assertNull(resolver.resolveResource(
        new MockHttpServletRequest(),
        "favicon.ico",
        List.of(new FileSystemResource(staticDirectory.toFile())),
        chain));
  }

  private ResourceResolverChain resourceResolverChainReturning(Resource resource) {
    var chain = mock(ResourceResolverChain.class);
    when(chain.resolveResource(any(), anyString(), anyList())).thenReturn(resource);
    return chain;
  }
}
