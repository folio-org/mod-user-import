package org.folio.pact;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import io.restassured.*;

import io.restassured.http.ContentType;
import static org.hamcrest.Matchers.*;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class ModUserImportPactTest {

  Map<String, String> headers = new HashMap<String, String>();

  Map<String, String> delHeaders = new HashMap<String, String>();
  String requestNewUser;
  String responseNewUser;
  String permissionRequestBody;

  @Rule
  public PactProviderRuleMk2 mockModUsersProvider = new PactProviderRuleMk2("mod-users", "localhost", 9135, this);
  
  @Rule
  public PactProviderRuleMk2 mockModPermissionsProvider = new PactProviderRuleMk2("mod-permissions", "localhost", 9132, this);

  @Pact(provider= "mod-users", consumer = "mod-user-import")
  public RequestResponsePact createUsersFragment(PactDslWithProvider builder) {
    headers.put("X-Okapi-Tenant", "diku");
    headers.put("Accept", "application/json");

    delHeaders.put("X-Okapi-Tenant", "diku");
    delHeaders.put("Accept", "text/plain");

    requestNewUser = ExpectedValues.getBody("add_user_request.json").toString();
    responseNewUser = ExpectedValues.getBody("add_user_response.json").toString();

    String expectedAddressTypes = ExpectedValues.getBody("address-types.json").toString();
    String expectedUserGroups = ExpectedValues.getBody("user-groups.json").toString();

    return builder

    .given("address types are set").uponReceiving("Request for all address types").path("/addresstypes").method("GET")
        .headers(headers).willRespondWith().status(200).body(expectedAddressTypes)

    .given("user groups are set").uponReceiving("Request for all user groups").path("/groups").method("GET")
        .headers(headers).willRespondWith().status(200).body(expectedUserGroups)

    .given("the user does not exits in db").uponReceiving("Request for adding a new user").path("/users").method("POST")
        .headers(headers).body(requestNewUser).willRespondWith().status(201).body(responseNewUser)

    .given("the user  exits in db").uponReceiving("Request for deleting the user")
        .path("/users/1ad737b0-d847-11e6-bf26-cec0c9329933").method("DELETE").headers(delHeaders).willRespondWith()
        .status(204)

    .toPact();
  }
    
    @Pact(provider= "mod-permissions", consumer = "mod-user-import")
    public RequestResponsePact createPermissionsFragment(PactDslWithProvider builder) {

      headers.put("X-Okapi-Tenant", "diku");
      
      permissionRequestBody = "{" +
          "\"userId\": \"1ad737b0-d847-11e6-bf26-cec0c9329933\"," +
           "\"permissions\": []" +
        "}";
             
      return builder
      .given("the user exists").uponReceiving("Request for updating the permissions").path("/perms/users").method("POST")
          .headers(headers).body(permissionRequestBody).willRespondWith().status(201)
      .toPact();

  }

  @Test
  @PactVerification({"mod-users", "mod-permissions"})
  public void runTest() {

    RestAssured.given().port(mockModUsersProvider.getPort()).contentType(ContentType.JSON).headers(headers).get("/addresstypes")
        .then().statusCode(200).and().body(hasJsonPath("$.totalRecords", equalTo(6)));
    RestAssured.reset();

    RestAssured.given().port(mockModUsersProvider.getPort()).contentType(ContentType.JSON).headers(headers).get("/groups")
        .then().statusCode(200).and().body(hasJsonPath("$.totalRecords", equalTo(4)));
    RestAssured.reset();

    RestAssured.given().port(mockModUsersProvider.getPort()).contentType(ContentType.JSON).headers(headers).body(requestNewUser).post("/users")
        .then().statusCode(201).body(hasJsonPath("$.id", equalTo("1ad737b0-d847-11e6-bf26-cec0c9329933")));
    RestAssured.reset();

    RestAssured.given().port(mockModUsersProvider.getPort()).contentType(ContentType.JSON).headers(delHeaders).delete("/users/1ad737b0-d847-11e6-bf26-cec0c9329933")
        .then().statusCode(204);
    RestAssured.reset();
    
    RestAssured.given().port(mockModPermissionsProvider.getPort()).contentType(ContentType.JSON).headers(headers).body(permissionRequestBody).post("/perms/users")
    .then().statusCode(equalTo(201));
RestAssured.reset();
  }

}
