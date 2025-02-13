/*
 * Copyright 2004-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.transaction;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import grails.core.GrailsApplication;
import grails.transaction.TransactionManagerAware;

/**
 * Injects the platform transaction manager into beans that implement {@link grails.transaction.TransactionManagerAware}.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class TransactionManagerPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, PriorityOrdered {

    private ConfigurableListableBeanFactory beanFactory;

    private PlatformTransactionManager transactionManager;

    private int order = Ordered.LOWEST_PRECEDENCE;

    private boolean initialized = false;

    /**
     * Gets the platform transaction manager from the bean factory if
     * there is one.
     * @param beanFactory The bean factory handling this post processor.
     * @throws BeansException
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
                "TransactionManagerPostProcessor requires a ConfigurableListableBeanFactory");

        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * Injects the platform transaction manager into the given bean if
     * that bean implements the {@link grails.transaction.TransactionManagerAware} interface.
     * @param bean The bean to process.
     * @param name The name of the bean.
     * @return The bean after the transaction manager has been injected.
     * @throws BeansException
     */
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String name) throws BeansException {
        if (bean instanceof TransactionManagerAware) {
            initialize();
            if (this.transactionManager != null) {
                TransactionManagerAware tma = (TransactionManagerAware) bean;
                tma.setTransactionManager(this.transactionManager);
            }
        }
        return true;
    }

    private void initialize() {
        if (this.transactionManager == null && this.beanFactory != null && !this.initialized) {
            if (this.beanFactory.containsBean(GrailsApplication.TRANSACTION_MANAGER_BEAN)) {
                this.transactionManager = this.beanFactory.getBean(GrailsApplication.TRANSACTION_MANAGER_BEAN, PlatformTransactionManager.class);
            }
            else {
                // Fetch the names of all the beans that are of type
                // PlatformTransactionManager. Note that we have to pass
                // "false" for the last argument to avoid eager initialisation,
                // otherwise we end up in an endless loop (it triggers the current method).
                String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                        this.beanFactory, PlatformTransactionManager.class, false, false);

                // If at least one is found, use the first of them as the
                // transaction manager for the application.
                if (beanNames.length > 0) {
                    this.transactionManager = (PlatformTransactionManager) this.beanFactory.getBean(beanNames[0]);
                }
            }
            this.initialized = true;
        }
    }

    public int getOrder() {
        return this.order;
    }

}
