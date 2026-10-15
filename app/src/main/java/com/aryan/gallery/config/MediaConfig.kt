package com.aryan.gallery.config

/**
 * Centralized configuration parameters for image/video thumbnail sizes, paging limits, and compression.
 */
data class MediaConfig(
    val defaultThumbnailWidth: Int = 300,
    val defaultThumbnailHeight: Int = 300,
    val highResThumbnailWidth: Int = 400,
    val highResThumbnailHeight: Int = 400,
    val listPageSize: Int = 50,
    val pagingPageSize: Int = 60,
    val pagingPrefetchDistance: Int = 20,
    val thumbnailJpegQuality: Int = 80
)
