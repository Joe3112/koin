package org.koin.core.instance

import org.koin.core.Koin
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.emptyParametersHolder
import org.koin.core.scope.ScopeBasedInteractor


/**
 * Instance resolution Context
 * Help support DefinitionContext & DefinitionParameters when resolving definition function
 */
class InstanceContext(
    val koin: Koin,
    val scope: ScopeBasedInteractor,
    _parameters: ParametersDefinition? = null
) {
    val parameters: DefinitionParameters = _parameters?.invoke() ?: emptyParametersHolder()
}
