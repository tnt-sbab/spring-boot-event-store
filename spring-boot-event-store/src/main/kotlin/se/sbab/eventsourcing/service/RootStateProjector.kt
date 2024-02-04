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

import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event

/**
 * This interface should be implemented by a spring bean in order to create the initial domain state from the
 * first event in the event stream.
 */
interface RootStateProjector {
    fun onEvent(event: Event): DomainState
}
