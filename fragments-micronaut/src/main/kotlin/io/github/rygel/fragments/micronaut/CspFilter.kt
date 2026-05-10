package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.adapter.FragmentsEngine
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import jakarta.inject.Inject
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

@Filter("/**")
class CspFilter
    @Inject
    constructor(
        private val engine: FragmentsEngine,
    ) : HttpServerFilter {
        override fun doFilter(
            request: HttpRequest<*>,
            chain: ServerFilterChain,
        ): Publisher<MutableHttpResponse<*>> = CspPublisher(chain.proceed(request), engine.cspHeader())
    }

private class CspPublisher(
    private val source: Publisher<MutableHttpResponse<*>>,
    private val cspHeaderValue: String,
) : Publisher<MutableHttpResponse<*>> {
    override fun subscribe(subscriber: Subscriber<in MutableHttpResponse<*>>) {
        source.subscribe(
            object : Subscriber<MutableHttpResponse<*>> {
                override fun onSubscribe(s: Subscription) = subscriber.onSubscribe(s)

                override fun onNext(response: MutableHttpResponse<*>) {
                    response.header("Content-Security-Policy", cspHeaderValue)
                    subscriber.onNext(response)
                }

                override fun onError(t: Throwable) = subscriber.onError(t)

                override fun onComplete() = subscriber.onComplete()
            },
        )
    }
}
