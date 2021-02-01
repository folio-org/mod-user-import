## 3.4.0 2021-01-29

 * [MODUIMP-41](https://issues.folio.org/browse/MODUIMP-41) Refactor: use GenericCompositeFuture rather than internal one
 * [MODUIMP-40](https://issues.folio.org/browse/MODUIMP-40) Permissions error when loading users
 * [MODUIMP-37](https://issues.folio.org/browse/MODUIMP-37) Upgrade to RMB 32

## 3.3.0 2020-12-18
 * [MODUIMP-28](https://issues.folio.org/browse/MODUIMP-28) Upgrade to RMB 31.1.1
 * [MODUIMP-21](https://issues.folio.org/browse/MODUIMP-21) Update custom field import
 * [MODUIMP-22](https://issues.folio.org/browse/MODUIMP-22) Migrate to Java 11 and RMB v31.0.2
 * [MODUIMP-20](https://issues.folio.org/browse/MODUIMP-20) Update patron group mapping handling
 * [MODUIMP-18](https://issues.folio.org/browse/MODUIMP-18) Update documentation
 * [MODUIMP-18](https://issues.folio.org/browse/MODUIMP-18) Create departments on import
 * [MODUIMP-16](https://issues.folio.org/browse/MODUIMP-16) Add Department field to mod-user-import
 * [MODUIMP-13](https://issues.folio.org/browse/MODUIMP-13) Support loading of Delivery request fields via mod-user-import
 * [MODUIMP-15](https://issues.folio.org/browse/MODUIMP-15) Implement support of preferred first name

## 3.2.0 2020-03-13

 * [MODUIMP-9](https://issues.folio.org/browse/MODUIMP-9) Upgrade to RAML 1.0 and RMB 29
 * Update for custom fields MODCFIELDS-10 MODCFIELDS-35

## 3.1.0 2018-09-18
 * Support `users` interface 15.0 (which removes `meta` object from proxy relationship, MODUSERS-75)

## 3.0.0 2018-02-28
 * Update readme with usage information
 * Improve logging
 * Make externalSystemId a required field
 * Use only one HttpClient instead of creating one for each request
 * Update copyright year
 * Update user schema and raml version

## 2.0.0 2017-12-17
 * The user import endpoint is now protected with a permission
 * The user import response was changed to a JSON

## 1.0.0 2017-11-17
 * First release
