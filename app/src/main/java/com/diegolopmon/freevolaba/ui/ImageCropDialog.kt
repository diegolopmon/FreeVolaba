package com.diegolopmon.freevolaba.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.diegolopmon.freevolaba.R

@Composable
fun ImageCropDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropSuccess: (croppedBitmap: Bitmap) -> Unit
) {
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val currentBitmap = remember(sourceBitmap, rotationDegrees) {
        if (rotationDegrees == 0) {
            sourceBitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.title_crop_image))
                Row {
                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees - 90 + 360) % 360
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = stringResource(R.string.action_rotate)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clipToBounds()
                        .background(Color.Black)
                        .onGloballyPositioned { viewSize = it.size }
                        .pointerInput(currentBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val minDim = minOf(canvasWidth, canvasHeight)
                        val circleRadius = minDim * 0.42f
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                        val imageBitmap = currentBitmap.asImageBitmap()

                        val imgWidth = imageBitmap.width.toFloat()
                        val imgHeight = imageBitmap.height.toFloat()

                        val circleDiameter = circleRadius * 2f
                        val baseScale = maxOf(circleDiameter / imgWidth, circleDiameter / imgHeight)
                        val totalScale = baseScale * scale

                        val drawWidth = imgWidth * totalScale
                        val drawHeight = imgHeight * totalScale

                        val drawLeft = center.x + offset.x - (drawWidth / 2f)
                        val drawTop = center.y + offset.y - (drawHeight / 2f)

                        drawImage(
                            image = imageBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(drawLeft.toInt(), drawTop.toInt()),
                            dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                        )

                        val circlePath = Path().apply {
                            addOval(Rect(center = center, radius = circleRadius))
                        }

                        clipPath(circlePath, clipOp = ClipOp.Difference) {
                            drawRect(color = Color.Black.copy(alpha = 0.65f))
                        }

                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = circleRadius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cropped = createCroppedBitmap(
                        source = currentBitmap,
                        scale = scale,
                        offset = offset,
                        viewWidth = if (viewSize.width > 0) viewSize.width.toFloat() else 300f,
                        viewHeight = if (viewSize.height > 0) viewSize.height.toFloat() else 300f
                    )
                    onCropSuccess(cropped)
                }
            ) {
                Text(stringResource(R.string.action_crop))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun createCroppedBitmap(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    viewWidth: Float,
    viewHeight: Float
): Bitmap {
    val targetSize = 512
    val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)

    val minDim = minOf(viewWidth, viewHeight)
    val circleRadius = minDim * 0.42f
    val circleDiameter = circleRadius * 2f

    val center = Offset(viewWidth / 2f, viewHeight / 2f)
    val circleLeft = center.x - circleRadius
    val circleTop = center.y - circleRadius

    val srcWidth = source.width.toFloat()
    val srcHeight = source.height.toFloat()

    val baseScale = maxOf(circleDiameter / srcWidth, circleDiameter / srcHeight)
    val totalScale = baseScale * scale

    val drawWidth = srcWidth * totalScale
    val drawHeight = srcHeight * totalScale

    val drawLeftOnScreen = center.x + offset.x - (drawWidth / 2f)
    val drawTopOnScreen = center.y + offset.y - (drawHeight / 2f)

    val relX = drawLeftOnScreen - circleLeft
    val relY = drawTopOnScreen - circleTop

    val factor = targetSize.toFloat() / circleDiameter

    val finalScale = totalScale * factor
    val finalLeft = relX * factor
    val finalTop = relY * factor

    val matrix = Matrix().apply {
        postScale(finalScale, finalScale)
        postTranslate(finalLeft, finalTop)
    }

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)

    return result
}
