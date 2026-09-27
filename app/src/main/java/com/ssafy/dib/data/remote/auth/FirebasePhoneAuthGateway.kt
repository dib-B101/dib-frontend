package com.ssafy.dib.data.remote.auth

import android.app.Activity
import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.ssafy.dib.BuildConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Firebase SDK 콜백을 기존 인증 화면의 요청/확인 단계에 연결한다. IO 스레드에서 호출한다. */
class FirebasePhoneAuthGateway(private val activity: Activity) {
    private val auth = FirebaseAuth.getInstance()

    init {
        // 로컬 에뮬레이터에서는 Play Integrity 앱 확인이 거부될 수 있다.
        // reCAPTCHA도 Firebase의 앱 확인 절차이며 릴리스 빌드에는 적용하지 않는다.
        if (BuildConfig.DEBUG && Build.PRODUCT.startsWith("sdk_")) {
            auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
        }
    }

    data class Challenge(val verificationId: String?, val automaticCredential: PhoneAuthCredential?)

    fun request(phoneNumber: String): Challenge {
        require(phoneNumber.matches(Regex("010\\d{8}")))
        auth.setLanguageCode("ko")
        val completed = CountDownLatch(1)
        val finished = AtomicBoolean(false)
        var outcome: Result<Challenge>? = null
        fun finish(result: Result<Challenge>) {
            if (finished.compareAndSet(false, true)) {
                outcome = result
                completed.countDown()
            }
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                finish(Result.success(Challenge(null, credential)))
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                finish(Result.failure(exception))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                finish(Result.success(Challenge(verificationId, null)))
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+82${phoneNumber.drop(1)}")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        activity.runOnUiThread { PhoneAuthProvider.verifyPhoneNumber(options) }
        if (!completed.await(70L, TimeUnit.SECONDS)) error("휴대전화 인증 요청 시간이 초과됐어요.")
        return outcome!!.getOrThrow()
    }

    fun signInWithCode(verificationId: String, code: String): String =
        signIn(PhoneAuthProvider.getCredential(verificationId, code))

    fun signIn(credential: PhoneAuthCredential): String {
        try {
            val result = Tasks.await(auth.signInWithCredential(credential))
            return Tasks.await(result.user!!.getIdToken(true)).token
                ?: error("Firebase 인증 토큰을 받지 못했어요.")
        } finally {
            auth.signOut()
        }
    }
}
