package com.android.test.imagesegmentation.ml

import android.graphics.Bitmap


data class Recognition(
    val bitmapResult: Bitmap,
    val bitmapOriginal: Bitmap,
    val bitmapMaskOnly: Bitmap,
    val transparentBitmap: Bitmap,
    // A map between labels and colors of the items found.
    val itemsFound: Map<String, Int>
)
