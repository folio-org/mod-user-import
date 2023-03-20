# mod-user-import

Copyright (C) 2017-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

This module is responsible for importing new or already existing users into FOLIO.

Currently the module contains one endpoint:
POST /user-import

## How to use

1. Login with a user who has permission for importing users (permission name: <code>User import</code>, permission code: <code>user-import.all</code>). This can be done by sending the following request to FOLIO:
<pre>URL: <code>{okapiUrl}/authn/login</code>
Headers:
<code>
  x-okapi-tenant: {tenantName}
  Content-Type: application/json
</code>
Body:
<code>
  {
    "username": "username",
    "password": "password"
  }
</code></pre>

2. The login request will return a header in the response which needs to be used for authentication in the following request. The authentication token is returned in the <code>x-okapi-token</code> header (use as <code>okapiToken</code>). The user import request can be sent in the following format:
<pre>URL: <code>{okapiUrl}/user-import</code>
Headers:
<code>
  x-okapi-tenant: {tenantName}
  Content-Type: application/json
  x-okapi-token: {okapiToken}
</code>
Body:
<code>{exampleImport}</code>
</pre>

3. The response of the import will be:
<pre><code>{
    "message": {message stating that the import was successful or failed or the users were deactivated (in case of successful import and deactivateMissingUsers=true)},
    "createdRecords": {number of newly created users},
    "updatedRecords": {number of updated users},
    "failedRecords": {number of users failed to create/update},
    "failedExternalSystemIds": [{a list of users that were failed to create/update}],
    "totalRecords": {number of total records processed by the user import}
}</code></pre>

The default <code>okapiUrl</code> is <code>http://localhost:9130</code>. The default <code>tenantName</code> is <code>diku</code>. An <code>exampleImport</code> can be found in the next section.

## Example import request
<pre><code>{
  "users": [
    {
      "username": "jhandey",
      "externalSystemId": "111_112",
      "barcode": "1234567",
      "active": true,
      "patronGroup": "staff",
      "personal": {
        "lastName": "Handey",
        "firstName": "Jack",
        "middleName": "Michael",
        "preferredFirstName": "Jackie",
        "phone": "+36 55 230 348",
        "mobilePhone": "+36 55 379 130",
        "dateOfBirth": "1995-10-10",
        "addresses": [
          {
            "countryId": "HU",
            "addressLine1": "Andrássy Street 1.",
            "addressLine2": "",
            "city": "Budapest",
            "region": "Pest",
            "postalCode": "1061",
            "addressTypeId": "Home",
            "primaryAddress": true
          }
        ],
        "preferredContactTypeId": "mail"
      },
      "enrollmentDate": "2017-01-01",
      "expirationDate": "2019-01-01",
      "customFields": {
        "scope": "Design",
        "specialization": [
          "Business",
          "Jurisprudence"
        ]
      },
      "requestPreference": {
        "holdShelf": true,
        "delivery": true,
        "defaultServicePointId": "00000000-0000-1000-a000-000000000000",
        "defaultDeliveryAddressTypeId": "Home",
        "fulfillment": "Hold Shelf"
      },
      "departments": [
        "Accounting",
        "Finance",
        "Chemistry"
      ]
    }
  ],
  "included": {
    "departments": [
      {
        "name": "Accounting",
        "code": "ACC"
      },
      {
        "name": "Finance"
      }
    ],
    "customFields":[
      {
        "refId": "specialization",
        "selectField": {
          "options": {
            "values": [
              {
                "value": "Business"
              },
              {
                "value": "Jurisprudence"
              }
            ]
          }
        }
      }
    ]
  },
  "totalRecords": 1,
  "deactivateMissingUsers": true,
  "updateOnlyPresentFields": false,
  "sourceType": "test"
}</code></pre>

