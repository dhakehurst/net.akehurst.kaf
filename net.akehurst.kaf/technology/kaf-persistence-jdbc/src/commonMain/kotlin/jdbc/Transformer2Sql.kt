package net.akehurst.kaf.technology.persistence.jdbc

import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.transform.binary.api.BinaryRule
import net.akehurst.transform.binary.api.BinaryTransformer
import net.akehurst.transform.binary.api.TransformException
import net.akehurst.transform.binary.basic.BinaryTransformerAbstract
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


class Transformer2Sql(val settings: Map<String, String>) : BinaryTransformerAbstract() {
    init {
        super.registerRule(KClass2CreateTableStatement::class)
        super.registerRule(KProperty2SqlCreateColumnString::class)
        super.registerRule(PrimitiveType2SqlType::class)
        super.registerRule(Any2InsertStatement::class)
    }

    fun <T : Any> transform2CreateTable(type: KClass<T>): List<String> {
        return listOf(this.transformLeft2Right(KClass2CreateTableStatement::class, type))
    }

    fun <T : Any> transform2CreateItem(type: KClass<T>, item: T): List<String> {
        val createTable = this.transform2CreateTable(type)
        val insertItem = this.transformLeft2Right(Any2InsertStatement::class, Pair(type,item))
        return createTable + insertItem
    }

}

class KClass2CreateTableStatement : BinaryRule<KClass<*>, String> {

    override fun isAMatch(left: KClass<*>, right: String, transformer: BinaryTransformer): Boolean {
        return true
    }

    override fun isValidForLeft2Right(left: KClass<*>, transformer: BinaryTransformer): Boolean {
        return true
    }

    override fun isValidForRight2Left(right: String, transformer: BinaryTransformer): Boolean {
        return true
    }

    override fun constructLeft2Right(left: KClass<*>, transformer: BinaryTransformer): String {
        val name: String = left.simpleName ?: throw Exception("Cannot get name of type") //TODO: better exception
        val properties = left.members.filterIsInstance<KProperty<*>>()
        val propertyList = transformer.transformAllLeft2Right(KProperty2SqlCreateColumnString::class, properties)
        return "CREATE TABLE IF NOT EXISTS $name ( ${propertyList.joinToString(", ")} )";
    }

    override fun constructRight2Left(right: String, transformer: BinaryTransformer): KClass<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateLeft2Right(left: KClass<*>, right: String, transformer: BinaryTransformer) {

    }

    override fun updateRight2Left(left: KClass<*>, right: String, transformer: BinaryTransformer) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class KProperty2SqlCreateColumnString : BinaryRule<KProperty<*>, String> {

    override fun isAMatch(left: KProperty<*>, right: String, transformer: BinaryTransformer): Boolean = true

    override fun isValidForLeft2Right(left: KProperty<*>, transformer: BinaryTransformer): Boolean {
        return null!=left.returnType.classifier && left.returnType.classifier is KClass<*>
    }

    override fun isValidForRight2Left(right: String, transformer: BinaryTransformer): Boolean = TODO("not implemented")

    override fun constructLeft2Right(left: KProperty<*>, transformer: BinaryTransformer): String {
        val colname = left.name
        val classifier = left.returnType.classifier ?: throw TransformException("")
        val type = transformer.transformLeft2Right(PrimitiveType2SqlType::class, classifier as KClass<*>)
        return "$colname ${type.name}"
    }

    override fun constructRight2Left(right: String, transformer: BinaryTransformer): KProperty<*> {
        TODO("not implemented")
    }

    override fun updateLeft2Right(left: KProperty<*>, right: String, transformer: BinaryTransformer) {
    }

    override fun updateRight2Left(left: KProperty<*>, right: String, transformer: BinaryTransformer) {
        TODO("not implemented")
    }

}

class PrimitiveType2SqlType : BinaryRule<KClass<*>, JdbcType> {

    override fun isAMatch(left: KClass<*>, right: JdbcType, transformer: BinaryTransformer): Boolean {
        return JdbcKotlinTypeMapping.toJdbc(left) == right
    }

    override fun isValidForLeft2Right(left: KClass<*>, transformer: BinaryTransformer): Boolean {
        try {
            JdbcKotlinTypeMapping.toJdbc(left)
            return true
        } catch (e: JdbcPersistenceException) {
            return false
        }
    }

    override fun isValidForRight2Left(right: JdbcType, transformer: BinaryTransformer): Boolean {
        try {
            JdbcKotlinTypeMapping.toKotlin(right)
            return true
        } catch (e: JdbcPersistenceException) {
            return false
        }
    }

    override fun constructLeft2Right(left: KClass<*>, transformer: BinaryTransformer): JdbcType {
        return JdbcKotlinTypeMapping.toJdbc(left)
    }

    override fun constructRight2Left(right: JdbcType, transformer: BinaryTransformer): KClass<*> {
        return JdbcKotlinTypeMapping.toKotlin(right)
    }

    override fun updateLeft2Right(left: KClass<*>, right: JdbcType, transformer: BinaryTransformer) {

    }

    override fun updateRight2Left(left: KClass<*>, right: JdbcType, transformer: BinaryTransformer) {

    }


}

class Any2InsertStatement : BinaryRule<Pair<KClass<*>,Any>, String> {
    override fun isAMatch(left: Pair<KClass<*>,Any>, right: String, transformer: BinaryTransformer): Boolean = true

    override fun isValidForLeft2Right(left: Pair<KClass<*>,Any>, transformer: BinaryTransformer): Boolean {
        return left.first.isInstance(left.second)
    }

    override fun isValidForRight2Left(right: String, transformer: BinaryTransformer): Boolean = true

    override fun constructLeft2Right(left: Pair<KClass<*>,Any>, transformer: BinaryTransformer): String {
        val tablename = left.first.simpleName ?: TransformException("KClass of left object must have a simpleName")
        val colValues = left.first.members.filterIsInstance<KProperty<*>>().map {
            val value = it.call(left.second)
            val sqlValue = when (value) {
                is String -> "'$value'"
                else -> value.toString()
            }
            Pair(it.name, sqlValue)
        }
        val colNames = colValues.map { it.first }
        val values = colValues.map { it.second }
        return "INSERT INTO $tablename ( ${colNames.joinToString(", ")} ) VALUES ( ${values.joinToString(", ")} )"
    }

    override fun constructRight2Left(right: String, transformer: BinaryTransformer): Pair<KClass<*>,Any> {
        TODO("not implemented")
    }

    override fun updateLeft2Right(left: Pair<KClass<*>,Any>, right: String, transformer: BinaryTransformer) {
    }

    override fun updateRight2Left(left: Pair<KClass<*>,Any>, right: String, transformer: BinaryTransformer) {
        TODO("not implemented")
    }

}