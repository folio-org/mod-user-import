package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import com.google.common.base.Strings;

public class HttpClientUtil {

  private HttpClientUtil() {
  }

  public static HttpClientInterface createClient(Map<String, String> okapiHeaders) {
    return HttpClientFactory.getHttpClient(getOkapiUrl(okapiHeaders), okapiHeaders.get(OKAPI_TENANT_HEADER));
  }

  public static HttpClientInterface createClientWithHeaders(Map<String, String> okapiHeaders, String accept, String contentType) {
    Map<String, String> headers = createHeaders(okapiHeaders, accept, contentType);

    HttpClientInterface client = createClient(okapiHeaders);
    client.setDefaultHeaders(headers);
    return client;
  }

  public static Map<String, String> createHeaders(Map<String, String> okapiHeaders, String accept, String contentType) {
    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    if (!Strings.isNullOrEmpty(accept)) {
      headers.put(HTTP_HEADER_ACCEPT, accept);
    }
    if (!Strings.isNullOrEmpty(contentType)) {
      headers.put(HTTP_HEADER_CONTENT_TYPE, contentType);
    }
    return headers;
  }

  private static String getOkapiUrl(Map<String, String> okapiHeaders) {
    return okapiHeaders.get(OKAPI_URL_HEADER);
  }
}
