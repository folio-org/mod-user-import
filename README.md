# mod-user-import

This module is responsible for importing new or already existing users into FOLIO.

Currently the module contains one endpoint:
POST /user-import

The structure of the body is:
<pre><code>
{
  "users": [],
  "total_records": 0,
  "deactivate_missing_users": false,
  "update_only_present_fields": false
}
</code></pre>

Deactivate missing users option: 
This should be true if the users missing from the current import batch should be deactivated in FOLIO.

Update only present fields:
This should be true if only the fields present in the import should be updated. E.g. if a user address was added in FOLIO but that type of address is not present in the imported data then the address will be preserved. 