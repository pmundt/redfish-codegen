# The Redfish Codegen Project

This project aims to develop tools which are used to translate the Redfish
specification into Rust code. The primary crate provided by this repository
is `redfish-codegen`, which contains an unopinionated translation of the
Redfish Schema Bundle (DSP8010) and the Redfish Base Registries Specification
(DSP8011).

See the [Rust docs][1] for more information.

# Building

Currently, the `build.rs` script for the redfish-codegen project invokes
`make(1)` with the makefile script in the root of the repository. This script
depends on the following utilities to be available in $PATH:

 * `curl(1)`
 * `unzip(1)`
 * `quilt(1)`
 * `java(1)`
 * `mvn`

[1]: https://docs.rs/redfish-codegen
