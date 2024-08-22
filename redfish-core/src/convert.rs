use redfish_models::{
    models::{event::v1_10_1::EventRecord, message::v1_2_1::Message},
    Registry,
};

pub trait IntoRedfishMessage {
    fn into_redfish_message(self) -> Message;
}

impl<S> IntoRedfishMessage for S
where
    S: Registry<'static> + Clone,
{
    fn into_redfish_message(self) -> Message {
        Message {
            message_id: self.id().to_string(),
            message_severity: Some(self.severity()),
            resolution: Some(self.resolution().to_string()),
            message: Some(self.clone().message()),
            message_args: self.args(),
            ..Default::default()
        }
    }
}

pub trait IntoEventRecord {
    fn into_event_record(self) -> EventRecord;
}

impl<S> IntoEventRecord for S
where
    S: Registry<'static> + Clone,
{
    fn into_event_record(self) -> EventRecord {
        EventRecord {
            message_id: self.id().to_string(),
            message_severity: Some(self.severity()),
            message: Some(self.clone().message()),
            message_args: self.args(),
            ..Default::default()
        }
    }
}
