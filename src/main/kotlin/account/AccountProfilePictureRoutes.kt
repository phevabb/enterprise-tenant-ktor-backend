package com.example.account



import com.cloudinary.utils.ObjectUtils

import com.example.cloudinary.CloudinaryClient
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart

import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post

import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray


fun Route.accountProfilePictureRoutes() {

    post("{id}") {
        println("✅ [profile-picture] Route hit")

        val id = call.parameters["id"]?.toIntOrNull()
        println("✅ [profile-picture] Raw id param = ${call.parameters["id"]}")
        println("✅ [profile-picture] Parsed id = $id")

        if (id == null) {
            println("❌ [profile-picture] Invalid account id")
            return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid account id")
            )
        }

        val contentTypeHeader = call.request.headers["Content-Type"]
        println("✅ [profile-picture] Request Content-Type = $contentTypeHeader")

        val multipart = try {
            call.receiveMultipart()
        } catch (e: Exception) {
            println("❌ [profile-picture] Failed to receive multipart: ${e.message}")
            e.printStackTrace()
            return@post call.respond(
                HttpStatusCode.UnsupportedMediaType,
                mapOf("error" to "Content-Type header is required or multipart body is invalid")
            )
        }

        var imageBytes: ByteArray? = null
        var contentType: String? = null
        var originalFileName: String? = null

        try {
            multipart.forEachPart { part ->
                println("➡️ [profile-picture] Part received: ${part::class.simpleName}")

                when (part) {
                    is PartData.FileItem -> {
                        originalFileName = part.originalFileName
                        contentType = part.contentType?.toString()

                        println("✅ [profile-picture] FileItem found")
                        println("   - originalFileName = $originalFileName")
                        println("   - contentType      = $contentType")

                        try {
                            imageBytes = part.provider().readRemaining().readByteArray()

                        } catch (e: Exception) {
                            println("❌ [profile-picture] Failed reading file bytes: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    is PartData.FormItem -> {
                        println("ℹ️ [profile-picture] Form field: ${part.name} = ${part.value}")
                    }

                    else -> {
                        println("ℹ️ [profile-picture] Ignored part type: ${part::class.simpleName}")
                    }
                }

                part.dispose()
            }
        } catch (e: Exception) {
            println("❌ [profile-picture] Error while iterating multipart parts: ${e.message}")
            e.printStackTrace()
            return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Failed to parse multipart body")
            )
        }

        if (imageBytes == null || imageBytes!!.isEmpty()) {
            println("❌ [profile-picture] No image uploaded or image is empty")
            return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "No image uploaded")
            )
        }

        val allowed = setOf("image/jpeg", "image/png", "image/webp")
        println("✅ [profile-picture] Allowed content types = $allowed")
        println("✅ [profile-picture] Incoming content type = $contentType")

        if (contentType !in allowed) {
            println("❌ [profile-picture] Unsupported content type: $contentType")
            return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Only JPG, PNG and WEBP are allowed")
            )
        }

        // ---------------------------------------------------------
        // Optional: delete old image first if it exists
        // ---------------------------------------------------------
        val oldPublicId = try {
            AccountRepository.getProfilePicturePublicId(id)
        } catch (e: Exception) {
            println("❌ [profile-picture] Failed to get old public id from DB: ${e.message}")
            e.printStackTrace()
            null
        }

        println("✅ [profile-picture] oldPublicId = $oldPublicId")

        if (!oldPublicId.isNullOrBlank()) {
            try {
                println("➡️ [profile-picture] Deleting old Cloudinary image: $oldPublicId")
                val destroyResult = CloudinaryClient.instance.uploader().destroy(
                    oldPublicId,
                    ObjectUtils.emptyMap()
                )
                println("✅ [profile-picture] Cloudinary destroy result = $destroyResult")
            } catch (e: Exception) {
                println("⚠️ [profile-picture] Failed to delete old Cloudinary image: ${e.message}")
                e.printStackTrace()
            }
        }

        // ---------------------------------------------------------
        // Upload new image to Cloudinary
        // ---------------------------------------------------------
        val uploadResult = try {
            println("➡️ [profile-picture] Uploading to Cloudinary...")
            CloudinaryClient.instance.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                    "folder", "profile_pics_kog",
                    "resource_type", "image",
                    "public_id", "account_${id}_${System.currentTimeMillis()}",
                    "overwrite", true
                )
            )
        } catch (e: Exception) {
            println("❌ [profile-picture] Cloudinary upload failed: ${e.message}")
            e.printStackTrace()
            return@post call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Cloudinary upload failed", "details" to (e.message ?: "Unknown error"))
            )
        }

        println("✅ [profile-picture] Cloudinary uploadResult = $uploadResult")

        val secureUrl = uploadResult["secure_url"] as? String
        val publicId = uploadResult["public_id"] as? String

        println("✅ [profile-picture] secureUrl = $secureUrl")
        println("✅ [profile-picture] publicId = $publicId")

        if (secureUrl.isNullOrBlank() || publicId.isNullOrBlank()) {
            println("❌ [profile-picture] Cloudinary returned null/blank secureUrl or publicId")
            return@post call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Cloudinary upload failed")
            )
        }

        // ---------------------------------------------------------
        // Save to DB
        // ---------------------------------------------------------
        val ok = try {
            println("➡️ [profile-picture] Updating DB for accountId=$id")
            AccountRepository.updateProfilePicture(
                accountId = id,
                profilePictureUrl = secureUrl,
                profilePicturePublicId = publicId
            )
        } catch (e: Exception) {
            println("❌ [profile-picture] DB update failed: ${e.message}")
            e.printStackTrace()
            return@post call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to save profile picture in database")
            )
        }

        println("✅ [profile-picture] DB update result = $ok")

        if (!ok) {
            println("❌ [profile-picture] Account not found for id=$id")
            return@post call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Account not found")
            )
        }

        println("✅ [profile-picture] Upload successful for accountId=$id")



        call.respond(
            HttpStatusCode.OK,
            com.example.account.dto.ProfilePictureUploadResponse(
                id = id,
                profilePictureUrl = secureUrl,
                profilePicturePublicId = publicId,
                originalFileName = originalFileName,
                contentType = contentType,
                sizeBytes = imageBytes!!.size
            )
        )


    }


    delete("{id}") {
        println("✅ [profile-picture DELETE] Route hit")

        val id = call.parameters["id"]?.toIntOrNull()
        println("✅ [profile-picture DELETE] Raw id param = ${call.parameters["id"]}")
        println("✅ [profile-picture DELETE] Parsed id = $id")

        if (id == null) {
            println("❌ [profile-picture DELETE] Invalid account id")
            return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid account id")
            )
        }

        // 1) get old public id
        val oldPublicId = try {
            AccountRepository.getProfilePicturePublicId(id)
        } catch (e: Exception) {
            println("❌ [profile-picture DELETE] Failed fetching old public id: ${e.message}")
            e.printStackTrace()
            return@delete call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to fetch existing profile picture")
            )
        }

        println("✅ [profile-picture DELETE] oldPublicId = $oldPublicId")

        // 2) delete from Cloudinary if exists
        var deletedFromCloudinary = false

        if (!oldPublicId.isNullOrBlank()) {
            try {
                println("➡️ [profile-picture DELETE] Deleting from Cloudinary: $oldPublicId")

                val destroyResult = CloudinaryClient.instance.uploader().destroy(
                    oldPublicId,
                    ObjectUtils.emptyMap()
                )

                println("✅ [profile-picture DELETE] Cloudinary destroy result = $destroyResult")

                val result = destroyResult["result"]?.toString()
                deletedFromCloudinary = result == "ok" || result == "not found"
            } catch (e: Exception) {
                println("⚠️ [profile-picture DELETE] Cloudinary delete failed: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("ℹ️ [profile-picture DELETE] No Cloudinary public ID found; skipping Cloudinary delete")
        }

        // 3) clear DB fields
        val dbCleared = try {
            AccountRepository.clearProfilePicture(id)
        } catch (e: Exception) {
            println("❌ [profile-picture DELETE] Failed clearing DB fields: ${e.message}")
            e.printStackTrace()
            return@delete call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to clear profile picture from database")
            )
        }

        println("✅ [profile-picture DELETE] DB clear result = $dbCleared")

        if (!dbCleared) {
            println("❌ [profile-picture DELETE] Account not found for id=$id")
            return@delete call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Account not found")
            )
        }

        call.respond(
            HttpStatusCode.OK,
            com.example.account.dto.ProfilePictureDeleteResponse(
                id = id,
                deleted = true,
                deletedFromCloudinary = deletedFromCloudinary,
                oldPublicId = oldPublicId,
                message = "Profile picture removed successfully"
            )
        )
    }




}