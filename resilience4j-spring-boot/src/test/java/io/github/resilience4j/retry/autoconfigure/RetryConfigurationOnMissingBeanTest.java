/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.retry.autoconfigure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.configure.RetryAspect;
import io.github.resilience4j.retry.configure.RetryAspectExt;
import io.github.resilience4j.retry.configure.RetryConfiguration;
import io.github.resilience4j.retry.event.RetryEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    RetryConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
    RetryAutoConfiguration.class,
    RetryConfigurationOnMissingBean.class
})
@EnableConfigurationProperties(RetryProperties.class)
public class RetryConfigurationOnMissingBeanTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private RetryAspect retryAspect;

    @Autowired
    private EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry;

    @Test
    public void testAllBeansFromRetryHasOnMissingBean() throws NoSuchMethodException {
        final Class<RetryConfiguration> originalClass = RetryConfiguration.class;
        final Class<RetryConfigurationOnMissingBean> onMissingBeanClass = RetryConfigurationOnMissingBean.class;

        for (Method methodRetryConfiguration : originalClass.getMethods()) {
            if (methodRetryConfiguration.isAnnotationPresent(Bean.class)) {
                final Method methodOnMissing = onMissingBeanClass
                    .getMethod(methodRetryConfiguration.getName(),
                        methodRetryConfiguration.getParameterTypes());

                assertThat(methodOnMissing.isAnnotationPresent(Bean.class)).isTrue();

                if (!"retryEventConsumerRegistry".equals(methodOnMissing.getName()) &&
                    !"retryRegistryEventConsumer".equals(methodOnMissing.getName())) {
                    assertThat(methodOnMissing.isAnnotationPresent(ConditionalOnMissingBean.class))
                        .isTrue();
                }
            }
        }
    }

    @Test
    public void testAllRetryConfigurationBeansOverridden() {
        assertEquals(retryAspect, configWithOverrides.retryAspect);
        assertNotEquals(retryEventConsumerRegistry, configWithOverrides.retryEventConsumerRegistry);
        assertEquals(retryRegistry, configWithOverrides.retryRegistry);
    }

    @Configuration
    public static class ConfigWithOverrides {

        private RetryRegistry retryRegistry;

        private RetryAspect retryAspect;

        private EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry;

        @Bean
        public RetryRegistry retryRegistry() {
            this.retryRegistry = RetryRegistry.ofDefaults();
            return retryRegistry;
        }

        @Bean
        public RetryAspect retryAspect(RetryRegistry retryRegistry,
                                       @Autowired(required = false) List<RetryAspectExt> retryAspectExts,
                                       FallbackDecorators fallbackDecorators) {
            this.retryAspect = new RetryAspect(new RetryProperties(), retryRegistry,
                retryAspectExts, fallbackDecorators);
            return retryAspect;
        }

        @Bean
        public EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry() {
            this.retryEventConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return retryEventConsumerRegistry;
        }
    }
}