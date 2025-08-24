/*
 * Copyright 2025 pjgomez97
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanic.maven.plugins.toolchain.config;

import java.util.Collections;
import java.util.Map;

public class ToolchainConfig {

    private final Map<String, Map<String, String>> toolchains;

    ToolchainConfig(Map<String, Map<String, String>> toolchains) {
        this.toolchains = toolchains;
    }

    public Map<String, Map<String, String>> getToolchains() {
        return Collections.unmodifiableMap(toolchains);
    }

    public Map<String, String> getParams(String type) {
        return Collections.unmodifiableMap(toolchains.get(type));
    }
}