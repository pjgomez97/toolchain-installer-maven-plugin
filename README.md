# Toolchain Installer Maven Plugin

A Maven plugin that automatically installs and configures Java toolchains for your Maven builds. This plugin integrates with multiple JDK management tools and can automatically download JDKs when needed.

> **Note**: This project is a fork of the original [toolchains-maven-plugin](https://github.com/linux-china/toolchains-maven-plugin) by linux-china, with additional improvements and modifications.

## Features

- **Automatic JDK Installation**: Automatically installs required JDKs if they're not available
- **Multiple JDK Sources**: Supports multiple JDK management tools:
  - **SDKMAN!**: Uses locally installed JDKs from SDKMAN!
  - **JBang**: Installs JDKs using JBang if available
  - **Foojay**: Downloads JDKs from Foojay API when other sources are unavailable
- **Maven Toolchain Integration**: Seamlessly integrates with Maven's toolchain system
- **Flexible Configuration**: Supports both JDK and test JDK toolchain types
- **Proxy Support**: Respects Maven proxy settings for downloads

## Requirements

- Java 21 or higher
- Maven 3.9.11 or higher
- Optional: SDKMAN! or JBang for local JDK management

## Installation

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>org.cyanic</groupId>
    <artifactId>toolchain-installer-maven-plugin</artifactId>
    <version>0.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <toolchains>
            <jdk>
                <version>17</version>
                <vendor>oracle_open_jdk</vendor>
            </jdk>
            <testJdk>
                <version>21</version>
                <vendor>oracle_open_jdk</vendor>
            </testJdk>
        </toolchains>
    </configuration>
</plugin>
```

## Configuration

### Toolchain Types

The plugin supports two main toolchain types:

- **`jdk`**: Main JDK for compilation and execution
- **`testJdk`**: JDK for running tests (automatically mapped to `jdk` type)

### Configuration Parameters

Each toolchain can be configured with the following parameters:

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `version` | Java version (e.g., "17", "21") | - | Yes |
| `vendor` | JDK vendor (e.g., "oracle_open_jdk", "eclipse_temurin") | `oracle_open_jdk` | No |

### Example Configurations

#### Basic Configuration
```xml
<toolchains>
    <jdk>
        <version>17</version>
    </jdk>
</toolchains>
```

#### Multiple JDKs
```xml
<toolchains>
    <jdk>
        <version>17</version>
        <vendor>eclipse_temurin</vendor>
    </jdk>
    <testJdk>
        <version>21</version>
        <vendor>oracle_open_jdk</vendor>
    </testJdk>
</toolchains>
```

#### Specific Vendor
```xml
<toolchains>
    <jdk>
        <version>17</version>
        <vendor>eclipse_temurin</vendor>
    </jdk>
</toolchains>
```

## How It Works

The plugin follows this sequence when looking for JDKs:

1. **Existing Toolchains**: First checks if a matching toolchain is already configured in `~/.m2/toolchains.xml`
2. **SDKMAN!**: If using the default vendor (`oracle_open_jdk`), checks for JDKs installed via SDKMAN!
3. **JBang**: Attempts to install the JDK using JBang if available
4. **Foojay**: Downloads the JDK from Foojay API as a last resort

### JDK Installation Process

When a JDK is found or installed, the plugin:

1. Adds the JDK to the Maven toolchain configuration
2. Stores the toolchain in the build context
3. Makes it available for the current Maven session

## Usage Examples

### Running the Plugin

The plugin runs automatically during the `validate` phase, but you can also run it manually:

```bash
mvn toolchain-installer:install
```

### Skipping Execution

You can skip the plugin execution using the `skip` parameter:

```xml
<configuration>
    <skip>true</skip>
    <toolchains>
        <!-- ... -->
    </toolchains>
</configuration>
```

Or via system property:

```bash
mvn clean install -Dtoolchain.installer.skip=true
```

## Supported JDK Vendors

The plugin supports various JDK vendors through the Foojay API:

- `oracle_open_jdk` (default)
- `eclipse_temurin`
- `amazon_corretto`
- `microsoft_openjdk`
- `azul_zulu`
- `graalvm_ce`
- `liberica`
- And many more...

## Troubleshooting

### Common Issues

1. **JDK Not Found**: Ensure the version and vendor are correct
2. **Download Failures**: Check your internet connection and proxy settings
3. **Permission Issues**: Ensure write access to `~/.m2/toolchains.xml`

### Debug Information

Enable debug logging to see detailed information about the plugin's operation:

```bash
mvn clean install -X
```

## Development

### Building from Source

```bash
git clone <repository-url>
cd toolchain-installer-maven-plugin
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/cyanic/maven/plugins/toolchain/
│   │       ├── config/           # Configuration parsing
│   │       ├── foojay/           # Foojay API integration
│   │       ├── jbang/            # JBang integration
│   │       ├── sdkman/           # SDKMAN! integration
│   │       └── xml/              # Toolchain XML utilities
│   └── resources/
│       └── META-INF/plexus/      # Plexus component configuration
└── test/
    └── java/                     # Unit tests
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the Apache License 2.0.

## Acknowledgments

- [linux-china](https://github.com/linux-china) for the original [toolchains-maven-plugin](https://github.com/linux-china/toolchains-maven-plugin)
- [Foojay](https://foojay.io/) for providing the JDK download API
- [SDKMAN!](https://sdkman.io/) for JDK management
- [JBang](https://www.jbang.dev/) for JDK installation capabilities
- Apache Maven team for the toolchain system
