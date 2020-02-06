/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koin.core.scope

import org.koin.core.Koin
import org.koin.core.definition.ThreadScope
import org.koin.core.definition.indexKey
import org.koin.core.error.ClosedScopeException
import org.koin.core.error.MissingPropertyException
import org.koin.core.error.NoBeanDefFoundException
import org.koin.core.logger.Level
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.registry.InstanceRegistry
import org.koin.core.state.MainIsolatedState
import org.koin.core.state.getFullName
import org.koin.core.state.mainOrBlock
import org.koin.core.state.value
import org.koin.core.time.measureDurationForResult
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

internal class ScopeState(koin: Koin, scope: Scope) {
    internal val _linkedScope: MutableList<Scope> = arrayListOf()
    internal val _instanceRegistry: InstanceRegistry = InstanceRegistry(koin, scope)
    internal val _callbacks: MutableList<ScopeCallback> = arrayListOf<ScopeCallback>()
    internal var _closed: Boolean = false
}

data class Scope(
        val id: ScopeID,
        val _scopeDefinition: ScopeDefinition,
        val _koin: Koin
) {
    internal val scopeState = MainIsolatedState(ScopeState(_koin, this))
    val _linkedScope: MutableList<Scope>
        get() = scopeState.value._linkedScope
    val _instanceRegistry: InstanceRegistry
        get() = scopeState.value._instanceRegistry
    val closed: Boolean
        get() = scopeState.value._closed

    internal fun create(links: List<Scope>) {
        _instanceRegistry.create(_scopeDefinition.definitions)
        _linkedScope.addAll(links)
    }

    /**
     * Add parent Scopes to allow instance resolution
     * i.e: linkTo(scopeC) - allow to resolve instance to current scope and scopeC
     *
     * @param scopes - Scopes to link with
     */
    fun linkTo(vararg scopes: Scope) {
        if (!_scopeDefinition.isRoot) {
            _linkedScope.addAll(scopes)
        } else {
            error("Can't add scope link to a root scope")
        }
    }

    /**
     * Remove linked scope
     */
    fun unlink(vararg scopes: Scope) {
        if (!_scopeDefinition.isRoot) {
            _linkedScope.removeAll(scopes)
        } else {
            error("Can't remove scope link to a root scope")
        }
    }

    /**
     * Lazy inject a Koin instance
     * @param qualifier
     * @param scope
     * @param parameters
     *
     * @return Lazy instance of type T
     */
    @JvmOverloads
    inline fun <reified T> inject(
            qualifier: Qualifier? = null,
            noinline parameters: ParametersDefinition? = null
    ): Lazy<T> =
            lazy(LazyThreadSafetyMode.NONE) { get<T>(qualifier, parameters) }

    /**
     * Lazy inject a Koin instance if available
     * @param qualifier
     * @param scope
     * @param parameters
     *
     * @return Lazy instance of type T or null
     */
    @JvmOverloads
    inline fun <reified T> injectOrNull(
            qualifier: Qualifier? = null,
            noinline parameters: ParametersDefinition? = null
    ): Lazy<T?> =
            lazy(LazyThreadSafetyMode.NONE) { getOrNull<T>(qualifier, parameters) }

    /**
     * Get a Koin instance
     * @param qualifier
     * @param scope
     * @param parameters
     */
    @JvmOverloads
    inline fun <reified T> get(
            qualifier: Qualifier? = null,
            noinline parameters: ParametersDefinition? = null
    ): T {
        return get(T::class, qualifier, parameters)
    }

    /**
     * Get a Koin instance if available
     * @param qualifier
     * @param scope
     * @param parameters
     *
     * @return instance of type T or null
     */
    @JvmOverloads
    inline fun <reified T> getOrNull(
            qualifier: Qualifier? = null,
            noinline parameters: ParametersDefinition? = null
    ): T? {
        return getOrNull(T::class, qualifier, parameters)
    }

    /**
     * Get a Koin instance if available
     * @param qualifier
     * @param scope
     * @param parameters
     *
     * @return instance of type T or null
     */
    @JvmOverloads
    fun <T> getOrNull(
            clazz: KClass<*>,
            qualifier: Qualifier? = null,
            parameters: ParametersDefinition? = null
    ): T? {
        return try {
            get(clazz, qualifier, parameters)
        } catch (e: Exception) {
            _koin._logger.error("Can't get instance for ${clazz.getFullName()}")
            null
        }
    }

    /**
     * Get a Koin instance
     * @param clazz
     * @param qualifier
     * @param parameters
     *
     * @return instance of type T
     */
    fun <T> get(
            clazz: KClass<*>,
            qualifier: Qualifier? = null,
            parameters: ParametersDefinition? = null
    ): T {
        return if (_koin._logger.isAt(Level.DEBUG)) {
            _koin._logger.debug("+- get '${clazz.getFullName()}' with qualifier '$qualifier'")
            val (instance: T, duration: Double) = measureDurationForResult {
                resolveInstance<T>(qualifier, clazz, parameters)
            }
            _koin._logger.debug("+- got '${clazz.getFullName()}' in $duration ms")
            return instance
        } else {
            resolveInstance(qualifier, clazz, parameters)
        }
    }

    private fun <T> resolveInstance(
            qualifier: Qualifier?,
            clazz: KClass<*>,
            parameters: ParametersDefinition?
    ): T = mainOrBlock {
        if (scopeState.value._closed) {
            throw ClosedScopeException("Scope '$id' is closed")
        }
        //TODO Resolve in Root or link
        val indexKey = indexKey(clazz, qualifier)
        scopeState.value._instanceRegistry.resolveInstance(indexKey, parameters)
                ?: findInOtherScope<T>(clazz, qualifier, parameters)
                ?: throwDefinitionNotFound(qualifier, clazz)
    }

    private fun <T> findInOtherScope(
            clazz: KClass<*>,
            qualifier: Qualifier?,
            parameters: ParametersDefinition?
    ): T? {
        return scopeState.value._linkedScope.firstOrNull { scope ->
            scope.getOrNull<T>(
                    clazz,
                    qualifier,
                    parameters
            ) != null
        }?.get(
                clazz,
                qualifier,
                parameters
        )
    }

    private fun throwDefinitionNotFound(
            qualifier: Qualifier?,
            clazz: KClass<*>
    ): Nothing {
        val qualifierString = qualifier?.let { " & qualifier:'$qualifier'" } ?: ""
        throw NoBeanDefFoundException("No definition found for class:'${clazz.getFullName()}'$qualifierString. Check your definitions!")
    }

    internal fun createEagerInstances() {
        if (_scopeDefinition.isRoot) {
            scopeState.value._instanceRegistry.createEagerInstances()
        }
    }

    /**
     * Declare a component definition from the given instance
     * This result of declaring a scoped/single definition of type T, returning the given instance
     * (single definition of th current scope is root)
     *
     * @param instance The instance you're declaring.
     * @param qualifier Qualifier for this declaration
     * @param secondaryTypes List of secondary bound types
     * @param override Allows to override a previous declaration of the same type (default to false).
     */
    fun <T : Any> declare(
            instance: T,
            qualifier: Qualifier? = null,
            secondaryTypes: List<KClass<*>>? = null,
            override: Boolean = false,
            threadScope: ThreadScope = ThreadScope.Main
            ) {
        val definition = _scopeDefinition.saveNewDefinition(instance, qualifier, secondaryTypes, override, threadScope)
        scopeState.value._instanceRegistry.saveDefinition(definition, override = true)
    }

    /**
     * Get current Koin instance
     */
    fun getKoin() = _koin

    /**
     * Get Scope
     * @param scopeID
     */
    fun getScope(scopeID: ScopeID) = getKoin().getScope(scopeID)

    /**
     * Register a callback for this Scope Instance
     */
    fun registerCallback(callback: ScopeCallback) {
        scopeState.value._callbacks += callback
    }

    /**
     * Get a all instance for given inferred class (in primary or secondary type)
     *
     * @return list of instances of type T
     */
    inline fun <reified T : Any> getAll(): List<T> = getAll(T::class)

    /**
     * Get a all instance for given class (in primary or secondary type)
     * @param clazz T
     *
     * @return list of instances of type T
     */
    fun <T : Any> getAll(clazz: KClass<*>): List<T> = mainOrBlock { scopeState.value._instanceRegistry.getAll(clazz) }

    /**
     * Get instance of primary type P and secondary type S
     * (not for scoped instances)
     *
     * @return instance of type S
     */
    inline fun <reified S, reified P> bind(noinline parameters: ParametersDefinition? = null): S = mainOrBlock {
        val secondaryType = S::class
        val primaryType = P::class
        bind(primaryType, secondaryType, parameters)
    }

    /**
     * Get instance of primary type P and secondary type S
     * (not for scoped instances)
     *
     * @return instance of type S
     */
    fun <S> bind(
            primaryType: KClass<*>,
            secondaryType: KClass<*>,
            parameters: ParametersDefinition?
    ): S = mainOrBlock {
        scopeState.value._instanceRegistry.bind(primaryType, secondaryType, parameters)
                ?: throw NoBeanDefFoundException("No definition found to bind class:'${primaryType.getFullName()}' & secondary type:'${secondaryType.getFullName()}'. Check your definitions!")
    }

    /**
     * Retrieve a property
     * @param key
     * @param defaultValue
     */
    fun <T> getProperty(key: String, defaultValue: T): T = _koin.getProperty(key, defaultValue)

    /**
     * Retrieve a property
     * @param key
     */
    fun <T> getPropertyOrNull(key: String): T? = _koin.getProperty(key)

    /**
     * Retrieve a property
     * @param key
     */
    fun <T> getProperty(key: String): T = _koin.getProperty(key)
            ?: throw MissingPropertyException("Property '$key' not found")

    /**
     * Close all instances from this scope
     */
    fun close() {
        scopeState.value._closed = true
        if (_koin._logger.isAt(Level.DEBUG)) {
            _koin._logger.info("closing scope:'$id'")
        }
        // call on close from callbacks
        scopeState.value._callbacks.forEach { it.onScopeClose(this) }
        scopeState.value._callbacks.clear()

        scopeState.value._instanceRegistry.close()
        _koin._koinState.value._scopeRegistry.deleteScope(this)
    }

    override fun toString(): String {
        return "['$id']"
    }

    fun dropInstances(scopeDefinition: ScopeDefinition) {
        scopeDefinition.definitions.forEach {
            scopeState.value._instanceRegistry.dropDefinition(it)
        }
    }

    fun loadDefinitions(scopeDefinition: ScopeDefinition) {
        scopeDefinition.definitions.forEach {
            scopeState.value._instanceRegistry.createDefinition(it)
        }
    }
}

typealias ScopeID = String
