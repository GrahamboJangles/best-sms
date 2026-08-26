# External implementation references

- Android SmsManager API: https://developer.android.com/reference/android/telephony/SmsManager
  Public APIs cover SMS and MMS operations, including sendMultimediaMessage and MMS error/result constants.
- Android default handlers guide: https://developer.android.com/guide/topics/permissions/default-handlers
  Default SMS apps must perform core messaging functionality and obtain user consent before sensitive permissions.
- Android Telephony.Mms API: https://developer.android.com/reference/android/provider/Telephony.Mms
  Standard platform MMS provider exposes MMS messages, addresses, and parts.
- Android IMS documentation: https://source.android.com/docs/core/connect/ims
  IMS/RCS services are carrier-configured or privileged/device-integrated; ordinary apps should not impersonate private RCS services.
- Android 15 features: https://developer.android.com/about/versions/15/features
  Android distinguishes SMS/MMS app support from preloaded RCS applications.
- Google Messages feature support: https://support.google.com/messages/answer/10456318?hl=en
  Modern reference features include scheduled messages and related conversation workflows.
- Google Messages overview: https://android.com/google-messages/
  Modern reference features include spam/phishing filtering and suggested replies.
