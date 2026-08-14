//package account
//
//import tenant.dto.response.SchoolLogoUploadResponse
//import com.cloudinary.utils.ObjectUtils
//import com.example.tenant.tables.TenantsTable
//import org.jetbrains.exposed.sql.selectAll
//import org.jetbrains.exposed.sql.transactions.transaction
//
//import com.example.cloudinary.CloudinaryClient
//import com.example.tenant.currentTenant
//import io.ktor.http.HttpStatusCode
//import io.ktor.http.content.PartData
//import io.ktor.http.content.forEachPart
//import io.ktor.server.request.receiveMultipart
//import io.ktor.server.response.respond
//import io.ktor.server.routing.Route
//import io.ktor.server.routing.delete
//
//import io.ktor.server.routing.post
//import io.ktor.utils.io.readRemaining
//import kotlinx.io.readByteArray
//import org.jetbrains.exposed.sql.update
//import tenant.dto.response.SchoolLogoDeleteResponse
//
//
//
//fun Route.schoolLogoRoutes() {
//
//    post {
//
//        val tenant = call.currentTenant()
//
//        val tenantSchema = tenant.tenantSchema
//
//        val multipart =
//            call.receiveMultipart()
//
//        var imageBytes: ByteArray? = null
//
//        var contentType: String? = null
//
//        multipart.forEachPart { part ->
//
//            if (part is PartData.FileItem) {
//
//                contentType =
//                    part.contentType?.toString()
//
//                imageBytes =
//                    part.provider()
//                        .readRemaining()
//                        .readByteArray()
//            }
//
//            part.dispose()
//        }
//
//        if (imageBytes == null) {
//
//            return@post call.respond(
//                HttpStatusCode.BadRequest,
//                mapOf(
//                    "error" to "No image uploaded"
//                )
//            )
//        }
//
//        val oldPublicId =
//            getSchoolLogoPublicId(
//                tenantSchema
//            )
//
//        if (!oldPublicId.isNullOrBlank()) {
//
//            CloudinaryClient
//                .instance
//                .uploader()
//                .destroy(
//                    oldPublicId,
//                    ObjectUtils.emptyMap()
//                )
//        }
//
//        val uploadResult =
//            CloudinaryClient
//                .instance
//                .uploader()
//                .upload(
//                    imageBytes,
//                    ObjectUtils.asMap(
//                        "folder",
//                        "school_logos",
//                        "resource_type",
//                        "image",
//                        "public_id",
//                        "school_logo_${tenant.tenantCode}_${System.currentTimeMillis()}",
//                        "overwrite",
//                        true
//                    )
//                )
//
//        val secureUrl =
//            uploadResult["secure_url"] as String
//
//        val publicId =
//            uploadResult["public_id"] as String
//
//        updateSchoolLogo(
//            tenantSchema = tenantSchema,
//            schoolLogoUrl = secureUrl,
//            schoolLogoPublicId = publicId
//        )
//
//        call.respond(
//            SchoolLogoUploadResponse(
//                schoolLogoUrl = secureUrl,
//                schoolLogoPublicId = publicId
//            )
//        )
//    }
//
//    delete {
//
//        val tenant =
//            call.currentTenant()
//
//        val tenantSchema =
//            tenant.tenantSchema
//
//        val oldPublicId =
//            getSchoolLogoPublicId(
//                tenantSchema
//            )
//
//        var deletedFromCloudinary = false
//
//        if (!oldPublicId.isNullOrBlank()) {
//
//            val result =
//                CloudinaryClient
//                    .instance
//                    .uploader()
//                    .destroy(
//                        oldPublicId,
//                        ObjectUtils.emptyMap()
//                    )
//
//            deletedFromCloudinary =
//                result["result"]?.toString() ==
//                        "ok"
//        }
//
//        clearSchoolLogo(
//            tenantSchema
//        )
//
//        call.respond(
//
//            SchoolLogoDeleteResponse(
//                deleted = true,
//                deletedFromCloudinary = deletedFromCloudinary,
//                oldPublicId = oldPublicId,
//                message = "School logo deleted"
//            )
//        )
//    }
//}
//
//
//fun getSchoolLogoPublicId(
//    tenantSchema: String
//): String? {
//
//    return transaction {
//
//        TenantsTable
//            .selectAll()
//            .where {
//                TenantsTable.tenantSchema eq tenantSchema
//            }
//            .singleOrNull()
//            ?.get(TenantsTable.schoolLogoPublicId)
//    }
//}
//
//fun updateSchoolLogo(
//    tenantSchema: String,
//    schoolLogoUrl: String,
//    schoolLogoPublicId: String
//): Boolean {
//
//    return transaction {
//
//        TenantsTable.update(
//            { TenantsTable.tenantSchema eq tenantSchema }
//        ) {
//
//            it[TenantsTable.schoolLogoUrl] =
//                schoolLogoUrl
//
//            it[TenantsTable.schoolLogoPublicId] =
//                schoolLogoPublicId
//        } > 0
//    }
//}
//
//fun clearSchoolLogo(
//    tenantSchema: String
//): Boolean {
//
//    return transaction {
//
//        TenantsTable.update(
//            { TenantsTable.tenantSchema eq tenantSchema }
//        ) {
//
//            it[schoolLogoUrl] = null
//            it[schoolLogoPublicId] = null
//        } > 0
//    }
//}
