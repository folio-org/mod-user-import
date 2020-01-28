package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_ACCEPT;
import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_CONTENT_TYPE;
import static org.folio.rest.util.UserImportAPIConstants.OKAPI_MODULE_ID_HEADER;
import static org.folio.rest.util.UserImportAPIConstants.OKAPI_TENANT_HEADER;
import static org.folio.rest.util.UserImportAPIConstants.OKAPI_TOKEN_HEADER;
import static org.folio.rest.util.UserImportAPIConstants.OKAPI_URL_HEADER;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

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
    return headers;
  }

//  public static Map<String, String> createHeaders(Map<String, String> okapiHeaders, String accept, String contentType) {
//    Map<String, String> headers = new CaseInsensitiveMap<>();
//    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
//    headers.put(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER));
//    if (!Strings.isNullOrEmpty(accept)) {
//      headers.put(HTTP_HEADER_ACCEPT, accept);
//    }
//    if (!Strings.isNullOrEmpty(contentType)) {
//      headers.put(HTTP_HEADER_CONTENT_TYPE, contentType);
//    }
//    okapiHeaders.computeIfPresent(OKAPI_MODULE_ID_HEADER, headers::put);
//    return headers;
//  }

  public static String getOkapiUrl(Map<String, String> okapiHeaders) {
    return okapiHeaders.get(OKAPI_URL_HEADER);
  }
}
