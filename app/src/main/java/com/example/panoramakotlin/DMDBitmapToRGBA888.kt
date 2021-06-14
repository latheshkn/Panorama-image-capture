package com.example.panoramakotlin

import android.graphics.Bitmap
import android.graphics.Color
import com.dermandar.dmd_lib.DMD_Capture
import java.nio.ByteBuffer

class DMDBitmapToRGBA888 {
    private val mDMDCapture: DMD_Capture? = null


companion object{
    fun GetPixels(bmp: Bitmap): IntArray? {
        val height = bmp.height
        val width = bmp.width
        val length = width * height
        val pixels = IntArray(length)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) pixels[i] = Color.red(pixels[i]) shl 24 or (Color.green(
            pixels[i]
        ) shl 16) or (Color.blue(pixels[i]) shl 8) or Color.alpha(
            pixels[i]
        )
        return pixels
    }

    public fun ImageToRGBA8888(bmp: Bitmap?): ByteArray? {
        if (bmp == null) return null
        val bytes = ByteArray(bmp.allocationByteCount)
        val bb = ByteBuffer.wrap(bytes)
        bb.asIntBuffer().put(GetPixels(bmp))
        return bytes
    }
}



}