/*
 * Copyright 2015-2022 the original author or authors.
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
package org.grails.cli.profile.commands

/**
 *  Creates a profile
 *
 * @author Graeme Rocher
 * @since 3.1
 */
class CreateProfileCommand extends CreateAppCommand {

    public static final String NAME = 'create-profile'

    CreateProfileCommand() {
        description.description = 'Creates a profile'
        description.usage = 'create-profile [NAME]'
    }

    @Override
    protected void populateDescription() {
        description.argument(name: 'Profile Name', description: 'The name of the plugin to create.', required: false)
    }

    @Override
    String getName() { NAME }

    @Override
    protected String getDefaultProfile() { 'profile' }

}
