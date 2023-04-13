package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.components.forms.widgets.TextInputWidget
import com.kotlindiscord.kord.extensions.utils.getKoin
import net.greemdev.cabinet.lib.util.invoking
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatformTools

fun<T : TextInputWidget<T>> T.get() =
    if (required)
        value!!
    else value ?: ""


fun<T : TextInputWidget<T>> T.getListValue(separator: Char = ';'): List<String> =
    when (val str = get()) {
        "" -> listOf()
        else -> str.split(separator)
    }

inline fun<reified T : Any> koinInject(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode(),
    noinline parameters: ParametersDefinition? = null
) = getKoin().inject<T>(qualifier, mode, parameters)

inline fun<reified T : Any> koinComponent(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = getKoin().get<T>(qualifier, parameters)
inline fun<reified T : Any> dynamicKoinComponent() = invoking { koinComponent<T>() }