### patronGroup
The value can be the name of an existing patron group in the system, e.g. <code>faculty</code>, <code>staff</code>,
<code>undergrad</code>, <code>graduate</code>. The import module will match the patron group names and replace with
the patron group ids. If the patron group with the specified name does not exist in the system, then the error will be thrown.
The currently available patron groups can be listed using a <code>GET</code> request for
<code>{okapiUrl}/groups</code>. The <code>x-okapi-token</code> and <code>x-okapi-tenant</code> headers are required.
The authenticated user needs to have a permission for retrieving patron groups (permission name: <code>users all</code>,
permission code: <code>users.all</code>).

### addressTypeId
The value can be the name of an existing address type in the system, e.g. <code>Home</code>, <code>Claim</code>, <code>Order</code>. The import module will match the address type names for the address type ids. It is important to note that two addresses for a user cannot have the same address type. The available address types can be queried with a <code>GET</code> request to <code>{okapiUrl}/addresstypes</code>. The <code>x-okapi-token</code> and <code>x-okapi-tenant</code> headers are required. The authenticated user needs to have a permission for retrieving address types (permission name: <code>users all</code>, permission code: <code>users.all</code>).

### preferredContactTypeId
The value can be one of the following: <code>mail</code>, <code>email</code>, <code>text</code>, <code>phone</code>, <code>mobile</code>.

### deactivateMissingUsers
This should be true if the users missing from the current import batch should be deactivated in FOLIO.

### updateOnlyPresentFields
This should be true if only the fields present in the import should be updated, e.g. if a user address was added in FOLIO but that type of address is not present in the imported data then the address will be preserved.

Currently this only works for addresses. Please more embedded fields need this feature open a story a https://issues.folio.org/projects/MODUIMP/issues

### sourceType
A prefix for the <code>externalSystemId</code> to be stored in the system. This field is useful for those organizations that has multiple sources of users. With this field the multiple sources can be separated. The source type is appended to the beginning of the <code>externalSystemId</code> with an underscore, e.g. if the user's <code>externalSystemId</code> in the import is somebody012 and the <code>sourceType</code> is test, the user's <code>externalSystemId</code> will be test_somebody012.

### requestPreference
Use this attribute to populate the user Request preference. The Request Preference contains following properties:

<code>holdShelf</code> - required field, should always be true;
<code>delivery</code> - required field, could be <code>true</code> or <code>false</code>;
<code>defaultServicePointId</code> - optional, the id of user's default service point
if <code>delivery</code> is <code>true</code> then next properties can be used
<code>fulfillment</code> - required field, can only have <code>Hold shelf</code> or <code>Delivery</code> value;
<code>defaultDeliveryAddressTypeId</code> - optional, the name of user's address type
* If the requestPreference exists in payload and exists in the system - the system will update existing user preference.
* If the requestPreference exists in payload and doesn't exist in the system - the system will create a new one.
* If the requestPreference does not exist in payload but exists in the system - the system will delete existing preference.
* If value provided for defaultServicePointId or defaultDeliveryAddressTypeId does not exist in the system - the system will return an error

### departments
Names of departments the user belongs to. To manage departments creation use attribute <code>departments</code> in <code>included</code>.
* If the department doesn't exist in included and exists in the system - assign the department to user
* If the department exists in included and doesn't exist in the system - create the department (with code generation if missing) and assign the department to user
* If the department with code exists in included and with the same code exists in the system - update department's name and assign the department to user
* If the department doesn't exist in included and not exist in the system - error

### customFields
Can be populated with pairs:
<code>refId: value</code>,
where <code>refId</code> - refId of an existing custom field,
<code>value</code> - one value or set of values.

**Note 1:** In case of setting values to one of RADIO_BUTTON, SINGLE_SELECT_DROPDOWN, MULTI_SELECT_DROPDOWN type of custom field - use option names. If one or more option names are not existed in related by `refId` custom field definition than the system will return an error.

**Note 2:** To manage custom fields updating use attribute <code>departments</code> in <code>included</code>. Specifying in this section custom field's `refId` with one or more another fields will update custom fields definition. To update the selectable field's options it is required to specify ALL options.
## Additional information

### Issue tracker

See project [MODUIMP](https://issues.folio.org/browse/MODUIMP)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)
