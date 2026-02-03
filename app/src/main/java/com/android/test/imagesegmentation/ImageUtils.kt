package com.android.test.imagesegmentation

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Collection of image reading and manipulation utilities in the form of static functions.
 */
abstract class ImageUtils {
  companion object {

    fun scaleBitmapAndKeepRatio(
      targetBmp: Bitmap,
      reqHeightInPixels: Int,
      reqWidthInPixels: Int
    ): Bitmap {
      if (targetBmp.height == reqHeightInPixels && targetBmp.width == reqWidthInPixels) {
        return targetBmp
      }
      val matrix = Matrix()
      matrix.setRectToRect(
        RectF(0f, 0f, targetBmp.width.toFloat(), targetBmp.width.toFloat()),
        RectF(0f, 0f, reqWidthInPixels.toFloat(), reqHeightInPixels.toFloat()),
        Matrix.ScaleToFit.FILL
      )
      return Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.width, targetBmp.width, matrix, true)
    }

    fun bitmapToByteBuffer(
      bitmapIn: Bitmap,
      width: Int,
      height: Int,
      mean: Float = 0.0f,
      std: Float = 255.0f
    ): ByteBuffer {
      val bitmap = scaleBitmapAndKeepRatio(bitmapIn, width, height)
      val inputImage = ByteBuffer.allocateDirect(1 * width * height * 3 * 4)
      inputImage.order(ByteOrder.nativeOrder())
      inputImage.rewind()

      val intValues = IntArray(width * height)
      bitmap.getPixels(intValues, 0, width, 0, 0, width, height)
      var pixel = 0
      for (y in 0 until height) {
        for (x in 0 until width) {
          val value = intValues[pixel++]

          // Normalize channel values to [-1.0, 1.0]. This requirement varies by
          // model. For example, some models might require values to be normalized
          // to the range [0.0, 1.0] instead.
          inputImage.putFloat(((value shr 16 and 0xFF) - mean) / std)
          inputImage.putFloat(((value shr 8 and 0xFF) - mean) / std)
          inputImage.putFloat(((value and 0xFF) - mean) / std)
        }
      }

      inputImage.rewind()
      return inputImage
    }

    fun createEmptyBitmap(imageWidth: Int, imageHeigth: Int, color: Int = 0): Bitmap {
      val ret = Bitmap.createBitmap(imageWidth, imageHeigth, Bitmap.Config.RGB_565)
      if (color != 0) {
        ret.eraseColor(color)
      }
      return ret
    }
  }
}
