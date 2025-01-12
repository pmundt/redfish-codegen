use axum::{http::request::Parts, response::Response};
use redfish_models::models::{odata_v4, redfish, session::v1_7_2};
use redfish_core::auth::{
    unauthorized, unauthorized_with_error, AuthenticateRequest, AuthenticatedUser,
};

pub trait SessionManagement {
    type Id;
    fn session_is_valid(
        &self,
        token: String,
        origin: Option<String>,
    ) -> Result<AuthenticatedUser, redfish::Error>;
    fn sessions(&self) -> Result<Vec<odata_v4::IdRef>, redfish::Error>;
    fn create_session(
        &mut self,
        session: v1_7_2::Session,
        base_path: String,
    ) -> Result<v1_7_2::Session, redfish::Error>;
    fn get_session(&self, id: Self::Id) -> Result<v1_7_2::Session, redfish::Error>;
    fn delete_session(&mut self, id: Self::Id) -> Result<(), redfish::Error>;
}

pub trait SessionAuthentication {
    fn open_session(
        &self,
        username: String,
        password: String,
    ) -> Result<AuthenticatedUser, redfish::Error>;
    fn close_session(&self) -> Result<(), redfish::Error>;
}

#[derive(Clone)]
pub struct SessionAuthenticationProxy<S>
where
    S: SessionManagement + Clone,
{
    authenticator: S,
}

impl<'a, S> AsRef<dyn AuthenticateRequest + 'a> for SessionAuthenticationProxy<S>
where
    S: SessionManagement + Clone + 'a,
{
    fn as_ref(&self) -> &(dyn AuthenticateRequest + 'a) {
        self
    }
}

impl<S> SessionAuthenticationProxy<S>
where
    S: SessionManagement + Clone,
{
    pub fn new(authenticator: S) -> Self {
        Self { authenticator }
    }
}

impl<S> AuthenticateRequest for SessionAuthenticationProxy<S>
where
    S: SessionManagement + Clone,
{
    fn authenticate_request(
        &self,
        parts: &mut Parts,
    ) -> Result<Option<AuthenticatedUser>, Response> {
        let token = parts
            .headers
            .get("X-Auth-Token")
            .ok_or_else(|| unauthorized(&self.challenge()))?
            .to_str()
            .map_err(|_| unauthorized(&self.challenge()))?
            .to_string();

        let origin = parts
            .headers
            .get("Origin")
            .map(|value| {
                Ok(value
                    .to_str()
                    .map_err(|_| unauthorized(&self.challenge()))?
                    .to_string())
            })
            .transpose()?;

        self.authenticator
            .session_is_valid(token, origin)
            .map(Some)
            .map_err(|error| unauthorized_with_error(error, &self.challenge()))
    }

    fn challenge(&self) -> Vec<&'static str> {
        vec!["Session"]
    }
}
