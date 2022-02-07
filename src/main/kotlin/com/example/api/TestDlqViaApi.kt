package com.example.api

import io.smallrye.mutiny.Uni
import io.smallrye.reactive.messaging.MutinyEmitter
import org.eclipse.microprofile.reactive.messaging.Channel
import javax.validation.Valid
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
@Path("/api")
class TestDlqViaApi(@Channel("smallrye-test-secondary") private val emitter: MutinyEmitter<String>) {
    @POST
    @Path("test")
    fun testDlqViaApi(@Valid payload: String) : Uni<Response> {
        return Uni.createFrom().voidItem()
            .invoke { -> emitter.send(payload) }
            .map { Response.accepted().build() }
    }
}