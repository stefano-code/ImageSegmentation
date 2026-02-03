package com.android.test.imagesegmentation.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.ColorUtils
import com.android.test.imagesegmentation.ImageUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random

/**
 * Class responsible to run the Image Segmentation model.
 * more information about the DeepLab model being used can
 * be found here:
 * https://ai.googleblog.com/2018/03/semantic-image-segmentation-with.html
 * https://github.com/tensorflow/models/tree/master/research/deeplab
 *
 * Label names: 'background', 'aeroplane', 'bicycle', 'bird', 'boat', 'bottle', 'bus',
 * 'car', 'cat', 'chair', 'cow', 'diningtable', 'dog', 'horse', 'motorbike',
 * 'person', 'pottedplant', 'sheep', 'sofa', 'train', 'tv'
 */
class ImageSegmentationModelExecutor(
  context: Context
) {
  companion object {

    const val TAG = "SegmentationInterpreter"
    private const val imageSegmentationModel = "deeplabv3_257.tflite"
    private const val imageSize = 257
    const val NUM_CLASSES = 21
    private const val IMAGE_MEAN = 127.5f
    private const val IMAGE_STD = 127.5f

    val segmentColors = IntArray(NUM_CLASSES)
    val labelsArrays = arrayOf(
      "background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus",
      "car", "cat", "chair", "cow", "dining table", "dog", "horse", "motorbike",
      "person", "potted plant", "sheep", "sofa", "train", "tv"
    )

    private fun getRandomRGBInt(random: Random) = (255 * random.nextFloat()).toInt()
  }

  private val segmentationMasks: ByteBuffer
  private val interpreter: Interpreter
  private var fullTimeExecutionTime = 0L
  private var preprocessTime = 0L
  private var imageSegmentationTime = 0L
  private var maskFlatteningTime = 0L
  private var numberThreads = 4

  init {
    interpreter = getInterpreter(context, imageSegmentationModel)
    segmentationMasks = ByteBuffer.allocateDirect(1 * imageSize * imageSize * NUM_CLASSES * 4)
    segmentationMasks.order(ByteOrder.nativeOrder())

    val random = Random(System.currentTimeMillis())
    segmentColors[0] = Color.TRANSPARENT
    for (i in 1 until NUM_CLASSES) {
      segmentColors[i] = Color.argb((128), getRandomRGBInt(random), getRandomRGBInt(random), getRandomRGBInt(random))
    }
  }

  fun segmentImage(data: Bitmap): Recognition {
    try {
      fullTimeExecutionTime = SystemClock.uptimeMillis()

      preprocessTime = SystemClock.uptimeMillis()
      val scaledBitmap = Bitmap.createScaledBitmap(data, imageSize, imageSize,false);

      val contentArray = ImageUtils.bitmapToByteBuffer(scaledBitmap, imageSize, imageSize, IMAGE_MEAN, IMAGE_STD)
      preprocessTime = SystemClock.uptimeMillis() - preprocessTime

      imageSegmentationTime = SystemClock.uptimeMillis()
      interpreter.run(contentArray, segmentationMasks)
      imageSegmentationTime = SystemClock.uptimeMillis() - imageSegmentationTime
      Log.d(TAG, "Time to run the model $imageSegmentationTime")

      maskFlatteningTime = SystemClock.uptimeMillis()
      val res = convertBytebufferMaskToBitmap(segmentationMasks, imageSize, imageSize, scaledBitmap, segmentColors, scaledBitmap)

      maskFlatteningTime = SystemClock.uptimeMillis() - maskFlatteningTime
      Log.d(TAG, "Time to flatten the mask result $maskFlatteningTime")

      fullTimeExecutionTime = SystemClock.uptimeMillis() - fullTimeExecutionTime
      Log.d(TAG, "Total time execution $fullTimeExecutionTime")

      return res;
    } catch (e: Exception) {
      val exceptionLog = "something went wrong: ${e.message}"
      Log.d(TAG, exceptionLog)

      val emptyBitmap = ImageUtils.createEmptyBitmap(imageSize, imageSize)
      return Recognition(emptyBitmap, emptyBitmap, emptyBitmap, emptyBitmap, /*exceptionLog,*/ HashMap<String, Int>())
    }
  }

  @Throws(IOException::class)
  private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(modelFile)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    fileDescriptor.close()
    return retFile
  }

  @Throws(IOException::class)
  private fun getInterpreter(
    context: Context,
    modelName: String
  ): Interpreter {
    val tfliteOptions = Interpreter.Options()
    tfliteOptions.setNumThreads(numberThreads)
    return Interpreter(loadModelFile(context, modelName), tfliteOptions)
  }

  fun close() {
    interpreter.close()
  }


  private fun convertBytebufferMaskToBitmap(
    inputBuffer: ByteBuffer,
    imageWidth: Int,
    imageHeight: Int,
    backgroundImage: Bitmap,
    colors: IntArray,
    scaledBitmap:Bitmap
  ): Recognition {
    val conf = Bitmap.Config.ARGB_8888
    val maskBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf)
    val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf)
    val transparentBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf)
    val scaledBackgroundImage =
      ImageUtils.scaleBitmapAndKeepRatio(
        backgroundImage,
        imageWidth,
        imageHeight
      )
    val mSegmentBits = Array(imageWidth) { IntArray(imageHeight) }
    val itemsFound = HashMap<String, Int>()
    inputBuffer.rewind()

    for (y in 0 until imageHeight) {
      for (x in 0 until imageWidth) {
        var maxVal = 0f
        mSegmentBits[x][y] = 0

        for (c in 0 until NUM_CLASSES) {
          val value = inputBuffer
            .getFloat((y * imageWidth * NUM_CLASSES + x * NUM_CLASSES + c) * 4)
          if (c == 0 || value > maxVal) {
            maxVal = value
            mSegmentBits[x][y] = c
          }
        }
        val label = labelsArrays[mSegmentBits[x][y]]
        val color = colors[mSegmentBits[x][y]]
        itemsFound.put(label, color)
        val newPixelColor = ColorUtils.compositeColors(
          colors[mSegmentBits[x][y]],
          scaledBackgroundImage.getPixel(x, y)
        )
        resultBitmap.setPixel(x, y, newPixelColor)
        maskBitmap.setPixel(x, y, colors[mSegmentBits[x][y]])
        if (mSegmentBits[x][y] != 0) {
          transparentBitmap.setPixel(x, y, scaledBitmap.getPixel(x, y))
        }
      }
    }

    Log.e(TAG,"${executionLog()}")
    return Recognition(resultBitmap,scaledBitmap, maskBitmap,transparentBitmap/*,formatExecutionLog()*/, itemsFound)
  }

  private fun executionLog(): String {
    val sb = StringBuilder()
    sb.append("Input Image Size: $imageSize x $imageSize \n")
    sb.append("Number of threads: $numberThreads\n")
    sb.append("Pre-process execution time: $preprocessTime ms\n")
    sb.append("Model execution time: $imageSegmentationTime ms\n")
    sb.append("Mask flatten time: $maskFlatteningTime ms\n")
    sb.append("Full execution time: $fullTimeExecutionTime ms\n")
    return sb.toString()
  }
}
