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

package se.sbab.eventsourcing

import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

private val UTF8_CHARSET = Charset.forName("UTF8")

fun Event.toJson(): String {
    ByteArrayOutputStream().use { outputStream ->
        val writer = SpecificDatumWriter<Event>(schema)
        val encoder = EncoderFactory.get().jsonEncoder(schema, outputStream)
        writer.write(this, encoder)
        encoder.flush()
        return outputStream.toString(se.sbab.eventsourcing.UTF8_CHARSET)
    }
}
