/*
 * Copyright 2025 [pjgomez97]
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

package org.cyanic.maven.plugins.toolchain;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cyanic.maven.plugins.toolchain.config.ToolchainConfig;
import org.cyanic.maven.plugins.toolchain.foojay.FoojayHelper;
import org.cyanic.maven.plugins.toolchain.jbang.JBangHelper;
import org.cyanic.maven.plugins.toolchain.sdkman.SdkmanHelper;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, configurator = "toolchain-configurator")
public class ToolchainInstallerMojo extends AbstractMojo {

    private static final String SKIP_PROPERTY = "toolchain.installer.skip";

    private static final String JDK_TOOLCHAIN_TYPE = "jdk";

    private static final String TEST_TOOLCHAIN_TYPE = "testJdk";

    private static final String DEFAULT_VENDOR = "oracle_open_jdk";

    @Component
    private ToolchainManagerPrivate toolchainManagerPrivate;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(required = true)
    private ToolchainConfig toolchains;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (shouldSkipExecution()) {
            getLog().info("Plugin execution skipped");

            return;
        }

        List<String> nonMatchedTypes = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : toolchains.getToolchains().entrySet()) {
            String type = entry.getKey();

            if (!selectToolchain(type, entry.getValue())) {
                nonMatchedTypes.add(type);
            }
        }

        if (!nonMatchedTypes.isEmpty()) {
            StringBuilder buff = new StringBuilder();

            buff.append("Cannot find matching toolchain definitions for the following toolchain types:");

            for (String type : nonMatchedTypes) {
                buff.append(System.lineSeparator());
                buff.append(getToolchainRequirementAsString(type, toolchains.getParams(type)));
            }

            getLog().error(buff.toString());

            throw new MojoFailureException(buff + System.lineSeparator()
                    + "Please make sure you define the required toolchains in your ~/.m2/toolchains.xml file");
        }
    }

    private boolean shouldSkipExecution() {
        String toolchainsSkip = System.getProperty(SKIP_PROPERTY);

        if (toolchainsSkip != null) {
            skip = Boolean.parseBoolean(toolchainsSkip);
        }

        return skip;
    }

    private boolean selectToolchain(String toolchainType, Map<String, String> requirements) throws MojoExecutionException {
        if (toolchainType.equals(TEST_TOOLCHAIN_TYPE)) {
            toolchainType = JDK_TOOLCHAIN_TYPE;
        }

        getLog().info("Required toolchain: " + getToolchainRequirementAsString(toolchainType, requirements));

        ToolchainPrivate toolchain = getToolchain(toolchainType, requirements);

        if (toolchain == null && toolchainType.equalsIgnoreCase(JDK_TOOLCHAIN_TYPE)) {
            String version = requirements.get("version");

            String vendor = requirements.get("vendor");

            if (StringUtils.isEmpty(vendor)) {
                getLog().info("No vendor specified, using default: " + DEFAULT_VENDOR);
                vendor = DEFAULT_VENDOR;
            }

            if (vendor.equalsIgnoreCase(DEFAULT_VENDOR)) {
                getLog().debug("Trying to retrieve toolchain from Sdkman");

                toolchain = SdkmanHelper.getJdkFromSdkman(getLog(), version);
            }

            if (toolchain == null && vendor.equalsIgnoreCase(DEFAULT_VENDOR)) {
                getLog().debug("Trying to retrieve toolchain from JBang");

                toolchain = JBangHelper.getJdkFromJbang(getLog(), version, vendor);
            }

            if (toolchain == null) {
                getLog().debug("Trying to download toolchain using Foojay");

                toolchain = FoojayHelper.getJdkFromFoojay(getLog(), session.getSettings().getActiveProxy(), version, vendor);
            }

            if (toolchain != null) {
                addToRequestToolchains(toolchain);
            }
        } else {
            getLog().info("Using existing toolchain: " + toolchain);
        }

        if (toolchain != null) {
            if (toolchainType.equals(JDK_TOOLCHAIN_TYPE)) {
                toolchainManagerPrivate.storeToolchainToBuildContext(toolchain, session);
            }

            return true;
        }

        return false;
    }

    private String getToolchainRequirementAsString(String type, Map<String, String> params) {
        StringBuilder buff = new StringBuilder();

        buff.append(type).append(" [");

        if (params.isEmpty()) {
            buff.append(" any");
        } else {
            for (Map.Entry<String, String> param : params.entrySet()) {
                buff.append(" ").append(param.getKey()).append(" = '").append(param.getValue());
                buff.append("'");
            }
        }

        buff.append(" ]");

        return buff.toString();
    }

    private ToolchainPrivate getToolchain(String toolchainType, Map<String, String> requirements) throws MojoExecutionException {
        try {
            ToolchainPrivate[] toolchains = getToolchains(toolchainType);

            getLog().debug("Available toolchains: " + Arrays.toString(toolchains));

            return Arrays.stream(toolchains)
                    .filter(toolchain -> toolchainType.equals(toolchain.getType()))
                    .filter(toolchain -> toolchain.matchesRequirements(requirements))
                    .findFirst()
                    .orElse(null);
        } catch (MisconfiguredToolchainException ex) {
            throw new MojoExecutionException("Misconfigured toolchains", ex);
        }
    }

    private ToolchainPrivate[] getToolchains(String type) throws MisconfiguredToolchainException {
        return toolchainManagerPrivate.getToolchainsForType(type, session);
    }

    private void addToRequestToolchains(ToolchainPrivate toolchain) {
        Map<String, List<ToolchainModel>> requestToolchains = session.getRequest().getToolchains();

        if (!requestToolchains.containsKey("jdk")) {
            requestToolchains.put("jdk", new ArrayList<>());
        }

        requestToolchains.get("jdk").add(toolchain.getModel());
    }

}
