# mod-user-import

Copyright (C) 2017-2019 The Open Library Foundation

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
  "users": [{
    "username": "somebody012",
    "externalSystemId": "somebody012",
    "barcode": "1657463",
    "active": true,
    "patronGroup": "faculty",
    "personal": {
      "lastName": "Somebody",
      "firstName": "Test",
      "middleName": "",
      "email": "test@email.com",
      "phone": "+36 55 230 348",
      "mobilePhone": "+36 55 379 130",
      "dateOfBirth": "1995-10-10",
      "addresses": [{
        "countryId": "HU",
        "addressLine1": "Andrássy Street 1.",
        "addressLine2": "",
        "city": "Budapest",
        "region": "Pest",
        "postalCode": "1061",
        "addressTypeId": "Home",
        "primaryAddress": true
      }],
      "preferredContactTypeId": "mail"
    },
    "enrollmentDate": "2017-01-01",
    "expirationDate": "2019-01-01"
  }],
  "totalRecords": 1,
  "deactivateMissingUsers": false,
  "updateOnlyPresentFields": false,
  "sourceType": "test"
}
</code></pre>

### patronGroup
The value can be the name of an existing patron group in the system, e.g. <code>faculty</code>, <code>staff</code>, <code>undergrad</code>, <code>graduate</code>. The import module will match the patron group names and replace with the patron group ids. The currently available patron groups can be listed using a <code>GET</code> request for <code>{okapiUrl}/groups</code>. The <code>x-okapi-token</code> and <code>x-okapi-tenant</code> headers are required. The authenticated user needs to have a permission for retrieving patron groups (permission name: <code>users all</code>, permission code: <code>users.all</code>).

### addressTypeId
The value can be the name of an existing address type in the system, e.g. <code>Home</code>, <code>Claim</code>, <code>Order</code>. The import module will match the address type names for the address type ids. It is important to note that two addresses for a user cannot have the same address type. The available address types can be queried with a <code>GET</code> request to <code>{okapiUrl}/addresstypes</code>. The <code>x-okapi-token</code> and <code>x-okapi-tenant</code> headers are required. The authenticated user needs to have a permission for retrieving address types (permission name: <code>users all</code>, permission code: <code>users.all</code>).

### preferredContactTypeId
The value can be one of the following: <code>mail</code>, <code>email</code>, <code>text</code>, <code>phone</code>, <code>mobile</code>.

### deactivateMissingUsers
This should be true if the users missing from the current import batch should be deactivated in FOLIO.

### updateOnlyPresentFields
This should be true if only the fields present in the import should be updated, e.g. if a user address was added in FOLIO but that type of address is not present in the imported data then the address will be preserved.

### sourceType
A prefix for the <code>externalSystemId</code> to be stored in the system. This field is useful for those organizations that has multiple sources of users. With this field the multiple sources can be separated. The source type is appended to the beginning of the <code>externalSystemId</code> with an underscore, e.g. if the user's <code>externalSystemId</code> in the import is somebody012 and the <code>sourceType</code> is test, the user's <code>externalSystemId</code> will be test_somebody012.

## Additional information

### Issue tracker

See project [MODUIMP](https://issues.folio.org/browse/MODUIMP)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)
