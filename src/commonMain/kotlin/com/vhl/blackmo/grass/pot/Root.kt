package com.vhl.blackmo.grass.pot

import com.vhl.blackmo.grass.errors.MissMatchedNumberOfFieldsException
import com.vhl.blackmo.grass.errors.MissMatchedFieldNameException
import com.vhl.blackmo.grass.vein.PrimitiveType
import kotlin.reflect.*

/**
 * @author blackmo18
 */
@ExperimentalStdlibApi
open class Root<out T>(
        val type: KClass<*>,
        private val trim: Boolean,
        val receivedKeyMap: Map<String, String>?
) {

    /**
     * Key-value pair containing the expression from converting from from data class property name
     * to actual type(class property definition)
     */
    protected val paramNTypes = mutableMapOf<String?, ((String) -> Any)? >()

    /**
     * Key value pair index(order) of the  data class property vs property name
     */
    protected val paramNIndex = mutableMapOf<String?, Int >()

    /**
     * User custom key mapping input
     */
    protected val customKeyMap = mutableMapOf<String,String>()

    /**
     * Method that is overridden to initialized the value of types and indexes mapping
     */

    protected fun createObject( row: Map<String, String>): Array<Any?> {

        val actualParams = Array<Any?>(paramNTypes.size){}
        validateNumberOfFields(row.keys.size, paramNTypes.size)

        loop@ for (mapRow in row) {
            val key = mapRow.key.trim()
            val value = mapRow.value.trimOrNot(trim)
            val hasKey = paramNTypes.containsKey(key)
            when {
                hasKey && mapRow.value.isNotBlank() -> {
                    val index = paramNIndex[key]!!
                    actualParams[index] = paramNTypes[key]!!.invoke(value)
                }
                hasKey && mapRow.value.isBlank() -> {
                    val index = paramNIndex[key]!!
                    actualParams[index] = null
                }
                else -> {
                    if (customKeyMap.isNotEmpty()) {
                        if (customKeyMap.containsKey(key)) {
                            val mappedKey = customKeyMap[key]?.trim()
                            if(paramNTypes.containsKey(mappedKey)) {
                                customKeyMap.remove(mappedKey)
                                val index = paramNIndex[mappedKey]!!
                                actualParams[index] = paramNTypes[mappedKey]!!.invoke(mapRow.value)
                                continue@loop
                            }
                        }
                    }
                    throw MissMatchedFieldNameException(mapRow.key)
                }
            }
        }
        return actualParams
    }

    private fun String.trimOrNot(boolean: Boolean): String = when {
        boolean -> this.trim()
        else -> this
    }

    private fun validateNumberOfFields(csvLength: Int, dataClassFieldLength: Int) {
        if (csvLength != dataClassFieldLength) {
            throw MissMatchedNumberOfFieldsException(csvLength, dataClassFieldLength)
        }
    }

    open fun getType(type: KType)  = PrimitiveType.mapTypes[type]
}