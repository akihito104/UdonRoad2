package com.freshdigitable.udonroad2.model.app.di

import androidx.lifecycle.ViewModel
import dagger.MapKey
import javax.inject.Scope
import kotlin.reflect.KClass

@Scope
@MustBeDocumented
@Retention
annotation class ActivityScope

@Scope
@MustBeDocumented
@Retention
annotation class FragmentScope

@Scope
@MustBeDocumented
@Retention
annotation class ViewModelScope

@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)
