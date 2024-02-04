package se.sbab.demo.es.avro

import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import se.sbab.demo.es.avro.DemoLogicalTypes.Companion.ACCOUNT_ID

class TypeFactories {
    abstract class AbstractStringLogicalTypeFactory(private val logicalTypeName: String) : LogicalTypes.LogicalTypeFactory {
        override fun fromSchema(schema: Schema): LogicalType = DemoLogicalTypes.StringLogicalType(logicalTypeName)

        override fun getTypeName(): String = logicalTypeName
    }

    class AccountIdTypeFactory : AbstractStringLogicalTypeFactory(ACCOUNT_ID)
}