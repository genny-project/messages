# MESSAGES
External Message Server used by the Genny-project.

# Project-Specific attributes to be added to GENNY sheet : 

# EMAIL SERVICE

Following are the attribute values to be added (to the Project-BaseEntity) in your project-specific sheet for using email-service for your project :

1) ENV_EMAIL_USERNAME
2) ENV_EMAIL_PASSWORD
3) ENV_MAIL_SMTP_AUTH
4) ENV_MAIL_SMTP_STARTTLS_ENABLE
5) ENV_MAIL_SMTP_HOST
6) ENV_MAIL_SMTP_PORT
7) ENV_MAIL_SMTP_SOURCE_EMAIL

The above attributes are sufficient for Gmail SMTP. Incase of Amazon Email accounts, we require the another following attribute mapped to the project BaseEntity :

8) ENV_MAIL_SMTP_SOURCE_EMAIL (for Amazon Email accounts)

To send an email to a recipient, the Person-BaseEntity should have the **PRI_EMAIL** attribute.

## For including List of Email IDs as BCC :

The following attribute needs to be added to the project baseentity for including a list of Email IDs as BCC is :

1) Attribute Code : PRI_EMAIL_BCC_LIST 

Value for this attribute code can be a comma-seperated string of email IDs.
Example for value : abc@gmail.com, def@gmail.com

## For including List of Test Email IDs for test/local/staging environments :

The following attribute needs to be added to the project baseentity for including a list of Test Email IDs is :

1) Attribute Code : PRI_TEST_EMAIL_IDS
Value for this attribute code can be a comma-seperated string of email IDs.

# SMS SERVICE

We use [Twilio](https://www.twilio.com/) for SMS-services in Genny. The following are the values to be added in your project-specific sheet as project-specific attributes for using TWILIO SMS-service for your project :

1) ENV_TWILIO_ACCOUNT_SID
2) ENV_TWILIO_AUTH_TOKEN
3) ENV_TWILIO_SOURCE_PHONE

To send an SMS to a recipient, the Person-BaseEntity should have the **PRI_MOBILE** attribute.



