/*
 * Copyright 2017-2023 the original author or authors.
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
package org.grails.plugins.datasource

import javax.sql.DataSource

import groovy.transform.CompileStatic
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.PropertyResolver
import org.springframework.jdbc.datasource.DataSourceTransactionManager

import org.grails.datastore.gorm.jdbc.connections.DataSourceConnectionSourceFactory
import org.grails.datastore.gorm.jdbc.connections.DataSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.core.connections.ConnectionSourcesInitializer

/**
 * A factory bean for creating the data sources
 *
 * @author Graeme Rocher
 * @since 3.3
 */
@CompileStatic
class DataSourceConnectionSourcesFactoryBean implements InitializingBean, FactoryBean<ConnectionSources<DataSource, DataSourceSettings>>,
        ApplicationContextAware {

    final PropertyResolver configuration
    ApplicationContext applicationContext
    private ConnectionSources<DataSource, ? extends ConnectionSourceSettings> connectionSources

    DataSourceConnectionSourcesFactoryBean(PropertyResolver configuration) {
        this.configuration = configuration
    }

    @Override
    ConnectionSources<DataSource, ? extends ConnectionSourceSettings> getObject() throws Exception {
        connectionSources
    }

    @Override
    Class<?> getObjectType() {
        ConnectionSources
    }

    @Override
    boolean isSingleton() {
        true
    }

    @Override
    void afterPropertiesSet() throws Exception {
        DataSourceConnectionSourceFactory factory = new DataSourceConnectionSourceFactory()
        this.connectionSources = ConnectionSourcesInitializer.create(factory, configuration)
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext
            for (ConnectionSource<DataSource, ConnectionSourceSettings> connectionSource in connectionSources.allConnectionSources) {
                if (connectionSource.name != ConnectionSource.DEFAULT) {
                    String suffix = "_${connectionSource.name}"
                    String dsName = "dataSource${suffix}"
                    String tmName = "transactionManager${suffix}"
                    if (!applicationContext.containsBean(dsName)) {
                        DataSource dataSource = connectionSource.source
                        configurableApplicationContext.beanFactory.registerSingleton(
                                dsName,
                                dataSource
                        )
                    }
                    if (!applicationContext.containsBean(tmName)) {
                        DataSource dataSource = connectionSource.source
                        configurableApplicationContext.beanFactory.registerSingleton(
                                tmName,
                                new DataSourceTransactionManager(dataSource)
                        )
                    }
                }
            }
        }
    }

}
