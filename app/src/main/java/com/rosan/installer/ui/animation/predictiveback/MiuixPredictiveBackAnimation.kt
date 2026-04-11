// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState

// TODO Add an config page for user to select predictiveBack implement
class MiuixPredictiveBackAnimation : PredictiveBackAnimationHandler {

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?
    ) {
        // Deliberately empty. Predictive back gesture progress is natively handled
        // and synchronized by the Compose transition engine.
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier {
        // NavDisplay automatically handles dimming and corner clipping internally
        // through NavDisplayTransitionEffects, so we return unmodified this.
        return this
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform {
        // 直接调用 NavDisplay 暴露的默认预测性返回动画
        return defaultPredictivePopTransitionSpec<NavKey>().invoke(this, swipeEdge)
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform {
        // 直接调用 NavDisplay 暴露的默认返回动画
        return defaultPopTransitionSpec<NavKey>().invoke(this)
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform {
        // 直接调用 NavDisplay 暴露的默认入场动画
        return defaultTransitionSpec<NavKey>().invoke(this)
    }
}