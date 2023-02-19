## 3.7.2 2023-02-20

 * [MODUIMP-84](https://issues.folio.org/browse/MODUIMP-84) RMB 35.0.6, Vert.x 4.3.8

## 3.7.1 2022-12-06

 * [MODUIMP-68](https://issues.folio.org/browse/MODUIMP-68): Make patronGroup optional
 * [MODUIMP-81](https://issues.folio.org/browse/MODUIMP-81): RMB 35.0.4, Vert.x 4.3.5
 * [FOLIO-602](https://issues.folio.org/browse/FOLIO-602): Fix baseUri
 * [MODUIMP-76](https://issues.folio.org/browse/MODUIMP-76): Javadoc and tests for overwriting fields

## 3.7.0 2022-10-25

 * [MODUIMP-75](https://issues.folio.org/browse/MODUIMP-75) Allow users interface 15.2 thru 16.0
 * [MODUIMP-79](https://issues.folio.org/browse/MODUIMP-79) Upgrade to RMB 35.0.2, Vert.x 4.3.4

## 3.6.6 2022-06-08

Fixes:

 * [MODUIMP-67](https://issues.folio.org/browse/MODUIMP-67) Unable to Update Custom Fields via User Import
 * [MODUIMP-69](https://issues.folio.org/browse/MODUIMP-69) Update RMB, Vertx, Jackson-Databind (CVE-2020-36518)
 * [MODUIMP-71](https://issues.folio.org/browse/MODUIMP-71) Update to RMB 34.0.0, Vert.x 4.3.1
 * [FOLIO-3484](https://issues.folio.org/browse/FOLIO-3484) Rebuild all released alpine-jre-openjdk11 containers fixing ZipException

## 3.6.5 2022-04-15

Fixes:

 * [MODUIMP-67](https://issues.folio.org/browse/MODUIMP-67) Unable to Update Custom Fields via User Import

## 3.6.4 2022-02-16

Fixes:

 * [MODUIMP-64](https://issues.folio.org/browse/MODUIMP-64) Use WebClient rather than HttpClientFactory, HttpClientInterface
 * [MODUIMP-63](https://issues.folio.org/browse/MODUIMP-63) Socket leak
 * [MODUIMP-62](https://issues.folio.org/browse/MODUIMP-62) Mod-user-import throws an error if users are assigned a department that is not
   part of the first ten departments in a /departments API call
 * [MODUIMP-30](https://issues.folio.org/browse/MODUIMP-30) mod-user-import module crashes on loading 30k Users in performance environment
 * [MODUIMP-29](https://issues.folio.org/browse/MODUIMP-29) Import of new users can result in users w/o permissions users record

## 3.6.3 2021-12-17

 * [MODUIMP-60](https://issues.folio.org/browse/MODUIMP-60) Log4j 2.16.0, Vert.x 4.2.2, RMB 33.2.2 (CVE-2021-45046)
 * [MODUIMP-58](https://issues.folio.org/browse/MODUIMP-58) Log4j 2.15.0 fixing remote execution (CVE-2021-44228)

## 3.6.2 2021-10-05

 * [MODUIMP-56](https://issues.folio.org/browse/MODUIMP-56) Update RMB to 33.1.1 and Vert.x to 4.1.4

## 3.6.1 2021-06-22

 * [MODUIMP-52](https://issues.folio.org/browse/MODUIMP-52) RMB 33.0.2, fixes HttpClientMock2 not enabled due to race condition
 * [MODUIMP-51](https://issues.folio.org/browse/MODUIMP-51) Update Vert.x from 4.1.0.CR1 to 4.1.0 final

## 3.6.0 2021-05-28

 * [MODUIMP-48](https://issues.folio.org/browse/MODUIMP-48) Upgrade to RMB 33.0.0, Vert.x 4.1.0.CR1

## 3.5.0 2021-05-24

 * [MODUIMP-42](https://issues.folio.org/browse/MODUIMP-42) Request Preferences are erroneously being deleted
 * [MODUIMP-44](https://issues.folio.org/browse/MODUIMP-44) Logging issues
 * [MODUIMP-47](https://issues.folio.org/browse/MODUIMP-47) Upgrade mod-user-import to RMB v33.x
 * [MODUIMP-35](https://issues.folio.org/browse/MODUIMP-35) Add personal data disclosure form

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
 * Update for custom fields [MODCFIELDS-10](https://issues.folio.org/browse/MODCFIELDS-10) MODCFIELDS-35

## 3.1.0 2018-09-18
 * Support `users` interface 15.0 (which removes `meta` object from proxy relationship, [MODUSERS-75](https://issues.folio.org/browse/MODUSERS-75))

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
