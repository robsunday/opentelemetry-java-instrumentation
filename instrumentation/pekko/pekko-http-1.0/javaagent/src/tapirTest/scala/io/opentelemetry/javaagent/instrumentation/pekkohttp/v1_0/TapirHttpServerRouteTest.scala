/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp.v1_0

import io.opentelemetry.instrumentation.test.utils.PortUtils
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension
import io.opentelemetry.sdk.testing.assertj.{SpanDataAssert, TraceAssert}
import io.opentelemetry.testing.internal.armeria.client.WebClient
import io.opentelemetry.testing.internal.armeria.common.{
  AggregatedHttpRequest,
  HttpMethod
}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.{concat, pathPrefix}
import org.apache.pekko.http.scaladsl.server.Route
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.{AfterAll, Test, TestInstance}
import sttp.tapir._
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import java.net.{URI, URISyntaxException}
import java.util.function.Consumer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TapirHttpServerRouteTest {
  @RegisterExtension private val testing: AgentInstrumentationExtension =
    AgentInstrumentationExtension.create
  private val client: WebClient = WebClient.of()

  implicit val system: ActorSystem = ActorSystem("my-system")

  private def buildAddress(port: Int): URI = try
    new URI("http://localhost:" + port + "/")
  catch {
    case exception: URISyntaxException =>
      throw new IllegalStateException(exception)
  }

  @Test def testSimple(): Unit = {
    import org.apache.pekko.http.scaladsl.server.Directives._
    val route = path("test") {
      complete("ok")
    }

    test(route, "/test", "GET /test")
  }

  @Test def testRoute(): Unit = {
    import org.apache.pekko.http.scaladsl.server.Directives._
    val route = concat(
      pathEndOrSingleSlash {
        complete("root")
      },
      pathPrefix("test") {
        concat(
          pathSingleSlash {
            complete("test")
          },
          path(IntNumber) { _ =>
            complete("ok")
          }
        )
      }
    )

    test(route, "/test/1", "GET /test/*")
  }

  @Test def testTapirRoutes(): Unit = {
    val interpreter = PekkoHttpServerInterpreter()(system.dispatcher)
    def makeRoute(input: EndpointInput[Unit]) = {
      interpreter.toRoute(
        endpoint.get
          .in(input)
          .errorOut(stringBody)
          .out(stringBody)
          .serverLogicPure[Future](_ => Right("ok"))
      )
    }

    val routes = concat(
      concat(makeRoute("test" / "1"), makeRoute("test" / "2")),
      concat(makeRoute("test" / "3"), makeRoute("test" / "4"))
    )

    test(routes, "/test/4", "GET /test/4")
  }

  @Test def testTapirWithPathPrefix(): Unit = {
    val interpreter = PekkoHttpServerInterpreter()(system.dispatcher)
    val tapirRoute = interpreter.toRoute(
      endpoint.get
        .in(path[Int]("i") / "bar")
        .errorOut(stringBody)
        .out(stringBody)
        .serverLogicPure[Future](_ => Right("ok"))
    )

    val prefixedRoute = pathPrefix("foo") { tapirRoute }
    test(prefixedRoute, "/foo/123/bar", "GET /foo/{i}/bar")

  }

  def test(route: Route, path: String, spanName: String): Unit = {
    val port = PortUtils.findOpenPort
    val address: URI = buildAddress(port)
    val binding =
      Await.result(Http().bindAndHandle(route, "localhost", port), 10.seconds)
    try {
      val request = AggregatedHttpRequest.of(
        HttpMethod.GET,
        address.resolve(path).toString
      )
      val response = client.execute(request).aggregate.join
      assertThat(response.status.code).isEqualTo(200)
      assertThat(response.contentUtf8).isEqualTo("ok")

      testing.waitAndAssertTraces(new Consumer[TraceAssert] {
        override def accept(trace: TraceAssert): Unit =
          trace.hasSpansSatisfyingExactly(new Consumer[SpanDataAssert] {
            override def accept(span: SpanDataAssert): Unit = {
              span.hasName(spanName)
            }
          })
      })
    } finally {
      binding.unbind()
    }
  }

  @AfterAll
  def cleanUp(): Unit = {
    system.terminate()
  }
}
