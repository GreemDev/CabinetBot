package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.components.forms.widgets.TextInputWidget
import com.kotlindiscord.kord.extensions.utils.getKoin
import org.koin.core.component.inject

fun<T : TextInputWidget<T>> TextInputWidget<T>.get(): String {
    return if (required)
        value!!
    else value ?: ""
}

inline fun<reified T : Any> koinInject() = getKoin().inject<T>()