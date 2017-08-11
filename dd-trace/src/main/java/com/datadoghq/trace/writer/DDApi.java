package com.datadoghq.trace.writer;

import com.datadoghq.trace.DDBaseSpan;
import com.datadoghq.trace.DDTraceInfo;
import com.datadoghq.trace.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/** The API pointing to a DD agent */
@Slf4j
public class DDApi {

  private static final String TRACES_ENDPOINT = "/v0.3/traces";
  private static final String SERVICES_ENDPOINT = "/v0.3/services";

  private final String tracesEndpoint;
  private final String servicesEndpoint;

  private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

  public DDApi(final String host, final int port) {
    this.tracesEndpoint = "http://" + host + ":" + port + TRACES_ENDPOINT;
    this.servicesEndpoint = "http://" + host + ":" + port + SERVICES_ENDPOINT;
  }

  /**
   * Send traces to the DD agent
   *
   * @param traces the traces to be sent
   * @return the staus code returned
   */
  public boolean sendTraces(final List<List<DDBaseSpan<?>>> traces) {
    final int status = callPUT(tracesEndpoint, traces);
    if (status == 200) {
      log.debug("Succesfully sent {} traces to the DD agent.", traces.size());
      return true;
    } else {
      log.warn("Error while sending {} traces to the DD agent. Status: {}", traces.size(), status);
      return false;
    }
  }

  /**
   * Send service extra information to the services endpoint
   *
   * @param services the services to be sent
   */
  public boolean sendServices(final Map<String, Service> services) {
    if (services == null) {
      return true;
    }
    final int status = callPUT(servicesEndpoint, services);
    if (status == 200) {
      log.debug("Succesfully sent {} services to the DD agent.", services.size());
      return true;
    } else {
      log.warn(
          "Error while sending {} services to the DD agent. Status: {}", services.size(), status);
      return false;
    }
  }

  /**
   * PUT to an endpoint the provided JSON content
   *
   * @param content
   * @return the status code
   */
  private int callPUT(final String endpoint, final Object content) {
    HttpURLConnection httpCon = null;
    try {
      httpCon = getHttpURLConnection(endpoint);
    } catch (final Exception e) {
      log.warn("Error thrown before PUT call to the DD agent.", e);
      return -1;
    }

    try {
      final OutputStream out = httpCon.getOutputStream();
      objectMapper.writeValue(out, content);
      out.flush();
      out.close();
      final int responseCode = httpCon.getResponseCode();
      if (responseCode == 200) {
        log.debug("Sent the payload to the DD agent.");
      } else {
        log.warn(
            "Could not send the payload to the DD agent. Status: {} ResponseMessage: {}",
            httpCon.getResponseCode(),
            httpCon.getResponseMessage());
      }
      return responseCode;
    } catch (final Exception e) {
      log.warn("Could not send the payload to the DD agent.", e);
      return -1;
    }
  }

  private HttpURLConnection getHttpURLConnection(final String endpoint) throws IOException {
    final HttpURLConnection httpCon;
    final URL url = new URL(endpoint);
    httpCon = (HttpURLConnection) url.openConnection();
    httpCon.setDoOutput(true);
    httpCon.setRequestMethod("PUT");
    httpCon.setRequestProperty("Content-Type", "application/msgpack");
    httpCon.setRequestProperty("Datadog-Meta-Lang", "java");
    httpCon.setRequestProperty("Datadog-Meta-Lang-Version", DDTraceInfo.JAVA_VERSION);
    httpCon.setRequestProperty("Datadog-Meta-Lang-Interpreter", DDTraceInfo.JAVA_VM_NAME);
    httpCon.setRequestProperty("Datadog-Meta-Tracer-Version", DDTraceInfo.VERSION);
    return httpCon;
  }
}
