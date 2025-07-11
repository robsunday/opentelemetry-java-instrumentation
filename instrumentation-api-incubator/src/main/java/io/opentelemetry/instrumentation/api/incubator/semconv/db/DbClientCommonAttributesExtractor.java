/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;
import static io.opentelemetry.semconv.DbAttributes.DB_NAMESPACE;
import static io.opentelemetry.semconv.DbAttributes.DB_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.DbAttributes.DB_SYSTEM_NAME;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import io.opentelemetry.instrumentation.api.internal.SpanKeyProvider;
import javax.annotation.Nullable;

abstract class DbClientCommonAttributesExtractor<
        REQUEST, RESPONSE, GETTER extends DbClientCommonAttributesGetter<REQUEST, RESPONSE>>
    implements AttributesExtractor<REQUEST, RESPONSE>, SpanKeyProvider {

  // copied from DbIncubatingAttributes
  private static final AttributeKey<String> DB_NAME = AttributeKey.stringKey("db.name");
  static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system");
  private static final AttributeKey<String> DB_USER = AttributeKey.stringKey("db.user");
  private static final AttributeKey<String> DB_CONNECTION_STRING =
      AttributeKey.stringKey("db.connection_string");

  final GETTER getter;

  DbClientCommonAttributesExtractor(GETTER getter) {
    this.getter = getter;
  }

  @SuppressWarnings("deprecation") // until old db semconv are dropped
  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    if (SemconvStability.emitStableDatabaseSemconv()) {
      internalSet(
          attributes,
          DB_SYSTEM_NAME,
          SemconvStability.stableDbSystemName(getter.getDbSystem(request)));
      internalSet(attributes, DB_NAMESPACE, getter.getDbNamespace(request));
    }
    if (SemconvStability.emitOldDatabaseSemconv()) {
      internalSet(attributes, DB_SYSTEM, getter.getDbSystem(request));
      internalSet(attributes, DB_USER, getter.getUser(request));
      internalSet(attributes, DB_NAME, getter.getDbNamespace(request));
      internalSet(attributes, DB_CONNECTION_STRING, getter.getConnectionString(request));
    }
  }

  @Override
  public final void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    if (SemconvStability.emitStableDatabaseSemconv()) {
      if (error != null) {
        internalSet(attributes, ERROR_TYPE, error.getClass().getName());
      }
      if (error != null || response != null) {
        internalSet(attributes, DB_RESPONSE_STATUS_CODE, getter.getResponseStatus(response, error));
      }
    }
  }

  /**
   * This method is internal and is hence not for public use. Its API is unstable and can change at
   * any time.
   */
  @Override
  public SpanKey internalGetSpanKey() {
    return SpanKey.DB_CLIENT;
  }
}
