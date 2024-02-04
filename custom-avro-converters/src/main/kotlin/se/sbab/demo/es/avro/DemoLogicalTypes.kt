package se.sbab.demo.es.avro

import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import kotlin.reflect.KClass


class DemoLogicalTypes {
    companion object {
        const val ACCOUNT_ID = "account-id"

        fun registerTypeFactories() {
            TypeFactories::class.nestedClasses.filter { shouldRegister(it) }.forEach {
                registerTypeFactory(instantiate(it))
            }
        }

        private fun shouldRegister(clazz: KClass<*>): Boolean = !clazz.isAbstract

        private fun instantiate(clazz: KClass<*>): LogicalTypes.LogicalTypeFactory =
            clazz.constructors.first { it.parameters.isEmpty() }.call() as LogicalTypes.LogicalTypeFactory

        private fun registerTypeFactory(logicalTypeFactory: LogicalTypes.LogicalTypeFactory) {
            LogicalTypes.register(logicalTypeFactory.typeName, logicalTypeFactory)
        }
    }

    class StringLogicalType(private val logicalTypeName: String) : LogicalType(logicalTypeName) {
        override fun validate(schema: Schema) {
            super.validate(schema)
            if (schema.type != Schema.Type.STRING) {
                throw IllegalAccessError("Only string type allowed for logical type \"$logicalTypeName\". Received type is ${schema.type.getName()}.")
            }
        }
    }
}