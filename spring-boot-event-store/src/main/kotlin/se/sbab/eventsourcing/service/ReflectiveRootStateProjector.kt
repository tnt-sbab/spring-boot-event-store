/**
 * Copyright 2024 SBAB Bank AB
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

package se.sbab.eventsourcing.service

import org.reflections.Reflections
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event
import java.lang.reflect.Constructor

@Service
@ConditionalOnProperty(name = ["events-domain-package"])
class ReflectiveRootStateProjector(
    @Value("\${events-domain-package}") private val domainPackage: String,
) : RootStateProjector {
    val constructors: Map<Class<Event>, (Event) -> DomainState> = findRootStateConstructors()

    private fun findRootStateConstructors(): Map<Class<Event>, (Event) -> DomainState> =
        findAggregateRootClasses().map { domainClass ->
            findRootStateConstructors(domainClass)
        }.flatMap { it.entries }.associate { it.toPair() }

    private fun findAggregateRootClasses(): Set<Class<out DomainState>> =
        Reflections(domainPackage).getSubTypesOf(DomainState::class.java)

    private fun findRootStateConstructors(domainClass: Class<out DomainState>): Map<Class<Event>, (Event) -> DomainState> =
        domainClass.declaredConstructors.filter(::isRootStateConstructor).associate { constructor ->
            val eventConstructor: (Event) -> DomainState = { event: Event ->
                constructor.newInstance(event) as DomainState
            }
            constructor.parameterTypes[0] as Class<Event> to eventConstructor
        }

    private fun isRootStateConstructor(constructor: Constructor<*>): Boolean =
        constructor.parameters.size == 1 && Event::class.java.isAssignableFrom(constructor.parameters[0].type)

    override fun onEvent(event: Event): DomainState {
        constructors[event::class.java]?.let { constructor ->
            return constructor(event)
        } ?: throw IllegalArgumentException("No constructor event found for account")
    }
}
