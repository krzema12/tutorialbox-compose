package com.pdrozz.tutorialbox.tutorialscope

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pdrozz.tutorialbox.state.TutorialBoxState
import com.pdrozz.tutorialbox.state.TutorialBoxTarget

class TutorialBoxScope(
    private val state: TutorialBoxState,
) {

    fun Modifier.markForTutorial(
        index: Int,
        content: @Composable BoxScope.() -> Unit,
    ): Modifier = tutorialTarget(
        state = state,
        index = index,
        content = content,
    )

    fun Modifier.markForTutorial(
        index: Int,
        title: String,
        description: String? = null
    ): Modifier = tutorialTarget(
        state = state,
        index = index,
        content = {
            TutorialText(title = title, description = description)
        }
    )

    @Composable
    internal fun TutorialCompose(
        state: TutorialBoxState,
        constraints: Constraints,
        onTutorialCompleted: () -> Unit,
        onTutorialIndexChanged: (Int) -> Unit
    ) {
        TutorialFocusBox(currentContent = state.currentTarget)

        TutorialTarget(currentContent = state.currentTarget, constraints)

        TutorialClickHandler {
            state.currentTargetIndex++
            onTutorialIndexChanged(state.currentTargetIndex)
            if (state.currentTargetIndex >= state.tutorialTargets.size) {
                onTutorialCompleted()
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun TutorialFocusBox(currentContent: TutorialBoxTarget?) {
        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = currentContent,
            transitionSpec = {
                fadeIn(tween(500)) with fadeOut(tween(500))
            }) { state ->
            state?.let { content ->
                Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
                    val cornerRadius = 18f
                    val focusPadding = 10
                    val offSetInRoot = content.coordinates.positionInRoot()
                    val contentSize = content.coordinates.size

                    val pathToClip = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = offSetInRoot.x - focusPadding,
                                top = offSetInRoot.y - focusPadding,
                                right = offSetInRoot.x + contentSize.width.toFloat() + focusPadding,
                                bottom = offSetInRoot.y + contentSize.height.toFloat() + focusPadding,
                                cornerRadius,
                                cornerRadius
                            )
                        )
                    }
                    clipPath(pathToClip, clipOp = ClipOp.Difference) {
                        drawRect(
                            SolidColor(Color.Black.copy(alpha = 0.6f)),
                            topLeft = Offset(0f, 0f)
                        )
                    }
                })
            }
        }
    }

    @Composable
    private fun TutorialTarget(currentContent: TutorialBoxTarget?, constraints: Constraints) {
        val alpha = remember { Animatable(0f) }

        LaunchedEffect(currentContent) {
            alpha.animateTo(1f, tween(500))
        }

        currentContent?.let { tutorialContent ->
            val composeWidth = remember(tutorialContent) {
                tutorialContent.coordinates.size.width
            }
            val composeHeight = remember(tutorialContent) {
                tutorialContent.coordinates.size.height
            }
            val x = remember(tutorialContent) {
                (tutorialContent.coordinates.positionInRoot().x.toInt())
            }
            val y = remember(tutorialContent) {
                (tutorialContent.coordinates.positionInRoot().y.toInt())
            }

            var tutorialSize by remember {
                mutableStateOf(IntSize.Zero)
            }

            val xWithDisplacement by remember(x, composeWidth, tutorialSize) {
                derivedStateOf {
                    val displacement = calculateDisplacementToMid(
                        startX = x, composeWidth = composeWidth, tutorialComposeWidth = tutorialSize.width
                    )

                    val xWithDisplacement = x + displacement

                    if(xWithDisplacement < 0) 0
                    else if (xWithDisplacement + tutorialSize.width > constraints.maxWidth) xWithDisplacement
                    else xWithDisplacement
                }
            }

            val outOfBoundsStart by remember(xWithDisplacement) {
                mutableStateOf(xWithDisplacement < 0)
            }

            val outOfBoundsEnd by remember(xWithDisplacement) {
                mutableStateOf(xWithDisplacement + tutorialSize.width > constraints.maxWidth)
            }

            val outOfBoundsTop by remember(xWithDisplacement) {
                mutableStateOf(y < 0)
            }

            val outOfBoundsBottom by remember(xWithDisplacement) {
                mutableStateOf(y + tutorialSize.height > constraints.maxHeight)
            }

            val xToDraw by remember(xWithDisplacement, tutorialSize, constraints) {
                derivedStateOf {
                    val xSafeRight =
                        xWithDisplacement - ((xWithDisplacement + tutorialSize.width) - constraints.maxWidth)

                    val safeX = if (outOfBoundsStart) 0
                    else if (outOfBoundsEnd) xSafeRight
                    else xWithDisplacement

                    safeX
                }
            }

            val yToDraw by remember(y, tutorialSize, constraints) {
                derivedStateOf {
                    val ySafeBottom = y - ((y + tutorialSize.height) - constraints.maxHeight)

                    var safeY = if (outOfBoundsTop) 0
                    else if (outOfBoundsBottom) ySafeBottom
                    else y

                    val isTutorialInFrontOfContent =
                        (safeY >= y && safeY <= (y + composeHeight)) ||
                                !(safeY <= y  && safeY >= (y + composeHeight))

                    if (isTutorialInFrontOfContent) {
                        val tutorialHeight = tutorialSize.height

                        if (safeY + composeHeight + tutorialHeight < constraints.maxHeight) {
                            // is safe to draw bottom to content
                            safeY += composeHeight + (18)
                        } else if (safeY - composeHeight - tutorialHeight > 0) {
                            // is safe to draw top to content
                            safeY -= (tutorialHeight + 18)
                        }
                    }
                    safeY
                }
            }

            Box(
                modifier = Modifier
                    .alpha(alpha.value)
                    .onSizeChanged { tutorialSize = it }
                    .offset {
                        IntOffset(xToDraw, yToDraw)
                    }
            ) {
                tutorialContent.content(this)
            }
        }
    }

    @Composable
    private fun TutorialClickHandler(onTutorialClick: () -> Unit){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClick = onTutorialClick
                )
        )
    }
}

internal fun calculateDisplacementToMid(
    startX: Int, composeWidth: Int, tutorialComposeWidth: Int
): Int {
    val xMidComponent = startX + (composeWidth / 2)
    val xMidTutorial = startX + (tutorialComposeWidth / 2)
    var displacementToMid = kotlin.math.abs(xMidComponent - xMidTutorial)
    if (xMidComponent < xMidTutorial) displacementToMid *= -1
    return displacementToMid
}

internal fun Modifier.tutorialTarget(
    state: TutorialBoxState,
    index: Int,
    content: @Composable BoxScope.() -> Unit,
): Modifier = onGloballyPositioned { coordinates ->
    state.tutorialTargets[index] = TutorialBoxTarget(
        index = index,
        coordinates = coordinates,
        content = content
    )
}

@Composable
internal fun TutorialText(
    title: String,
    description: String? = null
) {
    Column(
        modifier = Modifier
            .animateContentSize()
            .padding(horizontal = 12.dp)
            .background(
                color = Color.White, shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description
            )
        }
    }
}