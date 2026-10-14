package com.aryan.gallery.data.remote

import com.aryan.gallery.data.remote.models.TransferUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit service interface for file transfer uploads.
 */
interface TransferApi {

    /**
     * Uploads a media file for a staged transfer ID.
     */
    @Multipart
    @POST("/api/transfers/{id}/upload")
    suspend fun uploadTransferFile(
        @Path("id") transferId: String,
        @Part file: MultipartBody.Part
    ): Response<TransferUploadResponse>
}
