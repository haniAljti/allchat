package com.hanialjti.allchat.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException


fun Bitmap.scaleCenterCrop(newHeight: Int, newWidth: Int): ByteArray? {

    val xScale = newWidth.toFloat() / width
    val yScale = newHeight.toFloat() / height
    val scale = xScale.coerceAtLeast(yScale)

    val scaledWidth = scale * width
    val scaledHeight = scale * height

    val left = (newWidth - scaledWidth) / 2
    val top = (newHeight - scaledHeight) / 2

    val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

    val dest = Bitmap.createBitmap(newWidth, newHeight, config)
    val canvas = Canvas(dest)
    canvas.drawBitmap(this, null, targetRect, null)

    return try {
        val outputStream = ByteArrayOutputStream()
        val scaledBitmap = Bitmap.createScaledBitmap(dest, newHeight, newWidth, true)
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        Logger.e { "Failed to create file" }
        null
    } finally {
        dest.recycle()
    }
}

fun Uri.scaleCenterCrop(context: Context, newHeight: Int, newWidth: Int): ByteArray? {

    val bmp = toBitmap(context) ?: return null
    val xScale = newWidth.toFloat() / bmp.width
    val yScale = newHeight.toFloat() / bmp.height
    val scale = xScale.coerceAtLeast(yScale)

    val scaledWidth = scale * bmp.width
    val scaledHeight = scale * bmp.height

    val left = (newWidth - scaledWidth) / 2
    val top = (newHeight - scaledHeight) / 2

    val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

    val dest = Bitmap.createBitmap(newWidth, newHeight, bmp.config)
    val canvas = Canvas(dest)
    canvas.drawBitmap(bmp, null, targetRect, null)

    return try {
        val outputStream = ByteArrayOutputStream()
        val scaledBitmap = Bitmap.createScaledBitmap(dest, 32, 32, true)
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        Logger.e { "Failed to create file" }
        null
    } finally {
        dest.recycle()
    }
}

private fun Uri.toBitmap(context: Context): Bitmap? {
    val parcelFileDescriptor =
        context.contentResolver.openFileDescriptor(this, "r")
    return try {
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        BitmapFactory.decodeFileDescriptor(fileDescriptor)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    } finally {
        parcelFileDescriptor?.close()
    }
}