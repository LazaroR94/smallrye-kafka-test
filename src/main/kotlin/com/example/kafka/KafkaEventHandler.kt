package com.example.kafka

import com.example.database.InMemoryDatabase
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.infrastructure.Infrastructure
import io.smallrye.reactive.messaging.annotations.Merge
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Outgoing
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class KafkaEventHandler(private val inMemoryDatabase: InMemoryDatabase){
    // @Incoming & @Outgoing are merged for demonstration/testing purposes only
    @Incoming("smallrye-test-primary")
    @Outgoing("smallrye-test-secondary")
    @Merge
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    fun consume(record: Message<String>): Uni<Void> {
        return Uni.createFrom().voidItem()
            .call { -> inMemoryDatabase.save(record.payload) }
            .call { -> handleSuccess(record) }
            .onFailure().call { failure -> handleFailure(failure, record) }
            .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        // To get close to the actual implementation, I will leave this subscription on the default executor
        // as the actual implementation uses a blocking JDBC call to the database
    }

    private fun handleSuccess(record: Message<String>) =
        Uni.createFrom()
            .completionStage(record.ack())

    private fun handleFailure(failure: Throwable, record: Message<String>) =
        Uni.createFrom()
            .completionStage(record.nack(failure))
            .onFailure().invoke { f ->
                println("Failed to nack record. Check Kafka connection. Details=${f.localizedMessage}")
            }
}

@ApplicationScoped
class DeadLetterTopicReader(private val inMemoryDatabase: InMemoryDatabase) {
    @Incoming("smallrye-test-dlq")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    fun consume(record: Message<String>): Uni<Void> {
        val metadata = record.getMetadata(IncomingKafkaRecordMetadata::class.java).orElse(null)
        val cause: String? = metadata?.headers?.lastHeader("dead-letter-cause")?.let { String(it.value()) }
        if (cause != null) {
            println("Offset=${metadata.offset} was nacked due to=$cause")
        }
        return Uni.createFrom().voidItem()
            .call { -> inMemoryDatabase.save(record.payload) }
            .call { -> handleSuccess(record) }
            .onFailure().call { failure -> handleFailure(failure, record) }
            .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        // To get close to the actual implementation, I will leave this subscription on the default executor
        // as the actual implementation uses a blocking JDBC call to the database
    }

    private fun handleSuccess(record: Message<String>) =
        Uni.createFrom()
            .completionStage(record.ack())

    private fun handleFailure(failure: Throwable, record: Message<String>) =
        Uni.createFrom()
            .completionStage(record.ack())
            .onFailure().invoke { f ->
                println("Failed to nack record. Check Kafka connection. Details=${f.localizedMessage}")
            }
}