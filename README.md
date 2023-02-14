# @Contract Validator for JetBrains Annotations

![GitHub Workflow Status (branch)](https://img.shields.io/github/actions/workflow/status/zml2008/contract-validator/CI?branch=trunk) [![LGPL v3.0 or later License](https://img.shields.io/badge/license-LGPL--3.0-blue)](COPYING.LESSER) [![Maven Central](https://img.shields.io/maven-central/v/ca.stellardrift/contract-validator?label=stable)](https://search.maven.org/search?q=g:ca.stellardrift%20AND%20a:contract-validator*) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/ca.stellardrift/contract-validator?label=dev&server=https%3A%2F%2Foss.sonatype.org)

This is a simple annotation processor to validate the syntax used for `@Contract` annotations.

This does not validate the contracts themselves.

## Usage

The contract validator requires Java 8 or later, and is published on maven central. It runs as an annotation processor at compile time.

### Gradle

```gradle
dependencies {
  annotationProcessor("ca.stellardrift:contract-validator:1.0.1")
}
```

### <other build tools>

It should work, if you use another build tool feel free to help fill out this section

## License

`contract-validator` is released under the terms of the GNU Lesser General Public License version 3 or later.

## Contributing

Pull requests are accepted. Please open an issue to discuss any larger feature changes before starting. This helps ensure your design direction does not conflict with the goals of this tool.
