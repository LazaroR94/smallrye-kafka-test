package com.example.database

import io.smallrye.mutiny.Uni
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class InMemoryDatabase (
    private val map: ConcurrentHashMap<UUID, String> = ConcurrentHashMap(),
) {
    fun save(entity: String): Uni<Void> {
        val uuid = UUID.randomUUID()
        when (entity) {
            is String -> map[uuid] = entity
        }
        return Uni.createFrom().voidItem()
    }

    fun saveAll(entities: List<String>): Uni<Void> =
        Uni.createFrom().voidItem()
            .invoke { -> entities.forEach { save(it) } }
}