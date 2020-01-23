package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.*;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

public class HttpClientUtil {

  private HttpClientUtil() {
  }

  public static Map<String, String> createHeaders(Map<String, String> okapiHeaders, String accept, String contentType) {
    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put(HTTP_HEADER_ACCEPT, accept);
    if (!Strings.isNullOrEmpty(contentType)) {
      headers.put(HTTP_HEADER_CONTENT_TYPE, contentType);
    }
    okapiHeaders.computeIfPresent(OKAPI_MODULE_ID_HEADER, headers::put);
    return headers;
  }

  public static String getOkapiUrl(Map<String, String> okapiHeaders) {
    return okapiHeaders.get(OKAPI_URL_HEADER);
  }
}
