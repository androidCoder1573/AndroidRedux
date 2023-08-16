package com.cyworks.redux.annotations

import kotlin.reflect.KClass

// 定义一个注解，用于标记需要委托的属性
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ReactUIData(val delegateClass: KClass<out Any>)