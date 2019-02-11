/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.fabshortcut

import android.view.MotionEvent

enum class Direction constructor(
        val index: Int
) {
    UP(6),
    UP_RIGHT(7),
    RIGHT(0),
    DOWN_RIGHT(1),
    DOWN(2),
    DOWN_LEFT(3),
    LEFT(4),
    UP_LEFT(5),
    UNDEFINED(-1)
    ;

    internal val bothNeighbor: Array<Direction>
        get() = if (this == UNDEFINED) {
            arrayOf()
        } else arrayOf(
                findByIndex((index + 1) % 8),
                findByIndex(if ((index - 1) < 0) 7 else index - 1)
        )

    internal val isOnAxis: Boolean
        get() = index >= 0 && index % 2 == 0

    private val angleLowerThresh = index * Direction.ANGLE_DIVIDE - Direction.ANGLE_THRESHOLD
    private val angleUpperThresh = index * Direction.ANGLE_DIVIDE + Direction.ANGLE_THRESHOLD

    companion object {
        private const val SWIPE_MIN_DISTANCE = 120f * 120f
        private const val SWIPE_THRESH_VER = 200f * 200f
        private const val ANGLE_DIVIDE = 2 * Math.PI / 8
        private const val ANGLE_THRESHOLD = ANGLE_DIVIDE / 2

        internal fun getDirection(
                e1: MotionEvent,
                e2: MotionEvent
        ): Direction {
            val distX = e2.x - e1.x
            val distY = e2.y - e1.y
            val dist = distX * distX + distY * distY
            if (dist < SWIPE_MIN_DISTANCE) {
                return UNDEFINED
            }
            val angle = atan3(distY, distX)
            return Direction.values().filter { it != UNDEFINED }.firstOrNull { d ->
                if (RIGHT == d) {
                    return@firstOrNull angle < ANGLE_THRESHOLD
                            || angle > 2 * Math.PI - ANGLE_THRESHOLD
                }
                return@firstOrNull angle > d.angleLowerThresh
                        && angle < d.angleUpperThresh
            } ?: UNDEFINED
        }

        internal fun getDirection(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
        ): Direction {
            val speed = velocityX * velocityX + velocityY * velocityY
            return if (speed < SWIPE_THRESH_VER) UNDEFINED else getDirection(e1, e2)
        }

        private fun atan3(y: Float, x: Float): Double {
            val angle = Math.atan2(y.toDouble(), x.toDouble())
            return if (angle < 0) 2 * Math.PI + angle else angle
        }

        internal fun findByIndex(
                i: Int
        ): Direction = values().firstOrNull { it.index == i } ?: UNDEFINED

        internal fun findByAngle(angle: Int): Direction {
            if (angle < 0) {
                return UNDEFINED
            }
            val index = (8 - angle / 45) % 8 // XXX
            return findByIndex(index)
        }
    }
}