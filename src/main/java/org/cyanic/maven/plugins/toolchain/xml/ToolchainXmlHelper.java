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

package org.cyanic.maven.plugins.toolchain.xml;

import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Properties;

public final class ToolchainXmlHelper {

    private ToolchainXmlHelper() {}

    public static ToolchainPrivate addJDKToToolchains(Path jdkHome, String version, String vendor) throws Exception {
        ToolchainPrivate javaToolChain = buildJdkToolchain(version, vendor, jdkHome.toAbsolutePath().toString());

        File toolchainsXml = new File(new File(System.getProperty("user.home")), ".m2/toolchains.xml");

        Xpp3Dom toolchainsDom;

        if (toolchainsXml.exists()) {
            toolchainsDom = Xpp3DomBuilder.build(new FileReader(toolchainsXml));
        } else {
            toolchainsDom = new Xpp3Dom("toolchains");
        }

        toolchainsDom.addChild(jdkToolchainDom(version, vendor, jdkHome.toAbsolutePath().toString()));

        FileWriter writer = new FileWriter(toolchainsXml);

        Xpp3DomWriter.write(writer, toolchainsDom);

        writer.close();

        return javaToolChain;
    }

    private static ToolchainPrivate buildJdkToolchain(String version, String vendor, String jdkHome) {
        ToolchainModel toolchainModel = new ToolchainModel();

        toolchainModel.setType("jdk");

        Properties provides = new Properties();

        provides.setProperty("version", version);

        provides.setProperty("vendor", vendor);

        toolchainModel.setProvides(provides);

        Xpp3Dom configuration = new Xpp3Dom("configuration");

        configuration.addChild(createElement("jdkHome", jdkHome));

        toolchainModel.setConfiguration(configuration);

        DefaultJavaToolChain javaToolChain = new DefaultJavaToolChain(toolchainModel, new ConsoleLogger());

        javaToolChain.setJavaHome(jdkHome);

        return javaToolChain;
    }

    private static Xpp3Dom jdkToolchainDom(String version, String vendor, String jdkHome) {
        Xpp3Dom toolchainDom = new Xpp3Dom("toolchain");

        toolchainDom.addChild(createElement("type", "jdk"));

        Xpp3Dom providesDom = new Xpp3Dom("provides");

        providesDom.addChild(createElement("version", version));

        providesDom.addChild(createElement("vendor", vendor));

        Xpp3Dom configurationDom = new Xpp3Dom("configuration");

        configurationDom.addChild(createElement("jdkHome", jdkHome));

        toolchainDom.addChild(providesDom);

        toolchainDom.addChild(configurationDom);

        return toolchainDom;
    }

    private static Xpp3Dom createElement(String name, String value) {
        Xpp3Dom dom = new Xpp3Dom(name);

        dom.setValue(value);

        return dom;
    }
}
