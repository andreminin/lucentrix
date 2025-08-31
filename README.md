# Lucentrix

**Lucentrix** is a Java-based toolkit providing a unified API for working with various Lucene-compatible search engines (such as Solr, Elasticsearch, OpenSearch, and potentially others). It offers:

- **Wrappers** over search engines, allowing common query language support across multiple engines.
- **Ingestion tools** and a plugin-based crawler to streamline data migration and indexing workflows.

## Features

- **Engine-agnostic API**: Send queries through a unified interface regardless of the underlying search engine.
- **Plugin-based Crawler**: A standalone, plain Java crawler (no UI, no Spring overhead) that uses a plugin framework to support both source and target connectors.
- **Rapid indexing with dummy data**: Populate indexes (Solr or others) with synthetic documents for testing or benchmarking.
- **Modular design**: Supports adding new engine connectors via a plugin API layer.

## Getting Started

### Prerequisites

- Java 17 or later
- Maven (for building the project)

### Building the Project

```bash
git clone https://github.com/andreminin/lucentrix.git
cd lucentrix
mvn clean package
```

### Basic Usage

1. **Identify or configure your target engine**, for example:
   - Solr
   - Elasticsearch
   - OpenSearch

2. **Develop or configure plugins** for the required source and index connectors using the provided plugin API.

3. **Run the crawler** with your plugin configuration to ingest data:

Update application.properties
```text
id=crawler1
statistics.interval.sec=121
shutdown.timeout.sec=31
sleep.interval.ms=501
idle.sleep.interval.ms=10001
persistence.path=persistence
source.page.size=1001

source.plugin.id=dummy-source-plugin
target.plugin.id=solr-target-plugin

dummy-source-plugin.config=classpath:/config/dummy.json
solr-target-plugin.config=classpath:/config/solr.json

encryption.secret=QeLOFw7ETiBBcsQenVf5RMdTAqVvw9nXZdRw6wUvXs8=
encryption.salt=3RXxBn1QGLIU841xdka0Ng==
encryption.iv=b+LEFfyuhkQEFgpWqlGm7Q==
```
and run crawler:

```bash
run_crawler.sh
```

## Plugin API

Lucentrix follows a plugin framework pattern:

- Define connectors for data sources and indices using Java plugin implementations.
- Interface with the engine through common abstractions.
- More details on plugin development will be expanded in future documentation.

## Project Structure

```
lucentrix/
├── core/               # Core abstractions and APIs
├── ingestion/          # Ingestion logic
├── plugin-api/         # Plugin interfaces
├── plugins/            # Sample or built-in plugins
├── distribution/       # Distribution packaging
├── shared-assemblies/  # Shared dependencies
├── pom.xml             # Maven project file
└── README.md
```

## Roadmap & Future Enhancements

- Add a **Spring-based version** with UI for easier management.
- Expand plugin ecosystem for additional engines and data sources.
- Release official binaries and package artifacts for easier integration and reuse.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch with your feature or fix.
3. Open a Pull Request describing your changes.
4. Ensure tests pass and all Java code follows standard style conventions.

## License

This project is released under the **MIT License**. Feel free to use or modify it freely. ([github.com](https://github.com/andreminin/lucentrix))
