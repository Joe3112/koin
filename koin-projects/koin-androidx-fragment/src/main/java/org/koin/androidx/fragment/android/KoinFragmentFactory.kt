package org.koin.androidx.fragment.android

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.koin.core.KoinComponent
import org.koin.core.scope.Scope
import org.koin.core.scope.get

/**
 * FragmentFactory for Koin
 */
class KoinFragmentFactory(val scope: Scope? = null) : FragmentFactory(), KoinComponent {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val javaClass = Class.forName(className)
        return scope?.let { it.get<Fragment>(javaClass) }
            ?: getKoin().get(javaClass.kotlin)
    }

}