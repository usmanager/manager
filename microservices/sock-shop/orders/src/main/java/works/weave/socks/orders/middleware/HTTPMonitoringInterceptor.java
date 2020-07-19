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

package works.weave.socks.orders.middleware;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.prometheus.client.Histogram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class HTTPMonitoringInterceptor implements HandlerInterceptor {

  static final Histogram REQUEST_LATENCY = Histogram.build()
      .name("request_duration_seconds")
      .help("Request duration in seconds.")
      .labelNames("service", "method", "route", "status_code")
      .register();

  private static final String START_TIME_KEY = "startTime";

  @Autowired
  private ResourceMappings mappings;

  @Autowired
  private JpaHelper jpaHelper;

  @Autowired
  private RepositoryRestConfiguration repositoryConfiguration;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RequestMappingHandlerMapping requestMappingHandlerMapping;

  private Set<PatternsRequestCondition> urlPatterns;

  @Value("${spring.application.name:orders}")
  private String serviceName;

  @Override
  public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse
      httpServletResponse, Object o) {
    httpServletRequest.setAttribute(START_TIME_KEY, System.nanoTime());
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse
      httpServletResponse, Object o, ModelAndView modelAndView) {
    long start = (long) httpServletRequest.getAttribute(START_TIME_KEY);
    long elapsed = System.nanoTime() - start;
    double seconds = (double) elapsed / 1000000000.0;
    String matchedUrl = getMatchingURLPattern(httpServletRequest);
    if (!matchedUrl.isEmpty()) {
      REQUEST_LATENCY.labels(
          serviceName,
          httpServletRequest.getMethod(),
          matchedUrl,
          Integer.toString(httpServletResponse.getStatus())
      ).observe(seconds);
    }
  }

  @Override
  public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse
      httpServletResponse, Object o, Exception e) {
  }

  private String getMatchingURLPattern(HttpServletRequest httpServletRequest) {
    String res = "";
    for (PatternsRequestCondition pattern : getUrlPatterns()) {
      if (pattern.getMatchingCondition(httpServletRequest) != null
          && !httpServletRequest.getServletPath().equals("/error")) {
        res = pattern.getMatchingCondition(httpServletRequest).getPatterns().iterator().next();
        break;
      }
    }
    return res;
  }

  private Set<PatternsRequestCondition> getUrlPatterns() {
    if (this.urlPatterns == null) {
      this.urlPatterns = new HashSet<>();
      requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) ->
          urlPatterns.add(mapping.getPatternsCondition()));
      RepositoryRestHandlerMapping repositoryRestHandlerMapping = new
          RepositoryRestHandlerMapping(mappings, repositoryConfiguration);
      repositoryRestHandlerMapping.setJpaHelper(jpaHelper);
      repositoryRestHandlerMapping.setApplicationContext(applicationContext);
      repositoryRestHandlerMapping.afterPropertiesSet();
      repositoryRestHandlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) ->
          urlPatterns.add(mapping.getPatternsCondition()));
    }
    return this.urlPatterns;
  }

}