package io.github.rygel.fragments.quarkus

import io.github.rygel.fragments.adapter.FragmentsEngine
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.HEADER_DECORATOR)
class CspFilter
    @Inject
    constructor(
        private val engine: FragmentsEngine,
    ) : ContainerResponseFilter {
        override fun filter(
            requestContext: ContainerRequestContext,
            responseContext: ContainerResponseContext,
        ) {
            engine.securityHeaders().forEach { (name, value) ->
                responseContext.headers.add(name, value)
            }
        }
    }
