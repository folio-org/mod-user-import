# mod-user-import

Copyright (C) 2017 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

This module is responsible for importing new or already existing users into FOLIO.

Currently the module contains one endpoint:
POST /user-import

## Example import request

<pre><code>
{
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
  "sourceType": "test_"
}
</code></pre>

### patronGroup
The value can be the name of an existing patron group in the system. E.g. faculty, staff, undergrad, graduate. The import module will match the patron group names for the patron group ids.

### addressTypeId
The value can be the name of an existing address type in the system. E.g. Home, Claim, Order. The import module will match the address type names for the address type ids.

### preferredContactTypeId
The value can be one of the following: mail, email, text, phone, mobile.

### deactivateMissingUsers
This should be true if the users missing from the current import batch should be deactivated in FOLIO.

### updateOnlyPresentFields
This should be true if only the fields present in the import should be updated. E.g. if a user address was added in FOLIO but that type of address is not present in the imported data then the address will be preserved.

### sourceType
A prefix for the externalSystemId to be stored in the system. This field is useful for those organizations that has multiple sources of users. With this field the multiple sources can be separated.
