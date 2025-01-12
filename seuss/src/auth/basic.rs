use axum::{http::request::Parts, response::Response};
use base64::engine::{general_purpose, Engine};
use redfish_models::models::redfish;
use redfish_core::auth::{unauthorized, AuthenticateRequest, AuthenticatedUser};
use std::str;

pub trait BasicAuthentication {
    fn authenticate(
        &self,
        username: String,
        password: String,
    ) -> Result<AuthenticatedUser, redfish::Error>;
}

#[derive(Clone)]
pub struct BasicAuthenticationProxy<B>
where
    B: BasicAuthentication + Clone,
{
    authenticator: B,
}

impl<'a, B> AsRef<dyn AuthenticateRequest + 'a> for BasicAuthenticationProxy<B>
where
    B: BasicAuthentication + Clone + 'a,
{
    fn as_ref(&self) -> &(dyn AuthenticateRequest + 'a) {
        self
    }
}

impl<B> BasicAuthenticationProxy<B>
where
    B: BasicAuthentication + Clone,
{
    pub fn new(authenticator: B) -> Self {
        Self { authenticator }
    }
}

impl<B> AuthenticateRequest for BasicAuthenticationProxy<B>
where
    B: BasicAuthentication + Clone,
{
    fn authenticate_request(
        &self,
        parts: &mut Parts,
    ) -> Result<Option<AuthenticatedUser>, Response> {
        let authorization = parts
            .headers
            .get("Authorization")
            .ok_or_else(|| unauthorized(&self.challenge()))?
            .to_str()
            .map_err(|_| unauthorized(&self.challenge()))?
            .strip_prefix("Basic ")
            .ok_or_else(|| unauthorized(&self.challenge()))?
            .to_string();

        let result = general_purpose::STANDARD
            .decode(authorization)
            .map_err(|_| unauthorized(&self.challenge()))?;

        let credentials: Vec<&str> = str::from_utf8(&result)
            .map_err(|_| unauthorized(&self.challenge()))?
            .split(':')
            .collect();

        if credentials.len() != 2 {
            return Err(unauthorized(&self.challenge()));
        }

        self.authenticator
            .authenticate(credentials[0].to_string(), credentials[1].to_string())
            .map(Some)
            .map_err(|_| unauthorized(&self.challenge()))
    }

    fn challenge(&self) -> Vec<&'static str> {
        vec!["Basic"]
    }
}
