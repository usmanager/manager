/*
 * MIT License
 *
 * Copyright (c) 2020 usmanager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package works.weave.socks.orders.services;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import works.weave.socks.orders.config.RestProxyTemplate;

@Service
public class AsyncGetService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final RestProxyTemplate restProxyTemplate;

  @Autowired
  public AsyncGetService(RestProxyTemplate restProxyTemplate) {
    this.restProxyTemplate = restProxyTemplate;
    RestTemplate halTemplate = new RestTemplate(restProxyTemplate.getRestTemplate().getRequestFactory());
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new Jackson2HalModule());
    MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
    halConverter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_JSON));
    halConverter.setObjectMapper(objectMapper);
    halTemplate.setMessageConverters(Collections.singletonList(halConverter));
  }

  @Async
  public <T> Future<Resource<T>> getResource(URI url, TypeReferences.ResourceType<T> type) {
    RequestEntity<Void> request = RequestEntity.get(url).accept(MediaTypes.HAL_JSON).build();
    log.debug("Requesting: " + request.toString());
    Resource<T> body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
    log.debug("Received: " + body.toString());
    return new AsyncResult<>(body);
  }

  @Async
  public <T> Future<T> getJsonResource(URI url, ParameterizedTypeReference<T> type) {
    RequestEntity<Void> request = RequestEntity.get(url).accept(MediaType.APPLICATION_JSON).build();
    log.debug("Requesting: " + request.toString());
    T body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
    log.debug("Received: " + body.toString());
    return new AsyncResult<>(body);
  }

  @Async
  public <T> Future<Resources<T>> getDataList(URI url, TypeReferences.ResourcesType<T> type) {
    RequestEntity<Void> request = RequestEntity.get(url).accept(MediaTypes.HAL_JSON).build();
    log.debug("Requesting: " + request.toString());
    Resources<T> body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
    log.debug("Received: " + body.toString());
    return new AsyncResult<>(body);
  }

  @Async
  public <T> Future<List<T>> getDataList(URI url, ParameterizedTypeReference<List<T>> type) {
    RequestEntity<Void> request = RequestEntity.get(url).accept(MediaType.APPLICATION_JSON).build();
    log.debug("Requesting: " + request.toString());
    List<T> body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
    log.debug("Received: " + body.toString());
    return new AsyncResult<>(body);
  }

  @Async
  public <T, B> Future<T> postResource(URI uri, B body, ParameterizedTypeReference<T> returnType) {
    RequestEntity<B> request = RequestEntity
        .post(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body);
    log.debug("Requesting: " + request.toString());
    T responseBody = restProxyTemplate.getRestTemplate().exchange(request, returnType).getBody();
    log.debug("Received: " + responseBody);
    return new AsyncResult<>(responseBody);
  }

}