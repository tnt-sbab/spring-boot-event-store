package se.sbab.demo.es.avro

import org.apache.avro.Conversion
import org.apache.avro.LogicalType
import org.apache.avro.Schema

import se.sbab.demo.es.AccountId
import se.sbab.demo.es.avro.DemoLogicalTypes.Companion.ACCOUNT_ID

class Conversions {
    class AccountIdConversion : ConversionStringType<AccountId>(ACCOUNT_ID) {
        override fun getConvertedType(): Class<AccountId> = AccountId::class.java
        override fun fromCharSequence(value: CharSequence, schema: Schema?, type: LogicalType?) = AccountId(value.toString())
    }

    abstract class ConversionStringType<T>(private val name: String) : Conversion<T>() {
        override fun getRecommendedSchema(): Schema = LogicalType(name).addToSchema(Schema.create(Schema.Type.STRING))
        override fun getLogicalTypeName() = name
        override fun toCharSequence(value: T, schema: Schema?, type: LogicalType?) = value.toString()
    }
}