package io.andromeda.fragments.quarkus

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test

@QuarkusTest
class FragmentsQuarkusResourceTest {

    @Inject
    lateinit var resource: FragmentsQuarkusResource

    @Test
    fun resourceIsInstantiated() {
        assert(resource is FragmentsQuarkusResource)
    }

    @Test
    fun staticEngineIsInjected() {
        assert(resource.staticEngine is StaticPageEngine)
    }

    @Test
    fun blogEngineIsInjected() {
        assert(resource.blogEngine is BlogEngine)
    }
}
