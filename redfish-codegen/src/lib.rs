pub mod models;
pub mod registries;

/// The version of the Redfish Schema Specification provided by this crate.
pub const REDFISH_VERSION: &str = "2022.3";

/// The OData Version supported by this version of the redfish data model.
pub const ODATA_VERSION: &str = "4.0";

/// The base URL of the Redfish json-schema files.
pub const SCHEMA_BASE_URL: &str = "https://redfish.dmtf.org/schemas/v1";

/// Metadata about a model.
pub trait Metadata<'a> {
    const JSON_SCHEMA: &'a str;
}
