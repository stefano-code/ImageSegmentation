# 📱 Image Segmentation on Android with DeepLabV3 (TensorFlow Lite)

This project shows how to build an Android application that performs **image segmentation directly on a smartphone**, using a **DeepLabV3 model** in TensorFlow Lite format.

The app uses the phone camera or gallery images and labels **every pixel** in the image, creating smooth and accurate object masks — all **on-device**, without cloud processing.

---

## ✨ What this project does

- Uses **DeepLabV3** (`deeplabv3_257.tflite`) for semantic image segmentation
- Runs entirely **on-device** with **TensorFlow Lite**
- Supports images from:
  - 📸 Camera
  - 🖼️ Gallery
- Shows:
  - Original image
  - Segmentation mask
  - Blended result
  - Transparent foreground objects
- Designed to be **simple, readable, and easy to modify**

---

## 🧠 What is Image Segmentation?

Image segmentation assigns a label to **every pixel** in an image.

Unlike object detection (which draws bounding boxes), segmentation produces **pixel-accurate silhouettes**. This technique is used in:

- Background removal
- Portrait mode
- Augmented Reality (AR)
- Medical imaging
- Smart photo editing

---

## 🏗️ Project structure

The project is organized into a few key components:

- **MainActivity**
  - Handles permissions, UI, camera/gallery input
  - Connects the image source to the segmentation model

- **ImageSegmentationModelExecutor**
  - Loads and runs the DeepLabV3 TensorFlow Lite model
  - Converts model output into segmentation masks and images

- **Recognition**
  - data class for output DeepLabV3 model inference

- **ImageUtils**
  - Image preprocessing utilities
  - Bitmap scaling, EXIF orientation handling, and buffer conversion

- **Assets**
  - Contains the segmentation model:
    - `deeplabv3_257.tflite`
---

## 📷 Screenshot
<img width="360" height="780" alt="Screenshot_20260126_233506" src="https://github.com/user-attachments/assets/89850385-71f9-44eb-9698-a0cf6e71880a" />

---
## 🚀 Getting started

### 1. Clone the repository

```bash
git clone https://github.com/stefano-code/ImageSegmentation.git



