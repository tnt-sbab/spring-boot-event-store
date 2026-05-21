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

import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event
import java.lang.reflect.Constructor

class ReflectiveRootStateProjector(
    private val applicationContext: ApplicationContext,
) : RootStateProjector {
    val constructors: Map<Class<Event>, (Event) -> DomainState> = findRootStateConstructors()

    private fun findBasePackages(): List<String> =
        AutoConfigurationPackages.get(applicationContext.autowireCapableBeanFactory)

    private fun findRootStateConstructors(): Map<Class<Event>, (Event) -> DomainState> =
        findAggregateRootClasses().map { domainClass ->
            findRootStateConstructors(domainClass)
        }.flatMap { it.entries }.associate { it.toPair() }

    private fun findAggregateRootClasses(): Set<Class<out DomainState>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(DomainState::class.java))
        return findBasePackages().flatMap { pkg ->
            scanner.findCandidateComponents(pkg)
        }.mapNotNull { beanDef ->
            @Suppress("UNCHECKED_CAST")
            Class.forName(beanDef.beanClassName) as? Class<out DomainState>
        }.toSet()
    }

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
