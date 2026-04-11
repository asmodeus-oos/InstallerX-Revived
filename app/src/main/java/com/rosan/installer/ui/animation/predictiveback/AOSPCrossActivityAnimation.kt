// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 wxxsfxyzm
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.util.rememberDeviceCornerRadius
import timber.log.Timber

class AOSPCrossActivityAnimation(
    private val exitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT
) : PredictiveBackAnimationHandler {
    private var exitingPageKey: String? = null
    private val exitAnimatable = Animatable(0f)

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?,
    ) {
        if (transitionState is InProgress) {
            exitAnimatable.snapTo(0f)

            exitingPageKey = currentPageKey.toString()
            Timber.d("[BackAnim] onBackPressed: exitingPageKey='$exitingPageKey'")

            exitAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = LinearEasing)
            )
            Timber.d("[BackAnim] animateTo(1f) complete, value=${exitAnimatable.value}")
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier = composed {
        val windowInfo = LocalWindowInfo.current
        val containerHeightPx = windowInfo.containerSize.height
        val pageKey = contentPageKey.toString()
        val deviceCornerRadius = rememberDeviceCornerRadius()

        DisposableEffect(pageKey) {
            onDispose {
                if (exitingPageKey == pageKey) {
                    exitingPageKey = null
                }
            }
        }

        val enteringStartOffsetPx = with(LocalDensity.current) { 96.dp.toPx() }

        val linearProgress = exitAnimatable.value.coerceAtMost(1f)
        val emphasizedProgress = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(linearProgress)

        val progressInProgress = (transitionState as? InProgress)
        val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
        val touchY = progressInProgress?.latestEvent?.touchY
        val gestureProgress = progressInProgress?.latestEvent?.progress ?: 0f

        val directionMultiplier = when (exitDirection) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> if (edge == EDGE_LEFT) 1f else -1f
            PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
            PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
        }

        val isExitingPage = exitingPageKey != null && exitingPageKey == pageKey
        val isCurrentNavTarget = exitingPageKey == null && pageKey == currentPageKey.toString()

        val maxScale = 0.85f
        val dragScale = 1f - (1f - maxScale) * gestureProgress

        val currentPivotY = if (touchY != null && containerHeightPx > 0) {
            (touchY / containerHeightPx).coerceIn(0.1f, 0.9f)
        } else 0.5f
        val currentPivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f

        this
            .graphicsLayer {
                transformOrigin = TransformOrigin(currentPivotX, currentPivotY)

                when {
                    isExitingPage -> {
                        val computedScaleX = dragScale + (maxScale - dragScale) * emphasizedProgress
                        val computedTranslationX = enteringStartOffsetPx * directionMultiplier * emphasizedProgress
                        val computedAlpha = if (linearProgress >= 0.2f) 0f else (1f - linearProgress * 5f).coerceAtLeast(0f)

                        scaleX = computedScaleX
                        scaleY = computedScaleX
                        translationX = computedTranslationX
                        alpha = computedAlpha
                    }

                    isCurrentNavTarget -> {
                        scaleX = dragScale
                        scaleY = dragScale
                        translationX = 0f
                        alpha = 1f
                    }

                    else -> {
                        val initialTranslationX = -enteringStartOffsetPx * directionMultiplier

                        if (exitingPageKey != null) {
                            scaleX = dragScale + (1f - dragScale) * emphasizedProgress
                            scaleY = dragScale + (1f - dragScale) * emphasizedProgress
                            translationX = initialTranslationX * (1f - emphasizedProgress)
                            alpha = 1f
                        } else if (transitionState is InProgress) {
                            scaleX = dragScale
                            scaleY = dragScale
                            translationX = initialTranslationX
                            alpha = 1f
                        }
                    }
                }
            }
            .clip(
                if (isExitingPage || isCurrentNavTarget) RoundedCornerShape(deviceCornerRadius)
                else RoundedCornerShape(0.dp)
            )
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null
    )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            // 【修改】去掉了底下页面（targetContentEnter）的 fadeIn()
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(), // 顶层页面继续保持淡出+缩小
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { it }),
            // 【修改】去掉了底下页面（initialContentExit）的 fadeOut()，替换为 None 让它直接被新页面覆盖
            initialContentExit = ExitTransition.None,
            sizeTransform = null
        )
}