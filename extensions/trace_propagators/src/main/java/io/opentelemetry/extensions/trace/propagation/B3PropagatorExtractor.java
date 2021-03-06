/*
 * Copyright 2020, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.extensions.trace.propagation;

import static io.opentelemetry.extensions.trace.propagation.B3Propagator.MAX_SPAN_ID_LENGTH;
import static io.opentelemetry.extensions.trace.propagation.B3Propagator.MAX_TRACE_ID_LENGTH;
import static io.opentelemetry.extensions.trace.propagation.B3Propagator.TRUE_INT;

import io.grpc.Context;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.internal.StringUtils;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

@Immutable
interface B3PropagatorExtractor {

  <C> Context extract(Context context, C carrier, HttpTextFormat.Getter<C> getter);

  @Immutable
  class Util {
    private static final Logger logger = Logger.getLogger(Util.class.getName());
    private static final TraceFlags SAMPLED_FLAGS = TraceFlags.builder().setIsSampled(true).build();
    private static final TraceFlags NOT_SAMPLED_FLAGS =
        TraceFlags.builder().setIsSampled(false).build();

    private Util() {}

    static SpanContext buildSpanContext(String traceId, String spanId, String sampled) {
      try {
        TraceFlags traceFlags =
            TRUE_INT.equals(sampled) || Boolean.parseBoolean(sampled) // accept either "1" or "true"
                ? SAMPLED_FLAGS
                : NOT_SAMPLED_FLAGS;

        return SpanContext.createFromRemoteParent(
            TraceId.fromLowerBase16(StringUtils.padLeft(traceId, MAX_TRACE_ID_LENGTH), 0),
            SpanId.fromLowerBase16(spanId, 0),
            traceFlags,
            TraceState.getDefault());
      } catch (Exception e) {
        logger.log(Level.INFO, "Error parsing B3 header. Returning INVALID span context.", e);
        return SpanContext.getInvalid();
      }
    }

    static boolean isTraceIdValid(String value) {
      return !(StringUtils.isNullOrEmpty(value) || value.length() > MAX_TRACE_ID_LENGTH);
    }

    static boolean isSpanIdValid(String value) {
      return !(StringUtils.isNullOrEmpty(value) || value.length() > MAX_SPAN_ID_LENGTH);
    }
  }
}
