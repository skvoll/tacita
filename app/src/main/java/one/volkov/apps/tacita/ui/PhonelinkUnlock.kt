/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.volkov.apps.tacita.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Material's PhonelinkLock silhouette with an open shackle for the screen-on check.
internal val PhonelinkUnlock: ImageVector by lazy {
    ImageVector.Builder("PhonelinkUnlock", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 1f)
            lineTo(9f, 1f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(2f)
            lineTo(9f, 4f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(16f)
            lineTo(9f, 20f)
            verticalLineToRelative(-2f)
            lineTo(7f, 18f)
            verticalLineToRelative(3f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(10f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            lineTo(21f, 3f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()

            moveTo(10.8f, 11f)
            verticalLineTo(9.5f)
            curveTo(10.8f, 8.1f, 9.4f, 7f, 8f, 7f)
            curveTo(6.6f, 7f, 5.2f, 8.1f, 5.2f, 9.5f)
            horizontalLineTo(6.5f)
            curveTo(6.5f, 8.7f, 7.2f, 8.2f, 8f, 8.2f)
            curveTo(8.8f, 8.2f, 9.5f, 8.7f, 9.5f, 9.5f)
            verticalLineTo(11f)
            horizontalLineTo(5.2f)
            curveTo(4.6f, 11f, 4f, 11.6f, 4f, 12.2f)
            verticalLineToRelative(3.5f)
            curveToRelative(0f, 0.7f, 0.6f, 1.3f, 1.2f, 1.3f)
            horizontalLineToRelative(5.5f)
            curveToRelative(0.7f, 0f, 1.3f, -0.6f, 1.3f, -1.2f)
            verticalLineToRelative(-3.5f)
            curveToRelative(0f, -0.7f, -0.6f, -1.3f, -1.2f, -1.3f)
            close()
        }
    }.build()
}
