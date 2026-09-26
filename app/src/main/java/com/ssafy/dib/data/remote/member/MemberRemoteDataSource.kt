package com.ssafy.dib.data.remote.member

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import com.ssafy.dib.domain.member.MemberImageUpload
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class MemberRemoteDataSource(private val client: DibHttpClient) {
    fun getMe(): ApiResult<MemberProfileResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.MEMBERS_ME).get().build(),
            MemberProfileResponse.serializer()
        )
    }

    fun updateNickname(nickname: String): ApiResult<MemberProfileUpdateResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/profile"
        val request = MemberProfileUpdateRequest(nickname)
        client.execute(
            client.requestBuilder(path)
                .patch(client.jsonBody(request, MemberProfileUpdateRequest.serializer()))
                .build(),
            MemberProfileUpdateResponse.serializer()
        )
    }

    fun updateProfileImage(nickname: String?, image: MemberImageUpload): ApiResult<MemberProfileUpdateResponse> = configured {
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
        if (nickname != null) multipart.addFormDataPart("nickname", nickname)
        multipart.addFormDataPart("image", image.fileName,
            image.bytes.toRequestBody(image.mediaType.toMediaTypeOrNull()))
        client.executeUpload(
            client.requestBuilder("${ApiRoutes.MEMBERS_ME}/profile/image")
                .patch(multipart.build()).build(),
            MemberProfileUpdateResponse.serializer()
        )
    }

    fun requestWithdrawal(reason: String?): ApiResult<MemberWithdrawalResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/withdrawal"
        val request = MemberWithdrawalRequest(reason?.takeIf(String::isNotBlank))
        client.execute(
            client.requestBuilder(path)
                .post(client.jsonBody(request, MemberWithdrawalRequest.serializer()))
                .build(),
            MemberWithdrawalResponse.serializer()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
        }
}
