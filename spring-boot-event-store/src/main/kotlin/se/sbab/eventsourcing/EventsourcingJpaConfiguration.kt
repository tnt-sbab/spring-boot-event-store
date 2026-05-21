package se.sbab.eventsourcing

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ConditionalOnMissingBean(name = ["eventRepository"])
@EnableJpaRepositories("se.sbab.eventsourcing.repository")
@EntityScan("se.sbab.eventsourcing.repository")
class EventsourcingJpaConfiguration
